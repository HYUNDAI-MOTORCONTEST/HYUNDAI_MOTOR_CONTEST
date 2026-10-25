plugins {
    // AGP 9는 Kotlin 지원이 내장(built-in Kotlin)되어 org.jetbrains.kotlin.android 플러그인을 따로 적용하지 않는다.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// debug 빌드의 백엔드 주소. 다른 주소로 붙이려면
// `./gradlew :frontend:app:installDebug -Phmg.backendUrl=http://10.0.2.2:9090` 처럼 바꾼다.
val debugBackendUrl: String = providers.gradleProperty("hmg.backendUrl").getOrElse("http://10.0.2.2:8081")

android {
    namespace = "com.hmgchallenge.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.hmgchallenge.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.0.1"
    }

    buildTypes {
        debug {
            // 에뮬레이터에서 호스트 PC의 백엔드(localhost:8081)는 10.0.2.2로 접근한다.
            buildConfigField("String", "BACKEND_BASE_URL", "\"$debugBackendUrl\"")
        }
        release {
            isMinifyEnabled = false
            // TODO: 실제 배포 서버 주소로 교체 (HTTPS)
            buildConfigField("String", "BACKEND_BASE_URL", "\"https://example.invalid\"")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // CarUxRestrictionsManager 등 AAOS android.car API(SDK optional 라이브러리, compileOnly처럼 동작).
    useLibrary("android.car")

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    // Pleos Connect Vehicle SDK
    implementation(libs.pleos.vehicle)
}
