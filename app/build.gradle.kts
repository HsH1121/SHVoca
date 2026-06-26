plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.baekseok.shvoca"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.baekseok.shvoca"
        minSdk = 33          // Android 13 이상 (다이나믹 컬러·예측형 백·스플래시 모두 지원)
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        val geminiKey = rootProject.file("local.properties")
            .takeIf { it.exists() }
            ?.readLines()
            ?.firstOrNull { it.startsWith("GEMINI_API_KEY=") }
            ?.substringAfter("=")
            ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")
    }

    buildTypes {
        release {
            // 학습용이라 난독화는 꺼둔다. 배포 시 true로 바꾸면 됨.
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
        compose = true   // Jetpack Compose 활성화
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM: 아래 Compose 라이브러리들의 버전을 한 번에 맞춰준다.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)

    // 디버그 빌드에서만 미리보기 도구 포함
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.mlkit.text.recognition.latin)
    implementation(libs.mlkit.text.recognition.japanese)
    implementation(libs.mlkit.text.recognition.chinese)
    implementation(libs.generativeai)
}
