# 07 — Criar e editar agendamento — Tarefas

- [x] 1. Domain (TDD): `SaveScheduleError`, `SaveScheduleUseCase` + `SaveScheduleUseCaseTest` (R5).
- [x] 2. Domain (TDD): `ScheduleDraftTimeRules` + testes (R3.2–R3.4, R1.4).
- [x] 3. Presentation: `UpsertScheduleUiState/Event/Effect`, `ActiveInlinePicker`.
- [x] 4. `UpsertScheduleViewModel` (carga criação/edição, regras, salvar) + `UpsertScheduleViewModelTest` (R1, R2, R5).
- [x] 5. Componentes de formulário: `ModeSegmentedPill`, `TitleRow`, `SettingRow`, `ReminderOffsetRow`, `CalendarSourceRow`, campos de local/notas — com previews e tags (R1, R4, R6).
- [x] 6. `DateTimeSection` + `InlineDatePicker` (conversão UTC!) + `InlineTimePicker` + `QuickChipsRow` (R3) — previews por estado.
- [x] 7. `UpsertScheduleScreen`/`Content` + `UpsertScheduleScreenRobolectricTest`.
- [x] 8. `UpsertModule` + `upsertEntry`; ligar `AppNavHost` (Home → criar com data; Detail → editar). _Detail → editar ligado; a navegação Home → criar com data é da spec 05 (a rota `UpsertSchedule(initialDateEpochDay)` já é tratada)._
- [x] 9. Atualizar `KoinModulesVerificationTest`; verificação final conforme `specs/README.md`.

## Critérios de aceite

A spec só é considerada concluída — e o PR só é aberto — quando **todos** estiverem marcados `[x]` (cada um verificado de fato, não presumido), além dos **critérios comuns** em `.kiro/specs/README.md`.

- [x] `SaveScheduleUseCaseTest`: título em branco, fim antes do início, sucesso persiste e agenda, falha do alarme não derruba, falha do repositório não agenda, `trim`.
- [x] `ScheduleDraftTimeRulesTest` e `UpsertScheduleViewModelTest` cobrem R1–R5 (modos, duração preservada, chips, edição, item inexistente, duplo toque).
- [x] `UpsertScheduleScreenRobolectricTest`: abas `Role.Tab`, um seletor inline por vez, dia inteiro oculta horas, linha `Repetição: Não repetir`.
- [x] Conversão UTC do `DatePicker` coberta por teste (data escolhida = data salva).
- [x] Fluxo criar → salvar → aparece na Home verificável em teste de integração ou descrito no relatório. _(parcial: Home na spec 05 — verificado criar → salvar → item persistido no Room e alarme agendado, em `UpsertScheduleScreenRobolectricTest`.)_
