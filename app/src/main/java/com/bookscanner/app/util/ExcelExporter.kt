package com.bookscanner.app.util

import android.content.Context
import android.os.Environment
import android.util.Log
import com.bookscanner.app.model.Book
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Excel导出工具类
 * 使用Apache POI库生成Excel文件
 */
class ExcelExporter(private val context: Context) {

    /**
     * 导出书籍列表到Excel
     * @param books 书籍列表
     * @return 生成的Excel文件路径，失败返回null
     */
    fun exportToExcel(books: List<Book>): String? {
        if (books.isEmpty()) {
            Log.w(TAG, "Book list is empty")
            return null
        }

        return try {
            // 创建工作簿
            val workbook: Workbook = XSSFWorkbook()
            val sheet: Sheet = workbook.createSheet("书籍列表")

            // 创建标题行样式
            val headerStyle = createHeaderStyle(workbook)
            
            // 创建标题行
            val headerRow: Row = sheet.createRow(0)
            val headers = arrayOf("序号", "书名", "扫描时间")
            
            headers.forEachIndexed { index, header ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(header)
                cell.cellStyle = headerStyle
            }

            // 创建数据行
            books.forEachIndexed { index, book ->
                val row: Row = sheet.createRow(index + 1)
                
                // 序号
                row.createCell(0).setCellValue((index + 1).toDouble())
                
                // 书名
                row.createCell(1).setCellValue(book.title)
                
                // 扫描时间
                row.createCell(2).setCellValue(book.getFormattedTime())
            }

            // 手动设置列宽（Android 不支持 autoSizeColumn）
            sheet.setColumnWidth(0, 3000)   // 序号列：宽度 3000
            sheet.setColumnWidth(1, 10000)  // 书名列：宽度 10000
            sheet.setColumnWidth(2, 6000)   // 扫描时间列：宽度 6000

            // 生成文件名
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(Date())
            val fileName = "书籍列表_$timestamp.xlsx"

            // 保存文件
            val file = getOutputFile(fileName)
            FileOutputStream(file).use { outputStream ->
                workbook.write(outputStream)
            }
            workbook.close()

            Log.d(TAG, "Excel exported successfully: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Excel export failed", e)
            null
        }
    }

    /**
     * 创建标题行样式
     */
    private fun createHeaderStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()
        
        // 设置字体加粗
        font.bold = true
        font.fontHeightInPoints = 12
        
        style.setFont(font)
        style.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        style.borderBottom = BorderStyle.THIN
        style.borderTop = BorderStyle.THIN
        style.borderLeft = BorderStyle.THIN
        style.borderRight = BorderStyle.THIN
        style.alignment = HorizontalAlignment.CENTER
        style.verticalAlignment = VerticalAlignment.CENTER
        
        return style
    }

    /**
     * 获取输出文件
     */
    private fun getOutputFile(fileName: String): File {
        // 优先使用外部存储的Documents目录
        val documentsDir = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                "BookScanner"
            )
        } else {
            File(
                Environment.getExternalStorageDirectory(),
                "Documents/BookScanner"
            )
        }

        // 如果外部存储不可用，使用应用私有目录
        val outputDir = if (documentsDir.exists() || documentsDir.mkdirs()) {
            documentsDir
        } else {
            File(context.getExternalFilesDir(null), "exports").apply {
                if (!exists()) mkdirs()
            }
        }

        return File(outputDir, fileName)
    }

    companion object {
        private const val TAG = "ExcelExporter"
    }
}

