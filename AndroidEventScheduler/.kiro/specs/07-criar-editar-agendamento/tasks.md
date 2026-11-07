# 07 — Criar e editar agendamento — Tarefas

- [ ] 1. Domain (TDD): `SaveScheduleError`, `SaveScheduleUseCase` + `SaveScheduleUseCaseTest` (R5).
- [ ] 2. Domain (TDD): `ScheduleDraftTimeRules` + testes (R3.2–R3.4, R1.4).
- [ ] 3. Presentation: `UpsertScheduleUiState/Event/Effect`, `ActiveInlinePicker`.
- [ ] 4. `UpsertScheduleViewModel` (carga criação/edição, regras, salvar) + `UpsertScheduleViewModelTest` (R1, R2, R5).
- [ ] 5. Componentes de formulário: `ModeSegmentedPill`, `TitleRow`, `SettingRow`, `ReminderOffsetRow`, `CalendarSourceRow`, campos de local/notas — com previews e tags (R1, R4, R6).
- [ ] 6. `DateTimeSection` + `InlineDatePicker` (conversão UTC!) + `InlineTimePicker` + `QuickChipsRow` (R3) — previews por estado.
- [ ] 7. `UpsertScheduleScreen`/`Content` + `UpsertScheduleScreenRobolectricTest`.
- [ ] 8. `UpsertModule` + `upsertEntry`; ligar `AppNavHost` (Home → criar com data; Detail → editar).
- [ ] 9. Atualizar `KoinModulesVerificationTest`; verificação final conforme `specs/README.md`.

## Critérios de aceite

A spec só é considerada concluída — e o PR só é aberto — quando **todos** estiverem marcados `[x]` (cada um verificado de fato, não presumido), além dos **critérios comuns** em `.kiro/specs/README.md`.

- [ ] `SaveScheduleUseCaseTest`: título em branco, fim antes do início, sucesso persiste e agenda, falha do alarme não derruba, falha do repositório não agenda, `trim`.
- [ ] `ScheduleDraftTimeRulesTest` e `UpsertScheduleViewModelTest` cobrem R1–R5 (modos, duração preservada, chips, edição, item inexistente, duplo toque).
- [ ] `UpsertScheduleScreenRobolectricTest`: abas `Role.Tab`, um seletor inline por vez, dia inteiro oculta horas, linha `Repetição: Não repetir`.
- [ ] Conversão UTC do `DatePicker` coberta por teste (data escolhida = data salva).
- [ ] Fluxo criar → salvar → aparece na Home verificável em teste de integração ou descrito no relatório.
