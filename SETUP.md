# 🚀 快速开始指南

## 📋 前置要求

1. **Android Studio**: 下载并安装 [Android Studio](https://developer.android.com/studio)
2. **JDK 17**: Android Studio 通常自带，或单独安装
3. **Git**: 用于版本控制

## 🔧 初始化项目

### 方法一: 使用 Android Studio（推荐）

1. **克隆仓库**
   ```bash
   git clone https://github.com/jinhuihu/orc_book.git
   cd orc_book
   ```

2. **打开项目**
   - 启动 Android Studio
   - 选择 "Open an Existing Project"
   - 选择克隆的 `orc_book` 目录

3. **同步 Gradle**
   - Android Studio 会自动提示同步 Gradle
   - 点击 "Sync Now" 按钮
   - 等待依赖下载完成

4. **运行应用**
   - 连接安卓设备或启动模拟器
   - 点击绿色的 "Run" 按钮 ▶️
   - 选择目标设备
   - 等待应用安装并启动

### 方法二: 使用命令行

1. **克隆仓库**
   ```bash
   git clone https://github.com/jinhuihu/orc_book.git
   cd orc_book
   ```

2. **初始化 Gradle Wrapper**
   ```bash
   # 如果 gradlew 文件不存在或损坏，运行：
   gradle wrapper
   
   # 如果已有 gradlew，直接构建：
   chmod +x gradlew
   ./gradlew build
   ```

3. **构建 APK**
   ```bash
   # 构建 Debug 版本
   ./gradlew assembleDebug
   
   # APK 位置: app/build/outputs/apk/debug/app-debug.apk
   ```

4. **安装到设备**
   ```bash
   # 确保设备已连接并开启 USB 调试
   ./gradlew installDebug
   ```

## 📱 在手机上安装

### 启用开发者选项

1. 打开"设置" > "关于手机"
2. 连续点击"版本号"7次，启用开发者选项
3. 返回设置 > "开发者选项"
4. 开启 "USB 调试"

### 安装 APK

**方法一: USB 连接安装**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

**方法二: 直接在手机上安装**
1. 将 APK 文件传输到手机
2. 在手机上找到 APK 文件
3. 点击安装
4. 允许"未知来源"安装（如果需要）

## 🌐 GitHub 远程打包

### 配置仓库

1. **推送代码到 GitHub**
   ```bash
   git add .
   git commit -m "Initial commit: 书籍扫描器"
   git remote add origin https://github.com/jinhuihu/orc_book.git
   git branch -M main
   git push -u origin main
   ```

2. **GitHub Actions 自动构建**
   - 推送代码后，GitHub Actions 会自动触发构建
   - 访问仓库的 "Actions" 标签查看构建进度
   - 构建成功后，APK 会上传到 Artifacts
   - 推送到 main 分支还会自动创建 Release

3. **手动触发构建**
   - 进入 GitHub 仓库
   - 点击 "Actions" 标签
   - 选择 "Android CI/CD" 工作流
   - 点击 "Run workflow" 按钮

### 下载构建产物

**从 Actions Artifacts 下载:**
1. 进入 "Actions" 标签
2. 点击最新的工作流运行
3. 滚动到底部的 "Artifacts" 部分
4. 下载 `app-debug` 或 `app-release-unsigned`

**从 Releases 下载:**
1. 进入仓库的 "Releases" 页面
2. 下载最新版本的 APK
3. 直接安装到手机

## 🔑 签名发布版（可选）

如果要发布到应用商店，需要签名：

1. **生成密钥库**
   ```bash
   keytool -genkey -v -keystore my-release-key.jks \
     -keyalg RSA -keysize 2048 -validity 10000 \
     -alias my-key-alias
   ```

2. **配置签名**
   
   在 `app/build.gradle` 中添加：
   ```gradle
   android {
       signingConfigs {
           release {
               storeFile file("path/to/my-release-key.jks")
               storePassword "your-store-password"
               keyAlias "my-key-alias"
               keyPassword "your-key-password"
           }
       }
       buildTypes {
           release {
               signingConfig signingConfigs.release
               // ... 其他配置
           }
       }
   }
   ```

3. **构建签名版本**
   ```bash
   ./gradlew assembleRelease
   ```

## 🐛 常见问题

### Gradle 同步失败

**问题**: 无法下载依赖或同步超时

**解决**:
```bash
# 1. 清理项目
./gradlew clean

# 2. 清理 Gradle 缓存
rm -rf ~/.gradle/caches/

# 3. 重新同步
./gradlew build --refresh-dependencies
```

### ML Kit 下载失败

**问题**: OCR 模型下载失败

**解决**:
- 确保设备已连接互联网
- 首次使用时，应用会自动下载 ML Kit 模型（约 30MB）
- 可以在 WiFi 环境下先运行一次应用

### 权限被拒绝

**问题**: 相机或存储权限被拒绝

**解决**:
- 进入"设置" > "应用" > "书籍扫描器"
- 手动开启相机和存储权限

### APK 安装失败

**问题**: 提示"应用未安装"或"解析包时出现问题"

**解决**:
- 确保手机系统版本 >= Android 7.0
- 检查 APK 文件是否完整下载
- 卸载旧版本后重新安装
- 开启"允许未知来源"安装

## 📞 获取帮助

- 📝 [提交 Issue](https://github.com/jinhuihu/orc_book/issues)
- 📖 [查看文档](README.md)
- 💬 [讨论区](https://github.com/jinhuihu/orc_book/discussions)

## 🎉 开始使用

安装完成后：

1. 打开应用
2. 授予相机和存储权限
3. 点击"扫描书籍"开始使用
4. 扫描多本书后，点击"导出Excel"
5. 在 `/Documents/BookScanner/` 目录查看导出的文件

享受使用吧！ 📚✨

