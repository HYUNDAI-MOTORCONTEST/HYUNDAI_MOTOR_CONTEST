import org.jetbrains.gradle.ext.settings
import org.jetbrains.gradle.ext.taskTriggers

// 루트 빌드: 플러그인 버전만 선언하고 각 모듈에서 적용한다.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.spring.boot) apply false
    // IntelliJ IDEA·Android Studio가 Gradle 동기화 후 devSetup을 실행하게 한다(taskTriggers.afterSync).
    alias(libs.plugins.idea.ext)
}

// 개발환경 점검 + 에뮬레이터·백엔드 자동 기동(scripts/setup.sh 기본 모드). Claude Code·Codex SessionStart 훅과 같은 동작.
// - IDE: 아래 afterSync 트리거로 Gradle 동기화가 끝날 때마다 실행된다(첫 import 포함).
// - 명령줄: 아무 태스크도 devSetup에 의존하지 않으므로 ./gradlew build 등에서는 실행되지 않는다. 직접 돌릴 때만 ./gradlew devSetup.
// - 자동 기동을 끄려면 ~/.gradle/gradle.properties에 hmg.autostart=false(IDE는 셸 환경변수 HMG_AUTOSTART를 못 볼 수 있다).
// - setup.sh는 항상 0으로 끝나고, 실패해도 동기화·빌드를 깨지 않게 종료 코드를 무시한다. CI 환경(CI 변수)에서는 setup.sh가 자동 기동을 건너뛴다.
val devSetup = tasks.register<Exec>("devSetup") {
    group = "hmg"
    description = "scripts/setup.sh 실행: 개발환경 점검 + 에뮬레이터·백엔드 자동 기동(이미 실행 중이면 건너뜀)"
    workingDir = rootDir
    commandLine("bash", rootDir.resolve("scripts/setup.sh").absolutePath)
    isIgnoreExitValue = true
    if (providers.gradleProperty("hmg.autostart").orNull == "false") {
        environment("HMG_AUTOSTART", "0")
    }
}

idea {
    project {
        settings {
            taskTriggers {
                afterSync(devSetup)
            }
        }
    }
}
