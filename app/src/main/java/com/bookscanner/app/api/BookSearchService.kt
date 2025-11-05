package com.bookscanner.app.api

import android.util.Log
import com.bookscanner.app.model.BookInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * 图书信息搜索服务
 * 使用Google Books API搜索图书信息
 */
class BookSearchService {

    /**
     * 通过书名搜索图书信息
     * 优先使用豆瓣API（中文书籍信息更全），备用Google Books
     * @param title 书名
     * @return 搜索到的图书信息列表
     */
    suspend fun searchBookByTitle(title: String): List<BookInfo> = withContext(Dispatchers.IO) {
        try {
            // 1. 优先使用豆瓣读书API（中文书籍信息更全面）
            Log.d(TAG, "正在豆瓣搜索: $title")
            var results = searchFromDouban(title)
            
            if (results.isNotEmpty()) {
                Log.d(TAG, "豆瓣搜索成功，找到 ${results.size} 个结果")
                return@withContext results
            }
            
            // 2. 豆瓣搜索失败，尝试Google Books
            Log.d(TAG, "豆瓣搜索无结果，尝试Google Books")
            results = searchFromGoogleBooks(title)
            
            if (results.isNotEmpty()) {
                Log.d(TAG, "Google Books搜索成功，找到 ${results.size} 个结果")
                return@withContext results
            }
            
            Log.w(TAG, "所有搜索源都无结果")
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Search failed", e)
            emptyList()
        }
    }
    
    /**
     * 从豆瓣读书API搜索
     */
    private fun searchFromDouban(title: String): List<BookInfo> {
        return try {
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val urlString = "https://api.douban.com/v2/book/search?q=$encodedTitle&count=5"
            
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "BookScanner/1.0")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream, "UTF-8"))
                val response = reader.readText()
                reader.close()
                
                Log.d(TAG, "豆瓣API响应: ${response.take(500)}...")
                parseDoubanResponse(response)
            } else {
                Log.w(TAG, "豆瓣API HTTP错误: $responseCode")
                emptyList()
            }
        } catch (e: Exception) {
            Log.w(TAG, "豆瓣搜索失败", e)
            emptyList()
        }
    }
    
    /**
     * 从Google Books API搜索
     */
    private fun searchFromGoogleBooks(title: String): List<BookInfo> {
        return try {
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            val urlString = "https://www.googleapis.com/books/v1/volumes?q=intitle:$encodedTitle&langRestrict=zh-CN&maxResults=5"
            
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                
                Log.d(TAG, "Google Books响应: ${response.take(500)}...")
                parseGoogleBooksResponse(response)
            } else {
                Log.w(TAG, "Google Books HTTP错误: $responseCode")
                emptyList()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Google Books搜索失败", e)
            emptyList()
        }
    }
    
    /**
     * 解析豆瓣API响应
     */
    private fun parseDoubanResponse(response: String): List<BookInfo> {
        try {
            val json = JSONObject(response)
            val books = json.optJSONArray("books") ?: return emptyList()
            
            val results = mutableListOf<BookInfo>()
            
            for (i in 0 until minOf(books.length(), 5)) {
                try {
                    val book = books.getJSONObject(i)
                    
                    // 提取书名
                    val title = book.optString("title", "")
                    
                    // 提取作者（数组）
                    val authorsArray = book.optJSONArray("author")
                    val author = if (authorsArray != null && authorsArray.length() > 0) {
                        val authorList = mutableListOf<String>()
                        for (j in 0 until authorsArray.length()) {
                            authorList.add(authorsArray.getString(j))
                        }
                        authorList.joinToString(", ")
                    } else null
                    
                    // 提取出版社
                    val publisher = book.optString("publisher", null)
                        ?.takeIf { it.isNotEmpty() }
                    
                    // 提取ISBN
                    val isbn13 = book.optString("isbn13", "")
                    val isbn = if (isbn13.isNotEmpty()) {
                        "ISBN $isbn13"
                    } else {
                        book.optString("isbn10", "").takeIf { it.isNotEmpty() }?.let { "ISBN $it" }
                    }
                    
                    // 提取价格
                    val price = book.optString("price", null)
                        ?.takeIf { it.isNotEmpty() }
                    
                    if (title.isNotEmpty()) {
                        results.add(
                            BookInfo(
                                title = title,
                                author = author,
                                publisher = publisher,
                                isbn = isbn,
                                price = price
                            )
                        )
                        
                        Log.d(TAG, "豆瓣结果 $i - 书名:$title, 作者:$author, 出版社:$publisher, ISBN:$isbn")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "解析豆瓣结果 $i 失败", e)
                }
            }
            
            return results
        } catch (e: Exception) {
            Log.e(TAG, "解析豆瓣响应失败", e)
            return emptyList()
        }
    }
    
    /**
     * 解析Google Books API响应
     */
    private fun parseGoogleBooksResponse(response: String): List<BookInfo> {
        try {
            val json = JSONObject(response)
            val items = json.optJSONArray("items") ?: return emptyList()
            
            val results = mutableListOf<BookInfo>()
            
            for (i in 0 until minOf(items.length(), 5)) {
                try {
                    val item = items.getJSONObject(i)
                    val volumeInfo = item.getJSONObject("volumeInfo")
                    
                    // 提取书名
                    val title = volumeInfo.optString("title", "")
                    
                    // 提取作者
                    val authors = volumeInfo.optJSONArray("authors")
                    val author = if (authors != null && authors.length() > 0) {
                        val authorList = mutableListOf<String>()
                        for (j in 0 until authors.length()) {
                            authorList.add(authors.getString(j))
                        }
                        authorList.joinToString(", ")
                    } else null
                    
                    // 提取出版社
                    val publisher = volumeInfo.optString("publisher", null)
                        ?.takeIf { it.isNotEmpty() && it != "null" }
                    
                    // 提取ISBN
                    val industryIdentifiers = volumeInfo.optJSONArray("industryIdentifiers")
                    var isbn: String? = null
                    if (industryIdentifiers != null) {
                        for (j in 0 until industryIdentifiers.length()) {
                            val identifier = industryIdentifiers.getJSONObject(j)
                            val type = identifier.optString("type", "")
                            if (type.contains("ISBN")) {
                                isbn = "ISBN ${identifier.optString("identifier", "")}"
                                break
                            }
                        }
                    }
                    
                    if (title.isNotEmpty()) {
                        results.add(
                            BookInfo(
                                title = title,
                                author = author,
                                publisher = publisher,
                                isbn = isbn,
                                price = null
                            )
                        )
                        
                        Log.d(TAG, "Google结果 $i - 书名:$title, 作者:$author, 出版社:$publisher, ISBN:$isbn")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse item $i", e)
                }
            }
            
            return results
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse response", e)
            return emptyList()
        }
    }

    companion object {
        private const val TAG = "BookSearchService"
    }
}

