# project-foundation — fundação de qualidade para projetos Android/KMP

Esta pasta contém o **script `bootstrap-foundation.sh`**, que monta em qualquer projeto Gradle Android ou Kotlin Multiplatform a mesma fundação de qualidade usada neste projeto: um módulo **`build-logic`** com um *convention plugin* de **detekt + ktlint**, a configuração das regras, o **workflow de CI do GitHub Actions** e (opcionalmente) **git hooks** de pre-commit.

Foi extraído do projeto `DriveAssistant` (`build-logic/convention`, `CodeQualityConventionPlugin`, `ci-validation.yml`, `pre-commit`) e generalizado. Neste projeto o script foi **executado de verdade** (a fundação que existe hoje em `build-logic/`, `config/`, `.editorconfig`, `.githooks/`, `scripts/` e `../.github/` é a saída dele).

## Propósito

- **Padronizar** análise estática e estilo: todo módulo herda as mesmas regras, versões e relatórios.
- **Reduzir o custo de criar um projeto novo**: um comando em vez de copiar/colar arquivos de outro projeto e ajustar nomes à mão.
- **Nascer pronto para modularizar**: o convention plugin é aplicado a `subprojects`; módulos criados depois (`:core`, `:feature:x`) já são analisados sem editar nada.
- **Gate real desde o primeiro commit**: ao contrário do projeto de origem (que começou com `ignoreFailures = true`), aqui o padrão **reprova** o build em violação.

## Uso rápido

```bash
# a partir de qualquer lugar (o script é autocontido: usa a pasta templates/ ao lado dele)
./docs/project-foundation/bootstrap-foundation.sh --project-dir . --type android

# conferir antes, sem escrever nada
./docs/project-foundation/bootstrap-foundation.sh --project-dir ~/code/MeuApp --dry-run

# KMP, várias branches de integração, JDK explícito
./docs/project-foundation/bootstrap-foundation.sh --project-dir ~/code/MeuKmp \
  --type kmp --branches "main,develop,release/**" --java 21
```

Para usar em **outro** projeto, basta copiar esta pasta (`bootstrap-foundation.sh` + `templates/`) ou apontar para ela por caminho absoluto. Depois:

```bash
cd ~/code/MeuApp
./gradlew ktlintFormat          # normaliza o estilo do código existente
./gradlew ktlintCheck detekt    # deve passar (ou gere baseline: ./gradlew detektBaseline)
./scripts/setup-git-hooks.sh    # (opcional) ativa o pre-commit neste clone
```

### Opções

| Opção | Padrão | Descrição |
|---|---|---|
| `--project-dir DIR` | `.` | Pasta do projeto Gradle (onde está o `settings.gradle[.kts]`) |
| `--name NAME` | nome da pasta | Nome legível (aparece em comentários/documentos gerados) |
| `--slug SLUG` | nome da pasta, minúsculo alfanumérico | Prefixo do id do plugin: `<slug>.code.quality` |
| `--group GROUP` | `com.example.<slug>` | Grupo dos artefatos de `build-logic` (`<group>.buildlogic`) |
| `--type android\|kmp` | `android` | Muda a flag `android` do ktlint e as tarefas padrão da CI |
| `--branches LIST` | `main` | Branches da CI (push e PR), separadas por vírgula. Aceita `release/**` |
| `--java VERSION` | lê `gradle/gradle-daemon-jvm.properties`; senão `17` | JDK usado na CI |
| `--ci-test-task` | android: `testDebugUnitTest` · kmp: `allTests` | Tarefa de testes da CI |
| `--ci-build-task` | android: `:app:assembleDebug` · kmp: `assemble` | Tarefa de compilação da CI |
| `--detekt-version` | `2.0.0-alpha.6` se JDK ≥ 23, senão `1.23.8` | Versão do detekt (a major escolhe a variante do plugin) |
| `--ktlint-gradle-version` / `--ktlint-version` | `14.2.0` / `1.8.0` | Plugin Gradle do ktlint / engine do ktlint |
| `--no-ci` / `--no-hooks` | — | Não gera o workflow / os git hooks |
| `--force` | — | Sobrescreve arquivos existentes (sem isto, pula e avisa) |
| `--dry-run` | — | Só mostra o que faria |

