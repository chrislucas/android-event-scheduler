# Prompt de execução — Spec 18-toque-duplo-criar-agendamento

Cole o texto abaixo no chat do Kiro:

---

Execute a spec **Toque duplo numa data cria evento ou lembrete** do projeto AndroidEventScheduler, localizada em `.kiro/specs/18-toque-duplo-criar-agendamento/`.

1. **Leia antes de qualquer código**:
   - `.kiro/steering/arquitetura.md` e `.kiro/steering/fluxo-de-trabalho.md` (inclusive §1.1 "Todo commit compila");
   - `.kiro/specs/README.md`, `.kiro/specs/criterios-de-aceite.md` e `.kiro/specs/ordem-de-execucao.md`;
   - `requirements.md`, `design.md` e `tasks.md` desta spec.

   Confira no código que as dependências (05, 06, 07 e 09, ver `ordem-de-execucao.md` §1 e §3) estão mergeadas. Se não estiverem, pare e me avise.
2. **Prepare**: crie a branch `feat/18-toque-duplo-criar-agendamento` a partir de `main` atualizada. Abra o PR somente ao final (passo 8).
3. **Execute `tasks.md` em ordem**, uma tarefa por vez. Para cada tarefa:
   - escreva os testes primeiro quando indicado (TDD) e implemente;
   - rode os testes do que mudou;
   - só então marque `- [x]` na própria `tasks.md`.

   Commits por camada, em conventional commits pt-BR. **Cada commit compila**: contrato `sealed` e os ramos que o tratam entram juntos, e o parâmetro novo entra com os call sites.
4. **Respeite a arquitetura**:
   - `feature.home` não importa `feature.upsert`; a navegação passa pelo efeito existente `NavigateToUpsert`.
   - O `HomeViewModel` despacha por família (`DayCellEvent`), sem `@Suppress` nem `else ->`.
   - Conteúdo Compose stateless com `@Preview`; tags em `SemanticsTags`; textos em `strings.xml` pt-BR.
   - Testes `given/when/then` em inglês, sem Turbine.
   - Nenhum UseCase.
5. **Koin e navegação**: nada novo no `AppModules`. No `AppNavHost`, só a guarda contra criação duplicada.
6. **Verifique** conforme `criterios-de-aceite.md`:
   - `./gradlew ktlintCheck detekt testDebugUnitTest :app:assembleDebug --rerun-tasks` → BUILD SUCCESSFUL, rodado duas vezes;
   - `git rebase --exec "cd AndroidEventScheduler && ./gradlew :app:compileDebugUnitTestKotlin -q" origin/main` → todos os commits compilam.

   Não diga que terminou sem ter rodado.
7. **Atualize `.kiro/specs/ordem-de-execucao.md` §8** com o status desta spec: data, branch/PR e desvios.
8. **Abra UM PR** (push da branch + `gh pr create` contra `main`) somente com todos os critérios de aceite marcados e a verificação verde. Siga o modelo de corpo de PR de `criterios-de-aceite.md` e peça o review do Copilot. Se algum critério não puder ser verificado, não abra o PR: explique e me consulte.
9. **Relatório final** (curto):
   - o que foi implementado, por requisito;
   - resultados da verificação;
   - o que **não** foi possível verificar (gesto real e TalkBack em aparelho);
   - desvios do design, com justificativa.

Observações específicas desta spec:
- O toque simples **não pode** ganhar atraso. Não use `combinedClickable(onDoubleClick)` nem `detectTapGestures(onDoubleTap)` na célula: os dois adiam o `onClick`.
- O toque duplo é detectado por um `pointerInput` passivo com o tempo do evento (`uptimeMillis`), alimentando o `DayDoubleTapDetector` puro.
- Com TalkBack o toque duplo é "ativar": a ação de acessibilidade personalizada é obrigatória.

---
