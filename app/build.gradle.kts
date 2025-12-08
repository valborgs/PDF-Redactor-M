import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.hiltAndroid)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("kotlinx-serialization")
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
        versionCode = 9
        versionName = "0.0.9"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val apiBaseUrl = localProperties.getProperty("API_BASE_URL", "")
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")

        val coffeeChatUrl = localProperties.getProperty("COFFEE_CHAT_URL", "")
        buildConfigField("String", "COFFEE_CHAT_URL", "\"$coffeeChatUrl\"")

        val redeemApiKey = localProperties.getProperty("REDEEM_API_KEY", "")
        buildConfigField("String", "REDEEM_API_KEY", "\"$redeemApiKey\"")
    }

    buildTypes {
        // AdMob ID 가져오기 (없으면 빈 문자열)
        val admobAppIdTest = localProperties.getProperty("ADMOB_APP_ID_TEST", "")
        val admobBannerIdTest = localProperties.getProperty("ADMOB_BANNER_ID_TEST", "")
        val admobNativeIdTest = localProperties.getProperty("ADMOB_NATIVE_ID_TEST", "")

        val admobAppIdRelease = localProperties.getProperty("ADMOB_APP_ID_RELEASE", "")
        val admobBannerIdRelease = localProperties.getProperty("ADMOB_BANNER_ID_RELEASE", "")
        val admobNativeIdRelease = localProperties.getProperty("ADMOB_NATIVE_ID_RELEASE", "")

        debug {
            // Debug 빌드는 테스트 ID 사용
            buildConfigField("String", "ADMOB_APP_ID", "\"$admobAppIdTest\"")
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobBannerIdTest\"")
            buildConfigField("String", "ADMOB_NATIVE_ID", "\"$admobNativeIdTest\"")

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
            buildConfigField("String", "ADMOB_NATIVE_ID", "\"$admobNativeIdRelease\"")

            // Manifest placeholder로도 설정
            manifestPlaceholders["admobAppId"] = admobAppIdRelease
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
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

    // Core 모듈 의존성
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:datastore"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
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

    // Coil
    implementation(libs.coil.compose)

    // AdMob
    implementation(libs.play.services.ads)

    // firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics.ndk)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
