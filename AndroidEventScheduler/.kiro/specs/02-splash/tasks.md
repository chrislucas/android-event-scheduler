# 02 — Splash — Tarefas

- [x] 1. Adicionar tags `SPLASH_LOGO`/`SPLASH_TITLE` em `SemanticsTags` e strings pt-BR.
- [x] 2. Contrato State/Event/Effect sealed (`state/event/effect`) e `SplashViewModel` (R2) + `SplashViewModelTest` escrito antes (TDD).
- [x] 3. `SplashContent`/`SplashScreen` com previews (R1) + `SplashScreenRobolectricTest`.
- [x] 4. `SplashModule` Koin + `splashEntry`; registrar em `AppModules` e `AppNavHost`, removendo o placeholder da spec 00 (R2.2).
- [x] 5. `core-splashscreen` + tema de partida escuro (R3).
- [x] 6. Atualizar `KoinModulesVerificationTest`; verificação final conforme `specs/README.md`.

## Critérios de aceite

A spec só é considerada concluída — e o PR só é aberto — quando **todos** estiverem marcados `[x]` (cada um verificado de fato, não presumido), além dos **critérios comuns** em `.kiro/specs/README.md`.

- [x] `SplashViewModelTest`: emite `NavigateToHome` após 1500 ms (tempo virtual), uma única vez mesmo com 2 `OnSplashStarted`.
- [x] `SplashScreenRobolectricTest`: logo (`Role.Image`, descrição) e título (`heading`) existem com as tags.
- [x] Splash substitui o placeholder da spec 00 e, ao navegar, sai do back stack: após 1,5 s a Home (placeholder) aparece, a Splash some e voltar finaliza a Activity (`SplashNavigationRobolectricTest`, `AppNavigationTest`).
- [x] Tema de partida escuro com `core-splashscreen` (sem flash branco, conferido no tema/manifest): a `MainActivity` declara `Theme.AndroidEventScheduler.Starting` e o `windowSplashScreenBackground` resolve para `background_dark` (teste). *Visual real do primeiro frame não verificado em aparelho.*
