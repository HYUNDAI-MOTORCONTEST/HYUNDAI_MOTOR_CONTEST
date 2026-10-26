// 로컬 테스트용 샌드박스 앱 "아가랑 미니". 제출 대상 아님(제출 APK는 :frontend:app).
// 빌드 설정은 :frontend:app(frontend/app/build.gradle.kts)을 따른다. 백엔드·:shared는 쓰지 않는다.
plugins {
    // AGP 9 built-in Kotlin: org.jetbrains.kotlin.android 플러그인을 따로 적용하지 않는다.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.hmgchallenge.sandbox"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.hmgchallenge.sandbox"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

/**
 * Asta Sans 글꼴(5.7MB)을 레포에 한 벌 더 두지 않으려고, 빌드할 때 :frontend:app의 파일을
 * build/ 아래 res 폴더(font/)로 복사해 이 앱의 res로 붙인다. 원본: frontend/app/src/main/res/font/asta_sans_variable.ttf
 */
abstract class CopySharedFontTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val fontFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copy() {
        val fontDir = outputDir.get().asFile.resolve("font")
        fontDir.mkdirs()
        fontFile.get().asFile.copyTo(fontDir.resolve("asta_sans_variable.ttf"), overwrite = true)
    }
}

val copySharedFont = tasks.register<CopySharedFontTask>("copySharedFont") {
    fontFile.set(rootProject.layout.projectDirectory.file("frontend/app/src/main/res/font/asta_sans_variable.ttf"))
    outputDir.set(layout.buildDirectory.dir("generated/sharedFontRes"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.res?.addGeneratedSourceDirectory(copySharedFont, CopySharedFontTask::outputDir)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.kotlinx.coroutines.android)

    // Pleos Connect Vehicle SDK (배터리 잔량 SoC 구독만 사용)
    implementation(libs.pleos.vehicle)
}
