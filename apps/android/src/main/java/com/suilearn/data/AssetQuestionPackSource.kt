package com.suilearn.data

import android.content.Context
import com.suilearn.core.importer.QuestionPackSource

class AssetQuestionPackSource(
    context: Context,
    private val assetName: String = "question_pack_java_interview.json",
) : QuestionPackSource {
    private val appContext = context.applicationContext

    override fun loadQuestionPackJson(): String =
        appContext.assets.open(assetName).bufferedReader(Charsets.UTF_8).use { it.readText() }
}

