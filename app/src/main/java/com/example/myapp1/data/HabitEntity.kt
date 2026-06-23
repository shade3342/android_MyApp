package com.example.myapp1.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// @Entity = DB 테이블 한 줄(row)을 표현하는 클래스.
//   MyBatis 로 치면 ResultMap 이 매핑되는 VO/DTO + 테이블 DDL(CREATE TABLE)을 한 곳에 합친 것.
//   tableName 을 안 주면 클래스 이름(HabitEntity)이 테이블명이 됨 → 명시적으로 "habits" 로 지정.
@Entity(tableName = "habits")
data class HabitEntity(
    // @PrimaryKey + autoGenerate = AUTO_INCREMENT PK.
    //   기본값 0 으로 두면 "아직 저장 안 된 새 객체"라는 뜻 → insert 시 Room 이 실제 id 를 채워줌.
    //   (MyBatis 로 치면 <insert useGeneratedKeys="true" keyProperty="id"> 로 자동증가 PK 를
    //    돌려받던 것과 같음. DDL 상으로는 컬럼에 AUTO_INCREMENT 가 붙은 것.)
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // 각 컬럼. 필드 이름이 곧 컬럼 이름이 됨(emoji, name, done).
    //   타입 매핑: Kotlin String → TEXT, Boolean → INTEGER(0/1) 로 Room 이 자동 변환.
    val emoji: String,
    val name: String,
    val done: Boolean = false
)
