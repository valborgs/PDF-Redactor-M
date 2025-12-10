import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import java.util.Properties

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hiltAndroid)
    alias(libs.plugins.kotlin.serialization)
}

// local.properties 파일에서 API 키 읽기
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.reader())
}

android {
    namespace = "org.comon.pdfredactorm.core.data"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        // API 키를 BuildConfig로 제공
        val redeemApiKey = localProperties.getProperty("REDEEM_API_KEY", "")
        buildConfigField("String", "REDEEM_API_KEY", "\"$redeemApiKey\"")

        val redactApiKey = localProperties.getProperty("REDACT_API_KEY", "")
        buildConfigField("String", "REDACT_API_KEY", "\"$redactApiKey\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
}

dependencies {
    // Core 모듈 의존성
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(project(":core:datastore"))

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Retrofit (Repository에서 Response 클래스 사용)
    implementation(libs.retrofit)

    // Kotlinx Serialization
    implementation(libs.kotlinx.serialization.json)

    // PDF (LocalPdfRepositoryImpl에서 사용)
    implementation(libs.pdfbox.android)

    // DataStore (SettingsRepositoryImpl에서 사용)
    implementation(libs.androidx.datastore.preferences)

    // Coroutines
    implementation(libs.androidx.lifecycle.runtime.ktx)

    testImplementation(libs.junit)
}