package com.suilearn

import android.app.Application
import androidx.room.Room
import com.suilearn.core.database.SuiLearnDatabase
import com.suilearn.data.AssetQuestionPackSource
import com.suilearn.di.AppDependencies

class SuiLearnApp : Application() {
    private val database: SuiLearnDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            SuiLearnDatabase::class.java,
            "suilearn.db",
        ).build()
    }

    val appDependencies: AppDependencies by lazy {
        AppDependencies(
            questionPackSource = AssetQuestionPackSource(this),
            database = database,
        )
    }
}
