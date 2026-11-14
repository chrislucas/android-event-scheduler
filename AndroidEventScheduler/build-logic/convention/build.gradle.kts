plugins {
    `kotlin-dsl`
}

group = "com.br.androideventscheduler.buildlogic"

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
    // Só a API (KotlinCompilationTask): o KGP real vem do projeto, pelos plugins do Kotlin/AGP.
    compileOnly(libs.kotlin.gradlePluginApi)
}

gradlePlugin {
    plugins {
        // Convention de qualidade de código: Detekt (análise estática) + KtLint (estilo).
        register("codeQuality") {
            id = "androideventscheduler.code.quality"
            implementationClass = "CodeQualityConventionPlugin"
        }
    }
}
