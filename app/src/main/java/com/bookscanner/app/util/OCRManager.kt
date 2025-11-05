package com.bookscanner.app.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * OCR识别管理类
 * 使用Google ML Kit进行文字识别
 */
class OCRManager(context: Context) {

    // 中文识别器
    private val chineseRecognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build()
    )

    // 拉丁文识别器
    private val latinRecognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )

    /**
     * 从图片中识别文字
     * @param bitmap 要识别的图片
     * @return 识别出的书名（取最大的文本块）
     */
    suspend fun recognizeText(bitmap: Bitmap): String? {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            
            // 先尝试中文识别
            val chineseResult = chineseRecognizer.process(image).await()
            val chineseText = extractBookTitle(chineseResult)
            
            if (!chineseText.isNullOrEmpty()) {
                Log.d(TAG, "Chinese OCR result: $chineseText")
                return chineseText
            }
            
            // 如果中文识别失败，尝试拉丁文识别
            val latinResult = latinRecognizer.process(image).await()
            val latinText = extractBookTitle(latinResult)
            
            Log.d(TAG, "Latin OCR result: $latinText")
            latinText
        } catch (e: Exception) {
            Log.e(TAG, "OCR recognition failed", e)
            null
        }
    }

    /**
     * 从识别结果中提取书名
     * 策略：选择最大的文本块作为书名
     */
    private fun extractBookTitle(result: Text): String? {
        if (result.textBlocks.isEmpty()) {
            return null
        }

        // 找出面积最大的文本块（通常书名在封面上最显眼）
        val largestBlock = result.textBlocks.maxByOrNull { block ->
            val boundingBox = block.boundingBox
            if (boundingBox != null) {
                boundingBox.width() * boundingBox.height()
            } else {
                0
            }
        }

        // 提取文本并清理
        val title = largestBlock?.text?.trim()
        
        // 如果文本块为空或过短，尝试组合多个文本行
        if (title.isNullOrEmpty() || title.length < 2) {
            val allLines = result.textBlocks
                .flatMap { it.lines }
                .map { it.text.trim() }
                .filter { it.isNotEmpty() }
            
            return if (allLines.isNotEmpty()) {
                // 取前3行组合（如果有的话）
                allLines.take(3).joinToString(" ")
            } else {
                null
            }
        }

        return title
    }

    /**
     * 关闭识别器
     */
    fun close() {
        chineseRecognizer.close()
        latinRecognizer.close()
    }

    companion object {
        private const val TAG = "OCRManager"
    }
}

