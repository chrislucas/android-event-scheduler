#!/usr/bin/env bash
#
# bootstrap-foundation.sh — monta a "fundação de qualidade" de um projeto Android ou KMP:
#   * build-logic/ (composite build) com o convention plugin  <slug>.code.quality  (detekt + ktlint)
#   * config/detekt/detekt.yml e .editorconfig
#   * CI do GitHub Actions (.github/workflows/ci-validation.yml)
#   * (opcional) git hooks versionados: pre-commit com ktlintFormat + detekt
#   * ajustes idempotentes em settings.gradle(.kts), build.gradle(.kts) raiz, libs.versions.toml e .gitignore
#
# Documentação completa: docs/project-foundation/README.md
#
# Uso:
#   ./bootstrap-foundation.sh --project-dir /caminho/do/projeto [opções]
#
# Opções:
#   --project-dir DIR     Pasta do projeto Gradle (onde está o settings.gradle[.kts]). Padrão: .
#   --name NAME           Nome legível do projeto (padrão: nome da pasta)
#   --slug SLUG           Prefixo do id do plugin: <slug>.code.quality (padrão: nome da pasta, minúsculo/alfanumérico)
#   --group GROUP         Grupo dos artefatos de build-logic (padrão: com.example.<slug>)
#   --type android|kmp    Tipo do projeto (padrão: android). Muda regras do ktlint e tarefas padrão da CI
#   --branches LIST       Branches da CI, separadas por vírgula (padrão: main)
#   --java VERSION        JDK da CI (padrão: lê gradle/gradle-daemon-jvm.properties; senão 17)
#   --ci-test-task TASK   Tarefa de testes da CI (padrão: android=testDebugUnitTest, kmp=allTests)
#   --ci-build-task TASK  Tarefa de compilação da CI (padrão: android=:app:assembleDebug, kmp=assemble)
#   --detekt-version V    Versão do detekt. Padrão: 2.0.0-alpha.6 se o JDK for >= 23 (o 1.23.x não roda nele), senão 1.23.8.
#                         A major (1 ou 2) escolhe o convention plugin: o plugin 2.x usa o id 'dev.detekt'.
#   --ktlint-gradle-version V (padrão 14.2.0)    --ktlint-version V (padrão 1.8.0)
#   --no-ci               Não cria o workflow do GitHub Actions
#   --no-hooks            Não cria os git hooks
#   --force               Sobrescreve arquivos que já existem (padrão: pula e avisa)
#   --dry-run             Mostra o que faria, sem escrever nada
#   -h, --help            Esta ajuda
#
# Idempotente: pode rodar de novo sem duplicar nada. Requer: bash, awk, sed, git (git é opcional).

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEMPLATES_DIR="${SCRIPT_DIR}/templates"

PROJECT_DIR="."
NAME=""
SLUG=""
GROUP=""
TYPE="android"
BRANCHES="main"
JAVA_VERSION=""
CI_TEST_TASK=""
CI_BUILD_TASK=""
DETEKT_VERSION=""
KTLINT_GRADLE_VERSION="14.2.0"
KTLINT_VERSION="1.8.0"
WITH_CI=1
WITH_HOOKS=1
FORCE=0
DRY_RUN=0

usage() { sed -n '2,/^set -euo/p' "${BASH_SOURCE[0]}" | sed '$d' | sed 's/^# \{0,1\}//'; }
die() { echo "[bootstrap] ERRO: $*" >&2; exit 1; }
info() { echo "[bootstrap] $*"; }

