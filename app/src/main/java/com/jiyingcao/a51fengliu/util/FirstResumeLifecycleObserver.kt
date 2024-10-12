package com.jiyingcao.a51fengliu.util

import android.app.Activity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.core.os.bundleOf
import androidx.savedstate.SavedStateRegistryOwner

/**
 * A lifecycle observer that listens to the first onResume event of a LifecycleOwner.
 *
 * 监听 LifecycleOwner 的第一次 onResume 事件的生命周期观察者。
 *
 * @param listener the listener to be notified when the first onResume event occurs.
 * 当第一次 onResume 事件发生时要通知的监听器
 *
 * @see FirstResumeListener
 * @see addFirstResumeObserver
 */
class FirstResumeLifecycleObserver(private val listener: FirstResumeListener) : DefaultLifecycleObserver {

    /**
     * Interface definition for a callback to be invoked when the first onResume event occurs.
     *
     * 当第一次 onResume 事件发生时要调用的回调的接口定义。
     */
    interface FirstResumeListener {

        /**
         * Called when the first onResume event occurs.
         * 当第一次 onResume 事件发生时调用。
         * @param isRecreate true if the LifecycleOwner is being recreated, false otherwise.
         * 如果 LifecycleOwner 正在重新创建，则为 true，否则为 false
         */
        fun onFirstResume(isRecreate: Boolean)
    }

    /** Internal flag to track if the onResume event is the first one. */
    private var isInitialResume = true

    /** Internal flag to track if the LifecycleOwner is being recreated. */
    private var isRecreate = false

    override fun onCreate(owner: LifecycleOwner) {
        when (owner) {
            is SavedStateRegistryOwner -> {
                isRecreate = owner.savedStateRegistry.consumeRestoredStateForKey(RECREATE_KEY) != null
            }
            is Activity -> {
                // For activities without SavedStateRegistry support
                isRecreate = owner.intent.getBooleanExtra(RECREATE_KEY, false)
            }
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        if (isInitialResume) {
            listener.onFirstResume(isRecreate)
            isInitialResume = false
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        when (owner) {
            is SavedStateRegistryOwner -> {
                if (owner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
                    owner.savedStateRegistry.registerSavedStateProvider(RECREATE_KEY) {
                        bundleOf(RECREATE_KEY to true)
                    }
                } else {
                    resetState()
                }
            }
            is Activity -> {
                // For activities without SavedStateRegistry support
                if (owner.isChangingConfigurations) {
                    owner.intent.putExtra(RECREATE_KEY, true)
                } else {
                    resetState()
                }
            }
            else -> resetState()
        }
    }

    private fun resetState() {
        isInitialResume = true
        isRecreate = false
    }

    companion object {
        private const val RECREATE_KEY = "FirstResumeLifecycleObserver.Recreate"
    }
}

/**
 * Adds a [FirstResumeLifecycleObserver] to the LifecycleOwner that listens to the first onResume event.
 *
 * 为 LifecycleOwner 添加一个监听第一次 onResume 事件的 [FirstResumeLifecycleObserver]。
 */
fun LifecycleOwner.addFirstResumeObserver(listener: FirstResumeLifecycleObserver.FirstResumeListener) {
    lifecycle.addObserver(FirstResumeLifecycleObserver(listener))
}