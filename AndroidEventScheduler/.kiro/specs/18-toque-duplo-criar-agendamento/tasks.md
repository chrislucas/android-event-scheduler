# 18 — Toque duplo numa data cria evento ou lembrete — Tarefas

- [ ] 1. TDD: `DayDoubleTapDetector` + `DayDoubleTapDetectorTest` (R2.2–R2.4, R6.2).
- [ ] 2. Família `HomeUiEvent.DayCellEvent` (move `OnDayClicked`, mesmo nome; acrescenta `OnDayDoubleTapped`), `onDayCellEvent` no `HomeViewModel` + acréscimos no `HomeViewModelTest` (R1.1, R3.1). Contrato e handler no **mesmo commit** (`when` exaustivo).
- [ ] 3. `MonthGridSection`: `pointerInput` passivo + detector (ignora gestos acima do `touchSlop`, R2.5), ação de acessibilidade personalizada, parâmetro `onDayDoubleTap` e todos os call sites/previews no mesmo commit; `HomeScreen` repassa `OnDayDoubleTapped`; strings `home_day_add_action` (R1.1, R2.1, R4).
- [ ] 4. Dica no estado vazio (`home_empty_day_hint`, `HOME_EMPTY_DAY_HINT`) com preview (R5.1).
- [ ] 5. Guarda no `AppNavHost` contra criação duplicada (R6.1).
- [ ] 6. Acréscimos no `HomeScreenRobolectricTest` e ponta a ponta `DoubleTapDayCreatesScheduleRobolectricTest` (R1.2–R1.5, R2.1, R4.1, R6.1).
- [ ] 7. Verificação final conforme [`criterios-de-aceite.md`](../criterios-de-aceite.md), incluindo a checagem por commit.

## Critérios de aceite

A spec só é considerada concluída, e o PR só é aberto, quando **todos** estiverem marcados `[x]`, cada um verificado de fato e não presumido. Somam-se a eles os **critérios gerais** em [`.kiro/specs/criterios-de-aceite.md`](../criterios-de-aceite.md).

- [ ] `DayDoubleTapDetectorTest` cobre: mesmo dia dentro do intervalo, fora do intervalo, limite exato, dias diferentes, triplo (abre uma vez), quádruplo (dois duplos) e tempo que volta para trás.
- [ ] Toque simples seleciona o dia **sem atraso**: o teste de tela confere a seleção logo após um `performClick()`, sem avançar o relógio, e não chama a criação.
- [ ] Deslizar para trocar de mês não conta como toque (teste de tela com dois deslizes na mesma célula).
- [ ] Toque duplo num dia abre a criação com esse dia como data inicial. Num dia de mês vizinho, também muda o mês visível (ViewModel e tela).
- [ ] A ação de acessibilidade `Adicionar agendamento em <dia>` existe em cada célula e abre a criação. O `contentDescription` da célula não mudou (os testes da spec 05 continuam verdes sem edição).
- [ ] Ponta a ponta com `MainActivity`: toque duplo → trocar para Evento → salvar → item `EVENT` na data tocada e visível na Home. Cancelar não grava nada.
- [ ] A guarda do `AppNavHost` impede duas telas de criação empilhadas (ponta a ponta).
- [ ] Dica no estado vazio exibida, com resource e tag.
- [ ] Nenhuma rota, módulo Koin, permissão ou dependência nova; `OnDayClicked` manteve o nome.
- [ ] Não verificado em aparelho, declarado no PR: sensação do gesto (intervalo real do sistema) e TalkBack lendo e executando a ação personalizada.
