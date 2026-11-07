# 13 — Pesquisa de tarefas e lembretes — Tarefas

- [ ] 1. `SearchFilter` (domínio) e contrato State/Event/Effect (`SearchUiState/Event/Effect`, `SearchResultUi`).
- [ ] 2. TDD: `SearchResultMapper` + `SearchResultMapperTest`.
- [ ] 3. TDD: `SearchViewModel` (debounce, filtro imediato, `flatMapLatest`, `SavedStateHandle`, erro/retry) + `SearchViewModelTest` com tempo virtual (R2, R3, R4, R5).
- [ ] 4. Componentes: `SearchFilterChips`, `SearchResultItem`, `SearchEmptyState` — previews/tags/strings (R2.1, R3.4–R3.5).
- [ ] 5. `SearchScreen`/`SearchContent` (foco, teclado, limpar, voltar) + `SearchScreenRobolectricTest` (R1).
- [ ] 6. `SearchModule`, `searchEntry`; ligar `AppNavHost` (Home → Search → Detail).
- [ ] 7. `SearchIntegrationTest` com Room em memória (acentos/maiúsculas/recorrentes).
- [ ] 8. Atualizar `KoinModulesVerificationTest`; verificação final conforme `specs/README.md`.

## Critérios de aceite

A spec só é considerada concluída — e o PR só é aberto — quando **todos** estiverem marcados `[x]` (cada um verificado de fato, não presumido), além dos **critérios comuns** em `.kiro/specs/README.md`.

- [ ] `SearchViewModelTest` (tempo virtual): vazio não consulta; debounce de 250 ms agrupa; filtro reconsulta imediato; `flatMapLatest` descarta antigo; limpar zera; restauração; erro → retry.
- [ ] `SearchIntegrationTest` (Room em memória): `reuniao` encontra `REUNIÃO` em título/notas/local; filtro por tipo.
- [ ] `SearchScreenRobolectricTest`: foco inicial, chips `Role.RadioButton`, dica/vazio/erro, clique abre detalhe; Home → Search → Detail navegando.
