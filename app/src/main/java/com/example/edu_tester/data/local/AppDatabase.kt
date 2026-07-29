package com.example.edu_tester.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.edu_tester.data.local.dao.AttemptDao
import com.example.edu_tester.data.local.dao.SessionDao
import com.example.edu_tester.data.local.dao.TestDao
import com.example.edu_tester.data.local.entity.AttemptEntity
import com.example.edu_tester.data.local.entity.QuestionEntity
import com.example.edu_tester.data.local.entity.SessionEntity
import com.example.edu_tester.data.local.entity.TestEntity

@Database(
    entities = [TestEntity::class, QuestionEntity::class, AttemptEntity::class, SessionEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun testDao(): TestDao
    abstract fun attemptDao(): AttemptDao
    abstract fun sessionDao(): SessionDao
}
