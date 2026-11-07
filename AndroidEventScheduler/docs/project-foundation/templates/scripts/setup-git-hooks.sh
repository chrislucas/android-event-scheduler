#!/usr/bin/env bash
#
# Ativa os git hooks versionados (pasta .githooks) neste clone. Idempotente.
# .git/hooks não é versionado; apontamos core.hooksPath para .githooks (que está no repo).
# Robusto ao layout: a raiz do git pode ser esta pasta OU uma pasta acima.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
HOOKS_ABS="${PROJECT_ROOT}/.githooks"
GIT_ROOT="$(git -C "${PROJECT_ROOT}" rev-parse --show-toplevel)"

case "${HOOKS_ABS}" in
  "${GIT_ROOT}/"*) HOOKS_REL="${HOOKS_ABS#"${GIT_ROOT}/"}" ;;
  *) HOOKS_REL="${HOOKS_ABS}" ;;
esac

chmod +x "${HOOKS_ABS}"/* 2>/dev/null || true

CURRENT="$(git -C "${GIT_ROOT}" config --local core.hooksPath || true)"
if [ "${CURRENT}" != "${HOOKS_REL}" ]; then
  git -C "${GIT_ROOT}" config --local core.hooksPath "${HOOKS_REL}"
  echo "[setup-git-hooks] core.hooksPath => ${HOOKS_REL} (git root: ${GIT_ROOT})"
else
  echo "[setup-git-hooks] core.hooksPath já configurado (${HOOKS_REL})"
fi
echo "[setup-git-hooks] Pronto. O pre-commit agora roda ktlint + detekt nos arquivos Kotlin em stage."
