// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("androideventscheduler.code.quality") apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

// Qualidade de código (detekt + ktlint) em todos os módulos. Ver docs/project-foundation/README.md
subprojects {
    pluginManager.apply("androideventscheduler.code.quality")
}
