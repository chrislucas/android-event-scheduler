# Prompt de execução — Spec 21-higiene-avisos-e-dependencias

Cole o texto abaixo no chat do Kiro:

---

Execute a spec **Zero avisos e dependências em dia** do projeto AndroidEventScheduler, localizada em `.kiro/specs/21-higiene-avisos-e-dependencias/`.

1. **Leia antes de qualquer código**:
   - `.kiro/steering/arquitetura.md` (em especial §5.5 e §5.7) e `.kiro/steering/fluxo-de-trabalho.md` (inclusive §1.1 "Todo commit compila");
   - `.kiro/specs/README.md`, `.kiro/specs/criterios-de-aceite.md` e `.kiro/specs/ordem-de-execucao.md`;
   - `requirements.md`, `design.md` e `tasks.md` desta spec.
2. **Prepare**: crie a branch `chore/21-higiene-avisos-e-dependencias` a partir de `main` atualizada. Abra o PR somente ao final (passo 7).
3. **Execute `tasks.md` em ordem**, uma tarefa por vez. Para cada tarefa:
   - comece refazendo o inventário (tarefa 1), porque o da spec é de 2026-11-13;
   - rode as classes de teste afetadas antes e depois de cada troca e confira que o número de testes é o mesmo;
   - só então marque `- [x]` na própria `tasks.md`.

   Commits por tema, em conventional commits pt-BR. **Cada commit compila e passa nos testes.** Cada grupo de dependências vai num commit próprio, junto com as correções de depreciação que ele trouxe.
4. **Regras desta spec**:
   - Não remova nem enfraqueça asserções para um aviso sumir.
   - `@Suppress("DEPRECATION")` só no helper isolado do R1.4, com o motivo e quando remover.
   - Não rebaixe o detekt nem desligue regras do detekt ou do ktlint.
   - Não ative `allWarningsAsErrors` antes de o build estar sem avisos (design D1).
   - Se uma versão nova não for compatível, fique na mais recente que funciona e registre o motivo. Não force.
5. **Verifique** conforme `criterios-de-aceite.md`:
   - `./gradlew ktlintCheck detekt testDebugUnitTest :app:assembleDebug :app:lintDebug --rerun-tasks` → BUILD SUCCESSFUL, rodado duas vezes;
   - compilação do zero sem nenhuma linha `w:`;
   - `git rebase --exec "cd AndroidEventScheduler && ./gradlew :app:compileDebugUnitTestKotlin -q" origin/main` → todos os commits compilam.

   Não diga que terminou sem ter rodado.
6. **Atualize `.kiro/specs/ordem-de-execucao.md` §8** com o status desta spec: data, branch/PR, versões adotadas e desvios.
7. **Abra UM PR** (push da branch + `gh pr create` contra `main`) somente com todos os critérios de aceite marcados e a verificação verde. Siga o modelo de corpo de PR de `criterios-de-aceite.md`, inclua a tabela de versões "antes → depois" e peça o review do Copilot. Se algum critério não puder ser verificado, não abra o PR: explique e me consulte.
8. **Relatório final** (curto):
   - o que foi feito, por requisito;
   - versões adotadas e as que ficaram para trás, com o motivo;
   - helpers com `@Suppress` que sobraram;
   - resultados da verificação.

Observações específicas desta spec:
- O Kotlin (grupo 3 do design D3) é o passo de maior risco: confira a compatibilidade do KSP, do Room e do detekt 2.0 alpha antes de trocar a versão.
- `./gradlew wrapper --gradle-version <v>` regenera o wrapper inteiro. Commite o `.jar` e o `.properties` juntos (a CI valida o checksum).

---
