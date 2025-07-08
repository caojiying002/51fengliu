package com.jiyingcao.a51fengliu.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.common.truth.Truth.assertThat
import com.jiyingcao.a51fengliu.api.response.Merchant
import com.jiyingcao.a51fengliu.data.LoginStateManager
import com.jiyingcao.a51fengliu.repository.MerchantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)  // 自动初始化Mock对象
class MerchantDetailViewModelTest {

    // ========== 测试规则 ==========

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()  // LiveData立即执行

    private val testDispatcher = StandardTestDispatcher()    // 测试协程调度器

    // ========== Mock对象 ==========

    @Mock 
    private lateinit var repository: MerchantRepository
    
    @Mock 
    private lateinit var loginStateManager: LoginStateManager

    // ========== 测试对象 ==========

    private lateinit var viewModel: MerchantDetailViewModel
    private lateinit var loginStateFlow: MutableStateFlow<Boolean>

    // ========== 测试数据 ==========

    private val testMerchantId = "test_merchant_123"
    
    private val testMerchantWithContact = Merchant(
        id = testMerchantId,
        name = "测试商家",
        cityCode = "110100",
        showLv = "3",
        picture = null,
        coverPicture = null,
        intro = "测试介绍",
        desc = "测试描述",
        validStart = null,
        validEnd = null,
        vipProfileStatus = null,
        style = "merchant",
        status = "1",
        contact = "13800138000"  // 有联系方式
    )

    private val testMerchantWithoutContact = Merchant(
        id = testMerchantId,
        name = "测试商家",
        cityCode = "110100", 
        showLv = "3",
        picture = null,
        coverPicture = null,
        intro = "测试介绍",
        desc = "测试描述",
        validStart = null,
        validEnd = null,
        vipProfileStatus = null,
        style = "merchant",
        status = "1",
        contact = null  // 无联系方式
    )

    @Before
    fun setup() {
        // 设置测试协程调度器
        Dispatchers.setMain(testDispatcher)

        // 初始化登录状态流
        loginStateFlow = MutableStateFlow(false)
        whenever(loginStateManager.isLoggedIn).thenReturn(loginStateFlow)
    }

    // ========== 基础功能测试 ==========

    @Test
    fun `初始状态应该正确`() = runTest {
        // Arrange & Act
        createViewModel()

        // Assert
        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.merchant).isNull()
        assertThat(state.isError).isFalse()
        assertThat(state.isLoggedIn).isFalse()
    }

    @Test
    fun `加载详情成功应该更新UI状态`() = runTest {
        // Arrange
        whenever(repository.getMerchantDetail(testMerchantId))
            .thenReturn(flowOf(Result.success(testMerchantWithContact)))

        createViewModel()

        // Act
        viewModel.processIntent(MerchantDetailIntent.LoadDetail)
        testDispatcher.scheduler.advanceUntilIdle()  // 等待协程执行完成

        // Assert
        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.merchant).isEqualTo(testMerchantWithContact)
        assertThat(state.isError).isFalse()
        assertThat(state.showContent).isTrue()
    }

    @Test
    fun `加载详情失败应该显示错误状态`() = runTest {
        // Arrange
        val errorMessage = "网络错误"
        whenever(repository.getMerchantDetail(testMerchantId))
            .thenReturn(flowOf(Result.failure(Exception(errorMessage))))

        createViewModel()

        // Act
        viewModel.processIntent(MerchantDetailIntent.LoadDetail)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.isError).isTrue()
        assertThat(state.merchant).isNull()
        assertThat(state.showFullScreenError).isTrue()
    }

    // ========== 登录状态相关测试 ==========

    @Test
    fun `有联系方式时应该直接显示联系方式`() = runTest {
        // Arrange
        whenever(repository.getMerchantDetail(testMerchantId))
            .thenReturn(flowOf(Result.success(testMerchantWithContact)))

        createViewModel()

        // Act
        viewModel.processIntent(MerchantDetailIntent.LoadDetail)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertThat(state.showContact).isTrue()
        assertThat(state.contactText).isEqualTo("13800138000")
        assertThat(state.contactActionType).isEqualTo(ContactActionType.NONE)
    }

    @Test
    fun `未登录且无联系方式时应该显示登录提示`() = runTest {
        // Arrange
        whenever(repository.getMerchantDetail(testMerchantId))
            .thenReturn(flowOf(Result.success(testMerchantWithoutContact)))

        loginStateFlow.value = false  // 设置未登录状态
        createViewModel()

        // Act
        viewModel.processIntent(MerchantDetailIntent.LoadDetail)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertThat(state.isLoggedIn).isFalse()

        assertThat(state.showContact).isFalse()
        assertThat(state.contactActionType).isEqualTo(ContactActionType.LOGIN)
        assertThat(state.contactPromptMessage).contains("登录")
        assertThat(state.contactActionButtonText).contains("登录")
    }

    @Test
    fun `已登录但无联系方式时应该显示VIP升级提示`() = runTest {
        // Arrange
        whenever(repository.getMerchantDetail(testMerchantId))
            .thenReturn(flowOf(Result.success(testMerchantWithoutContact)))

        loginStateFlow.value = true  // 设置已登录状态
        createViewModel()

        // Act
        viewModel.processIntent(MerchantDetailIntent.LoadDetail)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertThat(state.isLoggedIn).isTrue()

        assertThat(state.showContact).isFalse()
        assertThat(state.contactActionType).isEqualTo(ContactActionType.UPGRADE_VIP)
        assertThat(state.contactPromptMessage).contains("VIP")
        assertThat(state.contactActionButtonText).contains("升级")
    }

    @Test
    fun `登录状态变化时UI应该自动更新`() = runTest {
        // Arrange
        whenever(repository.getMerchantDetail(testMerchantId))
            .thenReturn(flowOf(Result.success(testMerchantWithoutContact)))

        loginStateFlow.value = false  // 初始未登录
        createViewModel()

        viewModel.processIntent(MerchantDetailIntent.LoadDetail)
        testDispatcher.scheduler.advanceUntilIdle()

        // 验证初始状态
        var state = viewModel.uiState.value
        assertThat(state.contactActionType).isEqualTo(ContactActionType.LOGIN)

        // Act - 模拟用户登录
        loginStateFlow.value = true
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert - UI状态应该自动更新
        state = viewModel.uiState.value
        assertThat(state.isLoggedIn).isTrue()
        assertThat(state.contactActionType).isEqualTo(ContactActionType.UPGRADE_VIP)
    }

    // ========== 刷新相关测试 ==========

    @Test
    fun `下拉刷新应该重新加载数据`() = runTest {
        // Arrange
        whenever(repository.getMerchantDetail(testMerchantId))
            .thenReturn(flowOf(Result.success(testMerchantWithContact)))

        createViewModel()

        // Act
        viewModel.processIntent(MerchantDetailIntent.PullToRefresh)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert
        val state = viewModel.uiState.value
        assertThat(state.isRefreshing).isFalse()  // 刷新完成后应该为false
        assertThat(state.merchant).isEqualTo(testMerchantWithContact)
    }

    // ========== 私有方法 ==========

    private fun createViewModel() {
        viewModel = MerchantDetailViewModel(
            merchantId = testMerchantId,
            repository = repository,
            loginStateManager = loginStateManager
        )
    }
}