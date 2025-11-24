import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.hiltAndroid)
    alias(libs.plugins.ksp)
}

// local.properties 파일에서 AdMob ID 읽기
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.reader())
}

android {
    namespace = "org.comon.pdfredactorm"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.comon.pdfredactorm"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        // AdMob ID 가져오기 (없으면 빈 문자열)
        val admobAppIdTest = localProperties.getProperty("ADMOB_APP_ID_TEST", "")
        val admobBannerIdTest = localProperties.getProperty("ADMOB_BANNER_ID_TEST", "")
        val admobAppIdRelease = localProperties.getProperty("ADMOB_APP_ID_RELEASE", "")
        val admobBannerIdRelease = localProperties.getProperty("ADMOB_BANNER_ID_RELEASE", "")

        debug {
            // Debug 빌드는 테스트 ID 사용
            buildConfigField("String", "ADMOB_APP_ID", "\"$admobAppIdTest\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobBannerIdTest\"")

            // Manifest placeholder로도 설정 (AndroidManifest.xml에서 사용)
            manifestPlaceholders["admobAppId"] = admobAppIdTest
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // Release 빌드는 실제 ID 사용
            buildConfigField("String", "ADMOB_APP_ID", "\"$admobAppIdRelease\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobBannerIdRelease\"")

            // Manifest placeholder로도 설정
            manifestPlaceholders["admobAppId"] = admobAppIdRelease
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_1_8)
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    
    // Navigation
    implementation(libs.androidx.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // PDF
    implementation(libs.pdfbox.android)

    // Coil
    implementation(libs.coil.compose)

    // AdMob
    implementation(libs.play.services.ads)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}