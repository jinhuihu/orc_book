package com.bookscanner.app.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 书籍数据模型
 * @property title 书名
 * @property scannedTime 扫描时间（毫秒）
 */
data class Book(
    val title: String,
    val scannedTime: Long = System.currentTimeMillis()
) {
    /**
     * 获取格式化的扫描时间
     */
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(scannedTime))
    }
}

