plugins {
    id("java-library")
    alias(libs.plugins.kotlin.jvm)
}
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}
kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

dependencies {
    // 使用 Version Catalog 中的 Lint 依赖
    compileOnly(libs.lint.api)
    compileOnly(libs.lint.checks)

    // Kotlin 标准库
    implementation(libs.kotlin.stdlib)

    // 测试依赖
    testImplementation(libs.lint.core)
    testImplementation(libs.lint.tests)
    testImplementation(libs.junit)
}

// 配置 JAR 任务，声明 Lint Registry
tasks.jar {
    manifest {
        attributes(
            "Lint-Registry-v2" to "com.jiyingcao.lint.CustomIssueRegistry"
        )
    }
}

// 创建一个配置用于发布
val lintPublish = configurations.create("lintPublish")

// 将 JAR 添加到 lintPublish 配置
artifacts {
    add(lintPublish.name, tasks.jar)
}
