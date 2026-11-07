plugins {
    `kotlin-dsl`
}

group = "__GROUP__.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // 'implementation' para ficarem no classpath em runtime: o convention plugin os aplica
    // via pluginManager.apply(...).
    implementation(libs.detekt.gradlePlugin)
    implementation(libs.ktlint.gradlePlugin)
}

gradlePlugin {
    plugins {
        // Convention de qualidade de código: Detekt (análise estática) + KtLint (estilo).
        register("codeQuality") {
            id = "__SLUG__.code.quality"
            implementationClass = "CodeQualityConventionPlugin"
        }
    }
}
