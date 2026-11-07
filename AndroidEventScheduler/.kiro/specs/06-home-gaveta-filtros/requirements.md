# 06 — Home: gaveta lateral e filtros de calendário — Requisitos

## Introdução
Botão de menu (≡) na Home abre uma gaveta (`ModalNavigationDrawer`) onde o usuário liga/desliga a visibilidade de cada calendário (conta pessoal, Family, Meus lembretes) e acessa as Configurações. Os filtros afetam imediatamente a grade e a lista e são persistidos.

## Requisitos

### R1 — Abertura e fechamento
1. A barra superior SHALL ganhar o botão de menu (`HOME_MENU_BUTTON`, "Abrir menu de navegação").
2. WHEN tocado, ou ao deslizar da borda esquerda, THEN a gaveta (`HOME_DRAWER_SHEET`, "Menu lateral de calendários e configurações") SHALL abrir; fechar por toque fora, deslize ou botão voltar do sistema.
3. O estado aberto/fechado SHALL sobreviver à rotação.

### R2 — Filtros de calendário
1. A gaveta SHALL listar três linhas com caixa de seleção e marcador de cor: o e-mail da conta ativa (`PERSONAL_ACCOUNT`, vindo das configurações), `Family` e `Meus lembretes`.
2. Cada linha SHALL ter `Role.Checkbox`, `stateDescription` ("Visível"/"Oculto") e tag `HOME_DRAWER_FILTER_PREFIX + source.id`.
3. WHEN o usuário alterna uma linha THEN a grade (indicadores) e a lista do dia SHALL refletir o filtro imediatamente, sem fechar a gaveta.
4. Todos os calendários SHALL estar visíveis por padrão.
5. O conjunto de visíveis SHALL ser persistido no DataStore e restaurado ao reabrir o app.
6. IF todos estiverem ocultos THEN grade e lista SHALL aparecer sem itens (sem erro) e a lista SHALL mostrar o estado vazio.
7. IF o e-mail da conta mudar nas configurações THEN o rótulo SHALL atualizar sem reiniciar.
8. O filtro SHALL afetar somente a Home (pesquisa e alarmes ignoram o filtro).

### R3 — Atalho para Configurações
1. A gaveta SHALL ter o item "Configurações" (`HOME_DRAWER_SETTINGS_BUTTON`, "Abrir configurações do aplicativo") que fecha a gaveta e navega para `Settings`.

### R4 — Dados
1. As preferências da gaveta SHALL ser lidas/gravadas via `HomePreferencesRepository` (interface no domínio da Home), usando o `DataStore<Preferences>` único.
2. Valores desconhecidos persistidos (ex.: id removido em versão futura) SHALL ser ignorados.
