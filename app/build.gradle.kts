plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    id("kotlin-kapt")
}

android {
    namespace = "com.jiyingcao.a51fengliu"
    compileSdk = 35

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
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
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
    implementation(libs.jsoup)
    implementation(libs.smartrefreshlayout.kernel)
    implementation(libs.smartrefreshlayout.header)
    implementation(libs.smartrefreshlayout.footer)
    implementation(libs.brvah)
    implementation(libs.photoview)
    implementation(libs.flexbox)
    kapt(libs.bumptech.glide.compiler)
    kapt(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}