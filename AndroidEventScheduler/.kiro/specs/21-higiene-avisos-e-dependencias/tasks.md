# 21 — Zero avisos e dependências em dia — Tarefas

- [x] 1. Refazer o inventário: compilar do zero (`--rerun-tasks`) e listar os `w:`; rodar `:app:lintDebug` e listar os avisos. Comparar com `requirements.md` e anotar diferenças no PR.
- [x] 2. `Dialog.onBackPressed()` → helper `pressBackOnLatestDialog()` (`core.testing`) nos 5 testes (R1.1).
- [x] 3. Shadows do `AlarmManager`/`PendingIntent` → acessores não deprecados; `operation` isolado num helper se não houver substituto (R1.2, R1.4).
- [x] 4. `BaseCursor`/`setCursor` → `ContentProvider` de teste no `AgendaBackupRepositoryImplTest` (R1.3).
- [x] 5. Preview do `SystemThemeEffects` com strings de `strings.xml` (R3) e `tools:targetApi` + comentário no manifest (R4).
- [x] 6. Atualizar dependências, um grupo por commit, na ordem do `design.md` D3; corrigir as depreciações novas no mesmo commit do grupo que as trouxe (R2.1–R2.3).
- [x] 7. `.github/dependabot.yml` (R2.4).
- [x] 8. Travas: `allWarningsAsErrors` e Lint com `warningsAsErrors` + regras de versão desligadas; teste manual da trava (R5.1, R5.2).
- [x] 9. Steering: `arquitetura.md` §5.7 e `fluxo-de-trabalho.md` (R5.3).
- [x] 10. Verificação final conforme [`criterios-de-aceite.md`](../criterios-de-aceite.md), incluindo a checagem por commit.

## Critérios de aceite

A spec só é considerada concluída, e o PR só é aberto, quando **todos** estiverem marcados `[x]`, cada um verificado de fato e não presumido. Somam-se a eles os **critérios gerais** em [`.kiro/specs/criterios-de-aceite.md`](../criterios-de-aceite.md).

- [x] Compilação do zero (`./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin --rerun-tasks`) sem **nenhuma** linha `w:` (os `WARNING: sun.misc.Unsafe` do JDK não contam).
- [x] `:app:lintDebug` sem nenhum aviso nem erro.
- [x] Nenhum `@Suppress("DEPRECATION")` novo fora do helper previsto em R1.4; cada um justificado no código e no PR.
- [x] Mesmo número de testes antes e depois de R1, sem asserção removida (números no PR).
- [x] Dependências na versão estável mais recente na data da execução, ou na mais recente compatível com o motivo registrado (tabela "antes → depois" no PR).
- [x] `.github/dependabot.yml` válido (o GitHub não aponta erro na aba *Insights → Dependency graph → Dependabot* depois do merge; até lá, conferido contra a documentação).
- [x] `allWarningsAsErrors` ativo e testado: um aviso introduzido de propósito reprova o build (registrado no PR e desfeito).
- [x] Steering atualizada (§5.7 e `fluxo-de-trabalho.md`).
- [x] `ordem-de-execucao.md` §8 atualizado.
