# 08 — Recorrência (diálogo de repetição) — Tarefas

- [x] 1. `RecurrenceDraft` (+ `Saver`) e `RecurrenceDraftTest` escrito antes (TDD) (R2, R3.1).
- [x] 2. Acréscimos de `UpsertScheduleUiState/Event` e reducers no `UpsertScheduleViewModel` + testes (R1, R3.2).
- [x] 3. Strings pt-BR (dias curto/completo, rótulos) e tags (`RECURRENCE_*`) em `SemanticsTags`.
- [x] 4. `RecurrencePickerDialog` + `WeeklyDaySelectorRow` + `RecurrenceRadioRow` com semântica e previews (R2).
- [x] 5. Integração em `UpsertScheduleContent` (linha com rótulo + diálogo) (R1.1, R1.3).
- [x] 6. `RecurrencePickerDialogRobolectricTest` e `RecurrenceSaveIntegrationTest` (R4).
- [x] 7. Verificação final conforme `specs/README.md`.

## Critérios de aceite

A spec só é considerada concluída — e o PR só é aberto — quando **todos** estiverem marcados `[x]` (cada um verificado de fato, não presumido), além dos **critérios comuns** em `.kiro/specs/README.md`.

- [x] `RecurrenceDraftTest`: `from` de cada padrão, último dia não removível, filtro de dígitos (4), `toPattern` com vazio/0/5000, dia inicial pré-selecionado.
- [x] `RecurrencePickerDialogRobolectricTest`: fluxo semanal ter+qui 5000 → `Repetição: Toda semana em ter., qui. (5000 vezes)`; diário sem limite; cancelar não altera.
- [x] `RecurrenceSaveIntegrationTest`: salvar semanal gera ocorrências esperadas e agenda o alarme da 1ª ocorrência futura.
