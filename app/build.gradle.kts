plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    id("kotlin-kapt") // 暂时保留，直到所有ksp依赖迁移完成
}

android {
    namespace = "com.jiyingcao.a51fengliu"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jiyingcao.a51fengliu"
        minSdk = 24
        targetSdk = 32
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
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.material)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment.ktx)
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
    implementation(libs.squareup.retrofit)
    implementation(libs.squareup.retrofit.converter.gson)
    implementation(libs.squareup.okhttp)
    implementation(libs.squareup.okhttp.logginginterceptor)
    implementation(libs.squareup.okio)
    implementation(libs.gson)
    implementation(libs.bumptech.glide)
    implementation(libs.bumptech.glide.okhttp3integration)
    implementation(libs.coil)
    implementation(libs.coil.network.okhttp)
    implementation(libs.smartrefreshlayout.kernel)
    implementation(libs.smartrefreshlayout.header)
    implementation(libs.smartrefreshlayout.footer)
    implementation(libs.brvah)
    implementation(libs.photoview)
    implementation(libs.flexbox)
    ksp(libs.androidx.room.compiler)
    kapt(libs.bumptech.glide.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}