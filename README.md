# 📚 书籍扫描器 (Book Scanner)

一款简单易用的安卓应用，帮助你快速扫描和记录书籍信息。

## ✨ 功能特性

- 📷 **相机扫描**: 使用相机拍摄书籍封面，自动识别书名
- 🖼️ **相册选择**: 从相册中选择书籍封面图片进行识别
- 🔍 **智能OCR**: 支持中英文书名识别，基于Google ML Kit
- 📝 **批量管理**: 管理扫描的书籍列表，支持添加和删除
- 📊 **Excel导出**: 一键导出书籍列表到Excel文件
- 🎨 **现代UI**: Material Design设计，简洁美观

## 🚀 技术栈

- **开发语言**: Kotlin
- **最低SDK**: Android 7.0 (API 24)
- **目标SDK**: Android 14 (API 34)
- **架构**: MVVM + 协程
- **主要库**:
  - CameraX: 相机功能
  - Google ML Kit: OCR文字识别
  - Apache POI: Excel文件生成
  - Material Components: UI组件

## 📦 安装说明

### 方式一: 下载APK安装

1. 访问 [Releases](https://github.com/jinhuihu/orc_book/releases) 页面
2. 下载最新的 `app-debug.apk` 文件
3. 在安卓手机上安装APK
4. 授予相机和存储权限

### 方式二: 从源码构建

```bash
# 克隆仓库
git clone https://github.com/jinhuihu/orc_book.git
cd orc_book

# 使用 Gradle 构建
./gradlew assembleDebug

# APK 文件位置: app/build/outputs/apk/debug/app-debug.apk
```

## 🎯 使用说明

1. **扫描书籍**
   - 点击"扫描书籍"按钮打开相机
   - 将相机对准书籍封面
   - 点击屏幕拍照，自动识别书名

2. **选择图片**
   - 点击"选择图片"按钮
   - 从相册中选择书籍封面照片
   - 自动识别书名并添加到列表

3. **导出Excel**
   - 点击"导出Excel"按钮
   - 生成的Excel文件保存在 `/Documents/BookScanner/` 目录
   - 文件包含：序号、书名、扫描时间

4. **管理列表**
   - 点击书籍右侧的删除按钮删除单本
   - 点击顶部菜单的清空按钮清空所有书籍

## 🔧 权限说明

应用需要以下权限：

- **相机权限**: 用于拍摄书籍封面
- **存储权限**: 用于读取相册图片和保存Excel文件

所有权限都会在使用时请求，不会获取额外权限。

## 🏗️ 项目结构

```
app/
├── src/main/
│   ├── java/com/bookscanner/app/
│   │   ├── MainActivity.kt           # 主活动
│   │   ├── adapter/
│   │   │   └── BookAdapter.kt        # 列表适配器
│   │   ├── model/
│   │   │   └── Book.kt               # 数据模型
│   │   └── util/
│   │       ├── CameraManager.kt      # 相机管理
│   │       ├── OCRManager.kt         # OCR识别
│   │       ├── ExcelExporter.kt      # Excel导出
│   │       ├── PermissionManager.kt  # 权限管理
│   │       └── ImageProcessor.kt     # 图片处理
│   ├── res/
│   │   ├── layout/                   # 布局文件
│   │   ├── values/                   # 资源文件
│   │   └── menu/                     # 菜单文件
│   └── AndroidManifest.xml           # 清单文件
└── build.gradle                      # 构建配置
```

## 🔄 自动打包

本项目使用 GitHub Actions 自动构建和发布：

- ✅ 推送代码自动触发构建
- ✅ 自动生成 Debug 和 Release APK
- ✅ 自动创建 GitHub Release
- ✅ 自动上传 APK 到 Release

### 手动触发构建

1. 进入 GitHub 仓库
2. 点击 "Actions" 标签
3. 选择 "Android CI/CD" 工作流
4. 点击 "Run workflow" 按钮

## 📝 开发说明

### 环境要求

- Android Studio Arctic Fox 或更高版本
- JDK 17
- Gradle 8.0+

### 构建命令

```bash
# 构建 Debug 版本
./gradlew assembleDebug

# 构建 Release 版本
./gradlew assembleRelease

# 运行测试
./gradlew test

# 清理构建
./gradlew clean
```

## 🐛 问题反馈

如果遇到问题或有功能建议，请提交 [Issue](https://github.com/jinhuihu/orc_book/issues)。

## 📄 许可证

本项目采用 MIT 许可证，详见 [LICENSE](LICENSE) 文件。

## 🤝 贡献

欢迎提交 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 👨‍💻 作者

- GitHub: [@jinhuihu](https://github.com/jinhuihu)

## 🙏 致谢

- [Google ML Kit](https://developers.google.com/ml-kit) - OCR识别
- [Apache POI](https://poi.apache.org/) - Excel处理
- [CameraX](https://developer.android.com/training/camerax) - 相机功能
- [Material Components](https://material.io/develop/android) - UI组件

---

⭐ 如果这个项目对你有帮助，请给它一个星标！

