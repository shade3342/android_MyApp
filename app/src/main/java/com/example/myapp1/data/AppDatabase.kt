package com.example.myapp1.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// @Database = DB 본체 정의.
//   entities = 이 DB 가 관리하는 테이블 목록(여기선 habits 하나).
//   version  = 스키마 버전. 나중에 컬럼을 바꾸면 이 숫자를 올리고 '마이그레이션'을 적어야 함.
//   exportSchema = 스키마 JSON 내보내기(지금은 학습용이라 끔).
@Database(
    entities = [HabitEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // DAO 를 꺼내는 통로. 구현은 Room 이 자동 생성.
    //   호출부에서 db.habitDao().observeAll() 처럼 씀.
    abstract fun habitDao(): HabitDao

    // companion object = 자바의 static 영역. 인스턴스 없이 AppDatabase.getInstance(...) 로 호출.
    companion object {
        // @Volatile = 여러 스레드가 이 변수를 볼 때 항상 최신값을 보도록 보장.
        //   (DB 인스턴스는 앱 전체에서 딱 하나만 — 싱글톤 — 유지해야 하므로)
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // DB 인스턴스를 '한 번만' 생성해 재사용(싱글톤). 매번 새로 열면 비용이 크고 위험.
        //   MyBatis 의 SqlSessionFactory 를 앱에서 하나만 만들어 쓰던 것과 같은 발상.
        fun getInstance(context: Context): AppDatabase {
            // 이미 만들어졌으면 그대로 반환, 없을 때만 synchronized 블록에서 생성.
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "habit.db"          // 실제로 폰에 생기는 SQLite 파일 이름
                ).build().also { INSTANCE = it }
            }
        }
    }
}
