# 02 — Splash — Tarefas

- [ ] 1. Adicionar tags `SPLASH_LOGO`/`SPLASH_TITLE` em `SemanticsTags` e strings pt-BR.
- [ ] 2. Contrato State/Event/Effect sealed (`state/event/effect`) e `SplashViewModel` (R2) + `SplashViewModelTest` escrito antes (TDD).
- [ ] 3. `SplashContent`/`SplashScreen` com previews (R1) + `SplashScreenRobolectricTest`.
- [ ] 4. `SplashModule` Koin + `splashEntry`; registrar em `AppModules` e `AppNavHost`, removendo o placeholder da spec 00 (R2.2).
- [ ] 5. `core-splashscreen` + tema de partida escuro (R3).
- [ ] 6. Atualizar `KoinModulesVerificationTest`; verificação final conforme `specs/README.md`.

## Critérios de aceite

A spec só é considerada concluída — e o PR só é aberto — quando **todos** estiverem marcados `[x]` (cada um verificado de fato, não presumido), além dos **critérios comuns** em `.kiro/specs/README.md`.

- [ ] `SplashViewModelTest`: emite `NavigateToHome` após 1500 ms (tempo virtual), uma única vez mesmo com 2 `OnSplashStarted`.
- [ ] `SplashScreenRobolectricTest`: logo (`Role.Image`, descrição) e título (`heading`) existem com as tags.
- [ ] Splash substitui o placeholder da spec 00 e, ao navegar, sai do back stack (voltar na Home não retorna à Splash).
- [ ] Tema de partida escuro com `core-splashscreen` (sem flash branco, conferido no tema/manifest).
