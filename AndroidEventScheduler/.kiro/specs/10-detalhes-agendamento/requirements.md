# 10 — Detalhes do agendamento — Requisitos

## Introdução
Tela de leitura de um agendamento (aberta pela Home, pela pesquisa ou pela notificação) com título, intervalo de data/hora, calendário, alerta, repetição, local e notas, e uma barra inferior de ações em pílula. Esta spec entrega a tela e a ação **Editar**; Duplicar/Excluir são a spec 11 e Compartilhar a spec 12.

## Requisitos

### R1 — Conteúdo
1. A tela SHALL exibir: adesivo (se houver) + título (`DETAIL_TITLE`, `heading`), faixa/ponto na cor do item, intervalo (`DETAIL_DATE_RANGE`), calendário (`DETAIL_ACCOUNT_EMAIL`), alerta (`DETAIL_REMINDER_LABEL`), repetição (`DETAIL_RECURRENCE_LABEL`), local (`DETAIL_LOCATION`) e notas (`DETAIL_NOTES`).
2. **Intervalo**: evento com horas → `qui., 5 de nov., 22:00 → sex., 6 de nov., 23:30`; mesmo dia → `qui., 5 de nov., 22:00 – 23:30`; dia inteiro → `qui., 5 de nov. · Todo o dia` (ou intervalo de datas); lembrete → `qui., 5 de nov., 22:00`.
3. Local e notas SHALL aparecer somente se não vazios.
4. O rótulo do calendário SHALL ser o e-mail da conta ativa para `PERSONAL_ACCOUNT`, "Family" ou "Meus lembretes".
5. O alerta SHALL usar `ReminderOffset.toDisplayLabel` (`10 minutos antes`, `Sem alerta`); a repetição `RecurrencePattern.toDisplayLabel` (spec 01).

### R2 — Estados
1. Enquanto carrega, SHALL mostrar indicador de progresso.
2. IF o item não existe THEN SHALL mostrar mensagem "Agendamento não encontrado" e voltar.
3. WHEN o item é editado ou excluído em outra tela THEN a tela SHALL refletir a alteração (observação reativa); IF o item desaparece com a tela aberta THEN SHALL voltar automaticamente.
4. Para itens recorrentes a tela SHALL mostrar a data inicial da série.

### R3 — Navegação e ações
1. A seta de voltar (`DETAIL_BACK_BUTTON`, "Voltar") SHALL voltar à tela anterior.
2. A barra inferior (`ScheduleActionBar`) SHALL ser composta de ações independentes; esta spec adiciona **Editar** (`DETAIL_EDIT_BUTTON`, "Editar agendamento") que abre `UpsertSchedule(scheduleId)`.
3. Os botões SHALL ter ícone + rótulo, `Role.Button` e alvo ≥ 48dp.

### R4 — Acessibilidade
1. A ordem de foco SHALL seguir a ordem visual; cada linha com ícone SHALL ser um único nó semântico (`mergeDescendants`) lido como "Alerta: 10 minutos antes".