## O que o script faz

**Cria** (nunca sobrescreve sem `--force`):

| Arquivo | Para quê |
|---|---|
| `build-logic/settings.gradle.kts` | Composite build; reusa `gradle/libs.versions.toml` como catálogo `libs` |
| `build-logic/convention/build.gradle.kts` | `kotlin-dsl`; registra o plugin `<slug>.code.quality` |
| `build-logic/convention/src/main/kotlin/CodeQualityConventionPlugin.kt` | Aplica e configura detekt + ktlint |
| `config/detekt/detekt.yml` | Regras do time (Compose/`@Preview`/testes com crase já tratados) |
| `.editorconfig` | Estilo lido pelo ktlint e pelo Android Studio |
| `<raiz do git>/.github/workflows/ci-validation.yml` | CI: job de qualidade + job de testes/compilação |
| `.githooks/pre-commit`, `scripts/setup-git-hooks.sh` | Hook opcional (ktlintFormat nos staged + detekt) |

**Edita de forma idempotente** (rodar duas vezes não duplica nada):

| Arquivo | Alteração |
|---|---|
| `settings.gradle[.kts]` | `includeBuild("build-logic")` dentro de `pluginManagement { }` (cria o bloco se faltar) |
| `build.gradle[.kts]` raiz | `id("<slug>.code.quality") apply false` em `plugins { }` e `subprojects { pluginManager.apply(...) }` |
| `gradle/libs.versions.toml` | Versões `detekt`, `ktlintGradle`, `ktlint` e libs `detekt-gradlePlugin`, `ktlint-gradlePlugin` (só se ainda não existirem) |
| `.gitignore` | Bloco `# >>> project-foundation` ignorando `build-logic/.gradle`, `build-logic/**/build`, `.kotlin` |

**Pré-requisitos do projeto alvo:** `settings.gradle[.kts]`, `build.gradle[.kts]` raiz e **`gradle/libs.versions.toml`** (o script falha com mensagem clara se faltar). Precisa de `bash`, `awk` e `sed` (testado com o bash 3.2 do macOS); `git` é opcional.

## Como funciona o convention plugin

`<slug>.code.quality` é aplicado a cada subprojeto e:

- **detekt**: `buildUponDefaultConfig`, config em `config/detekt/detekt.yml`, baseline opcional em `config/detekt/baseline.xml` (usada automaticamente se existir), fontes em `src/` (cobre `main`, `test`, `androidTest` e source sets KMP), exclui `build/` e `generated/`, relatórios HTML + XML/checkstyle em `<módulo>/build/reports/detekt`.
- **ktlint**: versão do engine lida do catálogo (chave `ktlint`), `android = true` para `--type android`, exclui `build/`. Se a variável `KTLINT_STAGED_FILES` apontar para um arquivo com caminhos, o ktlint se restringe a eles (usado pelo pre-commit).
- **Gate**: reprova o build em violação. Para relatar sem reprovar (ex.: adoção gradual): `./gradlew detekt ktlintCheck -Pquality.ignoreFailures=true`. Para tolerar findings legados de forma permanente: `./gradlew detektBaseline`.

Tarefas: `./gradlew detekt`, `./gradlew ktlintCheck`, `./gradlew ktlintFormat`.

### Duas variantes de detekt (1.x e 2.x)

| | detekt 1.23.x | detekt 2.0 (alpha) |
|---|---|---|
| Plugin / pacote | `io.gitlab.arturbosch.detekt` | `dev.detekt` |
| API do plugin | propriedades simples | `Property<T>` |
| Nomes de regras | `threshold`, `UnusedPrivateMember`… | `allowedLines`, `UnusedPrivateFunction`… |
| JDK de execução | até 22 | testado até 25 |

O script escolhe a variante pela **major** de `--detekt-version` (`templates/variants/detekt-1|2/`). **Por que o padrão muda com o JDK:** o detekt 1.23.x falha em JDK ≥ 23 (`> 25.0.4` ao ler a versão do JDK, e o `--jvm-target` só aceita até 22); este projeto usa JDK 25, então o padrão é a 2.0 alpha. Quando a 2.0 sair estável, atualize `--detekt-version`/o padrão do script. A variante 1 fixa `jvmTarget = "17"` na tarefa por precaução.