# ---------------------------------------------------------------------------------------------
# Argumentos
# ---------------------------------------------------------------------------------------------
while [ $# -gt 0 ]; do
  case "$1" in
    --project-dir) PROJECT_DIR="${2:?}"; shift 2 ;;
    --name) NAME="${2:?}"; shift 2 ;;
    --slug) SLUG="${2:?}"; shift 2 ;;
    --group) GROUP="${2:?}"; shift 2 ;;
    --type) TYPE="${2:?}"; shift 2 ;;
    --branches) BRANCHES="${2:?}"; shift 2 ;;
    --java) JAVA_VERSION="${2:?}"; shift 2 ;;
    --ci-test-task) CI_TEST_TASK="${2:?}"; shift 2 ;;
    --ci-build-task) CI_BUILD_TASK="${2:?}"; shift 2 ;;
    --detekt-version) DETEKT_VERSION="${2:?}"; shift 2 ;;
    --ktlint-gradle-version) KTLINT_GRADLE_VERSION="${2:?}"; shift 2 ;;
    --ktlint-version) KTLINT_VERSION="${2:?}"; shift 2 ;;
    --no-ci) WITH_CI=0; shift ;;
    --no-hooks) WITH_HOOKS=0; shift ;;
    --force) FORCE=1; shift ;;
    --dry-run) DRY_RUN=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) die "opção desconhecida: $1 (use --help)" ;;
  esac
done

[ -d "${TEMPLATES_DIR}" ] || die "pasta de templates não encontrada: ${TEMPLATES_DIR}"
[ -d "${PROJECT_DIR}" ] || die "pasta do projeto não existe: ${PROJECT_DIR}"
PROJECT_DIR="$(cd "${PROJECT_DIR}" && pwd -P)"

case "${TYPE}" in
  android|kmp) ;;
  *) die "--type deve ser 'android' ou 'kmp' (recebido: ${TYPE})" ;;
esac

# ---------------------------------------------------------------------------------------------
# Detecção do projeto
# ---------------------------------------------------------------------------------------------
SETTINGS_FILE=""
for candidate in settings.gradle.kts settings.gradle; do
  [ -f "${PROJECT_DIR}/${candidate}" ] && { SETTINGS_FILE="${PROJECT_DIR}/${candidate}"; break; }
done
[ -n "${SETTINGS_FILE}" ] || die "settings.gradle[.kts] não encontrado em ${PROJECT_DIR}"

ROOT_BUILD_FILE=""
for candidate in build.gradle.kts build.gradle; do
  [ -f "${PROJECT_DIR}/${candidate}" ] && { ROOT_BUILD_FILE="${PROJECT_DIR}/${candidate}"; break; }
done
[ -n "${ROOT_BUILD_FILE}" ] || die "build.gradle[.kts] raiz não encontrado em ${PROJECT_DIR}"

CATALOG_FILE="${PROJECT_DIR}/gradle/libs.versions.toml"
[ -f "${CATALOG_FILE}" ] || die "version catalog não encontrado: ${CATALOG_FILE} (crie gradle/libs.versions.toml antes)"

case "${SETTINGS_FILE}" in *.kts) DSL="kts" ;; *) DSL="groovy" ;; esac

FOLDER_NAME="$(basename "${PROJECT_DIR}")"
[ -n "${NAME}" ] || NAME="${FOLDER_NAME}"
if [ -z "${SLUG}" ]; then
  SLUG="$(printf '%s' "${FOLDER_NAME}" | tr '[:upper:]' '[:lower:]' | tr -cd 'a-z0-9')"
fi
[ -n "${SLUG}" ] || die "não foi possível derivar o slug; informe --slug"
case "${SLUG}" in [0-9]*) die "slug não pode começar com número: ${SLUG}" ;; esac
[ -n "${GROUP}" ] || GROUP="com.example.${SLUG}"

# Raiz do git e caminho do projeto relativo a ela (o workflow precisa de working-directory).
if GIT_ROOT="$(git -C "${PROJECT_DIR}" rev-parse --show-toplevel 2>/dev/null)"; then
  GIT_ROOT="$(cd "${GIT_ROOT}" && pwd -P)"   # normaliza symlinks (ex.: /tmp -> /private/tmp no macOS)
else
  GIT_ROOT="${PROJECT_DIR}"
  info "AVISO: ${PROJECT_DIR} não está num repositório git; o workflow será criado em ${GIT_ROOT}/.github"
fi
if [ "${GIT_ROOT}" = "${PROJECT_DIR}" ]; then
  WORKDIR="."
else
  WORKDIR="${PROJECT_DIR#"${GIT_ROOT}/"}"
fi

