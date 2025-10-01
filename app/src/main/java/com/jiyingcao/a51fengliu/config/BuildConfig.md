# BuildConfig 构建变体配置说明

⚠️ **重要提示：本目录（main/）不应包含 `BuildConfig.kt` 文件**

## 说明

`BuildConfig` 类由构建变体（Build Variants）提供，根据不同的构建类型自动选择对应的实现：

- **Debug 构建**: `app/src/debug/java/com/jiyingcao/a51fengliu/config/BuildConfig.kt`
- **Release 构建**: `app/src/release/java/com/jiyingcao/a51fengliu/config/BuildConfig.kt`

## 配置项

目前提供的编译期常量：

- `IS_DEBUG: Boolean` - 标识当前是否为 Debug 构建
  - Debug 构建时为 `true`
  - Release 构建时为 `false`

## 如何修改

如需添加或修改构建配置：

1. **不要**在 `main/` 目录创建 `BuildConfig.kt`
2. 分别修改 `debug/` 和 `release/` 目录下对应的 `BuildConfig.kt` 文件
3. 使用 `const val` 定义编译期常量，享受编译器优化

## 优势

相比运行时判断 `ApplicationInfo.FLAG_DEBUGGABLE`：

- ✅ 零运行时开销，编译期确定
- ✅ ProGuard/R8 可完全移除 Release 中的调试代码
- ✅ 类型安全，IDE 自动补全支持
- ✅ 无需持有 Context 引用

## 使用示例

```kotlin
object AppConfig {
    object Debug {
        fun isLoggingEnabled(): Boolean = BuildConfig.IS_DEBUG

        fun isHttpLoggingEnabled(): Boolean =
            DEFAULT_HTTP_LOGGING_ENABLED && BuildConfig.IS_DEBUG
    }
}
```
