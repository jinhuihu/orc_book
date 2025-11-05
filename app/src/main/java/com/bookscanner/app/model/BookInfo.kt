package com.bookscanner.app.model

/**
 * OCR识别的书籍信息
 */
data class BookInfo(
    val title: String? = null,
    val author: String? = null,
    val publisher: String? = null,
    val isbn: String? = null,
    val price: String? = null
) {
    /**
     * 合并两次识别的结果
     */
    fun merge(other: BookInfo): BookInfo {
        return BookInfo(
            title = this.title ?: other.title,
            author = this.author ?: other.author,
            publisher = this.publisher ?: other.publisher,
            isbn = this.isbn ?: other.isbn,
            price = this.price ?: other.price
        )
    }
    
    /**
     * 转换为Book对象
     */
    fun toBook(): Book? {
        val bookTitle = title ?: return null
        return Book(
            title = bookTitle,
            author = author ?: "",
            publisher = publisher ?: "",
            isbn = isbn ?: "",
            price = price ?: ""
        )
    }
}