# JDK da CI: respeita o toolchain do daemon do Gradle, se declarado.
if [ -z "${JAVA_VERSION}" ]; then
  DAEMON_PROPS="${PROJECT_DIR}/gradle/gradle-daemon-jvm.properties"
  if [ -f "${DAEMON_PROPS}" ] && grep -q '^toolchainVersion=' "${DAEMON_PROPS}"; then
    JAVA_VERSION="$(grep '^toolchainVersion=' "${DAEMON_PROPS}" | head -1 | cut -d= -f2 | tr -d '[:space:]')"
  else
    JAVA_VERSION="17"
  fi
fi

# detekt: 1.23.x não suporta JDK >= 23 (falha ao ler a versão do JDK); a 2.x (alpha) suporta.
if [ -z "${DETEKT_VERSION}" ]; then
  JAVA_MAJOR="${JAVA_VERSION%%.*}"
  if [ "${JAVA_MAJOR}" -ge 23 ] 2>/dev/null; then DETEKT_VERSION="2.0.0-alpha.6"; else DETEKT_VERSION="1.23.8"; fi
fi
DETEKT_MAJOR="${DETEKT_VERSION%%.*}"
case "${DETEKT_MAJOR}" in
  1|2) ;;
  *) die "versão do detekt não suportada: ${DETEKT_VERSION} (major deve ser 1 ou 2)" ;;
esac
[ -f "${TEMPLATES_DIR}/variants/detekt-${DETEKT_MAJOR}/CodeQualityConventionPlugin.kt" ] \
  || die "template do convention plugin para detekt ${DETEKT_MAJOR} não encontrado"

if [ "${TYPE}" = "android" ]; then
  KTLINT_ANDROID="true"
  [ -n "${CI_TEST_TASK}" ] || CI_TEST_TASK="testDebugUnitTest"
  [ -n "${CI_BUILD_TASK}" ] || CI_BUILD_TASK=":app:assembleDebug"
else
  KTLINT_ANDROID="false"
  [ -n "${CI_TEST_TASK}" ] || CI_TEST_TASK="allTests"
  [ -n "${CI_BUILD_TASK}" ] || CI_BUILD_TASK="assemble"
fi

# Branches -> linhas YAML ("      - main") e texto inline ("main, develop").
CI_BRANCH_LINES=""
CI_BRANCHES_INLINE=""
OLD_IFS="${IFS}"; IFS=','
for branch in ${BRANCHES}; do
  branch="$(printf '%s' "${branch}" | tr -d '[:space:]')"
  [ -n "${branch}" ] || continue
  case "${branch}" in
    *\**) CI_BRANCH_LINES="${CI_BRANCH_LINES}      - '${branch}'"$'\n' ;;
    *) CI_BRANCH_LINES="${CI_BRANCH_LINES}      - ${branch}"$'\n' ;;
  esac
  CI_BRANCHES_INLINE="${CI_BRANCHES_INLINE:+${CI_BRANCHES_INLINE}, }${branch}"
done
IFS="${OLD_IFS}"
CI_BRANCH_LINES="${CI_BRANCH_LINES%$'\n'}"
[ -n "${CI_BRANCH_LINES}" ] || die "--branches vazio"

info "Projeto ........ ${NAME} (${PROJECT_DIR})"
info "Tipo / DSL ..... ${TYPE} / ${DSL}"
info "Plugin id ...... ${SLUG}.code.quality   (grupo ${GROUP}.buildlogic)"
info "Raiz do git .... ${GIT_ROOT} (working-directory da CI: ${WORKDIR})"
info "Detekt ......... ${DETEKT_VERSION} (convention plugin variante detekt-${DETEKT_MAJOR})"
info "CI ............. JDK ${JAVA_VERSION}; branches: ${CI_BRANCHES_INLINE}; testes: ${CI_TEST_TASK}; build: ${CI_BUILD_TASK}"
[ "${DRY_RUN}" = "1" ] && info "MODO DRY-RUN: nada será escrito."

# ---------------------------------------------------------------------------------------------
# Utilitários de escrita
# ---------------------------------------------------------------------------------------------
escape_sed() { printf '%s' "$1" | sed -e 's/[\\&|]/\\&/g'; }

