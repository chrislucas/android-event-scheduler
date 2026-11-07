# Prompt de execução — Spec 03-configuracoes

Cole o texto abaixo no chat do Kiro:

---

Execute a spec **Configurações** do projeto AndroidEventScheduler, localizada em `.kiro/specs/03-configuracoes/`.

1. **Leia antes de qualquer código**: `.kiro/steering/arquitetura.md`, `.kiro/steering/fluxo-de-trabalho.md`, `.kiro/specs/README.md` (convenções comuns e adaptações do PDF), e `requirements.md`, `design.md` e `tasks.md` desta spec. Leia também `.kiro/specs/ordem-de-execucao.md` e confira que as specs/tasks das quais esta depende (§1 e §3) já estão implementadas no código; se não estiverem, pare e me avise.
2. **Prepare**: crie a branch `feat/03-configuracoes` a partir de `main` (atualizada). Abra o PR somente ao final (passo 8).
3. **Execute `tasks.md` em ordem**, uma tarefa por vez. Para cada tarefa: escreva os testes primeiro quando indicado (TDD), implemente, rode os testes do que mudou e só então marque `- [x]` na própria `tasks.md`. Ao terminar as tasks, verifique **um a um** os **Critérios de aceite** (seção no fim do `tasks.md`) e os critérios comuns do README, marcando `[x]` só o que foi realmente verificado. Commits por camada (conventional commits em pt-BR).
4. **Respeite a arquitetura**: pacotes `core.*`/`feature.<nome>.{data,domain,presentation,di}` sob `com.br.androideventscheduler`; nenhuma feature importa outra; `domain` sem Android; ViewModel com `StateFlow` + `Channel`; conteúdo Compose stateless com `@Preview`; tags de semântica em `SemanticsTags`; textos em `strings.xml` pt-BR; testes `given/when/then` em inglês, sem Turbine. Só crie UseCase se estiver previsto no `design.md` desta spec.
5. **Use Koin**: registre os módulos em `AppModules` e a navegação em `AppNavHost`; atualize o `KoinModulesVerificationTest`.
6. **Verifique** ao final: `./gradlew ktlintCheck detekt testDebugUnitTest :app:assembleDebug` → BUILD SUCCESSFUL. Não diga que terminou sem ter rodado.
7. **Atualize `.kiro/specs/ordem-de-execucao.md`**: status desta spec em §8 (data, branch/PR, desvios) e, se alguma dependência real diferiu do previsto ou a ordem recomendada mudou, corrija §1–§5, a tabela do `README.md` e registre no histórico.
8. **Abra UM PR** (push da branch + `gh pr create` contra `main`) somente com todos os critérios de aceite marcados e a verificação verde; o corpo do PR traz o checklist dos critérios e o resultado da verificação. Peça o review do Copilot. Se algum critério não puder ser verificado, não abra o PR: explique e me consulte.
9. **Relatório final** (curto): o que foi implementado por requisito, resultados da verificação, o que **não** foi possível verificar (ex.: comportamento real em aparelho), desvios do design com justificativa e placeholders temporários deixados.

Observações específicas desta spec: A leitura de `UserSettings` é compartilhada (`core.preferences`); nenhuma feature pode importar `feature.settings`. A tela carrega o rascunho uma única vez.

---
