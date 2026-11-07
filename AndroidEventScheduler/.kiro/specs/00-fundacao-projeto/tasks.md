# 00 — Fundação do projeto — Tarefas

- [x] 1. Atualizar `libs.versions.toml` e `app/build.gradle.kts` (R1): pesquisar versões estáveis atuais, adicionar KSP, serialization, desugaring, opções de testes unitários. Validar `./gradlew :app:assembleDebug` já aqui (risco KSP/AGP 9).
- [x] 2. Mover `ui.theme` para `core.designsystem`; aplicar paleta escura e desligar cores dinâmicas (R4). Ajustar `MainActivity` para `enableEdgeToEdge()`.
- [x] 3. Criar `core.ui.CollectEffects` e `SemanticsTags` vazio (R5) + `CollectEffectsRobolectricTest` (efeito emitido com a tela parada é entregue uma vez ao voltar a `STARTED`). Nenhum ViewModel base.
- [x] 4. Criar `core.testing.MainDispatcherRule` e `FakeClock` (R7.1) + teste de `FakeClock`.
- [x] 5. Criar `core.di.DispatchersModule`, `app.AppModules`, `app.KoinInitializer`, `RememberMeApplication`; registrar no manifest o `InitializationProvider` (R3).
- [x] 6. Escrever `KoinModulesVerificationTest` (R3.4).
- [x] 7. Criar `core.navigation.AppRoute` e `app.AppNavHost` com placeholder em `Splash` (R6).
- [x] 8. Qualidade: `build-logic` com `androideventscheduler.code.quality` (detekt 2.0 alpha + ktlint 14.2.0), `config/detekt/detekt.yml`, `.editorconfig`, CI `ci-validation.yml` e hooks, gerados por `docs/project-foundation/bootstrap-foundation.sh`; código do template normalizado com `ktlintFormat` (R7.2–R7.5). Verificado: `ktlintCheck detekt testDebugUnitTest :app:assembleDebug` → BUILD SUCCESSFUL. O workflow roda no próprio PR desta spec; o hook é opcional por dev (`./scripts/setup-git-hooks.sh`).
- [x] 9. Smoke test Robolectric da `MainActivity` (R7.3); remover `ExampleUnitTest`/`ExampleInstrumentedTest`.
- [x] 10. Verificação final: `./gradlew testDebugUnitTest ktlintCheck detekt :app:assembleDebug`.

## Critérios de aceite

A spec só é considerada concluída — e o PR só é aberto — quando **todos** estiverem marcados `[x]` (cada um verificado de fato, não presumido), além dos **critérios comuns** em `.kiro/specs/README.md`.

- [x] `./gradlew ktlintCheck detekt testDebugUnitTest :app:assembleDebug` → BUILD SUCCESSFUL (inclui os testes novos desta spec).
- [x] `MainActivity` sobe (teste Robolectric) com `AndroidEventSchedulerTheme` escuro e **sem** cores dinâmicas mesmo com o sistema em modo claro (`ThemeRobolectricTest`); `enableEdgeToEdge()` ativo (barras transparentes, teste).
- [x] Koin é iniciado pelo App Startup: o `InitializationProvider` está no manifest mesclado com o `KoinInitializer` (teste `KoinStartupManifestRobolectricTest`), o initializer é idempotente e o grafo resolve os dispatchers (teste); `KoinModulesVerificationTest` passa. *Ajuste do critério:* o Robolectric não instancia `ContentProvider`s, então a ordem "antes de `Application.onCreate`" é garantia da plataforma e não é observável em JVM — os testes usam `TestRememberMeApplication`, que dispara o mesmo `AppInitializer`.
- [x] Rotas `@Serializable` existem em `core.navigation` e o `AppNavHost` abre em `Splash` (teste); `ExampleUnitTest`/`ExampleInstrumentedTest` removidos.
- [x] `CollectEffects` entrega cada efeito uma única vez e só em `STARTED` (teste); `MainDispatcherRule` e `FakeClock` têm teste; **nenhum** ViewModel base foi criado.
- [x] Build-logic, detekt, ktlint, CI (`ci-validation.yml`) e script `bootstrap-foundation.sh` presentes e funcionando.
