package com.haseeb.quranapp.data.local.ai

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.text.qa.BertQuestionAnswerer
import org.tensorflow.lite.task.text.qa.QaAnswer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BertQaHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var bertQuestionAnswerer: BertQuestionAnswerer? = null

    init {
        setupBertQuestionAnswerer()
    }

    private fun setupBertQuestionAnswerer() {
        val baseOptions = BaseOptions.builder()
            .setNumThreads(2)
            .build()

        try {
            val options = BertQuestionAnswerer.BertQuestionAnswererOptions.builder()
                .setBaseOptions(baseOptions)
                .build()

            // We anticipate the model file to be in assets
            bertQuestionAnswerer = BertQuestionAnswerer.createFromFileAndOptions(
                context,
                "mobilebert.tflite",
                options
            )
        } catch (e: Exception) {
            Log.e("BertQaHelper", "TFLite Q&A model failed to load internally: ${e.message}")
            // This is expected if the file isn't physically present yet
        }
    }

    fun answer(contextText: String, question: String): List<QaAnswer> {
        if (bertQuestionAnswerer == null) {
            setupBertQuestionAnswerer()
        }
        return bertQuestionAnswerer?.answer(contextText, question) ?: emptyList()
    }
}
