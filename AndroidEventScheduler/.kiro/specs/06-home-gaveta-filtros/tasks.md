# 06 — Home: gaveta lateral e filtros de calendário — Tarefas

- [x] 1. Domain+Data: `HomePreferencesRepository` e `Impl` + `HomePreferencesRepositoryImplTest` (TDD) (R2.4–R2.5, R4).
- [x] 2. `CalendarSource.label(...)` em `core.ui.format` + teste.
- [x] 3. Estender `HomeUiState/Event/Effect` e `HomeViewModel` (pipeline com filtro e e-mail) + novos testes (R2, R3).
- [x] 4. `CalendarDrawerSheet` com previews, tags e semântica (R2.2, R3).
- [x] 5. Integrar `ModalNavigationDrawer` e botão de menu em `HomeScreen`, sem laço de sincronização do `drawerState` (R1) + `CalendarHomeDrawerRobolectricTest`.
- [x] 6. Ligar `NavigateToSettings` em `homeEntry`/`AppNavHost`; registrar repositório no `HomeModule`.
- [x] 7. Atualizar `KoinModulesVerificationTest`; verificação final conforme `specs/README.md`. (Nenhum tipo extra necessário: `HomePreferencesRepositoryImpl` só depende do `DataStore<Preferences>` já definido; o teste verifica o grafo agregado e passou sem alteração.)

## Critérios de aceite

A spec só é considerada concluída — e o PR só é aberto — quando **todos** estiverem marcados `[x]` (cada um verificado de fato, não presumido), além dos **critérios comuns** em `.kiro/specs/README.md`.

- [x] `HomePreferencesRepositoryImplTest`: padrão = todos visíveis, round-trip, id desconhecido ignorado, dois toggles seguidos preservam ambos.
- [x] `HomeViewModelTest`: ocultar/reexibir calendário filtra mês e dia; todos ocultos → vazio sem erro; e-mail atualizado propaga; `OnSettingsClicked` fecha e navega; estado do drawer restaurado.
- [x] `CalendarHomeDrawerRobolectricTest`: abre pelo menu; checkboxes com `Role.Checkbox` e `stateDescription`; atalho Configurações.
- [x] Sem laço de sincronização entre `DrawerState` e o estado (teste cobre abrir/fechar por scrim).
