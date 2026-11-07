# 13 — Pesquisa de tarefas e lembretes — Requisitos

## Introdução
Tela aberta pela lupa da Home: campo de busca com resultados reativos enquanto o usuário digita, filtro por tipo (Todos / Eventos / Lembretes) e acesso ao detalhe do resultado.

## Requisitos

### R1 — Campo de busca
1. A tela SHALL abrir com o campo (`SEARCH_QUERY_INPUT`, "Campo de pesquisa de tarefas e lembretes") focado e o teclado visível, `ImeAction.Search`.
2. WHEN há texto THEN SHALL exibir botão "Limpar" (`SEARCH_CLEAR_BUTTON`) que esvazia o campo e a lista, mantendo o foco.
3. A seta de voltar (`SEARCH_BACK_BUTTON`) SHALL fechar a tela.

### R2 — Filtros
1. Três *chips* em grupo exclusivo: `Todos` (padrão), `Eventos`, `Lembretes` (`SEARCH_FILTER_CHIP_PREFIX + filter.name`, `Role.RadioButton`, `selected`, descrições "Filtro Todos/Eventos/Lembretes").
2. WHEN o filtro muda THEN a lista SHALL ser recalculada imediatamente (sem esperar debounce).

### R3 — Resultado
1. A busca SHALL considerar título, notas e local, sem diferenciar maiúsculas/minúsculas **nem acentos** (spec 01 R3.5).
2. WHEN o texto muda THEN a consulta SHALL aguardar 250 ms de inatividade (*debounce*) e cancelar consulta anterior em andamento.
3. IF o texto (após `trim`) está vazio THEN SHALL mostrar o estado inicial "Digite para pesquisar" e nenhuma consulta é feita.
4. IF há texto e nenhum resultado THEN SHALL mostrar "Nenhum resultado para “<texto>”" (`SEARCH_EMPTY_STATE`).
5. Cada resultado SHALL mostrar: marcador de cor, adesivo (se houver), título, data/hora (do início da série), tipo (Evento/Lembrete) e calendário; recorrentes mostram o rótulo de repetição.
6. Resultados SHALL ser ordenados em ordem cronológica crescente pela data/hora de início da série.
7. WHEN o usuário toca um resultado THEN SHALL abrir o detalhe; ao voltar, a busca e o filtro SHALL estar preservados.
8. Os resultados SHALL atualizar sozinhos se o banco mudar enquanto a tela está aberta.

### R4 — Estado
1. Texto e filtro SHALL sobreviver à rotação e à morte do processo (`SavedStateHandle`).
2. A pesquisa NÃO SHALL ser afetada pelos filtros de calendário da Home.
3. Enquanto a primeira consulta de um texto não responde, SHALL mostrar indicador discreto de progresso (estado `Searching`) sem esvaziar os resultados anteriores.

### R5 — Erros
1. IF a consulta falhar THEN SHALL mostrar mensagem de erro com ação "Tentar novamente"; a tela NÃO SHALL fechar nem lançar.