# Renderiza um template substituindo os placeholders __X__ e expandindo __CI_BRANCH_LINES__.
render() {
  local src="$1"
  local tmp
  tmp="$(mktemp "${TMPDIR:-/tmp}/bootstrap.XXXXXX")"
  sed \
    -e "s|__SLUG__|$(escape_sed "${SLUG}")|g" \
    -e "s|__GROUP__|$(escape_sed "${GROUP}")|g" \
    -e "s|__PROJECT_NAME__|$(escape_sed "${NAME}")|g" \
    -e "s|__WORKDIR__|$(escape_sed "${WORKDIR}")|g" \
    -e "s|__JAVA_VERSION__|$(escape_sed "${JAVA_VERSION}")|g" \
    -e "s|__KTLINT_ANDROID__|${KTLINT_ANDROID}|g" \
    -e "s|__CI_TEST_TASK__|$(escape_sed "${CI_TEST_TASK}")|g" \
    -e "s|__CI_BUILD_TASK__|$(escape_sed "${CI_BUILD_TASK}")|g" \
    -e "s|__CI_BRANCHES_INLINE__|$(escape_sed "${CI_BRANCHES_INLINE}")|g" \
    "${src}" |
    CI_BRANCH_LINES="${CI_BRANCH_LINES}" awk '$0 == "__CI_BRANCH_LINES__" { print ENVIRON["CI_BRANCH_LINES"]; next } { print }' > "${tmp}" \
    || { rm -f "${tmp}"; return 1; }
  # Remove "./" do working-directory quando o projeto é a própria raiz do git (".//**" -> "**").
  if [ "${WORKDIR}" = "." ]; then
    sed -e 's|\./\*\*|**|g' "${tmp}" > "${tmp}.2" && mv "${tmp}.2" "${tmp}"
  fi
  printf '%s' "${tmp}"
}

# install_file <template> <destino-absoluto> [executavel]
install_file() {
  local src="$1" dest="$2" exec_flag="${3:-}"
  local shown="${dest#"${GIT_ROOT}/"}"
  if [ -e "${dest}" ] && [ "${FORCE}" != "1" ]; then
    info "pulado (já existe) ... ${shown}   [use --force para sobrescrever]"
    return 0
  fi
  if [ "${DRY_RUN}" = "1" ]; then
    info "criaria .............. ${shown}"
    return 0
  fi
  local rendered
  rendered="$(render "${src}")" || die "falha ao renderizar o template ${src}"
  mkdir -p "$(dirname "${dest}")"
  mv "${rendered}" "${dest}"
  if [ -n "${exec_flag}" ]; then chmod 755 "${dest}"; else chmod 644 "${dest}"; fi
  info "criado ............... ${shown}"
}

write_changed() { # <arquivo> <novo-conteudo-em-arquivo-temporario> <mensagem>
  if [ "${DRY_RUN}" = "1" ]; then
    rm -f "$2"; info "alteraria ............ $3"; return 0
  fi
  mv "$2" "$1"
  info "atualizado ........... $3"
}

