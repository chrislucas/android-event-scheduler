# 05 — Home: calendário mensal e agenda do dia — Tarefas

- [ ] 1. Domain: `MonthGridBuilder` + `MonthGridBuilderTest` (TDD) (R1.1).
- [ ] 2. Presentation: `CalendarDayCell`, `HomeUiState`, `HomeUiEvent`, `HomeUiEffect`.
- [ ] 3. Presentation: `HomeViewModel` (pipeline reativo, `SavedStateHandle`, regras R1/R3/R4) + `HomeViewModelTest` (TDD).
- [ ] 4. Componentes: `MonthGridSection` (R1, R8), `ScheduleListItem`/`DayScheduleList` (R4), `QuickAddBar` (R5) — com previews e tags/strings.
- [ ] 5. `MonthPickerSheetContent` + `ModalBottomSheet` (R3) com preview.
- [ ] 6. `HomeScreen`/`HomeContent` (topBar, bottomBar, insets, `NotificationPermissionEffect`) + `HomeScreenRobolectricTest` (R2, R7).
- [ ] 7. `HomeModule`, `homeEntry`; ligar no `AppModules`/`AppNavHost` (Splash → Home, Home → Search/Upsert/Detail/Settings, com destinos ainda inexistentes apontando para placeholders removíveis).
- [ ] 8. Atualizar `KoinModulesVerificationTest`; verificação final conforme `specs/README.md`.

## Critérios de aceite

A spec só é considerada concluída — e o PR só é aberto — quando **todos** estiverem marcados `[x]` (cada um verificado de fato, não presumido), além dos **critérios comuns** em `.kiro/specs/README.md`.

- [ ] `MonthGridBuilderTest`: 42 datas, mês começando no sábado, fevereiro bissexto, nov/2026.
- [ ] `HomeViewModelTest`: estado inicial = hoje, dia de mês vizinho muda o mês, recorrente aparece, ≤ 3 indicadores, multi-dia, atualização reativa, clamp 31→30, restauração por `SavedStateHandle`, erro → `Error`.
- [ ] `HomeScreenRobolectricTest`: título abre o seletor de mês, escolha de mês atualiza o título, clique em item emite navegação, estado vazio, `contentDescription` das células, busca e adição rápida.
- [ ] Home pede a permissão de notificação (`NotificationPermissionEffect`); placeholders para Search/Upsert/Detail/Settings listados no relatório.
