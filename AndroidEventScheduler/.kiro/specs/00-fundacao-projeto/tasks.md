# 00 — Fundação do projeto — Tarefas

- [ ] 1. Atualizar `libs.versions.toml` e `app/build.gradle.kts` (R1): pesquisar versões estáveis atuais, adicionar KSP, serialization, desugaring, opções de testes unitários. Validar `./gradlew :app:assembleDebug` já aqui (risco KSP/AGP 9).
- [ ] 2. Mover `ui.theme` para `core.designsystem`; aplicar paleta escura e desligar cores dinâmicas (R4). Ajustar `MainActivity` para `enableEdgeToEdge()`.
- [ ] 3. Criar `core.ui.CollectEffects` e `SemanticsTags` vazio (R5) + `CollectEffectsRobolectricTest` (efeito emitido com a tela parada é entregue uma vez ao voltar a `STARTED`). Nenhum ViewModel base.
- [ ] 4. Criar `core.testing.MainDispatcherRule` e `FakeClock` (R7.1) + teste de `FakeClock`.
- [ ] 5. Criar `core.di.DispatchersModule`, `app.AppModules`, `app.KoinInitializer`, `RememberMeApplication`; registrar no manifest o `InitializationProvider` (R3).
- [ ] 6. Escrever `KoinModulesVerificationTest` (R3.4).
- [ ] 7. Criar `core.navigation.AppRoute` e `app.AppNavHost` com placeholder em `Splash` (R6).
- [x] 8. Qualidade: `build-logic` com `androideventscheduler.code.quality` (detekt 2.0 alpha + ktlint 14.2.0), `config/detekt/detekt.yml`, `.editorconfig`, CI `ci-validation.yml` e hooks, gerados por `docs/project-foundation/bootstrap-foundation.sh`; código do template normalizado com `ktlintFormat` (R7.2–R7.5). Verificado: `ktlintCheck detekt testDebugUnitTest :app:assembleDebug` → BUILD SUCCESSFUL. Pendente: rodar o workflow no GitHub e ativar o hook (`./scripts/setup-git-hooks.sh`).
- [ ] 9. Smoke test Robolectric da `MainActivity` (R7.3); remover `ExampleUnitTest`/`ExampleInstrumentedTest`.
- [ ] 10. Verificação final: `./gradlew testDebugUnitTest ktlintCheck detekt :app:assembleDebug`.

## Critérios de aceite

A spec só é considerada concluída — e o PR só é aberto — quando **todos** estiverem marcados `[x]` (cada um verificado de fato, não presumido), além dos **critérios comuns** em `.kiro/specs/README.md`.

- [ ] `./gradlew ktlintCheck detekt testDebugUnitTest :app:assembleDebug` → BUILD SUCCESSFUL (inclui os testes novos desta spec).
- [ ] `MainActivity` sobe (teste Robolectric) com `AndroidEventSchedulerTheme` escuro e **sem** cores dinâmicas; `enableEdgeToEdge()` ativo.
- [ ] Koin é iniciado pelo App Startup antes de `Application.onCreate` (teste) e `KoinModulesVerificationTest` passa.
- [ ] Rotas `@Serializable` existem em `core.navigation` e o `AppNavHost` abre em `Splash` (teste); `ExampleUnitTest`/`ExampleInstrumentedTest` removidos.
- [ ] `CollectEffects` entrega cada efeito uma única vez e só em `STARTED` (teste); `MainDispatcherRule` e `FakeClock` têm teste; **nenhum** ViewModel base foi criado.
- [ ] Build-logic, detekt, ktlint, CI (`ci-validation.yml`) e script `bootstrap-foundation.sh` presentes e funcionando.