# ---------------------------------------------------------------------------------------------
# libs.versions.toml (idempotente, sensível à seção)
# ---------------------------------------------------------------------------------------------
toml_has() { # <arquivo> <[secao]> <chave>
  awk -v sec="$2" -v key="$3" '
    /^\[/ { insec = ($0 == sec) }
    insec && index($0, key) == 1 { rest = substr($0, length(key) + 1); if (rest ~ /^[ \t]*=/) found = 1 }
    END { exit found ? 0 : 1 }' "$1"
}

toml_add() { # <arquivo> <[secao]> <linha>
  local file="$1" sec="$2" line="$3" tmp
  tmp="$(mktemp "${TMPDIR:-/tmp}/bootstrap.XXXXXX")"
  if grep -qxF "${sec}" "${file}"; then
    awk -v sec="${sec}" -v line="${line}" '{ print } $0 == sec { print line }' "${file}" > "${tmp}"
  else
    { cat "${file}"; printf '\n%s\n%s\n' "${sec}" "${line}"; } > "${tmp}"
  fi
  write_changed "${file}" "${tmp}" "gradle/libs.versions.toml (+ $(printf '%s' "${line}" | cut -d= -f1 | tr -d ' '))"
}

toml_ensure() { # <[secao]> <chave> <linha>
  if toml_has "${CATALOG_FILE}" "$1" "$2"; then
    info "já existe ............ libs.versions.toml $1 $2"
  else
    toml_add "${CATALOG_FILE}" "$1" "$3"
  fi
}

ensure_catalog() {
  toml_ensure "[versions]" "detekt" "detekt = \"${DETEKT_VERSION}\""
  toml_ensure "[versions]" "ktlintGradle" "ktlintGradle = \"${KTLINT_GRADLE_VERSION}\"   # plugin org.jlleitschuh.gradle.ktlint"
  toml_ensure "[versions]" "ktlint" "ktlint = \"${KTLINT_VERSION}\"   # engine do KtLint"
  if [ "${DETEKT_MAJOR}" = "2" ]; then
    toml_ensure "[libraries]" "detekt-gradlePlugin" \
      'detekt-gradlePlugin = { group = "dev.detekt", name = "detekt-gradle-plugin", version.ref = "detekt" }'
  else
    toml_ensure "[libraries]" "detekt-gradlePlugin" \
      'detekt-gradlePlugin = { group = "io.gitlab.arturbosch.detekt", name = "detekt-gradle-plugin", version.ref = "detekt" }'
  fi
  toml_ensure "[libraries]" "ktlint-gradlePlugin" \
    'ktlint-gradlePlugin = { group = "org.jlleitschuh.gradle", name = "ktlint-gradle", version.ref = "ktlintGradle" }'
}

# ---------------------------------------------------------------------------------------------
# settings.gradle[.kts]: includeBuild("build-logic") dentro de pluginManagement
# ---------------------------------------------------------------------------------------------
ensure_settings() {
  if grep -Eq "includeBuild[ (]+[\"']build-logic[\"']" "${SETTINGS_FILE}"; then
    info "já existe ............ includeBuild(\"build-logic\") em $(basename "${SETTINGS_FILE}")"
    return 0
  fi
  local tmp
  tmp="$(mktemp "${TMPDIR:-/tmp}/bootstrap.XXXXXX")"
  if grep -Eq '^pluginManagement[[:space:]]*\{' "${SETTINGS_FILE}"; then
    awk '{ print } /^pluginManagement[ \t]*\{/ && !done { print "    includeBuild(\"build-logic\")"; done = 1 }' \
      "${SETTINGS_FILE}" > "${tmp}"
  else
    { printf 'pluginManagement {\n    includeBuild("build-logic")\n}\n\n'; cat "${SETTINGS_FILE}"; } > "${tmp}"
  fi
  write_changed "${SETTINGS_FILE}" "${tmp}" "$(basename "${SETTINGS_FILE}") (+ includeBuild build-logic)"
}

# ---------------------------------------------------------------------------------------------
# build.gradle[.kts] raiz: plugin (apply false) + subprojects { apply }
# ---------------------------------------------------------------------------------------------
ensure_root_build() {
  local plugin_id="${SLUG}.code.quality"
  if grep -q "${plugin_id}" "${ROOT_BUILD_FILE}"; then
    info "já existe ............ ${plugin_id} em $(basename "${ROOT_BUILD_FILE}")"
    return 0
  fi
  local plugin_line subprojects_block tmp
  if [ "${DSL}" = "kts" ]; then
    plugin_line="    id(\"${plugin_id}\") apply false"
    subprojects_block="
// Qualidade de código (detekt + ktlint) em todos os módulos. Ver docs/project-foundation/README.md
subprojects {
    pluginManager.apply(\"${plugin_id}\")
}"
  else
    plugin_line="    id '${plugin_id}' apply false"
    subprojects_block="
// Qualidade de código (detekt + ktlint) em todos os módulos. Ver docs/project-foundation/README.md
subprojects {
    pluginManager.apply('${plugin_id}')
}"
  fi
  tmp="$(mktemp "${TMPDIR:-/tmp}/bootstrap.XXXXXX")"
  if grep -Eq '^plugins[[:space:]]*\{' "${ROOT_BUILD_FILE}"; then
    awk -v line="${plugin_line}" '{ print } /^plugins[ \t]*\{/ && !done { print line; done = 1 }' \
      "${ROOT_BUILD_FILE}" > "${tmp}"
  else
    { printf 'plugins {\n%s\n}\n\n' "${plugin_line}"; cat "${ROOT_BUILD_FILE}"; } > "${tmp}"
  fi
  printf '%s\n' "${subprojects_block}" >> "${tmp}"
  write_changed "${ROOT_BUILD_FILE}" "${tmp}" "$(basename "${ROOT_BUILD_FILE}") (+ ${plugin_id})"
}

# ---------------------------------------------------------------------------------------------
# .gitignore do projeto
# ---------------------------------------------------------------------------------------------
ensure_gitignore() {
  local file="${PROJECT_DIR}/.gitignore" tmp
  if [ -f "${file}" ] && grep -q '# >>> project-foundation' "${file}"; then
    info "já existe ............ bloco project-foundation em .gitignore"
    return 0
  fi
  tmp="$(mktemp "${TMPDIR:-/tmp}/bootstrap.XXXXXX")"
  { [ -f "${file}" ] && cat "${file}"
    printf '\n# >>> project-foundation (bootstrap-foundation.sh)\nbuild-logic/.gradle/\nbuild-logic/**/build/\n.kotlin/\n# <<< project-foundation\n'
  } > "${tmp}"
  write_changed "${file}" "${tmp}" ".gitignore (+ build-logic, .kotlin)"
}

# ---------------------------------------------------------------------------------------------
# Execução
# ---------------------------------------------------------------------------------------------
ensure_catalog
install_file "${TEMPLATES_DIR}/build-logic/settings.gradle.kts" "${PROJECT_DIR}/build-logic/settings.gradle.kts"
install_file "${TEMPLATES_DIR}/build-logic/convention/build.gradle.kts" "${PROJECT_DIR}/build-logic/convention/build.gradle.kts"
install_file "${TEMPLATES_DIR}/variants/detekt-${DETEKT_MAJOR}/CodeQualityConventionPlugin.kt" \
  "${PROJECT_DIR}/build-logic/convention/src/main/kotlin/CodeQualityConventionPlugin.kt"
install_file "${TEMPLATES_DIR}/variants/detekt-${DETEKT_MAJOR}/detekt.yml" "${PROJECT_DIR}/config/detekt/detekt.yml"
install_file "${TEMPLATES_DIR}/editorconfig" "${PROJECT_DIR}/.editorconfig"
ensure_settings
ensure_root_build
ensure_gitignore

if [ "${WITH_CI}" = "1" ]; then
  install_file "${TEMPLATES_DIR}/github/workflows/ci-validation.yml" "${GIT_ROOT}/.github/workflows/ci-validation.yml"
fi
if [ "${WITH_HOOKS}" = "1" ]; then
  install_file "${TEMPLATES_DIR}/githooks/pre-commit" "${PROJECT_DIR}/.githooks/pre-commit" exec
  install_file "${TEMPLATES_DIR}/scripts/setup-git-hooks.sh" "${PROJECT_DIR}/scripts/setup-git-hooks.sh" exec
fi

echo
info "Concluído. Próximos passos:"
echo "  1. cd ${PROJECT_DIR} && ./gradlew ktlintFormat      # normaliza o estilo do código existente"
echo "  2. ./gradlew ktlintCheck detekt                     # deve passar (ou gere baseline: ./gradlew detektBaseline)"
[ "${WITH_HOOKS}" = "1" ] && echo "  3. ./scripts/setup-git-hooks.sh                     # ativa o pre-commit neste clone"
[ "${WITH_CI}" = "1" ] && echo "  4. Commit + push: o workflow 'CI Validation' roda em PRs/pushes para: ${CI_BRANCHES_INLINE}"
