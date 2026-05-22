package com.suilearn.core.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        StudyPackEntity::class,
        CategoryEntity::class,
        KnowledgePointEntity::class,
        QuestionEntity::class,
        QuestionOptionEntity::class,
        QuestionKnowledgePointEntity::class,
        AnswerRecordEntity::class,
        PracticeSessionEntity::class,
        WrongQuestionEntity::class,
        FavoriteQuestionEntity::class,
        AppSettingEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class SuiLearnDatabase : RoomDatabase() {
    abstract fun studyPackDao(): StudyPackDao
    abstract fun questionDao(): QuestionDao
    abstract fun learningDao(): LearningDao
    abstract fun appSettingDao(): AppSettingDao
}
