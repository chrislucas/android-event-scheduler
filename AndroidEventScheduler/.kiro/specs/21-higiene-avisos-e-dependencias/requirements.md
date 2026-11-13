# 21 — Zero avisos e dependências em dia — Requisitos

## Introdução

O PR [#37](https://github.com/chrislucas/android-event-scheduler/pull/37) corrigiu os erros simples do código de produção e criou a regra `arquitetura.md` §5.7. Ficaram de fora, de propósito, quatro grupos de avisos:

1. **avisos de depreciação nos testes**: shadows do Robolectric, `BaseCursor` e `Dialog.onBackPressed()`;
2. **dependências desatualizadas** apontadas pelo Lint;
3. **texto fixo** na preview do `SystemThemeEffects`;
4. **`UnusedAttribute`** no manifest: `showWhenLocked` e `turnScreenOn` são intencionais, mas não estão marcados como tal.

Esta spec zera esses avisos e trava a regressão: depois dela, **qualquer aviso novo do compilador Kotlin reprova o build** e as atualizações de dependência passam a chegar por PRs automáticos do Dependabot.

O inventário abaixo foi levantado em 2026-11-13, no commit `93e0f39`. **Confira-o de novo antes de começar**: arquivos e versões disponíveis podem ter mudado.

Fora do escopo:
- funcionalidade nova ou mudança de comportamento para o usuário;
- migrar o AGP para outra versão principal (o Lint não aponta atualização do AGP 9.5.1);
- os avisos `WARNING: sun.misc.Unsafe…` impressos pelo próprio compilador Kotlin no JDK 25 (vêm da ferramenta, não do nosso código).

## Requisitos

### R1 — Testes sem avisos de depreciação
1. `Dialog.onBackPressed()` (deprecado) SHALL ser trocado pelo voltar do `OnBackPressedDispatcher` do diálogo, sem mudar o que o teste prova (voltar fecha o diálogo sem escolher nada). Ocorrências: `ColorPickerDialogRobolectricTest:100`, `StickerPickerDialogRobolectricTest:111`, `RecurrencePickerDialogRobolectricTest:302`, `HomeScreenRobolectricTest:431`, `UpsertScheduleScreenRobolectricTest:484`.
2. Os campos deprecados de `ShadowAlarmManager.ScheduledAlarm` (`type`, `allowWhileIdle`, `operation`) e de `ShadowPendingIntent` (`isBroadcastIntent`, `isActivityIntent`) SHALL ser trocados pelos acessores não deprecados. Ocorrências: `AndroidAlarmSchedulerRobolectricTest:48,50,57,62`, `BootRestoreIntegrationTest:130`, `SnoozeThenFireIntegrationTest:131,202`, `ScheduleNotifierRobolectricTest:100`.
3. `BaseCursor` e `ShadowContentResolver.setCursor` (deprecados) SHALL ser trocados por um `ContentProvider` de teste registrado no Robolectric, mantendo o caso "o provedor declara um tamanho acima do limite e o arquivo é recusado antes de abrir o stream". Ocorrências: `AgendaBackupRepositoryImplTest:35,238,323`.
4. Se para algum uso **não houver** substituto não deprecado na versão do Robolectric em uso (caso esperado: `ScheduledAlarm.operation`, que não tem acessor na 4.17), o acesso SHALL ficar **num único helper** em `core.testing`, com `@Suppress("DEPRECATION")` restrito a ele e um comentário dizendo o motivo e quando remover. O motivo também SHALL ir para o PR.
5. Nenhuma asserção SHALL ser removida ou enfraquecida para o aviso sumir.

### R2 — Dependências em dia
1. As versões apontadas pelo Lint SHALL ser atualizadas para a mais recente **estável** disponível na data da execução. Na data do levantamento:

   | Item | Atual | Disponível |
   |---|---|---|
   | Gradle (wrapper) | 9.6.0 | 9.9.1 |
   | Compose BOM | 2026.02.01 | 2026.09.00 |
   | Kotlin (e plugins `compose`/`serialization`) | 2.2.10 | 2.4.21 |
   | kotlinx-serialization-json | 1.9.0 | 1.13.0 |
   | MockK | 1.14.11 | 1.14.12 |
   | Robolectric | 4.17 | 4.18 |

2. Cada atualização SHALL manter a verificação completa verde. Se uma versão não puder ser adotada (incompatibilidade com KSP, detekt 2.0 alpha, Room, AGP ou JDK 25), ela SHALL ficar na mais recente compatível, com o motivo registrado no PR e em `ordem-de-execucao.md` §8.
3. Atualizar uma dependência pode trazer **depreciações novas** no código de produção ou nos testes. Elas SHALL ser corrigidas na mesma spec, seguindo R1.4 quando não houver substituto.
4. A checagem de "versão mais nova disponível" NÃO SHALL reprovar o build: ela depende da rede e da data, então o mesmo commit passaria hoje e falharia amanhã. A atualização contínua SHALL vir do **Dependabot** (`.github/dependabot.yml`), com PRs semanais para o catálogo do Gradle (`gradle/libs.versions.toml`), o wrapper e as GitHub Actions.

### R3 — Preview sem texto fixo
1. Os três textos da `SystemThemeEffectsPreview` ("Fundo e texto do tema", "Texto secundário", "Acento") SHALL vir de `strings.xml`, conforme `arquitetura.md` §5.5 (as previews usam os mesmos resources).

### R4 — Manifest sem `UnusedAttribute`
1. `android:showWhenLocked` e `android:turnScreenOn` da `FullScreenAlarmActivity` SHALL continuar no manifest (API 27+). Abaixo do 27, o código já usa os flags de janela equivalentes. O fato de serem intencionais SHALL ficar explícito com `tools:targetApi`, e o comentário do manifest SHALL citar o fallback.

### R5 — Travas contra regressão
1. Com os avisos zerados, o compilador Kotlin SHALL tratar **avisos como erros** (`allWarningsAsErrors`) em todas as compilações do `:app` (produção e testes).
2. O `:app:lintDebug` SHALL terminar **sem nenhum aviso**: as regras de versão (`NewerVersionAvailable`, `GradleDependency`, `AndroidGradlePluginVersion`) são desligadas no bloco `lint` (R2.4) e o Lint passa a usar `warningsAsErrors = true`.
3. A steering SHALL registrar as duas regras: em `arquitetura.md` §5.7, "nenhum aviso do compilador ou do Lint, nem nos testes" e `@Suppress("DEPRECATION")` só em helper isolado e justificado; em `fluxo-de-trabalho.md`, o Dependabot como fonte das atualizações e o que fazer com o PR dele.
