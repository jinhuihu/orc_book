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
import com.bookscanner.app.api.BookSearchService
import com.bookscanner.app.databinding.ActivityMainBinding
import com.bookscanner.app.model.Book
import com.bookscanner.app.model.BookInfo
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
    private lateinit var bookSearchService: BookSearchService

    // 书籍列表
    private val bookList = mutableListOf<Book>()
    
    // 临时保存的书籍信息（用于多步扫描）
    private var tempBookInfo: com.bookscanner.app.model.BookInfo? = null
    private var currentScanStep = ScanStep.NONE
    
    // 扫描步骤枚举
    enum class ScanStep {
        NONE,           // 未开始
        SCAN_ISBN,      // 第1步：扫描ISBN
        SCAN_TITLE,     // 第2步：扫描书名
        SCAN_AUTHOR,    // 第3步：扫描作者
        SCAN_PUBLISHER  // 第4步：扫描出版社
    }

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
        bookSearchService = BookSearchService()

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
        bookAdapter = BookAdapter(
            onDeleteClick = { book ->
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
            },
            onItemClick = { book ->
                // 编辑书籍
                showEditBookDialog(book)
            }
        )

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
     * 智能多步扫描：根据缺失字段逐步提示补充
     */
    private fun handleBookInfoResult(bookInfo: BookInfo) {
        showLoading(false)
        
        when (currentScanStep) {
            ScanStep.NONE, ScanStep.SCAN_ISBN -> {
                // 第1步：扫描ISBN
                handleISBNScanResult(bookInfo)
            }
            ScanStep.SCAN_TITLE -> {
                // 第2步：补充书名
                handleTitleScanResult(bookInfo)
            }
            ScanStep.SCAN_AUTHOR -> {
                // 第3步：补充作者
                handleAuthorScanResult(bookInfo)
            }
            ScanStep.SCAN_PUBLISHER -> {
                // 第4步：补充出版社
                handlePublisherScanResult(bookInfo)
            }
        }
    }
    
    /**
     * 处理ISBN扫描结果
     */
    private fun handleISBNScanResult(bookInfo: BookInfo) {
        val isbn = bookInfo.isbn
        if (!isbn.isNullOrEmpty()) {
            // 识别到ISBN，通过ISBN查询完整信息
            Toast.makeText(this, "ISBN识别成功，正在查询书籍信息...", Toast.LENGTH_SHORT).show()
            queryBookInfoByISBN(isbn, bookInfo)
        } else {
            // 未识别到ISBN，提示重新扫描
            Toast.makeText(this, R.string.scan_isbn_first, Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 处理书名扫描结果
     */
    private fun handleTitleScanResult(bookInfo: BookInfo) {
        val title = bookInfo.title
        if (!title.isNullOrEmpty()) {
            // 识别到书名，合并信息
            tempBookInfo = tempBookInfo!!.copy(title = title)
            Toast.makeText(this, "书名识别成功: $title", Toast.LENGTH_SHORT).show()
            
            // 检查下一步需要补充什么
            checkNextStepAndPrompt()
        } else {
            Toast.makeText(this, "未识别到书名，请重新扫描", Toast.LENGTH_SHORT).show()
            showScanStepDialog(ScanStep.SCAN_TITLE)
        }
    }
    
    /**
     * 处理作者扫描结果
     */
    private fun handleAuthorScanResult(bookInfo: BookInfo) {
        val author = bookInfo.author
        if (!author.isNullOrEmpty()) {
            // 识别到作者，合并信息
            tempBookInfo = tempBookInfo!!.copy(author = author)
            Toast.makeText(this, "作者识别成功: $author", Toast.LENGTH_SHORT).show()
            
            // 检查下一步
            checkNextStepAndPrompt()
        } else {
            Toast.makeText(this, "未识别到作者", Toast.LENGTH_SHORT).show()
            // 允许跳过作者，继续检查出版社
            checkNextStepAndPrompt()
        }
    }
    
    /**
     * 处理出版社扫描结果
     */
    private fun handlePublisherScanResult(bookInfo: BookInfo) {
        val publisher = bookInfo.publisher
        if (!publisher.isNullOrEmpty()) {
            // 识别到出版社，合并信息
            tempBookInfo = tempBookInfo!!.copy(publisher = publisher)
            Toast.makeText(this, "出版社识别成功: $publisher", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "未识别到出版社", Toast.LENGTH_SHORT).show()
        }
        
        // 所有步骤完成，保存书籍
        saveCurrentBook()
    }
    
    /**
     * 检查下一步需要扫描什么并提示
     */
    private fun checkNextStepAndPrompt() {
        val info = tempBookInfo ?: return
        
        // 按优先级检查缺失字段
        when {
            info.title.isNullOrEmpty() -> {
                // 缺少书名（必填）
                currentScanStep = ScanStep.SCAN_TITLE
                showScanStepDialog(ScanStep.SCAN_TITLE)
            }
            info.author.isNullOrEmpty() -> {
                // 缺少作者
                currentScanStep = ScanStep.SCAN_AUTHOR
                showScanStepDialog(ScanStep.SCAN_AUTHOR)
            }
            info.publisher.isNullOrEmpty() -> {
                // 缺少出版社
                currentScanStep = ScanStep.SCAN_PUBLISHER
                showScanStepDialog(ScanStep.SCAN_PUBLISHER)
            }
            else -> {
                // 所有信息齐全，保存
                saveCurrentBook()
            }
        }
    }
    
    /**
     * 显示扫描步骤提示对话框
     */
    private fun showScanStepDialog(step: ScanStep) {
        val (title, message, fieldName) = when (step) {
            ScanStep.SCAN_TITLE -> Triple(
                "扫描书名",
                "未查询到书名信息\n\n请扫描书籍正面识别书名",
                "书名"
            )
            ScanStep.SCAN_AUTHOR -> Triple(
                "扫描作者",
                "未查询到作者信息\n\n请扫描书籍正面识别作者\n（包含\"著\"字的行）",
                "作者"
            )
            ScanStep.SCAN_PUBLISHER -> Triple(
                "扫描出版社",
                "未查询到出版社信息\n\n请扫描书籍正面识别出版社\n（包含\"出版社\"的文字）",
                "出版社"
            )
            else -> return
        }
        
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("扫描") { _, _ ->
                currentScanStep = step
                if (permissionManager.hasCameraPermission()) {
                    startCameraPreview()
                } else {
                    permissionManager.requestCameraPermission(cameraPermissionLauncher)
                }
            }
            .setNegativeButton("跳过") { _, _ ->
                // 跳过当前字段，检查下一个
                when (step) {
                    ScanStep.SCAN_TITLE -> {
                        // 书名不能跳过
                        Toast.makeText(this, R.string.title_required, Toast.LENGTH_SHORT).show()
                        showScanStepDialog(step)
                    }
                    ScanStep.SCAN_AUTHOR -> {
                        // 跳过作者，检查出版社
                        checkNextStepAndPrompt()
                    }
                    ScanStep.SCAN_PUBLISHER -> {
                        // 跳过出版社，直接保存
                        saveCurrentBook()
                    }
                    else -> {}
                }
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * 保存当前书籍
     */
    private fun saveCurrentBook() {
        val book = tempBookInfo?.toBook()
        if (book != null) {
            bookList.add(0, book)
            updateBookList()
            showBookInfoDetails(book)
            Toast.makeText(this, "书籍信息已保存", Toast.LENGTH_SHORT).show()
        }
        
        // 重置状态
        tempBookInfo = null
        currentScanStep = ScanStep.NONE
    }
    
    /**
     * 通过ISBN查询完整的书籍信息
     */
    private fun queryBookInfoByISBN(isbn: String, ocrInfo: BookInfo) {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val apiResult = withContext(Dispatchers.IO) {
                    bookSearchService.searchBookByISBN(isbn)
                }
                
                showLoading(false)
                
                if (apiResult != null) {
                    // ISBN查询成功，合并OCR识别的信息
                    tempBookInfo = apiResult.merge(ocrInfo)
                    currentScanStep = ScanStep.SCAN_ISBN
                    
                    Toast.makeText(
                        this@MainActivity,
                        R.string.isbn_query_success,
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    // 检查需要补充哪些信息
                    checkNextStepAndPrompt()
                } else {
                    // ISBN查询失败，需要手动扫描所有信息
                    Toast.makeText(this@MainActivity, "ISBN查询无结果，请手动扫描", Toast.LENGTH_SHORT).show()
                    
                    tempBookInfo = ocrInfo
                    currentScanStep = ScanStep.SCAN_ISBN
                    checkNextStepAndPrompt()
                }
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(
                    this@MainActivity,
                    "${getString(R.string.isbn_query_failed)}: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                
                // 查询失败，使用OCR信息继续
                tempBookInfo = ocrInfo
                currentScanStep = ScanStep.SCAN_ISBN
                checkNextStepAndPrompt()
            }
        }
    }
    
    /**
     * 显示书籍信息详情
     */
    private fun showBookInfoDetails(book: Book) {
        val message = buildString {
            append("《${book.title}》\n\n")
            if (book.author.isNotEmpty()) append("作者：${book.author}\n")
            if (book.publisher.isNotEmpty()) append("出版社：${book.publisher}\n")
            if (book.isbn.isNotEmpty()) append("ISBN：${book.isbn}\n")
            if (book.price.isNotEmpty()) append("价格：${book.price}")
        }
        
        AlertDialog.Builder(this)
            .setTitle("识别成功")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }
    
    /**
     * 提示扫描出版社的对话框
     */
    private fun showScanPublisherDialog(title: String) {
        val currentInfo = tempBookInfo
        val message = buildString {
            append("《$title》\n\n")
            append("已通过ISBN查询到：\n")
            currentInfo?.author?.let { append("作者：$it\n") }
            currentInfo?.isbn?.let { append("ISBN：$it\n") }
            append("\n是否扫描书籍正面补充出版社信息？")
        }
        
        AlertDialog.Builder(this)
            .setTitle("扫描正面补充出版社")
            .setMessage(message)
            .setPositiveButton("扫描正面") { _, _ ->
                // 扫描正面获取出版社
                isScanningBackCover = true  // 复用标志位表示第二步
                if (permissionManager.hasCameraPermission()) {
                    startCameraPreview()
                } else {
                    permissionManager.requestCameraPermission(cameraPermissionLauncher)
                }
            }
            .setNegativeButton("跳过") { _, _ ->
                // 直接保存，使用API的出版社（可能为空）
                val book = tempBookInfo?.toBook()
                if (book != null) {
                    bookList.add(0, book)
                    updateBookList()
                    showBookInfoDetails(book)
                }
                tempBookInfo = null
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * 提示扫描ISBN的对话框
     */
    private fun showScanISBNDialog(title: String) {
        AlertDialog.Builder(this)
            .setTitle("建议扫描ISBN")
            .setMessage("已识别书名：《$title》\n\n建议扫描书籍背面的ISBN条形码，可自动获取完整信息。")
            .setPositiveButton("扫描ISBN") { _, _ ->
                if (permissionManager.hasCameraPermission()) {
                    startCameraPreview()
                } else {
                    permissionManager.requestCameraPermission(cameraPermissionLauncher)
                }
            }
            .setNegativeButton("直接保存") { _, _ ->
                val book = tempBookInfo?.toBook()
                if (book != null) {
                    bookList.add(0, book)
                    updateBookList()
                }
                tempBookInfo = null
            }
            .show()
    }
    
    /**
     * 显示信息补充选择对话框
     */
    private fun showInfoCompletionDialog(title: String) {
        AlertDialog.Builder(this)
            .setTitle("补充书籍信息")
            .setMessage("《$title》\n\n选择补充方式：")
            .setPositiveButton(R.string.auto_fill) { _, _ ->
                // 在线搜索自动填充
                searchAndFillBookInfo(title)
            }
            .setNeutralButton(R.string.manual_scan) { _, _ ->
                // 手动扫描背面
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
                    Toast.makeText(this, "已保存: ${book.title}", Toast.LENGTH_SHORT).show()
                }
                tempBookInfo = null
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * 在线搜索并填充书籍信息
     */
    private fun searchAndFillBookInfo(title: String) {
        showLoading(true)
        
        lifecycleScope.launch {
            try {
                val searchResults = withContext(Dispatchers.IO) {
                    bookSearchService.searchBookByTitle(title)
                }
                
                showLoading(false)
                
                if (searchResults.isEmpty()) {
                    // 搜索失败，提示手动扫描
                    Toast.makeText(this@MainActivity, R.string.search_failed, Toast.LENGTH_SHORT).show()
                    showScanBackCoverDialog()
                } else if (searchResults.size == 1) {
                    // 只有一个结果，直接使用
                    val mergedInfo = tempBookInfo!!.merge(searchResults[0])
                    saveBook(mergedInfo)
                } else {
                    // 多个结果，让用户选择
                    showBookSelectionDialog(searchResults)
                }
            } catch (e: Exception) {
                showLoading(false)
                Toast.makeText(
                    this@MainActivity,
                    "${getString(R.string.search_failed)}: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                showScanBackCoverDialog()
            }
        }
    }
    
    /**
     * 显示书籍选择对话框
     */
    private fun showBookSelectionDialog(results: List<BookInfo>) {
        val items = results.map { info ->
            buildString {
                append(info.title ?: "未知书名")
                info.author?.let { append("\n作者: $it") }
                info.publisher?.let { append("\n出版: $it") }
            }
        }.toTypedArray()
        
        AlertDialog.Builder(this)
            .setTitle(R.string.select_book)
            .setItems(items) { _, which ->
                val selectedInfo = results[which]
                val mergedInfo = tempBookInfo!!.merge(selectedInfo)
                saveBook(mergedInfo)
            }
            .setNegativeButton(R.string.manual_scan) { _, _ ->
                showScanBackCoverDialog()
            }
            .show()
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
                    Toast.makeText(this, "已保存: ${book.title}", Toast.LENGTH_SHORT).show()
                }
                tempBookInfo = null
            }
            .setCancelable(false)
            .show()
    }
    
    /**
     * 保存书籍信息
     */
    private fun saveBook(bookInfo: BookInfo) {
        val book = bookInfo.toBook()
        if (book != null) {
            bookList.add(0, book)
            updateBookList()
            Toast.makeText(this, R.string.search_success, Toast.LENGTH_SHORT).show()
        }
        tempBookInfo = null
        isScanningBackCover = false
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

    /**
     * 显示编辑书籍对话框
     */
    private fun showEditBookDialog(book: Book) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_book, null)
        
        // 获取输入框
        val etTitle = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etTitle)
        val etAuthor = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etAuthor)
        val etPublisher = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPublisher)
        val etISBN = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etISBN)
        val etPrice = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etPrice)
        
        // 填充当前值
        etTitle.setText(book.title)
        etAuthor.setText(book.author)
        etPublisher.setText(book.publisher)
        etISBN.setText(book.isbn)
        etPrice.setText(book.price)
        
        // 显示对话框
        AlertDialog.Builder(this)
            .setTitle(R.string.edit_book)
            .setView(dialogView)
            .setPositiveButton(R.string.save) { _, _ ->
                // 获取编辑后的值
                val newTitle = etTitle.text.toString().trim()
                val newAuthor = etAuthor.text.toString().trim()
                val newPublisher = etPublisher.text.toString().trim()
                val newISBN = etISBN.text.toString().trim()
                val newPrice = etPrice.text.toString().trim()
                
                // 验证书名不能为空
                if (newTitle.isEmpty()) {
                    Toast.makeText(this, R.string.title_required, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                // 创建新的Book对象
                val updatedBook = Book(
                    title = newTitle,
                    author = newAuthor,
                    publisher = newPublisher,
                    isbn = newISBN,
                    price = newPrice,
                    scannedTime = book.scannedTime
                )
                
                // 更新列表
                val index = bookList.indexOf(book)
                if (index >= 0) {
                    bookList[index] = updatedBook
                    updateBookList()
                    Toast.makeText(this, R.string.edit_success, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
    
    override fun onBackPressed() {
        if (binding.previewView.visibility == View.VISIBLE) {
            stopCameraPreview()
        } else {
            super.onBackPressed()
        }
    }
}

