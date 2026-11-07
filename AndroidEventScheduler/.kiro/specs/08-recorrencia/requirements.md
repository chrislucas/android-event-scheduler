# 08 — Recorrência (diálogo de repetição) — Requisitos

## Introdução
Permite configurar a repetição de um agendamento a partir da linha "Repetição" da tela de criar/editar: **Não repetir**, **Diariamente** ou **Toda semana** (dias da semana à escolha) com limite opcional de ocorrências. O cálculo das ocorrências já existe no `RecurrenceEngine` (spec 01) e o alarme reage via spec 04.

## Requisitos

### R1 — Abertura
1. WHEN o usuário toca a linha de repetição (`UPSERT_RECURRENCE_ROW`) THEN SHALL abrir o diálogo `RECURRENCE_DIALOG` ("Diálogo de configuração de repetição") com título "Repetição" (`heading`).
2. O diálogo SHALL iniciar refletindo o padrão atual: opção marcada, dias e limite preenchidos.
3. A linha de repetição SHALL passar a exibir o rótulo do padrão confirmado (`Não repetir`, `Diariamente`, `Toda semana em ter., qui. (5000 vezes)`).

### R2 — Opções
1. Três opções exclusivas em grupo de rádio: `Não repetir` (`RECURRENCE_OPTION_NONE`), `Diariamente` (`..._DAILY`), `Toda semana` (`..._WEEKLY`), cada uma com `Role.RadioButton` e `selected`.
2. WHEN "Toda semana" está marcada THEN SHALL exibir sete alternadores `D S T Q Q S S` (`RECURRENCE_DAY_TOGGLE_PREFIX + DayOfWeek.name`), com `Role.Checkbox`, `contentDescription` do nome completo ("Terça-feira") e `stateDescription` ("Selecionado"/"Não selecionado").
3. Ao escolher "Toda semana" pela primeira vez, SHALL vir selecionado o dia da semana da **data inicial** do agendamento.
4. O usuário SHALL NOT conseguir desmarcar o último dia selecionado.
5. WHEN a opção é "Diariamente" ou "Toda semana" THEN SHALL aparecer o campo numérico "Número de vezes (opcional)" (`RECURRENCE_COUNT_INPUT`), teclado numérico, aceitando apenas dígitos, até 4 dígitos.

### R3 — Confirmação
1. "Concluir" (`RECURRENCE_CONFIRM_BUTTON`) SHALL devolver: `None`; `Daily(limit)`; `Weekly(days, limit)` onde `limit` é o número digitado se > 0, senão `null`.
2. "Cancelar" (`RECURRENCE_DISMISS_BUTTON`), toque fora ou voltar SHALL fechar sem alterar o padrão.
3. O diálogo SHALL ser puramente local até confirmar (alternar opções não altera o estado da tela).

### R4 — Efeitos no restante do app
1. WHEN o agendamento é salvo com recorrência THEN as ocorrências SHALL aparecer na Home (spec 05) e o alarme SHALL ser agendado para a próxima ocorrência (spec 04), sem código novo nessas specs.
2. Selecionar `Weekly` com data inicial em dia fora do conjunto SHALL ser aceito (a 1ª ocorrência é o primeiro dia marcado após a data inicial).
