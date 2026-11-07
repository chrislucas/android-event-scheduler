import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.withType
import org.jlleitschuh.gradle.ktlint.KtlintExtension

/**
 * Convention plugin de QUALIDADE DE CÓDIGO.
 *
 * Aplica dois verificadores de forma padronizada a qualquer módulo que o use:
 *  - **Detekt** (io.gitlab.arturbosch.detekt): análise estática — code smells, complexidade,
 *    potenciais bugs e más práticas de Kotlin.
 *  - **KtLint** (org.jlleitschuh.gradle.ktlint): lint de ESTILO/formatação, com `ktlintFormat`
 *    para auto-corrigir.
 *
 * Por que num convention plugin, e não em cada build.gradle.kts?
 *  - Centraliza regras, versões e relatórios: todo módulo herda a mesma configuração.
 *  - Ao modularizar, o novo módulo já nasce com a mesma análise (o build.gradle.kts raiz aplica
 *    o plugin em `subprojects`).
 *
 * Tarefas:
 *   ./gradlew detekt         # análise estática (relatórios em <módulo>/build/reports/detekt)
 *   ./gradlew ktlintCheck    # verifica o estilo
 *   ./gradlew ktlintFormat   # corrige o estilo automaticamente
 *
 * Propriedades Gradle (opcionais, ex.: -Pquality.ignoreFailures=true):
 *   quality.ignoreFailures   # true = relata mas não reprova o build (padrão: false = gate real)
 *
 * Variável de ambiente (usada pelo pre-commit):
 *   KTLINT_STAGED_FILES      # arquivo com caminhos absolutos; restringe o ktlint a esses arquivos
 */
class CodeQualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        val ignoreFailures = providers.gradleProperty(IGNORE_FAILURES_PROPERTY)
            .map(String::toBoolean)
            .getOrElse(false)
        // O catálogo "libs" é registrado no projeto raiz; subprojetos não o têm como extensão própria.
        val catalog = rootProject.extensions.findByType<VersionCatalogsExtension>()?.find("libs")?.orElse(null)

        // --- Detekt ---
        pluginManager.apply("io.gitlab.arturbosch.detekt")
        extensions.configure<DetektExtension> {
            // Regras padrão + as do time (config/detekt/detekt.yml), se o arquivo existir.
            buildUponDefaultConfig = true
            val sharedConfig = rootProject.file("config/detekt/detekt.yml")
            if (sharedConfig.exists()) config.setFrom(sharedConfig)
            // Baseline opcional: findings legados listados ali não reprovam (./gradlew detektBaseline).
            val baselineFile = rootProject.file("config/detekt/baseline.xml")
            if (baselineFile.exists()) baseline = baselineFile
            // 'src' cobre main, test, androidTest e source sets KMP (commonMain, androidMain...).
            source.setFrom(files("src"))
            this.ignoreFailures = ignoreFailures
            autoCorrect = false
            parallel = true
        }
        tasks.withType<Detekt>().configureEach {
            // O detekt 1.23.x só aceita jvmTarget até 22; com JDK mais novo (ex.: 25) ele falha se não fixarmos.
            jvmTarget = DETEKT_JVM_TARGET
            include("**/*.kt", "**/*.kts")
            exclude("**/build/**", "**/generated/**")
            reports {
                html.required.set(true)
                xml.required.set(true)
                sarif.required.set(false)
                txt.required.set(false)
            }
        }

        // --- KtLint ---
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")
        val stagedFiles = System.getenv("KTLINT_STAGED_FILES")
            ?.let(::File)
            ?.takeIf(File::isFile)
            ?.readLines(Charsets.UTF_8)
            ?.filter(String::isNotBlank)
            ?.map { File(it).canonicalPath }
            ?.toSet()
        extensions.configure<KtlintExtension> {
            // Engine do KtLint: versão do catálogo (chave 'ktlint'), igual à do build-logic.
            catalog?.findVersion("ktlint")?.ifPresent { version.set(it.requiredVersion) }
            android.set(ANDROID_RULES)
            this.ignoreFailures.set(ignoreFailures)
            filter {
                exclude { element -> element.file.path.contains("/build/") }
                if (stagedFiles != null) {
                    include { element -> element.file.canonicalPath in stagedFiles }
                }
            }
        }
    }

    private companion object {
        const val DETEKT_JVM_TARGET = "17"
        const val IGNORE_FAILURES_PROPERTY = "quality.ignoreFailures"

        /** Regras específicas de projeto Android no ktlint (gerado pelo bootstrap: --type). */
        const val ANDROID_RULES = __KTLINT_ANDROID__
    }
}
