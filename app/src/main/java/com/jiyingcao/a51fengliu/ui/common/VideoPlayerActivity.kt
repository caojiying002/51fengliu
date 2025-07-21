package com.jiyingcao.a51fengliu.ui.common

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import com.jiyingcao.a51fengliu.databinding.ActivityVideoPlayerBinding
import com.jiyingcao.a51fengliu.ui.base.BaseActivity
import com.jiyingcao.a51fengliu.util.setContentViewWithSystemBarPaddings
import com.shuyu.gsyvideoplayer.GSYVideoManager
import com.shuyu.gsyvideoplayer.builder.GSYVideoOptionBuilder
import com.shuyu.gsyvideoplayer.cache.CacheFactory
import com.shuyu.gsyvideoplayer.listener.GSYSampleCallBack
import com.shuyu.gsyvideoplayer.listener.LockClickListener
import com.shuyu.gsyvideoplayer.player.PlayerFactory
import com.shuyu.gsyvideoplayer.utils.OrientationUtils
import tv.danmaku.ijk.media.exo2.Exo2PlayerManager
import tv.danmaku.ijk.media.exo2.ExoPlayerCacheManager

class VideoPlayerActivity : BaseActivity() {
    private lateinit var binding: ActivityVideoPlayerBinding
    private var orientationUtils: OrientationUtils? = null
    
    // Default test URL for m3u8 stream
    private var videoUrl = "https://jdforrepam.com/api/v1/movies/ttm3u8/preview/331749/0/720p.m3u8?sign=a8sqb36ru8.04d15e191433c1c99500caf79f62cc25&t=1746348915"
    private var videoTitle = "M3U8 Video Stream"
    
    companion object {
        private const val EXTRA_VIDEO_URL = "extra_video_url"
        private const val EXTRA_VIDEO_TITLE = "extra_video_title"
        
        fun start(context: Context, videoUrl: String, videoTitle: String = "Video") {
            val intent = Intent(context, VideoPlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_URL, videoUrl)
                putExtra(EXTRA_VIDEO_TITLE, videoTitle)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentViewWithSystemBarPaddings(binding.root)

        // 确保活动可以处理配置更改（屏幕旋转）
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Get video URL from intent or use default
        videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL) ?: videoUrl
        videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: videoTitle

        // Configure EXOPlayer for m3u8
        PlayerFactory.setPlayManager(Exo2PlayerManager::class.java)
        CacheFactory.setCacheManager(ExoPlayerCacheManager::class.java)

        // 处理返回手势
        setupBackPressedCallback()
        
        setupVideoPlayer()
    }

    private fun setupBackPressedCallback() {
        // 创建一个回调来处理返回手势和返回按钮
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 检查播放器是否处于全屏模式
                if (binding.videoPlayer.isIfCurrentIsFullscreen) {
                    // 如果是全屏，先退回到竖屏
                    binding.videoPlayer.onBackFullscreen()
                } else {
                    // 如果已经是竖屏，则退出活动
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun setupVideoPlayer() {
        // Setup orientation detection
        orientationUtils = OrientationUtils(this, binding.videoPlayer).apply { 
            isEnable = true
        }
        
        // Configure video player UI
        binding.videoPlayer.titleTextView.visibility = View.VISIBLE
        
        // 设置播放器配置以支持手势
        binding.videoPlayer.setIsTouchWiget(true) // 允许触摸控制
        binding.videoPlayer.setBackFromFullScreenListener {
            // 从全屏返回时的回调
            orientationUtils?.backToProtVideo()
            true // 返回true表示已处理该事件
        }
        
        // Configure GSYVideoPlayer with Exo2 for m3u8 streaming
        GSYVideoOptionBuilder()
            .setIsTouchWiget(true)
            .setRotateViewAuto(false)
            .setLockLand(false)
            .setAutoFullWithSize(true)
            .setShowFullAnimation(false)
            .setNeedLockFull(true)
            .setCacheWithPlay(false) // Don't cache m3u8 streams
            .setVideoTitle(videoTitle)
            .setVideoAllCallBack(object : GSYSampleCallBack() {
                override fun onPrepared(url: String?, vararg objects: Any?) {
                    super.onPrepared(url, *objects)
                    // When video starts playing
                    orientationUtils?.isEnable = true
                }
                
                override fun onQuitFullscreen(url: String?, vararg objects: Any?) {
                    super.onQuitFullscreen(url, *objects)
                    // Handle exit fullscreen
                    orientationUtils?.backToProtVideo()
                }
                
                override fun onEnterFullscreen(url: String?, vararg objects: Any?) {
                    super.onEnterFullscreen(url, *objects)
                    // 处理进入全屏模式
                }
            })
            .setLockClickListener(object : LockClickListener {
                override fun onClick(view: View?, lock: Boolean) {
                    // Handle screen rotation lock
                    orientationUtils?.isEnable = !lock
                }
            })
            .build(binding.videoPlayer)

        // 设置全屏按钮的监听器
        binding.videoPlayer.fullscreenButton.setOnClickListener {
            // 切换全屏模式
            orientationUtils?.resolveByClick()
        }

        // Set the URL and start playing
        binding.videoPlayer.setUp(videoUrl, true, videoTitle)
        
        // Start playback
        binding.videoPlayer.startPlayLogic()
    }

    override fun onPause() {
        super.onPause()
        binding.videoPlayer.onVideoPause()
    }

    override fun onResume() {
        super.onResume()
        binding.videoPlayer.onVideoResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        GSYVideoManager.releaseAllVideos()
        orientationUtils?.releaseListener()
        binding.videoPlayer.release()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // 处理方向变化，确保UI正确更新
        binding.videoPlayer.onConfigurationChanged(this, newConfig, orientationUtils, true, true)
    }
}