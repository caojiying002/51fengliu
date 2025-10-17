# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 核心构建命令

```bash
# 构建和测试命令
./gradlew build                    # 完整构建（包含测试）
./gradlew testDebugUnitTest       # 运行单元测试
./gradlew lint                    # 运行Lint检查（包含自定义规则）
./gradlew assembleDebug           # 构建Debug APK
./gradlew connectedAndroidTest    # 运行设备测试（需要连接设备）

# 单独运行特定测试
./gradlew test --tests "com.jiyingcao.a51fengliu.viewmodel.*Test"

# 清理重建
./gradlew clean build
```

## 项目架构概览

这是一个基于 **MVVM + MVI** 模式的Android应用，使用Kotlin Flow进行响应式编程。

### 技术栈组合
- **传统View系统** + **Jetpack Compose** 混合架构
- **Hilt** 依赖注入
- **Retrofit + Gson** 网络层（逐步迁移到Moshi）
- **Room** 数据库
- **DataStore** 数据持久化
- **Coil** 图片加载（统一使用Coil）
- **自定义Lint规则** 强制代码规范

### 核心架构模式

**数据层设计**:
- `TokenManager`: 单例Token管理，基于DataStore持久化
- `LoginStateManager`: 双流设计，分离持久状态(StateFlow)和事件(SharedFlow)
- `RemoteLoginManager`: 全局会话管理，原子状态处理

**Repository模式**:
- `BaseRepository.apiCall()`: 标准化Flow-based API调用封装
- 所有Repository使用Flow流进行响应式数据流
- **分页数据去重**: 返回 `PageData<T>` 的方法必须使用 `PageDataDeduplicator` 去重，防止 LazyColumn key 冲突崩溃
  - 参考：`MerchantRepository`、`RecordRepository`、`StreetRepository`
- **多态响应特殊处理**: 登录和举报接口的 `data` 字段类型根据业务情况不同，**无法使用** `apiCall()`，必须手动处理
  - `UserRepository.login()`: `LoginData.Success`(token字符串) 或 `LoginData.Error`(字段错误Map)
  - `RecordRepository.report()`: `ReportData.Success`(空字符串) 或 `ReportData.Error`(字段错误Map)
  - 参见 `LoginData.kt` 和 `ReportData.kt` 的注释和响应示例
  - ViewModel层判断字段验证错误：检查 `result.data is Map<*, *>`（不需要检查code值）

**ViewModel层(MVI)**:
- 单一`UiState`数据类包含所有UI状态
- 密封`Intent`类表示用户行为
- 参考`MerchantDetailViewModel`的`ContactDisplayState`管理模式

**网络层**:
- `@TokenPolicy` 注解控制API方法认证策略
- `AuthInterceptor` 自动Token注入
- `ApiResponse<T>` 统一响应处理

**API响应类型防御式设计**:
```kotlin
// ✅ 正确做法 - 防御性类型定义
data class Profile(
    val id: String?,             // 字符串类型定义为String? - 防止后端漏传字段导致崩溃
    val name: String?,
    val score: String?,          // Int - 注释标注实际类型
    val expiredAt: String?,      // Long - 防止后端类型变更导致崩溃
    val isVip: Boolean?          // Boolean可空
)

// ❌ 错误做法 - 直接使用基本类型
data class Profile(
    val score: Int,              // 后端改为字符串时会崩溃
    val expiredAt: Long          // 后端返回null时会崩溃
)
```

**规则说明**:
- 字符串类型统一定义为 `String?`
- 非字符串基本类型（Int/Long/Double/Float等）统一定义为 `String?`
- Boolean类型定义为 `Boolean?`
- 在注释中标注实际类型（如 `// Int`、`// Long`）以保持可读性
- 客户端在使用时进行类型转换和验证
- 防止后端类型变更或异常数据导致应用崩溃

### 图片加载策略

**统一使用 Coil**:
- View系统和Compose系统统一使用 **Coil** 进行图片加载
- Compose中使用 `AsyncImage` composable
- View中使用 `ImageView.load()` 扩展函数
- 使用 `HostInvariantKeyer` 实现主机无关缓存键，确保BASE_IMAGE_URL变更时缓存不失效

