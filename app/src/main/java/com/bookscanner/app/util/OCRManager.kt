package com.bookscanner.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import kotlin.math.max

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
            // 1. 图像预处理 - 提高对比度和清晰度
            val enhancedBitmap = enhanceImage(bitmap)
            val image = InputImage.fromBitmap(enhancedBitmap, 0)
            
            // 2. 先尝试中文识别
            val chineseResult = chineseRecognizer.process(image).await()
            val chineseText = extractBookTitle(chineseResult)
            
            if (!chineseText.isNullOrEmpty() && isValidBookTitle(chineseText)) {
                Log.d(TAG, "Chinese OCR result: $chineseText")
                return cleanBookTitle(chineseText)
            }
            
            // 3. 如果中文识别失败，尝试拉丁文识别
            val latinResult = latinRecognizer.process(image).await()
            val latinText = extractBookTitle(latinResult)
            
            if (!latinText.isNullOrEmpty() && isValidBookTitle(latinText)) {
                Log.d(TAG, "Latin OCR result: $latinText")
                return cleanBookTitle(latinText)
            }
            
            // 4. 如果都失败，返回任何可用的文本
            chineseText?.let { cleanBookTitle(it) } ?: latinText?.let { cleanBookTitle(it) }
        } catch (e: Exception) {
            Log.e(TAG, "OCR recognition failed", e)
            null
        }
    }
    
    /**
     * 增强图像 - 提高对比度和清晰度
     */
    private fun enhanceImage(bitmap: Bitmap): Bitmap {
        val enhanced = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(enhanced)
        val paint = Paint()
        
        // 创建颜色矩阵增强对比度
        val colorMatrix = ColorMatrix(floatArrayOf(
            1.5f, 0f, 0f, 0f, -50f,     // Red
            0f, 1.5f, 0f, 0f, -50f,     // Green
            0f, 0f, 1.5f, 0f, -50f,     // Blue
            0f, 0f, 0f, 1f, 0f          // Alpha
        ))
        
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return enhanced
    }
    
    /**
     * 验证是否为有效的书名
     */
    private fun isValidBookTitle(title: String): Boolean {
        // 过滤太短的文本
        if (title.length < 2) return false
        
        // 过滤纯数字
        if (title.all { it.isDigit() || it.isWhitespace() }) return false
        
        // 过滤只有特殊字符的文本
        if (title.all { !it.isLetterOrDigit() }) return false
        
        return true
    }
    
    /**
     * 清理书名 - 移除无效字符
     */
    private fun cleanBookTitle(title: String): String {
        return title
            .trim()
            // 移除多余的空格
            .replace(Regex("\\s+"), " ")
            // 移除常见的干扰字符
            .replace(Regex("[|｜]"), "")
            // 移除首尾的特殊字符
            .trim { char ->
                char in ".。,，;；:：!！?？\"'`[]【】()（）<>《》\u201c\u201d\u2018\u2019"
            }
    }

    /**
     * 从识别结果中提取书名
     * 改进策略：综合考虑文本块大小、位置和内容
     */
    private fun extractBookTitle(result: Text): String? {
        if (result.textBlocks.isEmpty()) {
            return null
        }

        // 1. 按优先级排序文本块
        val rankedBlocks = result.textBlocks
            .filter { block -> 
                val text = block.text.trim()
                text.length >= 2 && !text.all { it.isDigit() }
            }
            .map { block ->
                val boundingBox = block.boundingBox
                val text = block.text.trim()
                
                // 计算得分
                var score = 0.0
                
                if (boundingBox != null) {
                    // 面积得分（最重要）
                    val area = boundingBox.width() * boundingBox.height()
                    score += area * 0.5
                    
                    // 位置得分（书名通常在上半部分）
                    val imageHeight = max(1, result.textBlocks.maxOfOrNull { 
                        it.boundingBox?.bottom ?: 0 
                    } ?: 1)
                    val topRatio = 1.0 - (boundingBox.top.toDouble() / imageHeight)
                    score += topRatio * 1000
                    
                    // 宽度得分（书名通常比较宽）
                    score += boundingBox.width() * 0.3
                }
                
                // 文本长度得分（书名通常有一定长度）
                val lengthScore = when (text.length) {
                    in 2..4 -> 500.0
                    in 5..10 -> 1000.0
                    in 11..20 -> 800.0
                    else -> 300.0
                }
                score += lengthScore
                
                // 包含中文加分
                if (text.any { it in '\u4e00'..'\u9fff' }) {
                    score += 500.0
                }
                
                Pair(block, score)
            }
            .sortedByDescending { it.second }
        
        if (rankedBlocks.isEmpty()) {
            return null
        }

        // 2. 选择得分最高的文本块
        val bestBlock = rankedBlocks.first().first
        val title = bestBlock.text.trim()
        
        // 3. 如果单个文本块太短，尝试组合多行
        if (title.length < 4 && bestBlock.lines.size > 1) {
            return bestBlock.lines
                .take(2)
                .joinToString(" ") { it.text.trim() }
        }

        return title
    }

    /**
     * 识别书籍详细信息（正面或背面）
     * @param bitmap 要识别的图片
     * @return 识别出的书籍信息
     */
    suspend fun recognizeBookInfo(bitmap: Bitmap): com.bookscanner.app.model.BookInfo {
        return try {
            val enhancedBitmap = enhanceImage(bitmap)
            val image = InputImage.fromBitmap(enhancedBitmap, 0)
            
            // 使用中文识别器获取所有文本
            val result = chineseRecognizer.process(image).await()
            val allText = result.text
            
            // 打印所有识别到的文本（用于调试）
            Log.d(TAG, "========== OCR识别到的完整文本 ==========")
            Log.d(TAG, allText)
            Log.d(TAG, "======================================")
            
            // 提取各个字段
            val title = extractTitle(result)
            val author = extractAuthor(allText)
            val publisher = extractPublisher(allText)
            val isbn = extractISBN(allText)
            val price = extractPrice(allText)
            
            Log.d(TAG, "提取结果 - 书名: $title")
            Log.d(TAG, "提取结果 - 作者: $author")
            Log.d(TAG, "提取结果 - 出版社: $publisher")
            Log.d(TAG, "提取结果 - ISBN: $isbn")
            Log.d(TAG, "提取结果 - 价格: $price")
            
            com.bookscanner.app.model.BookInfo(
                title = title,
                author = author,
                publisher = publisher,
                isbn = isbn,
                price = price
            )
        } catch (e: Exception) {
            Log.e(TAG, "Book info recognition failed", e)
            com.bookscanner.app.model.BookInfo()
        }
    }
    
    /**
     * 提取标题
     */
    private fun extractTitle(result: Text): String? {
        return extractBookTitle(result)
    }
    
    /**
     * 提取作者信息
     * 查找包含"著"、"编"、"作者"等关键词的文本
     */
    private fun extractAuthor(text: String): String? {
        val lines = text.split("\n").map { it.trim() }
        
        for (line in lines) {
            // 模式1：[国家] 作者名 著/编 或 [国家] 作者名 (英文名) 著/编
            if ((line.contains("著") || line.contains("编")) && line.length in 3..80) {
                var author = line
                    .replace(Regex("\\[.*?\\]"), "") // 移除 [美]、[中] 等
                    .replace(Regex("\\(.*?\\)"), "") // 暂时移除英文名，后面再加回
                    .replace("著", "")
                    .replace("编", "")
                    .replace("作者", "")
                    .replace(Regex("[：:]"), "")
                    .trim()
                
                // 如果原文包含英文名，保留它
                val englishName = Regex("\\((.*?)\\)").find(line)?.groupValues?.get(1)
                if (englishName != null && englishName.isNotEmpty()) {
                    author = "$author ($englishName)"
                }
                
                if (author.isNotEmpty() && author.length in 2..50) {
                    return author
                }
            }
            
            // 模式2：作者: XXX 或 作者：XXX
            if (line.startsWith("作者") && line.length < 50) {
                val author = line
                    .replace("作者", "")
                    .replace(Regex("[：:]"), "")
                    .trim()
                if (author.isNotEmpty() && author.length in 2..30) {
                    return author
                }
            }
            
            // 模式3：XX 编著
            if (line.contains("编著") && line.length in 3..50) {
                val author = line
                    .replace(Regex("\\[.*?\\]"), "")
                    .replace("编著", "")
                    .replace(Regex("[：:]"), "")
                    .trim()
                if (author.isNotEmpty() && author.length in 2..30) {
                    return author
                }
            }
        }
        return null
    }
    
    /**
     * 提取出版社信息
     * 查找包含"出版社"、"出版"等关键词的文本
     */
    private fun extractPublisher(text: String): String? {
        val lines = text.split("\n").map { it.trim() }
        
        // 优先查找完整匹配
        for (line in lines) {
            if (line.contains("出版社") && line.length < 50) {
                // 方法1：提取 XXX出版社
                val pattern1 = Regex("([\\u4e00-\\u9fa5]{2,20}出版社)")
                val match1 = pattern1.find(line)
                if (match1 != null) {
                    return match1.value
                }
                
                // 方法2：如果整行都是出版社信息
                if (line.endsWith("出版社") && !line.contains("著") && !line.contains("作者")) {
                    return line
                }
            }
        }
        
        // 查找"出版"关键词（可能是"XX出版集团"等）
        for (line in lines) {
            if (line.contains("出版") && line.length < 30) {
                val pattern = Regex("([\\u4e00-\\u9fa5]{2,20}出版[社集团传媒]{0,4})")
                val match = pattern.find(line)
                if (match != null) {
                    return match.value
                }
            }
        }
        
        return null
    }
    
    /**
     * 提取ISBN
     * 查找ISBN格式的数字
     */
    private fun extractISBN(text: String): String? {
        // ISBN-13 格式: 978-X-XXXX-XXXX-X 或 9787XXXXXXXXX
        val isbnPattern = Regex("(?:ISBN[:\\s-]*)?(?:978|979)[-\\s]?\\d{1,5}[-\\s]?\\d{1,7}[-\\s]?\\d{1,7}[-\\s]?\\d")
        val match = isbnPattern.find(text)
        return match?.value?.replace(Regex("[^0-9]"), "")?.let { digits ->
            if (digits.length == 13) "ISBN $digits" else null
        }
    }
    
    /**
     * 提取价格
     * 查找"定价"、"价格"等关键词后的金额
     */
    private fun extractPrice(text: String): String? {
        val pricePattern = Regex("(?:定价|价格)[：:￥]?\\s*([0-9.]+)\\s*元")
        val match = pricePattern.find(text)
        return match?.groupValues?.get(1)?.let { "¥$it" }
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

