# 05 — Home: calendário mensal e agenda do dia — Requisitos

## Introdução
Tela principal: grade mensal com indicadores coloridos por dia, cabeçalho do dia selecionado ("5 QUI."), lista dos compromissos do dia, seletor rápido de mês/ano e barra "Adicionar agendamento". Gaveta lateral e filtros ficam na spec 06; adesivo do dia na spec 09.

## Requisitos

### R1 — Grade mensal
1. A grade SHALL exibir o mês visível em 6 linhas × 7 colunas (domingo → sábado) incluindo dias dos meses vizinhos esmaecidos, com cabeçalho de dias `D S T Q Q S S`.
2. O dia atual SHALL ter destaque (círculo ciano) e o dia selecionado SHALL ter destaque distinto; ambos podem coincidir.
3. Cada dia com compromissos SHALL mostrar até 3 indicadores com a cor de cada item (cores distintas, na ordem de horário); mais de 3 itens não adicionam indicador extra.
4. WHEN o usuário toca um dia THEN o dia SHALL ser selecionado; tocar em dia de mês vizinho SHALL também mudar o mês visível para aquele mês.
5. O usuário SHALL poder trocar de mês por deslize horizontal **e** por botões "Mês anterior"/"Próximo mês".
6. Itens recorrentes SHALL aparecer em todas as ocorrências do período visível (via `ScheduleRepository.observeOccurrences`). Itens multi-dia SHALL marcar todos os dias que cobrem.

### R2 — Barra superior
1. O título SHALL mostrar mês e ano do mês visível (ex.: `novembro 2026`) e ser clicável (`HOME_MONTH_HEADER`) para abrir o seletor de mês.
2. O botão de pesquisa (`HOME_SEARCH_BUTTON`, descrição "Pesquisar tarefas e lembretes") SHALL navegar para a pesquisa.

### R3 — Seletor de mês e ano
1. WHEN aberto THEN SHALL exibir um *bottom sheet* com o ano atual do seletor, botões de ano anterior/próximo e os 12 meses (`JAN.` … `DEZ.`), com o mês visível selecionado.
2. WHEN o usuário escolhe um mês THEN o sheet SHALL fechar, o mês visível SHALL mudar e o dia selecionado SHALL ser o mesmo dia do mês anterior limitado ao último dia do novo mês (ou hoje, se o mês escolhido for o atual).

### R4 — Agenda do dia
1. O cabeçalho do dia SHALL mostrar `d EEE.` em maiúsculas pt-BR (ex.: `5 QUI.`) com `HOME_DAY_HEADER`.
2. A lista (`HOME_SCHEDULE_LIST`) SHALL mostrar os compromissos do dia selecionado ordenados por horário (dia inteiro primeiro), cada um com barra/ponto na cor do item, adesivo (se houver), título, intervalo (`22:00 – 23:30`, `Todo o dia`) e nome do calendário de origem.
3. IF não há compromissos THEN SHALL mostrar estado vazio (`HOME_EMPTY_DAY`, "Nenhum compromisso neste dia").
4. WHEN o usuário toca num item THEN SHALL navegar para o detalhe.
5. Itens que cruzam a meia-noite SHALL aparecer nos dois dias.

### R5 — Adição rápida
1. A barra "Adicionar agendamento" (`HOME_QUICK_ADD_BAR`) SHALL abrir a criação com a data selecionada como data inicial.

### R6 — Estado e ciclo de vida
1. WHEN dados do banco mudam (criar/editar/excluir em outra tela) THEN a grade e a lista SHALL atualizar sem ação do usuário.
2. Mês visível e dia selecionado SHALL sobreviver à rotação e à morte do processo (`SavedStateHandle`).
3. O relógio ("hoje") SHALL ser injetado.

### R7 — Permissão de notificação
1. A tela SHALL incluir `NotificationPermissionEffect` (spec 04) para pedir a permissão na primeira abertura.

### R8 — Acessibilidade
1. Cada célula SHALL ter `contentDescription` do tipo `5 de novembro, 2 compromissos` e `selected`; indicadores são decorativos.
