# Prompt de execução — Spec 19-tema-claro-escuro

Cole o texto abaixo no chat do Kiro:

---

Execute a spec **Tema claro e escuro persistente** do projeto AndroidEventScheduler, localizada em `.kiro/specs/19-tema-claro-escuro/`.

1. **Leia antes de qualquer código**:
   - `.kiro/steering/arquitetura.md` e `.kiro/steering/fluxo-de-trabalho.md` (inclusive §1.1 "Todo commit compila");
   - `.kiro/specs/README.md`, `.kiro/specs/criterios-de-aceite.md` e `.kiro/specs/ordem-de-execucao.md`;
   - `docs/decisoes/persistencia-do-tema-datastore-vs-sharedpreferences.md`;
   - `requirements.md`, `design.md` e `tasks.md` desta spec.

   Confira no código que as dependências (00, 02, 03, 14, ver `ordem-de-execucao.md` §1 e §3) estão mergeadas. Se não estiverem, pare e me avise.
2. **Prepare**: crie a branch `feat/19-tema-claro-escuro` a partir de `main` atualizada. Abra o PR somente ao final (passo 8).
3. **Execute `tasks.md` em ordem**, uma tarefa por vez. Para cada tarefa:
   - escreva os testes primeiro quando indicado (TDD) e implemente;
   - rode os testes do que mudou;
   - só então marque `- [x]` na própria `tasks.md`.

   Commits por camada, em conventional commits pt-BR. **Cada commit compila**: interface, implementações, fakes e Koin juntos; membro novo de `sealed` com os ramos que o tratam.
4. **Respeite a arquitetura**:
   - `ThemeMode` e o repositório ficam em `core.preferences`; nenhuma feature importa outra.
   - O mesmo `DataStore<Preferences>` único, sem `SharedPreferences`.
   - Configurações com a família `AppearanceEvent`, sem `@Suppress` nem `else ->`.
   - Conteúdo Compose stateless com `@Preview`; tags em `SemanticsTags`; textos em `strings.xml` pt-BR.
   - Testes `given/when/then` em inglês, sem Turbine.
   - Nenhum UseCase.
5. **Koin**: registre o repositório no `PreferencesModule`. O `KoinModulesVerificationTest` deve continuar verde.
6. **Verifique** conforme `criterios-de-aceite.md`:
   - `./gradlew ktlintCheck detekt testDebugUnitTest :app:assembleDebug --rerun-tasks` → BUILD SUCCESSFUL, rodado duas vezes;
   - `git rebase --exec "cd AndroidEventScheduler && ./gradlew :app:compileDebugUnitTestKotlin -q" origin/main` → todos os commits compilam.

   Não diga que terminou sem ter rodado.
7. **Atualize `.kiro/specs/ordem-de-execucao.md` §8** com o status desta spec: data, branch/PR e desvios.
8. **Abra UM PR** (push da branch + `gh pr create` contra `main`) somente com todos os critérios de aceite marcados e a verificação verde. Siga o modelo de corpo de PR de `criterios-de-aceite.md` e peça o review do Copilot. Se algum critério não puder ser verificado, não abra o PR: explique e me consulte.
9. **Relatório final** (curto):
   - o que foi implementado, por requisito;
   - resultados da verificação;
   - o que **não** foi possível verificar (aparência real, Splash em partida a frio);
   - desvios do design, com justificativa.

Observações específicas desta spec:
- Não grave o tema pelo `UserSettingsRepository.save()`: ele regrava o formulário inteiro. Use o repositório próprio.
- Não use `runBlocking` para ler o tema na partida: segure a SplashScreen com `setKeepOnScreenCondition`.
- O tema escuro atual e o padrão (Escuro) não podem mudar para quem já usa o app.

---
