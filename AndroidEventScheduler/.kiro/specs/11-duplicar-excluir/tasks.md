# 11 — Duplicar e excluir agendamento — Tarefas

- [ ] 1. Confirmar que `ScheduleNotFoundException` (spec 01) e `StringProvider` (spec 10) existem; criar o que faltar + testes.
- [ ] 2. TDD: `DuplicateScheduleUseCase` + `DuplicateScheduleUseCaseTest` (R1.2, R1.3, R1.5, R1.6).
- [ ] 3. TDD: `DeleteScheduleUseCase` + `DeleteScheduleUseCaseTest` (R2.3, R2.5, R2.6).
- [ ] 4. Estender `ScheduleDetailUiState/Event/Effect` e `ScheduleDetailViewModel` (+ testes) incl. proteção de toques repetidos e diálogo restaurável (R1.4, R2, R3).
- [ ] 5. `DeleteConfirmationDialog` com previews, tags e aviso de recorrência (R2.2).
- [ ] 6. Acrescentar Duplicar/Excluir ao `ScheduleActionBar`, Snackbar com ação, `onOpenCopy` no `detailEntry`/`AppNavHost`.
- [ ] 7. Registrar use cases no `DetailModule`; atualizar `KoinModulesVerificationTest`; testes de tela; verificação final conforme `specs/README.md`.

## Critérios de aceite

A spec só é considerada concluída — e o PR só é aberto — quando **todos** estiverem marcados `[x]` (cada um verificado de fato, não presumido), além dos **critérios comuns** em `.kiro/specs/README.md`.

- [ ] `DuplicateScheduleUseCaseTest`: novo id + sufixo, copia todos os campos, inexistente falha, falha do `upsert` não agenda, falha do alarme não desfaz.
- [ ] `DeleteScheduleUseCaseTest`: banco antes do alarme (`coVerifyOrder`), falha do banco não cancela alarme, falha do alarme ainda sucesso.
- [ ] ViewModel/tela: diálogo de exclusão (com aviso de recorrência) restaurável, duplo toque ignorado, Snackbar "Cópia criada" com ação "Abrir" navega à cópia.
