package com.bookscanner.app

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bookscanner.app.adapter.BookAdapter
import com.bookscanner.app.databinding.ActivityMainBinding
import com.bookscanner.app.model.Book
import com.bookscanner.app.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 主活动
 * 负责UI交互和协调各个管理器
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var permissionManager: PermissionManager
    private lateinit var cameraManager: CameraManager
    private lateinit var ocrManager: OCRManager
    private lateinit var excelExporter: ExcelExporter
    private lateinit var bookAdapter: BookAdapter

    // 书籍列表
    private val bookList = mutableListOf<Book>()
    
    // 临时保存的书籍信息（用于两步扫描）
    private var tempBookInfo: com.bookscanner.app.model.BookInfo? = null
    private var isScanningBackCover = false

    // 权限请求启动器 - 相机
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCameraPreview()
        } else {
            Toast.makeText(
                this,
                R.string.camera_permission_required,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // 权限请求启动器 - 存储
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.all { it }) {
            pickImageFromGallery()
        } else {
            Toast.makeText(
                this,
                R.string.storage_permission_required,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // 图片选择启动器
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                processImageFromGallery(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化工具类
        permissionManager = PermissionManager(this)
        cameraManager = CameraManager(this)
        ocrManager = OCRManager(this)
        excelExporter = ExcelExporter(this)

        // 设置工具栏
        setSupportActionBar(binding.toolbar)

        // 初始化RecyclerView
        setupRecyclerView()

        // 设置按钮点击事件
        setupButtonListeners()

        // 更新UI
        updateUI()
    }

    /**
     * 设置RecyclerView
     */
    private fun setupRecyclerView() {
        bookAdapter = BookAdapter { book ->
            // 删除书籍
            AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage("确定要删除《${book.title}》吗？")
                .setPositiveButton(R.string.confirm) { _, _ ->
                    bookList.remove(book)
                    updateBookList()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = bookAdapter
        }
    }

    /**
     * 设置按钮监听器
     */
    private fun setupButtonListeners() {
        // 拍照按钮
        binding.btnCamera.setOnClickListener {
            if (permissionManager.hasCameraPermission()) {
                startCameraPreview()
            } else {
                permissionManager.requestCameraPermission(cameraPermissionLauncher)
            }
        }

        // 相册按钮
        binding.btnGallery.setOnClickListener {
            if (permissionManager.hasStoragePermission()) {
                pickImageFromGallery()
            } else {
                permissionManager.requestStoragePermission(storagePermissionLauncher)
            }
        }

        // 导出按钮
        binding.btnExport.setOnClickListener {
            exportToExcel()
        }
    }

    /**
     * 启动相机预览
     */
    private fun startCameraPreview() {
        binding.previewView.visibility = View.VISIBLE
        binding.bookListContainer.visibility = View.GONE

        cameraManager.startCamera(binding.previewView, this) { imageProxy ->
            processImageFromCamera(imageProxy)
        }

        // 点击预览区域拍照
        binding.previewView.setOnClickListener {
            showLoading(true)
            cameraManager.takePicture(
                onImageCaptured = { imageProxy ->
                    processImageFromCamera(imageProxy)
                },
                onError = { exception ->
                    showLoading(false)
                    Toast.makeText(
                        this,
                        "${getString(R.string.ocr_failed)}: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    /**
     * 关闭相机预览
     */
    private fun stopCameraPreview() {
        binding.previewView.visibility = View.GONE
        binding.bookListContainer.visibility = View.VISIBLE
        cameraManager.stopCamera()  // 只停止相机，不关闭线程池
    }

    /**
     * 从相册选择图片
     */
    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    /**
     * 处理相机拍摄的图片
     */
    private fun processImageFromCamera(imageProxy: androidx.camera.core.ImageProxy) {
        lifecycleScope.launch {
            try {
                // 转换为Bitmap
                val bitmap = withContext(Dispatchers.Default) {
                    ImageProcessor.toBitmap(imageProxy)
                }

                // OCR识别详细信息
                val bookInfo = withContext(Dispatchers.Default) {
                    ocrManager.recognizeBookInfo(bitmap)
                }

                imageProxy.close()

                // 处理结果
                handleBookInfoResult(bookInfo)
                
                // 关闭相机预览
                stopCameraPreview()
            } catch (e: Exception) {
                showLoading(false)
                imageProxy.close()
                Toast.makeText(
                    this@MainActivity,
                    "${getString(R.string.ocr_failed)}: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * 处理从相册选择的图片
     */
    private fun processImageFromGallery(uri: Uri) {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                // 加载Bitmap
                val bitmap = withContext(Dispatchers.IO) {
                    ImageProcessor.loadBitmapFromUri(contentResolver, uri)
                }

                // 压缩图片
                val compressedBitmap = withContext(Dispatchers.Default) {
                    ImageProcessor.compressBitmap(bitmap)
                }

                // OCR识别详细信息
                val bookInfo = withContext(Dispatchers.Default) {
                    ocrManager.recognizeBookInfo(compressedBitmap)
                }

                // 处理结果
                handleBookInfoResult(bookInfo)
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(
                    this@MainActivity,
                    "${getString(R.string.ocr_failed)}: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * 处理OCR识别结果
     */
    private fun handleOCRResult(bookTitle: String?) {
        showLoading(false)

        if (bookTitle.isNullOrEmpty()) {
            Toast.makeText(this, R.string.no_text_detected, Toast.LENGTH_SHORT).show()
            return
        }

        // 添加到列表
        val book = Book(title = bookTitle)
        bookList.add(0, book) // 添加到列表开头
        updateBookList()

        Toast.makeText(this, "识别成功: $bookTitle", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * 处理书籍详细信息识别结果
     */
    private fun handleBookInfoResult(bookInfo: com.bookscanner.app.model.BookInfo) {
        showLoading(false)
        
        if (isScanningBackCover && tempBookInfo != null) {
            // 第二步：扫描背面，合并信息
            val mergedInfo = tempBookInfo!!.merge(bookInfo)
            val book = mergedInfo.toBook()
            
            if (book != null) {
                bookList.add(0, book)
                updateBookList()
                Toast.makeText(this, "书籍信息已完整: ${book.title}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, R.string.ocr_failed, Toast.LENGTH_SHORT).show()
            }
            
            tempBookInfo = null
            isScanningBackCover = false
        } else {
            // 第一步：扫描正面
            val title = bookInfo.title
            if (title.isNullOrEmpty()) {
                Toast.makeText(this, R.string.no_text_detected, Toast.LENGTH_SHORT).show()
                return
            }
            
            // 检查是否已有完整信息
            val hasBackInfo = bookInfo.isbn != null || bookInfo.price != null
            
            if (hasBackInfo || (bookInfo.author != null && bookInfo.publisher != null)) {
                // 已经有完整信息，直接保存
                val book = bookInfo.toBook()
                if (book != null) {
                    bookList.add(0, book)
                    updateBookList()
                    Toast.makeText(this, "识别成功: ${book.title}", Toast.LENGTH_SHORT).show()
                }
            } else {
                // 提示扫描背面补充信息
                tempBookInfo = bookInfo
                showScanBackCoverDialog()
            }
        }
    }
    
    /**
     * 显示扫描背面的对话框
     */
    private fun showScanBackCoverDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.scan_back_for_isbn)
            .setMessage(R.string.scan_back_prompt)
            .setPositiveButton(R.string.scan) { _, _ ->
                // 扫描背面
                isScanningBackCover = true
                if (permissionManager.hasCameraPermission()) {
                    startCameraPreview()
                } else {
                    permissionManager.requestCameraPermission(cameraPermissionLauncher)
                }
            }
            .setNegativeButton(R.string.skip) { _, _ ->
                // 跳过，直接保存
                val book = tempBookInfo?.toBook()
                if (book != null) {
                    bookList.add(0, book)
                    updateBookList()
                    Toast.makeText(this, "识别成功: ${book.title}", Toast.LENGTH_SHORT).show()
                }
                tempBookInfo = null
            }
            .setCancelable(false)
            .show()
    }

    /**
     * 导出到Excel
     */
    private fun exportToExcel() {
        if (bookList.isEmpty()) {
            Toast.makeText(this, R.string.no_books, Toast.LENGTH_SHORT).show()
            return
        }

        showLoading(true)

        lifecycleScope.launch {
            val filePath = withContext(Dispatchers.IO) {
                excelExporter.exportToExcel(bookList)
            }

            showLoading(false)

            if (filePath != null) {
                Toast.makeText(
                    this@MainActivity,
                    getString(R.string.export_success, filePath),
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this@MainActivity,
                    R.string.export_failed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * 更新书籍列表显示
     */
    private fun updateBookList() {
        bookAdapter.submitList(bookList.toList())
        updateUI()
    }

    /**
     * 更新UI状态
     */
    private fun updateUI() {
        if (bookList.isEmpty()) {
            binding.recyclerView.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
        }
    }

    /**
     * 显示/隐藏加载状态
     */
    private fun showLoading(show: Boolean) {
        binding.loadingContainer.visibility = if (show) View.VISIBLE else View.GONE
    }

    /**
     * 创建选项菜单
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    /**
     * 处理菜单项点击
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear -> {
                if (bookList.isNotEmpty()) {
                    AlertDialog.Builder(this)
                        .setTitle(R.string.clear_all)
                        .setMessage(R.string.confirm_clear)
                        .setPositiveButton(R.string.confirm) { _, _ ->
                            bookList.clear()
                            updateBookList()
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.shutdown()
        ocrManager.close()
    }

    override fun onBackPressed() {
        if (binding.previewView.visibility == View.VISIBLE) {
            stopCameraPreview()
        } else {
            super.onBackPressed()
        }
    }
}

