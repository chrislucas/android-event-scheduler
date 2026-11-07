# 15 — Restauração de alarmes após reinício — Requisitos

## Introdução
O `AlarmManager` perde todos os alarmes quando o aparelho reinicia, o app é atualizado ou o relógio/fuso muda. Esta spec reagenda o próximo alarme de todos os agendamentos nessas situações, usando a regra da spec 04.

## Requisitos

### R1 — Gatilhos
1. O app SHALL reagendar os alarmes ao receber: `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIMEZONE_CHANGED`, `TIME_SET` e (API 34+) `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`.
2. O receiver SHALL declarar `RECEIVE_BOOT_COMPLETED`, `exported=true` apenas onde exigido pelo sistema para essas ações e NÃO SHALL tratar `LOCKED_BOOT_COMPLETED` nem ser `directBootAware`: o banco Room vive em armazenamento protegido por credencial, indisponível antes do primeiro desbloqueio.
3. Ações desconhecidas ou `null` SHALL ser ignoradas.
4. O receiver SHALL usar `goAsync()` e finalizar o `PendingResult` em `finally`, concluindo dentro da janela do sistema.

### R2 — Reagendamento
1. O `RescheduleAllAlarmsUseCase` SHALL ler todos os itens e, para cada um, aplicar `ScheduleNextAlarmUseCase`.
2. Itens únicos já no passado, itens com alerta `NONE` e séries encerradas SHALL ficar sem alarme (e qualquer alarme residual cancelado).
3. Itens recorrentes cujo disparo passou enquanto o aparelho estava desligado SHALL ser reagendados para a **próxima ocorrência futura**, sem tocar no registro do banco.
4. Alertas perdidos durante o desligamento NÃO SHALL ser disparados retroativamente (comportamento documentado).
5. Falha em um item NÃO SHALL interromper os demais; o resultado SHALL informar `scheduled`, `skipped` e `failed`.
6. A operação SHALL ser idempotente (executar duas vezes produz o mesmo conjunto de alarmes).

### R3 — Integração e ciclo de vida
1. O Koin SHALL estar pronto quando o receiver executar (App Startup, spec 00).
2. IF o banco estiver vazio THEN SHALL concluir sem erro.
3. O receiver NÃO SHALL abrir UI nem notificações.

### R4 — Manifest
1. Permissão `RECEIVE_BOOT_COMPLETED` e `<receiver>` com intent-filter das ações do R1.1, `enabled=true`.
