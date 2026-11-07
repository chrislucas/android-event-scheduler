# 06 — Home: gaveta lateral e filtros de calendário — Tarefas

- [ ] 1. Domain+Data: `HomePreferencesRepository` e `Impl` + `HomePreferencesRepositoryImplTest` (TDD) (R2.4–R2.5, R4).
- [ ] 2. `CalendarSource.label(...)` em `core.ui.format` + teste.
- [ ] 3. Estender `HomeUiState/Event/Effect` e `HomeViewModel` (pipeline com filtro e e-mail) + novos testes (R2, R3).
- [ ] 4. `CalendarDrawerSheet` com previews, tags e semântica (R2.2, R3).
- [ ] 5. Integrar `ModalNavigationDrawer` e botão de menu em `HomeScreen`, sem laço de sincronização do `drawerState` (R1) + `CalendarHomeDrawerRobolectricTest`.
- [ ] 6. Ligar `NavigateToSettings` em `homeEntry`/`AppNavHost`; registrar repositório no `HomeModule`.
- [ ] 7. Atualizar `KoinModulesVerificationTest`; verificação final conforme `specs/README.md`.

## Critérios de aceite

A spec só é considerada concluída — e o PR só é aberto — quando **todos** estiverem marcados `[x]` (cada um verificado de fato, não presumido), além dos **critérios comuns** em `.kiro/specs/README.md`.

- [ ] `HomePreferencesRepositoryImplTest`: padrão = todos visíveis, round-trip, id desconhecido ignorado, dois toggles seguidos preservam ambos.
- [ ] `HomeViewModelTest`: ocultar/reexibir calendário filtra mês e dia; todos ocultos → vazio sem erro; e-mail atualizado propaga; `OnSettingsClicked` fecha e navega; estado do drawer restaurado.
- [ ] `CalendarHomeDrawerRobolectricTest`: abre pelo menu; checkboxes com `Role.Checkbox` e `stateDescription`; atalho Configurações.
- [ ] Sem laço de sincronização entre `DrawerState` e o estado (teste cobre abrir/fechar por scrim).
