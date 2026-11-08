# 15 — Restauração de alarmes após reinício — Tarefas

- [x] 1. Domain (TDD): `RescheduleSummary` + `RescheduleAllAlarmsUseCase` + `RescheduleAllAlarmsUseCaseTest` (R2).
- [x] 2. Registrar o use case no `AlarmModule`.
- [x] 3. Data: `BootCompletedReceiver` (goAsync, ações suportadas) + `BootCompletedReceiverRobolectricTest` (R1, R3).
- [x] 4. Manifest: permissão `RECEIVE_BOOT_COMPLETED` (comentada e documentada em `docs/permissoes.md`, removendo-a de "planejadas"; atualizar `RequestedPermissionsGuardRobolectricTest`) + receiver sem `directBootAware` e sem `LOCKED_BOOT_COMPLETED` (R1.2, R4); conferir `exported` na documentação atual e testar o manifest.
- [x] 5. `BootRestoreIntegrationTest` (Room em memória + `ShadowAlarmManager`).
- [ ] 6. Verificação manual sugerida no emulador (`adb shell am broadcast -a android.intent.action.BOOT_COMPLETED -p <pacote>` e `adb shell dumpsys alarm | grep <pacote>`); relatar o que foi ou não executado. _(não executado: os emuladores conectados são compartilhados com 4 agentes paralelos e o `applicationId` é o mesmo; instalar sobrescreveria os builds dos outros)_
- [x] 7. Atualizar `KoinModulesVerificationTest`; verificação final conforme `specs/README.md`.

## Critérios de aceite

A spec só é considerada concluída — e o PR só é aberto — quando **todos** estiverem marcados `[x]` (cada um verificado de fato, não presumido), além dos **critérios comuns** em `.kiro/specs/README.md`.

- [x] `RescheduleAllAlarmsUseCaseTest`: futuro agendado, passado ignorado, diário perdido → próximo dia, semanal, série encerrada, falha isolada não interrompe, banco vazio, idempotência.
- [x] `BootCompletedReceiverRobolectricTest`: BOOT, MY_PACKAGE_REPLACED, TIMEZONE_CHANGED, TIME_SET acionam; ação desconhecida/`null` não; `PendingResult` sempre finalizado.
- [x] Manifest sem `directBootAware` e sem `LOCKED_BOOT_COMPLETED`; `BootRestoreIntegrationTest` verde.
- [x] Relatório declara (não testado no emulador: ver nota na task 6 e no PR) se o broadcast de boot foi testado no emulador (`adb`) ou não.
