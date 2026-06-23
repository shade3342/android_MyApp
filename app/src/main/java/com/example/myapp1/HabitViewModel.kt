package com.example.myapp1

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapp1.data.AppDatabase
import com.example.myapp1.data.HabitEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ViewModel = 화면이 쓸 '상태'를 들고 있고, DB 와 화면 사이를 잇는 계층.
//   백엔드로 치면 Service 계층. 화면(Controller/View)은 여기에 일을 시키고 결과(상태)만 구독한다.
//   AndroidViewModel = 일반 ViewModel + Application(앱 전역 Context) 을 받는 버전.
//     DB 싱글톤(AppDatabase.getInstance)을 열려면 Context 가 필요해서 이걸 씀.
//   핵심 성질: 화면 회전·프로세스 재구성에도 ViewModel 은 살아남는다 → 상태 유실 방지.
class HabitViewModel(app: Application) : AndroidViewModel(app) {

    // DAO 한 개를 들고 시작. MyBatis 에서 Service 가 Mapper 를 주입받아 쓰던 것과 같은 자리.
    private val dao = AppDatabase.getInstance(app).habitDao()

    // 화면이 구독할 습관 목록.
    //   dao.observeAll() 은 'habits 테이블이 바뀔 때마다 새 리스트를 흘려보내는' Flow.
    //   그걸 화면에서 바로 쓰기 좋은 StateFlow(항상 '현재값'을 들고 있는 Flow)로 변환한다.
    //     - scope: viewModelScope = 이 ViewModel 이 살아있는 동안만 구독 유지(없어지면 자동 정리).
    //     - started: 화면이 구독을 끊어도 5초는 유지(회전 등 짧은 단절에 DB 재조회 안 하도록).
    //     - initialValue: 첫 조회 전 잠깐 보여줄 빈 목록.
    val habits: StateFlow<List<HabitEntity>> = dao.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // 앱 첫 실행 등 테이블이 비어 있을 때 넣어줄 기본 습관(시드 데이터).
    private val seed = listOf(
        "🚶" to "30분 걷기",
        "📖" to "책 10쪽 읽기",
        "💧" to "물 8잔 마시기",
        "🧘" to "5분 명상하기",
        "😴" to "밤 11시 전 자기",
    )

    // init = 생성자 직후 1회 실행되는 블록.
    init {
        // DB 접근은 suspend 함수라 코루틴 안에서만 호출 가능 → viewModelScope.launch 로 감싼다.
        //   launch = 백그라운드에서 비동기 실행(메인 스레드 안 막음). 결과를 기다리지 않고 흘려보냄.
        viewModelScope.launch {
            // 이미 데이터가 있으면(재실행) 시드를 또 넣으면 안 됨 → count 로 '비었는지'만 확인.
            if (dao.count() == 0) {
                // DAO 의 insert 는 한 건짜리 → 시드 개수만큼 루프로 호출(쿼리가 그대로 보이는 방식).
                for ((emoji, name) in seed) {
                    dao.insert(emoji = emoji, name = name, done = false)
                }
            }
        }
    }

    // 습관 체크 토글. 화면에서 항목을 탭하면 호출됨.
    //   done 값만 뒤집어 updateDone 으로 UPDATE → habits 테이블이 바뀌면 위 Flow 가
    //   새 목록을 자동으로 흘려보내 화면이 알아서 갱신된다(우리가 setState 안 해도 됨).
    fun toggle(habit: HabitEntity) {
        viewModelScope.launch {
            dao.updateDone(id = habit.id, done = !habit.done)
        }
    }
}
