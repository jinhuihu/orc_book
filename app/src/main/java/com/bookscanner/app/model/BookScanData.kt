package com.bookscanner.app.model

import android.graphics.Bitmap

/**
 * 书籍扫描数据
 * 用于在两步扫描过程中保存中间数据
 */
data class BookScanData(
    var title: String = "",
    var author: String = "",
    var publisher: String = "",
    var isbn: String = "",
    var frontBitmap: Bitmap? = null,  // 正面图片（可选，用于预览）
    var backBitmap: Bitmap? = null    // 背面图片（可选，用于预览）
) {
    /**
     * 检查是否已完成正面扫描
     */
    fun hasFrontInfo(): Boolean {
        return title.isNotEmpty()
    }
    
    /**
     * 检查是否已完成背面扫描
     */
    fun hasBackInfo(): Boolean {
        return isbn.isNotEmpty()
    }
    
    /**
     * 转换为Book对象
     */
    fun toBook(): Book {
        return Book(
            title = title.ifEmpty { "未知书名" },
            author = author,
            publisher = publisher,
            isbn = isbn
        )
    }
}

