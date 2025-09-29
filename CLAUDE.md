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
- **Retrofit + Moshi** 网络层（逐步从Gson迁移）
- **Room** 数据库
- **DataStore** 数据持久化
- **Coil + Glide** 图片加载（并存，自定义Lint规则约束）
- **自定义Lint规则** 强制代码规范

### 核心架构模式

**数据层设计**:
- `TokenManager`: 单例Token管理，基于DataStore持久化
- `LoginStateManager`: 双流设计，分离持久状态(StateFlow)和事件(SharedFlow)
- `RemoteLoginManager`: 全局会话管理，原子状态处理

**Repository模式**:
- `BaseRepository.apiCall()`: 标准化Flow-based API调用封装
- 所有Repository使用Flow流进行响应式数据流

**ViewModel层(MVI)**:
- 单一`UiState`数据类包含所有UI状态
- 密封`Intent`类表示用户行为
- 参考`MerchantDetailViewModel`的`ContactDisplayState`管理模式

**网络层**:
- `@TokenPolicy` 注解控制API方法认证策略
- `AuthInterceptor` 自动Token注入
- `ApiResponse<T>` 统一响应处理

### 图片加载策略

**强制使用模式**（自定义Lint规则约束）:
```kotlin
// ✅ 正确用法 - 使用HostInvariantGlideUrl
AppConfig.Network.createImageUrl(imagePath)

// ❌ 禁止用法 - 直接字符串URL
Glide.with(context).load("https://example.com/image.jpg")
```

**双引擎并存**:
- **Glide**: 传统View系统，复杂加载逻辑
- **Coil**: Jetpack Compose，现代化API

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
- `BaseViewModel.handleFailure()` 扩展统一错误处理
- `RemoteLoginException` 触发全局登出流程
- 领域特定异常在`/domain/exception/`目录

### 混合UI架构

**View + Compose共存**:
- 传统Activity/Fragment作为主要导航
- `MerchantDetailComposeActivity` Compose集成示例
- `/ui/theme/`目录下Material 3自定义主题

**自定义组件**:
- `StatefulLayout`: 加载/错误/内容状态管理
- `TitleBarBack`: 可复用工具栏组件
- Compose组件在`/ui/components/`目录

## 开发约定

**文件组织原则**:
- ViewModel处理所有业务逻辑和状态管理
- Activity/Fragment作为轻量级UI控制器
- 自定义Lint规则强制`HostInvariantGlideUrl`使用规范

**配置管理**:
- `AppConfig`: 分环境配置管理（Debug/Release变体）
- `gradle/libs.versions.toml` 版本目录统一依赖管理
- 构建变体控制调试功能和屏幕方向

**认证流程**:
- 登录状态变化自动传播到所有观察的ViewModel
- VIP状态通过`LoginStateManager.isVip`派生属性检查
- 网络层透明处理Token刷新

**自定义Lint规则**:
- `/lint-rules`模块强制Glide URL处理模式
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
- Material 3主题配合自定义配色方案