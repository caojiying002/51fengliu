package com.jiyingcao.a51fengliu.viewmodel

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
            //CityState::class -> CityState.Loading(loadingType) as T
            //DetailState::class -> DetailState.Loading(loadingType) as T
            // 可以继续添加其他状态类型
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
            //CityState::class -> CityState.Error(message, errorType) as T
            //DetailState::class -> DetailState.Error(message, errorType) as T
            // 可以继续添加其他状态类型
            else -> Error(message, errorType) as T
        }
    }
}

/**
 * LoadingType扩展函数 - 提供类型安全的状态转换
 */
inline fun <reified T : BaseState> LoadingType.toLoadingState(): T {
    return StateFactory.createLoading<T>(this)
}

inline fun <reified T : BaseState> LoadingType.toErrorState(message: String): T {
    return StateFactory.createError<T>(message, this)
}