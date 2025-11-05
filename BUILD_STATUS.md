# 🚀 构建状态和下载指南

## 📊 当前构建状态

代码已成功推送到 GitHub！GitHub Actions 正在自动构建 APK。

## 🔍 查看构建进度

### 方法一：通过 Actions 页面查看

1. 访问：https://github.com/jinhuihu/orc_book/actions
2. 你会看到 "Android CI/CD" 工作流正在运行
3. 点击最新的运行记录查看详细进度

### 方法二：通过命令行查看（需要 GitHub CLI）

```bash
# 安装 GitHub CLI（如果没有）
brew install gh

# 登录
gh auth login

# 查看工作流状态
gh run list --repo jinhuihu/orc_book

# 查看最新运行的详细信息
gh run view --repo jinhuihu/orc_book
```

## 📦 下载构建的 APK

### 从 Actions Artifacts 下载（构建完成后）

1. 进入 https://github.com/jinhuihu/orc_book/actions
2. 点击最新的成功运行记录（绿色✓）
3. 滚动到页面底部的 "Artifacts" 部分
4. 下载以下文件：
   - **app-debug** - 调试版本（推荐测试使用）
   - **app-release-unsigned** - 发布版本（未签名）

### 从 Releases 下载（自动创建）

1. 访问 https://github.com/jinhuihu/orc_book/releases
2. 下载最新版本的 APK 文件
3. 直接安装到安卓手机

## 📱 安装 APK

### 方法一：直接在手机上安装

1. 将下载的 APK 文件传输到手机
2. 在手机的文件管理器中找到 APK
3. 点击安装
4. 如果提示"未知来源"，允许安装

### 方法二：通过 ADB 安装

```bash
# 确保手机已连接并开启 USB 调试
adb devices

# 安装 APK
adb install path/to/app-debug.apk

# 如果需要覆盖安装
adb install -r path/to/app-debug.apk
```

## 🔄 重新触发构建

### 方法一：推送新代码

```bash
# 修改代码后
git add .
git commit -m "feat: 添加新功能"
git push origin main
```

### 方法二：手动触发

1. 访问 https://github.com/jinhuihu/orc_book/actions
2. 点击左侧的 "Android CI/CD"
3. 点击右侧的 "Run workflow" 按钮
4. 选择分支（main）
5. 点击绿色的 "Run workflow" 按钮

## 📝 构建产物说明

| 文件名 | 说明 | 用途 |
|--------|------|------|
| app-debug.apk | 调试版本 | 开发测试使用，包含调试信息 |
| app-release-unsigned.apk | 未签名发布版 | 需要签名后才能发布到应用商店 |

## ⏱️ 构建时间

- 首次构建：约 5-8 分钟（需要下载依赖）
- 后续构建：约 3-5 分钟（使用缓存）

## 🔔 构建通知

GitHub 会在构建完成或失败时发送邮件通知到你的 GitHub 注册邮箱。

## 🐛 构建失败？

如果构建失败，请：

1. 查看 Actions 页面的错误日志
2. 常见问题：
   - **依赖下载失败**：重新运行工作流
   - **Gradle 版本问题**：检查 `gradle-wrapper.properties`
   - **权限问题**：确保 `gradlew` 有执行权限

3. 如果无法解决，提交 Issue：https://github.com/jinhuihu/orc_book/issues

## 📊 查看构建统计

访问仓库的 Actions 标签可以看到：
- ✅ 成功构建次数
- ❌ 失败构建次数
- ⏱️ 平均构建时间
- 📦 构建历史记录

## 🎉 首次构建成功后

1. 下载 APK 到手机
2. 安装应用
3. 授予相机和存储权限
4. 开始扫描书籍！

---

**提示**：构建大约需要 3-8 分钟，请耐心等待。你可以先去喝杯咖啡 ☕️

