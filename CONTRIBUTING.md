# 🤝 贡献指南

感谢你考虑为书籍扫描器项目做出贡献！

## 📋 目录

- [行为准则](#行为准则)
- [如何贡献](#如何贡献)
- [开发流程](#开发流程)
- [代码规范](#代码规范)
- [提交规范](#提交规范)
- [问题报告](#问题报告)
- [功能请求](#功能请求)

## 行为准则

参与本项目即表示你同意遵守我们的行为准则：

- 尊重所有贡献者
- 使用友好和包容的语言
- 优雅地接受建设性批评
- 专注于对社区最有利的事情

## 如何贡献

### 🐛 报告 Bug

1. 检查 [Issues](https://github.com/jinhuihu/orc_book/issues) 确保问题未被报告
2. 创建新 Issue，使用 Bug 报告模板
3. 提供详细的复现步骤
4. 附上设备信息、Android 版本、日志等

### ✨ 提交功能

1. 先创建 Issue 讨论新功能
2. 等待维护者反馈
3. Fork 仓库并创建分支
4. 实现功能并提交 PR

### 📝 改进文档

文档改进非常欢迎！包括：
- 修正拼写或语法错误
- 添加使用示例
- 改进 README 或 API 文档
- 翻译文档

## 开发流程

### 1. Fork 仓库

点击仓库页面右上角的 "Fork" 按钮

### 2. 克隆到本地

```bash
git clone https://github.com/your-username/orc_book.git
cd orc_book
```

### 3. 创建特性分支

```bash
git checkout -b feature/amazing-feature
# 或
git checkout -b fix/bug-description
```

分支命名规范：
- `feature/xxx` - 新功能
- `fix/xxx` - Bug 修复
- `docs/xxx` - 文档更新
- `refactor/xxx` - 代码重构
- `test/xxx` - 测试相关

### 4. 开发和测试

```bash
# 确保代码可以编译
./gradlew build

# 运行测试
./gradlew test

# 在真机或模拟器上测试
./gradlew installDebug
```

### 5. 提交更改

```bash
git add .
git commit -m "feat: 添加书籍分类功能"
```

### 6. 推送到 Fork

```bash
git push origin feature/amazing-feature
```

### 7. 创建 Pull Request

1. 访问你的 Fork 仓库页面
2. 点击 "Pull Request" 按钮
3. 填写 PR 描述模板
4. 等待审核

## 代码规范

### Kotlin 代码风格

遵循 [Kotlin 官方代码风格指南](https://kotlinlang.org/docs/coding-conventions.html)

**关键点:**

```kotlin
// ✅ 好的示例
class BookScanner {
    private val books = mutableListOf<Book>()
    
    fun scanBook(image: Bitmap): Book? {
        return ocrManager.recognizeText(image)?.let { title ->
            Book(title = title)
        }
    }
}

// ❌ 不好的示例
class bookscanner {
    var books = mutableListOf<Book>()
    
    fun scanBook(image:Bitmap):Book?{
        val title=ocrManager.recognizeText(image)
        if(title!=null){
            return Book(title=title)
        }
        return null
    }
}
```

### 命名规范

- **类名**: PascalCase (如 `BookAdapter`)
- **函数名**: camelCase (如 `scanBook`)
- **常量**: UPPER_SNAKE_CASE (如 `MAX_BOOKS`)
- **资源ID**: snake_case (如 `btn_scan`, `tv_title`)

### 注释规范

```kotlin
/**
 * 书籍扫描器主类
 * 
 * 负责协调相机、OCR 和 Excel 导出功能
 * 
 * @property books 已扫描的书籍列表
 */
class BookScanner {
    
    /**
     * 扫描书籍封面并识别书名
     * 
     * @param image 书籍封面图片
     * @return 识别成功返回Book对象，失败返回null
     */
    fun scanBook(image: Bitmap): Book? {
        // 使用 OCR 识别文字
        val title = ocrManager.recognizeText(image)
        
        // 如果识别成功，创建 Book 对象
        return title?.let { Book(title = it) }
    }
}
```

### XML 布局规范

```xml
<!-- ✅ 好的示例 -->
<LinearLayout
    android:id="@+id/container"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="16dp">
    
    <TextView
        android:id="@+id/tvTitle"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="@string/title"
        android:textSize="18sp"
        android:textColor="@color/text_primary" />
        
</LinearLayout>
```

## 提交规范

使用 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

### 格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type 类型

- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式（不影响功能）
- `refactor`: 重构（不是新功能也不是修复）
- `perf`: 性能优化
- `test`: 添加测试
- `chore`: 构建过程或辅助工具的变动

### 示例

```bash
# 新功能
git commit -m "feat(ocr): 添加英文书名识别支持"

# Bug 修复
git commit -m "fix(camera): 修复相机预览旋转问题"

# 文档
git commit -m "docs: 更新安装说明"

# 重构
git commit -m "refactor(excel): 优化导出性能"
```

## 问题报告

### Bug 报告模板

```markdown
## 描述
简要描述问题

## 复现步骤
1. 打开应用
2. 点击"扫描书籍"
3. ...

## 期望行为
应该发生什么

## 实际行为
实际发生了什么

## 环境信息
- 设备: Pixel 6
- Android 版本: 13
- 应用版本: 1.0.0

## 截图/日志
如果可能，提供截图或日志
```

## 功能请求

### 功能请求模板

```markdown
## 功能描述
简要描述建议的功能

## 使用场景
为什么需要这个功能？它解决什么问题？

## 期望行为
功能应该如何工作

## 替代方案
是否考虑过其他解决方案？

## 额外信息
其他相关信息
```

## Pull Request 检查清单

提交 PR 前，请确保：

- [ ] 代码遵循项目的代码风格
- [ ] 已添加必要的注释
- [ ] 已更新相关文档
- [ ] 所有测试通过
- [ ] 新功能已添加测试
- [ ] 提交信息遵循规范
- [ ] PR 描述清晰完整

## 审核流程

1. **自动检查**: CI/CD 会自动运行测试和构建
2. **代码审查**: 至少一位维护者会审查代码
3. **反馈处理**: 根据反馈修改代码
4. **合并**: 审核通过后合并到主分支

## 许可

提交代码即表示你同意代码在 MIT 许可证下发布。

## 联系方式

- 📧 Email: 通过 GitHub 联系
- 💬 讨论: [GitHub Discussions](https://github.com/jinhuihu/orc_book/discussions)
- 🐛 问题: [GitHub Issues](https://github.com/jinhuihu/orc_book/issues)

---

再次感谢你的贡献！ 🎉

