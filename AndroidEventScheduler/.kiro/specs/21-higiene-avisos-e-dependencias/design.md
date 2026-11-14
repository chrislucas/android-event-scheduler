# 21 — Zero avisos e dependências em dia — Design

## Visão geral

```
app/src/test/...                                 (alterado: R1, substitutos dos usos deprecados)
app/src/test/.../core/testing/ScheduledAlarms.kt (novo, só se R1.4 se aplicar: acesso isolado ao campo deprecado)
gradle/libs.versions.toml                        (alterado: R2)
gradle/wrapper/gradle-wrapper.properties         (alterado: R2, via ./gradlew wrapper)
.github/dependabot.yml                           (novo: R2.4)
app/src/main/.../core/ui/theme/SystemThemeEffects.kt, res/values/strings.xml  (alterado: R3)
app/src/main/AndroidManifest.xml                 (alterado: R4)
app/build.gradle.kts (ou build-logic)            (alterado: R5, allWarningsAsErrors e lint)
.kiro/steering/arquitetura.md, fluxo-de-trabalho.md  (alterado: R5.3)
```

Nenhuma permissão, rota, módulo Koin, tag ou UseCase novo. Nenhuma mudança de comportamento para o usuário.

## Decisões

### D1 — Ordem: primeiro zerar avisos, depois atualizar, por último travar
Zerar os avisos na versão atual (R1, R3, R4) dá uma base limpa. Assim, toda depreciação que aparecer durante a atualização (R2) vem claramente dela. A trava (R5) entra por último, quando o build já está limpo; antes disso, ela reprovaria o próprio trabalho em andamento.

### D2 — Substitutos previstos para R1 (confirmar na execução)

| Uso deprecado | Substituto | Observação |
|---|---|---|
| `Dialog.onBackPressed()` | `(dialog as ComponentDialog).onBackPressedDispatcher.onBackPressed()` | O `Dialog` do Compose é um `ComponentDialog`. Para não repetir o cast nos 5 testes, criar um helper de teste `pressBackOnLatestDialog()` em `core.testing` |
| `ScheduledAlarm.type` | `alarm.getType()` | Na 4.17, `getType()` e `isAllowWhileIdle()` existem. No Kotlin, `alarm.type` resolve para o **campo** público deprecado, não para o getter: chame o método explicitamente (o ktlint/detekt podem sugerir a sintaxe de propriedade; ignore a sugestão com um comentário de uma linha, sem `@Suppress`) |
| `ScheduledAlarm.allowWhileIdle` | `isAllowWhileIdle()` | idem |
| `ScheduledAlarm.operation` | **sem acessor na 4.17** | Conferir na 4.18 (R2). Se continuar sem acessor: helper único `ScheduledAlarm.pendingOperation()` com `@Suppress("DEPRECATION")` (R1.4) |
| `ShadowPendingIntent.isBroadcastIntent` | `isBroadcast()` | existe na 4.17 |
| `ShadowPendingIntent.isActivityIntent` | `isActivity()` | existe na 4.17 |
| `BaseCursor` + `setCursor` | `ContentProvider` de teste (`query` devolve `MatrixCursor` com `OpenableColumns.SIZE`) registrado com `Robolectric.setupContentProvider` ou `ShadowContentResolver.registerProviderInternal` | Conferir se `registerInputStream` continua valendo com um provider registrado; se não, o provider também implementa `openFile`/`openAssetFile` |

### D3 — Atualização de dependências, um grupo por commit
Cada commit atualiza um grupo coeso e mantém a verificação verde (`fluxo-de-trabalho.md` §1.1):

1. MockK e Robolectric (só testes; menor risco);
2. Compose BOM;
3. Kotlin + plugins `compose`/`serialization` + kotlinx-serialization-json + **KSP compatível** (as versões do KSP2 não seguem mais as do Kotlin; conferir a matriz de compatibilidade) + conferir Room e detekt;
4. Gradle wrapper (`./gradlew wrapper --gradle-version <v>`, que atualiza também o `gradle-wrapper.jar` e o checksum validado na CI).

O grupo 3 é o de maior risco: o detekt 2.0 alpha e o Room/KSP podem não aceitar o Kotlin mais novo. Se travar, fique na mais recente compatível (R2.2) e registre o motivo. Nunca rebaixe o detekt nem desligue regras para "passar".

### D4 — Versão nova não reprova o build; Dependabot cuida disso
`NewerVersionAvailable`/`GradleDependency`/`AndroidGradlePluginVersion` consultam a rede e mudam de resultado com o tempo. Como trava de build, isso seria não determinístico: o mesmo commit passaria hoje e falharia amanhã. Por isso essas regras são **desligadas** no Lint (R5.2), e a atualização vira processo: Dependabot semanal, com PRs agrupados.

```yaml
# .github/dependabot.yml (esboço)
version: 2
updates:
  - package-ecosystem: gradle
    directory: /AndroidEventScheduler
    schedule: { interval: weekly }
    groups:
      kotlin: { patterns: ["org.jetbrains.kotlin*", "com.google.devtools.ksp*"] }
      androidx: { patterns: ["androidx.*"] }
      test: { patterns: ["io.mockk*", "org.robolectric*", "junit*"] }
  - package-ecosystem: github-actions
    directory: /
    schedule: { interval: weekly }
```

O PR do Dependabot segue o fluxo normal: a CI tem de ficar verde, e depreciação nova é corrigida no próprio PR antes do merge.

### D5 — Trava do compilador
`allWarningsAsErrors = true` em `kotlin { compilerOptions { … } }`. Se for aplicado no convention plugin (`build-logic`), vale para qualquer módulo futuro; preferir esse caminho. Os avisos do JDK impressos pelo compilador (`sun.misc.Unsafe`) não são diagnósticos do Kotlin e não são afetados.

### D6 — Strings da preview
Três strings novas com nome por contexto (`arquitetura.md` §5.5), usadas só na preview: `theme_preview_background_text`, `theme_preview_secondary_text`, `theme_preview_accent_text`. Como a preview fica em `main`, elas não acionam `UnusedResources`.

## Testes

- Os testes alterados em R1 continuam provando a mesma coisa. Rode cada classe isoladamente antes e depois da troca e confira que o número de testes e de asserções é o mesmo.
- R5: um teste manual da trava. Introduza temporariamente um aviso (ex.: variável não usada) e confira que `compileDebugKotlin` falha. Depois desfaça. Registre no PR.
- Verificação completa duas vezes (`criterios-de-aceite.md`).
