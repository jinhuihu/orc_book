package com.bookscanner.app.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 书籍数据模型
 * @property title 书名
 * @property author 作者
 * @property publisher 出版社
 * @property isbn 书号（ISBN）
 * @property price 价格
 * @property scannedTime 扫描时间（毫秒）
 */
data class Book(
    val title: String,
    val author: String = "",
    val publisher: String = "",
    val isbn: String = "",
    val price: String = "",
    val scannedTime: Long = System.currentTimeMillis()
) {
    /**
     * 获取格式化的扫描时间
     */
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(scannedTime))
    }
    
    /**
     * 获取显示用的详细信息
     */
    fun getDetailInfo(): String {
        val details = mutableListOf<String>()
        if (author.isNotEmpty()) details.add("作者: $author")
        if (publisher.isNotEmpty()) details.add("出版: $publisher")
        if (isbn.isNotEmpty()) details.add("ISBN: $isbn")
        if (price.isNotEmpty()) details.add("价格: $price")
        return if (details.isEmpty()) "暂无详细信息" else details.joinToString(" | ")
    }
    
    /**
     * 检查是否有完整信息
     */
    fun hasCompleteInfo(): Boolean {
        return author.isNotEmpty() && publisher.isNotEmpty() && isbn.isNotEmpty()
    }
}

