# 14 — Alarme em tela cheia e botão Adiar — Requisitos

## Introdução
Tela de alerta de **Eventos**, exibida em tela cheia (inclusive sobre a tela de bloqueio) na antecedência configurada, no estilo Google Calendar: mostra o evento e permite **Adiar** (5, 10 ou 15 min — padrão das configurações) ou **Dispensar**. Adiar mantém data e hora originais do evento; só o alerta é reagendado. A decisão de quando abrir essa tela é da spec 04.

## Requisitos

### R1 — Tela
1. A `FullScreenAlarmActivity` (pacote `feature.alarm`) SHALL ser aberta pela ação `com.br.androideventscheduler.action.SHOW_FULL_SCREEN_ALARM` com extra `scheduleId`; SHALL ser `exported=false`.
2. SHALL aparecer sobre a tela de bloqueio e ligar a tela (`setShowWhenLocked(true)`, `setTurnScreenOn(true)`; atributos equivalentes no manifest para API 24–26), manter a tela acesa enquanto visível e rodar em tarefa própria (`taskAffinity=""`, `excludeFromRecents`).
3. Conteúdo: selo (`ALARM_FULLSCREEN_BADGE`) "Evento em X minutos" (ou "Evento agora" quando X ≤ 0), título, horário (`22:00 – 23:30`), local e notas (somente se existirem), na cor do item.
4. Botões: **Adiar** (`ALARM_FULLSCREEN_SNOOZE_BUTTON`, "Adiar lembrete do evento por X minutos") e **Dispensar** (`ALARM_FULLSCREEN_DISMISS_BUTTON`, "Dispensar lembrete do evento").
5. Chips de adiamento 5 / 10 / 15 min (`ALARM_SNOOZE_CHIP_PREFIX + minutos`, `Role.RadioButton`, "Adiar por X minutos"); o selecionado inicial SHALL ser `defaultSnoozeMinutes` das configurações.
6. IF o agendamento não existir mais THEN a tela SHALL fechar sozinha sem erro.

### R2 — Adiar
1. WHEN o usuário toca Adiar THEN o app SHALL agendar novo disparo para `agora + N minutos` (substituindo o alarme pendente do item), cancelar a notificação ativa do item e fechar a tela.
2. O `ScheduleItem` (data, hora, recorrência) NÃO SHALL ser alterado.
3. N SHALL ser > 0, senão falha com erro de validação (sem agendar).
4. WHEN o alarme adiado dispara THEN o receiver (spec 04) SHALL apresentá-lo normalmente e reagendar a próxima ocorrência da série, restaurando o fluxo regular.
5. IF o item não existir THEN falha com `ScheduleNotFoundException` e a tela fecha.
6. IF o agendamento falhar THEN a tela SHALL permanecer, exibindo mensagem de erro.
7. Toques repetidos enquanto processa SHALL ser ignorados.

### R3 — Dispensar e voltar
1. Dispensar SHALL cancelar a notificação do item e fechar; não altera alarmes futuros (a próxima ocorrência já foi agendada no disparo).
2. O botão voltar do sistema SHALL fechar a tela sem cancelar a notificação.

### R4 — Qualidade
1. A tela SHALL funcionar em retrato e paisagem e respeitar fonte grande e TalkBack (ordem: selo → título → horário → local → notas → chips → botões).
2. O conteúdo SHALL sobreviver à rotação (ViewModel).
