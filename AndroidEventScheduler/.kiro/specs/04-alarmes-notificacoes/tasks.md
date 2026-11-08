# 04 — Alarmes e notificações — Tarefas

- [x] 1. Domain: `AlarmRequest`, `AlertPresentationMode`, `AlarmScheduler` (R2.1).
- [x] 2. Domain (TDD): `ScheduleNextAlarmUseCase` + `ScheduleNextAlarmUseCaseTest` com todos os cenários do design (R1).
- [x] 3. Data: `AlarmIntents` + `AndroidAlarmScheduler` + `AndroidAlarmSchedulerRobolectricTest` (R2).
- [x] 4. Data: `NotificationChannels` (criados na inicialização) e `ScheduleNotifier` + testes (R3.2–R3.4, R4.1).
- [x] 5. Data: `ScheduleAlarmReceiver` + manifest (receiver `exported=false`, permissões do R4.2) + `ScheduleAlarmReceiverRobolectricTest` (R3).
- [x] 6. `AlarmModule` Koin (scheduler, notifier, use case, `Clock.systemDefaultZone()`); registrar em `AppModules`.
- [x] 7. `core.permissions.NotificationPermissionEffect` + teste (R4.3).
- [x] 8. Deep link: `DeepLinkState`, `MainActivity` (`onCreate`/`onNewIntent`) e consumo no `AppNavHost` (R5) + teste do fluxo com `ActivityScenario`.
- [ ] 9. Atualizar `KoinModulesVerificationTest`; verificação final conforme `specs/README.md`. Informar que disparo real de alarme/tela cheia só pode ser validado em aparelho/emulador.

## Critérios de aceite

A spec só é considerada concluída — e o PR só é aberto — quando **todos** estiverem marcados `[x]` (cada um verificado de fato, não presumido), além dos **critérios comuns** em `.kiro/specs/README.md`.

- [ ] `ScheduleNextAlarmUseCaseTest` cobre: único futuro, único passado (cancela), offset `NONE`, diário/semanal, série encerrada, dia inteiro às 09:00, offset que cruza meia-noite, modo EVENT×REMINDER.
- [ ] `AndroidAlarmSchedulerRobolectricTest`: agenda exato, substitui por `scheduleId`, cancela, fallback inexato sem permissão.
- [ ] `ScheduleAlarmReceiverRobolectricTest`: lembrete → canal `reminders`; evento com tela cheia habilitada → `fullScreenIntent`; desabilitada/sem atividade → degrada; item inexistente → nada; sempre reagenda.
- [ ] Canais criados na inicialização; permissões no manifest; `NotificationPermissionEffect` testado; deep link da notificação testado com `ActivityScenario`.
- [ ] Relatório declara o que só vale em aparelho (alarme exato real, `canUseFullScreenIntent`).
