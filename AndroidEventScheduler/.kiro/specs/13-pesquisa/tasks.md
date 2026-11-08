# 13 — Pesquisa de tarefas e lembretes — Tarefas

- [x] 1. `SearchFilter` (domínio) e contrato State/Event/Effect (`SearchUiState/Event/Effect`, `SearchResultUi`).
- [x] 2. TDD: `SearchResultMapper` + `SearchResultMapperTest`.
- [x] 3. TDD: `SearchViewModel` (debounce, filtro imediato, `flatMapLatest`, `SavedStateHandle`, erro/retry) + `SearchViewModelTest` com tempo virtual (R2, R3, R4, R5).
- [x] 4. Componentes: `SearchFilterChips`, `SearchResultItem`, `SearchEmptyState` — previews/tags/strings (R2.1, R3.4–R3.5).
- [x] 5. `SearchScreen`/`SearchContent` (foco, teclado, limpar, voltar) + `SearchScreenRobolectricTest` (R1).
- [x] 6. `SearchModule`, `searchEntry`; ligar `AppNavHost` (Home → Search → Detail). *(parcial: `searchEntry` registrada e Search → `ScheduleDetail` ligado no `AppNavHost`; a Home e a entry do detalhe são das specs 05/10)*
- [x] 7. `SearchIntegrationTest` com Room em memória (acentos/maiúsculas/recorrentes).
- [x] 8. Atualizar `KoinModulesVerificationTest`; verificação final conforme `specs/README.md`.

## Critérios de aceite

A spec só é considerada concluída — e o PR só é aberto — quando **todos** estiverem marcados `[x]` (cada um verificado de fato, não presumido), além dos **critérios comuns** em `.kiro/specs/README.md`.

- [x] `SearchViewModelTest` (tempo virtual): vazio não consulta; debounce de 250 ms agrupa; filtro reconsulta imediato; `flatMapLatest` descarta antigo; limpar zera; restauração; erro → retry.
- [x] `SearchIntegrationTest` (Room em memória): `reuniao` encontra `REUNIÃO` em título/notas/local; filtro por tipo.
- [x] `SearchScreenRobolectricTest`: foco inicial, chips `Role.RadioButton`, dica/vazio/erro, clique no resultado abre o detalhe (efeito/callback `onOpenDetail`, verificado também ponta a ponta em `SearchFlowRobolectricTest`); `searchEntry` registrada no `AppNavHost`.
- [ ] Home → Search → Detail navegando *(parcial: Home e Detail em specs 05/10 — ainda inalcançável pela UI; só a parte da pesquisa foi verificada)*.
