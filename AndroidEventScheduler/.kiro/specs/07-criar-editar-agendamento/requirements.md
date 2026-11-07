# 07 — Criar e editar agendamento — Requisitos

## Introdução
Tela única para criar/editar **Eventos** e **Lembretes**: alternador no topo, título, dia inteiro, data/hora com seletores inline expansíveis e atalhos rápidos, alerta, calendário, local, notas e linha de repetição. Ao salvar, valida, persiste e agenda o alarme. O diálogo de repetição é a spec 08; seletores de cor/emoji são a spec 09.

## Requisitos

### R1 — Modos
1. A tela SHALL ter um alternador em pílula **Evento | Lembrete** (`UPSERT_MODE_EVENT_TAB`/`UPSERT_MODE_REMINDER_TAB`, `Role.Tab`, `selected`, "Aba Evento"/"Aba Lembrete"). Em nova criação o modo inicial é **Lembrete**.
2. **Evento**: título, dia inteiro, início (data+hora), fim (data+hora), alerta, calendário (`PERSONAL_ACCOUNT` ou `FAMILY`), repetição, local, notas.
3. **Lembrete**: título, data, hora, alerta, repetição, notas; sem fim, sem dia inteiro, sem local; calendário fixo `MY_REMINDERS`.
4. WHEN o modo muda THEN título, data e hora SHALL ser preservados; ao ir para Evento o fim SHALL ser início + 1 h; ao ir para Lembrete os campos exclusivos de Evento SHALL ser descartados do rascunho.

### R2 — Valores iniciais
1. Criação: data = `initialDateEpochDay` da rota (ou hoje); hora = próxima hora cheia; alerta = antecedência padrão das configurações; sem repetição; cor padrão `LIGHT_BLUE`.
2. Edição (`scheduleId` presente): SHALL carregar o item e preencher todos os campos; IF o item não existir THEN mostrar mensagem e voltar.
3. O título da tela SHALL ser "Novo agendamento" ou "Editar agendamento".

### R3 — Data e hora
1. Tocar na linha de data (`UPSERT_DATE_SELECTOR_ROW`) SHALL expandir/recolher um calendário mensal inline (`UPSERT_INLINE_CALENDAR`, "Seletor de calendário mensal expandido"); tocar na hora (`UPSERT_TIME_SELECTOR_ROW`) SHALL expandir/recolher um seletor de horário inline (`UPSERT_INLINE_TIME_PICKER`, "Seletor de horário expandido"). Apenas **um** seletor aberto por vez.
2. Em Lembrete, o seletor de data SHALL oferecer atalhos: `Hoje`, `Amanhã`, `Daqui a 3 dias`, `Daqui a 1 semana`. O seletor de hora (ambos modos) SHALL oferecer: `Daqui a 1 hora`, `07:00`, `15:00`.
3. WHEN o início muda THEN o fim SHALL manter a duração anterior; WHEN o fim fica antes do início THEN o fim SHALL ser ajustado para o início (+0 min) e nunca ficar inválido na UI.
4. Com **dia inteiro** ligado, as linhas de hora SHALL ficar ocultas e as datas permanecem (fim ≥ início).
5. Datas/horas SHALL ser exibidas em pt-BR (`qui., 5 de nov.`, `22:00`).

### R4 — Demais campos
1. Alerta: `Sem alerta`, `10`, `15`, `30 minutos antes`, `1 hora antes`.
2. Local e notas: texto livre, notas multilinha.
3. Linha de repetição (`UPSERT_RECURRENCE_ROW`, descrição "Repetição: <rótulo>") SHALL exibir o rótulo atual; a interação é entregue na spec 08.

### R5 — Salvar
1. O botão Salvar (`UPSERT_SAVE_BUTTON`) SHALL chamar `SaveScheduleUseCase`.
2. Regras: título após `trim` não vazio (erro `BlankTitle` → campo com erro); fim ≥ início (`EndBeforeStart`).
3. WHEN válido THEN o item SHALL ser gravado (novo `id` UUID ou `id` existente), o alarme agendado conforme a spec 04, e a tela SHALL fechar.
4. Falha de gravação → mensagem (Snackbar) e a tela permanece com os dados.
5. Falha **apenas** no agendamento do alarme NÃO SHALL impedir a gravação.
6. Botão Salvar SHALL ficar desabilitado enquanto salva (sem duplo toque).
7. Cancelar (`UPSERT_CANCEL_BUTTON`/voltar) SHALL descartar sem gravar.

### R6 — Acessibilidade e UX
1. Teclado: título com `ImeAction.Next`, notas multilinha; `windowSoftInputMode=adjustResize`/`imePadding`.
2. Cada linha interativa com `role = Button`, descrição e estado expandido (`stateDescription`).
