package com.example.myapp1.data

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// @Dao = MyBatis 의 Mapper 인터페이스에 해당. 메서드마다 SQL 을 붙인다.
//   실제 구현 클래스는 Room(KSP)이 빌드 시 자동 생성 → 우리는 인터페이스만 선언.
//   (MyBatis 가 Mapper XML/어노테이션 보고 구현체를 만들어주던 것과 같은 원리)
@Dao
interface HabitDao {

    // 전체 조회. 반환 타입이 Flow<List<...>> 인 게 핵심.
    //   MyBatis: List<Habit> 를 '한 번' 받아오고 끝.
    //   Room  : Flow 로 받으면 '구독(subscribe)' 이 된다. habits 테이블이 바뀔 때마다
    //           Room 이 쿼리를 다시 돌려 최신 목록을 자동으로 흘려보냄.
    //           → 화면은 데이터를 '당겨오는' 게 아니라 '구독'만 하면 알아서 갱신됨.
    @Query("SELECT * FROM habits ORDER BY id")
    fun observeAll(): Flow<List<HabitEntity>>

    // 현재 행 개수. 앱 첫 실행 때 '시드(기본 습관)를 넣어야 하나' 판단용.
    //   suspend = 코루틴 안에서만 호출 가능한 '일시 중단 함수'. DB 접근은 시간이 걸리는
    //   작업이라 메인(UI) 스레드를 막으면 안 됨 → Room 이 백그라운드 스레드에서 돌리도록 강제.
    @Query("SELECT COUNT(*) FROM habits")
    suspend fun count(): Int

    // 한 건 삽입(시드용). SQL 을 직접 적는다 → 무슨 쿼리가 나가는지 눈에 보임.
    //   id 는 컬럼 목록에서 뺐다 → autoGenerate PK 라 SQLite 가 알아서 채움(직접 NULL 넣을 필요 없음).
    //   :emoji 같은 표기 = MyBatis 의 #{emoji} 와 동일한 파라미터 바인딩.
    //   여러 건이 필요하면 ViewModel 에서 이 메서드를 그만큼 호출(루프)한다.
    @Query("INSERT INTO habits (emoji, name, done) VALUES (:emoji, :name, :done)")
    suspend fun insert(emoji: String, name: String, done: Boolean)

    // 체크 토글. 바뀌는 done 컬럼만 UPDATE → 불필요하게 emoji/name 까지 다시 쓰지 않음.
    //   (@Update 자동생성은 PK 기준 '전 컬럼 통째 교체'였음 — 그래서 명시 SQL 로 대체)
    @Query("UPDATE habits SET done = :done WHERE id = :id")
    suspend fun updateDone(id: Int, done: Boolean)
}
