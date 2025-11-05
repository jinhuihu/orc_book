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
     * @param title 书名
     * @return 搜索到的图书信息列表
     */
    suspend fun searchBookByTitle(title: String): List<BookInfo> = withContext(Dispatchers.IO) {
        try {
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
                
                parseGoogleBooksResponse(response, title)
            } else {
                Log.e(TAG, "HTTP error: $responseCode")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search failed", e)
            emptyList()
        }
    }
    
    /**
     * 解析Google Books API响应
     */
    private fun parseGoogleBooksResponse(response: String, originalTitle: String): List<BookInfo> {
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

