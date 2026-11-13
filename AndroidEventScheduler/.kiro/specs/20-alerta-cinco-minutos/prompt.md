# Prompt de execução — Spec 20-alerta-cinco-minutos

Cole o texto abaixo no chat do Kiro:

---

Execute a spec **Alerta 5 minutos antes** do projeto AndroidEventScheduler, localizada em `.kiro/specs/20-alerta-cinco-minutos/`.

1. **Leia antes de qualquer código**:
   - `.kiro/steering/arquitetura.md` e `.kiro/steering/fluxo-de-trabalho.md` (inclusive §1.1 "Todo commit compila");
   - `.kiro/specs/README.md`, `.kiro/specs/criterios-de-aceite.md` e `.kiro/specs/ordem-de-execucao.md`;
   - `docs/formato-agenda-json.md`;
   - `requirements.md`, `design.md` e `tasks.md` desta spec.

   Confira no código que as dependências (01, 03, 04, 07, 10, 17) estão mergeadas. Se não estiverem, pare e me avise.
2. **Prepare**: crie a branch `feat/20-alerta-cinco-minutos` a partir de `main` atualizada. Abra o PR somente ao final.
3. **Execute `tasks.md` em ordem**, com TDD onde indicado, marcando `- [x]` só depois de implementar e testar. Commits por camada, em conventional commits pt-BR; **cada commit compila** (o valor novo do enum entra junto com os testes que o usam).
4. **Respeite o escopo**: só o valor `FIVE_MINUTES` no catálogo. Nada de antecedência livre, múltiplos alertas ou mudança de padrão. Nenhuma migração, string, tag, rota, módulo Koin ou UseCase novo; se algum se mostrar necessário, registre como desvio no PR.
5. **Verificação**: `./gradlew ktlintFormat` e depois `./gradlew ktlintCheck detekt testDebugUnitTest :app:assembleDebug --rerun-tasks` → BUILD SUCCESSFUL **duas vezes**; checagem por commit com `git rebase --exec`.
6. **Documente**: `ordem-de-execucao.md` §8 e histórico, `README.md` das specs, `docs/formato-agenda-json.md`.
7. **PR**: push da branch e um PR contra `main` com o modelo de `criterios-de-aceite.md`, o mapeamento requisito → teste, o que não foi verificado em aparelho e o review do Copilot.
