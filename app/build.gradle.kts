plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    id("kotlin-kapt") // 暂时保留，直到所有ksp依赖迁移完成
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.dexcount)
}

android {
    namespace = "com.jiyingcao.a51fengliu"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jiyingcao.a51fengliu"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            manifestPlaceholders += mapOf(
                "activityExported" to true,
                "screenOrientation" to "unspecified"  // 允许旋转
            )
        }
        release {
            manifestPlaceholders += mapOf(
                "activityExported" to false,
                "screenOrientation" to "portrait"     // 锁定竖屏
            )
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 正式打release包不应该用debug版本的keystore，这里只是为了能在Android Studio中运行release包
            // 有时候需要查看release包运行起来的一些特性，比如日志打印是否隐藏
            //signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        compose = true
    }

    lint {
        // 启用 Lint 检查
        checkReleaseBuilds = true
        abortOnError = true

        // 设置错误级别
        error.add("DirectGlideStringUsage")

        // HTML 报告
        htmlReport = true
        htmlOutput = file("$buildDir/reports/lint-results.html")

        // XML 报告
        xmlReport = true
        xmlOutput = file("$buildDir/reports/lint-results.xml")

        // 可选：基线文件（用于忽略现有问题）
        // baseline = file("lint-baseline.xml")
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.gridlayout)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    //implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    //implementation(libs.androidx.startup.runtime)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.squareup.retrofit)
    implementation(libs.squareup.retrofit.converter.moshi)
    implementation(libs.squareup.retrofit.converter.gson)   // 待移除
    implementation(libs.squareup.moshi)
    implementation(libs.squareup.moshi.kotlin)
    implementation(libs.squareup.okhttp)
    implementation(libs.squareup.okhttp.logginginterceptor)
    implementation(libs.squareup.okio)
    implementation(libs.gson)   // 待移除
    implementation(libs.bumptech.glide)
    implementation(libs.bumptech.glide.okhttp3integration)
    implementation(libs.coil)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.smartrefreshlayout.kernel)
    implementation(libs.smartrefreshlayout.header)
    implementation(libs.smartrefreshlayout.footer)
    implementation(libs.photoview)
    implementation(libs.flexbox)
    implementation(libs.gsyvideoplayer)
    implementation(libs.gsyvideoplayer.exo2)
    implementation(libs.hilt.android)
    //debugImplementation(libs.squareup.leakcanary.android)
    ksp(libs.hilt.compiler)
    ksp(libs.squareup.moshi.kotlin.codegen)
    ksp(libs.androidx.room.compiler)
    //noinspection KaptUsageInsteadOfKsp
    kapt(libs.bumptech.glide.compiler)

    // Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // 添加自定义 Lint 规则
    lintChecks(project(":lint-rules"))

    // 测试依赖
    testImplementation(libs.junit)
    testImplementation(libs.test.mockito.core)
    testImplementation(libs.test.mockito.kotlin)
    testImplementation(libs.test.arch.core)
    testImplementation(libs.test.coroutines)
    testImplementation(libs.test.truth)
    testImplementation(libs.test.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}