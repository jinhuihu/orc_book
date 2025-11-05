package com.bookscanner.app.util

import android.content.Context
import android.content.SharedPreferences
import com.bookscanner.app.model.Book
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 数据管理器
 * 负责书籍数据的持久化存储和读取
 */
class DataManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private val gson = Gson()
    
    companion object {
        private const val PREFS_NAME = "book_scanner_data"
        private const val KEY_BOOK_LIST = "book_list"
    }
    
    /**
     * 保存书籍列表
     */
    fun saveBooks(books: List<Book>) {
        val json = gson.toJson(books)
        prefs.edit().putString(KEY_BOOK_LIST, json).apply()
    }
    
    /**
     * 加载书籍列表
     */
    fun loadBooks(): MutableList<Book> {
        val json = prefs.getString(KEY_BOOK_LIST, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<MutableList<Book>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                e.printStackTrace()
                mutableListOf()
            }
        } else {
            mutableListOf()
        }
    }
    
    /**
     * 添加单个书籍
     */
    fun addBook(book: Book) {
        val books = loadBooks()
        books.add(book)
        saveBooks(books)
    }
    
    /**
     * 更新书籍
     */
    fun updateBook(position: Int, book: Book) {
        val books = loadBooks()
        if (position in books.indices) {
            books[position] = book
            saveBooks(books)
        }
    }
    
    /**
     * 删除书籍
     */
    fun removeBook(position: Int) {
        val books = loadBooks()
        if (position in books.indices) {
            books.removeAt(position)
            saveBooks(books)
        }
    }
    
    /**
     * 清空所有书籍
     */
    fun clearBooks() {
        prefs.edit().remove(KEY_BOOK_LIST).apply()
    }
    
    /**
     * 获取书籍数量
     */
    fun getBookCount(): Int {
        return loadBooks().size
    }
}

