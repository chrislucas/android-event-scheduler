# 14 — Alarme em tela cheia e botão Adiar — Tarefas

- [x] 1. Confirmar `AlarmIntents` (`ACTION_SHOW_FULL_SCREEN_ALARM`, `EXTRA_SCHEDULE_ID`, `notificationId`) e `ScheduleNotFoundException` (specs 04/11); criar o que faltar.
- [x] 2. TDD: `SnoozeAlarmUseCase` + `SnoozeAlarmUseCaseTest` (R2.1–R2.3, R2.5–R2.6).
- [x] 3. Contrato State/Event/Effect (sealed) e `FullScreenAlarmViewModel` + `FullScreenAlarmViewModelTest` (R1.5–R1.6, R2, R3).
- [x] 4. Componentes `SnoozeChipsRow`, `AlarmActionButtons` e `FullScreenAlarmContent` com previews, strings e tags (R1.3–R1.5, R4).
- [x] 5. `FullScreenAlarmActivity` + manifest (sem `WAKE_LOCK`/`DISABLE_KEYGUARD`: `setShowWhenLocked`/`setTurnScreenOn` dispensam permissão; atualizar a seção 2.4 de `docs/permissoes.md` com o uso real) + `AlarmFeatureModule`; `onNewIntent` e flags por API (R1.1–R1.2).
- [x] 6. `FullScreenAlarmActivityRobolectricTest` e `SnoozeThenFireIntegrationTest` (R2.4, R3).
- [x] 7. Atualizar `KoinModulesVerificationTest`; verificação final conforme `specs/README.md`. Informar que a exibição sobre a tela de bloqueio só é verificável em aparelho/emulador. (O teste agrega `appModules` e passou sem `extraTypes` novos; a exibição sobre o bloqueio **não foi verificada** — só em aparelho/emulador.)

## Critérios de aceite

A spec só é considerada concluída — e o PR só é aberto — quando **todos** estiverem marcados `[x]` (cada um verificado de fato, não presumido), além dos **critérios comuns** em `.kiro/specs/README.md`.

- [x] `SnoozeAlarmUseCaseTest`: agenda `agora + N`, modo por tipo, N ≤ 0 falha, inexistente falha, não altera o item.
- [x] `FullScreenAlarmViewModelTest`: chip padrão vindo das configurações (fallback 10), inexistente fecha, sucesso fecha cancelando notificação, falha mantém tela, duplo toque ignorado, voltar não cancela.
- [x] `FullScreenAlarmActivityRobolectricTest` e `SnoozeThenFireIntegrationTest` verdes; manifest com `showWhenLocked`/`turnScreenOn`/`exported=false`.
- [x] Relatório declara que a exibição sobre a tela de bloqueio não foi verificável em JVM.
