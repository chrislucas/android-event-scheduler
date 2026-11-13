# 20 — Alerta 5 minutos antes — Requisitos

## Introdução
Hoje a antecedência do alerta de um agendamento (`ReminderOffset`, spec 01) aceita **Sem alerta**, **10**, **15**, **30** minutos e **1 hora**. O usuário pediu para poder escolher receber o alerta **5 minutos antes**. A antecedência já é escolhida por agendamento (criar/editar, spec 07) e tem um padrão em Configurações (spec 03). O alarme é calculado como início da ocorrência menos a antecedência (spec 04). Esta spec acrescenta a opção de 5 minutos a esse catálogo, sem mudar as regras de disparo.

Fora do escopo:
- antecedência livre (minutos digitados pelo usuário);
- mais de um alerta por agendamento (ex.: 15 min **e** 5 min antes);
- alerta no horário exato (0 min);
- mudar o padrão, que continua **15 minutos**.

## Requisitos

### R1 — Nova opção de antecedência
1. `ReminderOffset` SHALL ter `FIVE_MINUTES` com `minutes = 5`, declarado entre `NONE` e `TEN_MINUTES`. As listas de opções seguem a ordem do enum: `Sem alerta`, `5 minutos antes`, `10 minutos antes`, `15 minutos antes`, `30 minutos antes`, `1 hora antes`.
2. O rótulo SHALL ser `5 minutos antes` (plural `reminder_minutes_before` já existente) em todos os lugares que exibem o alerta: formulário, Configurações, detalhe e texto compartilhado.
3. O padrão SHALL continuar `FIFTEEN_MINUTES` (`ReminderOffset.DEFAULT`, `UserSettings`).

### R2 — Escolha pelo usuário
1. Em criar/editar (spec 07), a linha **Alerta** SHALL oferecer `5 minutos antes`. Escolhida a opção e salvo o item, ele SHALL ser gravado com `FIVE_MINUTES`.
2. Ao editar um item gravado com `FIVE_MINUTES`, a linha **Alerta** SHALL mostrar `5 minutos antes`.
3. Em Configurações (spec 03), **Alerta padrão dos novos agendamentos** SHALL oferecer `5 minutos antes`. Com essa opção salva, a criação de um agendamento SHALL abrir com `5 minutos antes`.

### R3 — Disparo do alarme
1. Para um item com `FIVE_MINUTES`, o alarme SHALL ser agendado para **início da ocorrência − 5 minutos**, estritamente depois de agora (spec 04 R1.1). Item de dia inteiro: 09:00 − 5 min = **08:55**.
2. Itens recorrentes SHALL alertar 5 minutos antes de cada ocorrência, inclusive após o reagendamento pós-boot (spec 15), que usa o mesmo `ScheduleNextAlarmUseCase`.
3. IF início − 5 min já passou AND não há ocorrência futura THEN nenhum alarme é agendado. É a mesma regra que já vale para as outras antecedências.

### R4 — Persistência e compatibilidade
1. Room: o item SHALL ser gravado com `reminderMinutes = 5` e lido de volta como `FIVE_MINUTES`. A coluna (`Int?`) já existe: **sem migração** de banco.
2. DataStore: o padrão de Configurações SHALL ser gravado como `default_reminder_minutes = 5` e lido de volta como `FIVE_MINUTES`.
3. Arquivo de agenda (spec 17): a exportação SHALL escrever `"reminderOffset": "FIVE_MINUTES"` e a importação SHALL aceitar esse valor. O formato continua na **versão 1**: uma versão anterior do app que receber o arquivo **ignora só os itens com `FIVE_MINUTES`**, contados como itens com dados inválidos. Isso SHALL ficar documentado em `docs/formato-agenda-json.md`.
4. Nomes desconhecidos de antecedência SHALL continuar recusando o item na importação.