## CI gerada (`.github/workflows/ci-validation.yml`)

- Gatilhos: `push` e `pull_request` nas branches informadas + `workflow_dispatch`. Uma execução por push (branches de trabalho rodam via PR). `concurrency` cancela execuções antigas da mesma ref; `permissions: contents: read`.
- `defaults.run.working-directory` aponta para o projeto Gradle dentro do repositório (calculado: `.` ou o caminho relativo à raiz do git). **O arquivo vai sempre para a raiz do git**, porque o GitHub ignora workflows em subpastas.
- Job **`quality`**: JDK (Temurin) → validação do checksum do wrapper → cache do Gradle → `./gradlew ktlintCheck detekt` → upload dos relatórios (`if: always()`).
- Job **`build`**: testes (`testDebugUnitTest` / `allTests`) → compilação (`:app:assembleDebug` / `assemble`) → upload dos relatórios de teste.
- Os dois jobs rodam em paralelo (feedback de estilo rápido, sem esperar os testes).
- Não gera APK publicável nem faz *secret scan* (o `DriveAssistant` tem workflows separados para isso; adicione se precisar).

## Pre-commit opcional

`./scripts/setup-git-hooks.sh` aponta `core.hooksPath` para `.githooks` (**altera a config local do git do clone** — por isso é um passo explícito, nunca feito pelo bootstrap). O hook roda só quando há `.kt/.kts` em stage: `ktlintFormat` restrito aos arquivos staged (re-adiciona ao index) e `detekt`. Pular: `SKIP_QUALITY=1 git commit …`. Funciona também quando a raiz do git está acima do projeto Gradle. A versão do `DriveAssistant` também roda `gitleaks`; aqui foi omitida (depende de ferramenta externa e de `gitleaks.toml`).

## Como evoluir

- **Mudar regras/versões para todos os projetos futuros**: edite `templates/` (e, se for versão padrão, as variáveis no topo do script). Para um projeto já gerado, rode com `--force` (sobrescreve apenas os arquivos de template; os edits em settings/build/catálogo são idempotentes).
- **Novo placeholder**: use `__NOME__` no template e adicione o `-e "s|__NOME__|...|g"` na função `render` do script.
- **Placeholders existentes**: `__SLUG__`, `__GROUP__`, `__PROJECT_NAME__`, `__WORKDIR__`, `__JAVA_VERSION__`, `__KTLINT_ANDROID__`, `__CI_TEST_TASK__`, `__CI_BUILD_TASK__`, `__CI_BRANCHES_INLINE__`, `__CI_BRANCH_LINES__`.
- **Outros convention plugins** (versionamento, hardware, etc., como no `DriveAssistant`) entram em `build-logic/convention` e são registrados em `gradlePlugin { }`; não fazem parte deste script.

## Verificação feita

Executado neste projeto (Android, DSL Kotlin, detekt 2.0.0-alpha.6, JDK 25, Gradle 9.6): `./gradlew ktlintCheck detekt testDebugUnitTest :app:assembleDebug` → **BUILD SUCCESSFUL**; segunda execução do script sem diferenças (idempotência). Em projeto-teste descartável (KMP, DSL Groovy, git na pasta pai, `--java 17`): gerou a variante detekt 1, `working-directory` correto e branches múltiplas. **Não verificado:** execução real do workflow no GitHub Actions, execução do hook de pre-commit, e rodar o detekt 1.23.x / projetos KMP com Gradle (apenas a geração de arquivos foi testada nesses casos).

## Limitações conhecidas

- Edição de `settings`/`build.gradle` é textual (procura `pluginManagement {` e `plugins {` no início da linha); formatações incomuns podem exigir ajuste manual — o script avisa o que alterou e é idempotente.
- Exige `gradle/libs.versions.toml` e um projeto com subprojetos ou módulo `:app` (o plugin só é aplicado a `subprojects`; num projeto de módulo único na raiz, aplique `id("<slug>.code.quality")` no próprio `build.gradle`).
- O detekt 2.0 é alpha: a API e os nomes de regras podem mudar entre alphas.
