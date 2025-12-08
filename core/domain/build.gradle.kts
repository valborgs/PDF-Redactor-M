plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    // Core 모듈 의존성
    implementation(project(":core:model"))
    implementation(project(":core:common"))

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Javax Inject (for @Inject annotation without Hilt)
    implementation(libs.javax.inject)
}
