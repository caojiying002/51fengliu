package com.jiyingcao.a51fengliu.viewmodel

/**
 * MVI状态管理统一定义
 * 包含加载类型、状态接口、通用状态实现和状态工厂
 */

// ========== 加载类型定义 ==========

/**
 * 通用加载类型枚举
 * 统一管理所有ViewModel的加载场景
 */
enum class LoadingType {
    FULL_SCREEN,     // 全屏加载
    PULL_TO_REFRESH, // 下拉刷新
    LOAD_MORE,       // 加载更多
    OVERLAY          // 覆盖层加载
}

// ========== 状态基础接口 ==========

/**
 * 通用状态基接口
 * 所有ViewModel的State都应该实现这个接口
 */
interface BaseState

/**
 * 支持加载状态的接口
 */
interface LoadingState : BaseState {
    val loadingType: LoadingType
}

/**
 * 支持错误状态的接口
 */
interface ErrorState : BaseState {
    val message: String
    val errorType: LoadingType
}

// ========== 通用状态实现 ==========

/**
 * 通用加载状态
 */
data class Loading(override val loadingType: LoadingType) : LoadingState

/**
 * 通用错误状态
 */
data class Error(
    override val message: String,
    override val errorType: LoadingType
) : ErrorState

/**
 * 通用成功状态（无数据）
 */
object Success : BaseState

/**
 * 通用初始状态
 */
object Init : BaseState

// ========== 状态工厂 ==========

/**
 * 状态工厂类 - 统一创建状态实例
 */
object StateFactory {
    
    /**
     * 创建加载状态
     */
    inline fun <reified T : BaseState> createLoading(loadingType: LoadingType): T {
        return when (T::class) {
            MerchantDetailState::class -> MerchantDetailState.Loading(loadingType) as T
            //FavoriteState::class -> FavoriteState.Loading(loadingType) as T
            // 必须手动添加其他状态类型 TODO 待优化
            else -> Loading(loadingType) as T
        }
    }
    
    /**
     * 创建错误状态
     */
    inline fun <reified T : BaseState> createError(
        message: String,
        errorType: LoadingType
    ): T {
        return when (T::class) {
            MerchantDetailState::class -> MerchantDetailState.Error(message, errorType) as T
            //FavoriteState::class -> FavoriteState.Error(message, errorType) as T
            // 必须手动添加其他状态类型 TODO 待优化
            else -> Error(message, errorType) as T
        }
    }
}

// ========== 扩展函数 ==========

/**
 * LoadingType扩展函数 - 提供类型安全的状态转换
 */
inline fun <reified T : BaseState> LoadingType.toLoadingState(): T {
    return StateFactory.createLoading<T>(this)
}

inline fun <reified T : BaseState> LoadingType.toErrorState(message: String): T {
    return StateFactory.createError<T>(message, this)
}

// ========== 使用示例和文档 ==========

/**
 * ## 使用示例
 * 
 * ### 1. 定义具体状态类
 * ```kotlin
 * sealed class YourState : BaseState {
 *     object Init : YourState()
 *     data class Loading(override val loadingType: LoadingType) : YourState(), LoadingState
 *     data class Success(val data: YourData) : YourState()
 *     data class Error(
 *         override val message: String,
 *         override val errorType: LoadingType
 *     ) : YourState(), ErrorState
 * }
 * ```
 * 
 * ### 2. 在ViewModel中使用
 * ```kotlin
 * private fun fetchData(loadingType: LoadingType = LoadingType.FULL_SCREEN) {
 *     _state.value = loadingType.toLoadingState<YourState>()
 *     // ... 网络请求
 *     // 成功：_state.value = YourState.Success(data)
 *     // 失败：_state.value = loadingType.toErrorState<YourState>(errorMessage)
 * }
 * ```
 * 
 * ### 3. 在Activity/Fragment中处理
 * ```kotlin
 * when (state) {
 *     is YourState.Loading -> handleLoadingState(state.loadingType)
 *     is YourState.Error -> handleErrorState(state.message, state.errorType)
 *     // ...
 * }
 * ```
 */