**使用示例**:
```kotlin
// Compose中加载图片
AsyncImage(
    model = imageUrl,
    contentDescription = null
)

// View中加载图片
imageView.load(imageUrl)
```

### 状态管理核心

**登录状态集成**:
- ViewModel观察`LoginStateManager.isLoggedIn` StateFlow
- 使用`collectLogin()`扩展函数自动状态更新
- 登录事件触发UI更新，无需手动状态管理

**MVI状态更新模式**:
```kotlin
// 项目中通用的ViewModel状态更新模式
_uiState.update { currentState ->
    currentState.copy(isLoading = false, data = newData)
}
```

**异常处理**:
- 全局错误码（如1003异地登录）由 `BusinessErrorInterceptor` 在网络层统一拦截处理
- 领域特定异常在`/domain/exception/`目录
- **多态响应的字段验证错误**：登录/举报接口返回 `ApiResult.ApiError`，`data` 字段包含 `Map<String, String>` 错误详情

### 混合UI架构

**View + Compose共存**:
- 传统Activity/Fragment作为主要导航
- `MerchantDetailComposeActivity` Compose集成示例
- `/ui/theme/`目录下Material 3自定义主题

**自定义组件**:
- `StatefulLayout`: 加载/错误/内容状态管理
- `TitleBarBack`: 可复用工具栏组件
- Compose组件在`/ui/components/`目录

**UI风格约定**:
- 🎨 **国内APP风格优先**：本项目面向国内用户，UI风格应参考国内主流APP，而非Material Design
- ⚠️ **限制Material组件使用**（Compose部分）：
  - ✅ 允许使用骨架级Material组件（如Scaffold、LazyColumn等不影响视觉的基础组件）
  - ❌ 避免使用有明显Material风格的组件（如FloatingActionButton、Material Card的elevation等）
  - ❌ 禁用默认的水波纹点击效果（ripple effect）
- 🎯 **使用Foundation组件**（Compose部分）：优先使用`androidx.compose.foundation`包中的基础组件
  - 使用`Box`、`Column`、`Row`等布局组件
  - 使用`Canvas`、`Shape`自定义绘制和外观
  - 使用`Modifier.clickable(indication = null)`移除点击水波纹
- 🔧 **自定义组件**：通过`/ui/components/`目录下的自定义组件实现国内APP常见UI模式
  - 扁平化设计、简洁的分割线
  - 自定义按钮样式（无elevation、简洁背景色）
  - 国内常见的列表样式和卡片样式

## 开发约定

**文件组织原则**:
- ViewModel处理所有业务逻辑和状态管理
- Activity/Fragment作为轻量级UI控制器

**配置管理**:
- `AppConfig`: 分环境配置管理（Debug/Release变体）
- `gradle/libs.versions.toml` 版本目录统一依赖管理
- 构建变体控制调试功能和屏幕方向

**认证流程**:
- 登录状态变化自动传播到所有观察的ViewModel
- VIP状态通过`LoginStateManager.isVip`派生属性检查
- 网络层透明处理Token刷新

**自定义Lint规则**:
- `/lint-rules`模块用于添加自定义代码规范检查
- 通过`lintChecks(project(":lint-rules"))`集成
- 运行`./gradlew lint`应用自定义规则

## 开发注意事项

**现代API要求** - 严禁使用已弃用的API:
- 使用`ActivityResultContracts`处理Activity结果和权限请求
- 使用`OnBackPressedDispatcher.addCallback()`处理返回导航
- 强制使用View Binding，禁止`findViewById()`
- 使用Kotlin协程配合`viewModelScope`或`lifecycleScope`
- RecyclerView使用`ListAdapter`配合`DiffUtil.ItemCallback`
- Fragment导航使用Navigation Component配合SafeArgs

**Compose集成**:
- 新界面优先使用Jetpack Compose
- 使用`ActivityResultContracts`处理View和Compose界面间导航
- 状态提升模式，ViewModel为Composable提供状态
- UI风格遵循国内APP规范，Material 3主题仅作为基础，配合自定义配色方案（详见"UI风格约定"章节）