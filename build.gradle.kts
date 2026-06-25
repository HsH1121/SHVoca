// 프로젝트 전체에 적용되는 플러그인. apply false = 여기선 선언만 하고
// 실제 적용은 각 모듈(app/build.gradle.kts)에서 한다.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
