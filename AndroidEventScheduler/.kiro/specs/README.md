# Specs do RememberMe (Android-only)

Origem: `desenvolvimento_remember_me.pdf` (conversa de design de um app **KMP**: Compose Multiplatform + Voyager + Room KMP + DataStore KMP).
Esta versão é **somente Android**, um único módulo `:app`, organizado por pacotes conforme `steering/arquitetura.md`.

Cada pasta abaixo contém: `requirements.md` (o quê), `design.md` (como), `tasks.md` (checklist executável) e `prompt.md` (prompt pronto para colar no chat e mandar o Kiro executar a spec).

## Ordem de execução e dependências

| # | Spec | Depende de |
|---|---|---|
| 00 | `00-fundacao-projeto` | — |
| 01 | `01-nucleo-agendamentos` | 00 |
| 02 | `02-splash` | 00 |
| 03 | `03-configuracoes` | 00, 01 (task 1: `ReminderOffset`) |
| 04 | `04-alarmes-notificacoes` | 00, 01, 03 |
| 05 | `05-home-calendario` | 00, 01, 04 (task 7: permissão de notificação) |
| 06 | `06-home-gaveta-filtros` | 05, 03 |
| 07 | `07-criar-editar-agendamento` | 01, 03, 04 |
| 08 | `08-recorrencia` | 07 |
| 09 | `09-seletor-cor-emoji` | 07, 06 (task 1: `HomePreferencesRepository`) |
| 10 | `10-detalhes-agendamento` | 01, 03 (07 só para o "Editar" ponta a ponta) |
| 11 | `11-duplicar-excluir` | 10, 04 |
| 12 | `12-compartilhar` | 10 |
| 13 | `13-pesquisa` | 01, 03 |
| 14 | `14-alarme-tela-cheia-adiar` | 04, 03 |
| 15 | `15-restauracao-pos-boot` | 04 |

**Ordem de execução recomendada, dependências por task, ondas de paralelização e status de cada spec: [`ordem-de-execucao.md`](./ordem-de-execucao.md)** (atualizar ao concluir cada spec).

Ordem linear resumida: 00 → 02 → 01 → 03 → 04 → 05 → 07 → 10 → 11 → 12 → 14 → 15 → 08 → 06 → 09 → 13.

Paralelização (ver `steering/fluxo-de-trabalho.md` §3 e as ondas em `ordem-de-execucao.md` §5): specs de correntes diferentes tocam pacotes distintos e só colidem de forma trivial (`AppModules`, `AppNavHost`, `strings.xml`, `SemanticsTags.kt`); specs da mesma corrente (05→06, 07→08→09, 10→11→12) são sequenciais.

## Adaptações do PDF (KMP) para Android-only

| PDF (KMP) | Aqui (Android) |
|---|---|
| Voyager `ScreenModel` + `MviScreenModel` + `ScreenRegistry` | `ViewModel` direto (sem base): `UiState` sealed class, `UiEvent` sealed interface, `UiEffect` por `Channel` + **Navigation 3** |
| `expect/actual` (Alarm, Share, Database, DataStore) | Implementação Android direta; `AlarmScheduler` continua interface (testabilidade) |
| `ShareManager` + `ShareScheduleUseCase` | Efeito `ShareText` executado pela tela com `Intent.ACTION_SEND` (sem UseCase) |
| `kotlinx-datetime` | `java.time` + *core library desugaring* (minSdk 24) |
| `commonMain/androidMain/iosMain` | `main/` e `test/` do `:app` |
| Features `home`, `schedule` (tudo junto), `search`, `settings` importando `feature.schedule` | Dados compartilhados sobem para `core` (`core.schedule`, `core.alarm`, `core.preferences`); features separadas: `splash`, `home`, `upsert`, `detail`, `search`, `settings`, `alarm` — **nenhuma importa outra** |
| `CalendarSource.displayName` com e-mail fixo | Enum só com `id`/cor; o rótulo da conta pessoal vem da configuração (e-mail ativo) na camada de apresentação |
| Recorrência com `remainingOccurrences` decrementado e item "andando" no banco | Item guarda `startDate` da série + `occurrenceLimit`; ocorrências são **calculadas** pelo `RecurrenceEngine` (puro). Nada é reescrito no disparo do alarme; o histórico e o mês exibem as ocorrências corretamente |
| Alarme "15 min antes" fixo | Antecedência vem de `ReminderOffset` (padrão da configuração, 15 min) |
| `AlarmRequest` carrega título/notas/local | `AlarmRequest` leva só `scheduleId` + horário + modo; o receiver lê o item atualizado do banco ao disparar |
| `BootCompletedReceiver` com `LOCKED_BOOT_COMPLETED` e `directBootAware` | Só `BOOT_COMPLETED` (+ atualização do app, mudança de fuso/hora e permissão de alarme exato): o Room fica em armazenamento protegido por credencial e não abre antes do desbloqueio |
| `RescheduleAllAlarmsUseCase` avança e regrava itens recorrentes | Reagenda com `ScheduleNextAlarmUseCase`; não regrava o banco |
| `SearchSchedulesUseCase`, `Observe*UseCase`, `Get*ByIdUseCase`, `Settings*UseCase`, `ShareScheduleUseCase` | Removidos (delegação pura); ViewModels usam repositórios; compartilhar é efeito de UI |
| Busca com `LIKE` simples | Coluna `searchText` normalizada (sem acentos/caixa) |
| Detalhe carregado uma vez | Detalhe observa o item (`observeById`) e reflete edição/exclusão |

## Pacotes (base `com.br.androideventscheduler`)

```
core/        ui · designsystem · navigation · common · di · testing · database · datastore ·
             schedule (domain+data: modelos, repositório, RecurrenceEngine) ·
             alarm (AlarmScheduler, receivers, notificações, casos de uso de alarme) ·
             preferences (UserSettings compartilhado)
feature/     splash · home · upsert · detail · search · settings · alarm (tela cheia)
app/         (pacote raiz) AppNavHost, KoinInitializer, Application — único lugar que conhece todas as features
```

`AppNavHost` fica fora de `core` de propósito: `core` não pode importar `feature`. Cada feature expõe `navigation/<Feature>Entry.kt` com seu `entry<Rota>{}`.

## Política de UseCases (decisão do projeto)

UseCase **só** quando há ao menos um destes motivos, registrado na seção "Decisões de UseCase" de cada `design.md`:
1. regra compartilhada por 2+ pontos de entrada (ViewModels, receivers);
2. orquestração de 2+ fontes que precisam ficar consistentes (ex.: banco + alarme);
3. regra de negócio com lógica própria que merece teste isolado.

Passa-direto de repositório (`observeMonth`, `getById`, `search`) **não** vira UseCase: o ViewModel injeta a interface do repositório.

UseCases previstos: `SaveScheduleUseCase` (07), `ScheduleNextAlarmUseCase` (04), `DuplicateScheduleUseCase` e `DeleteScheduleUseCase` (11), `SnoozeAlarmUseCase` (14), `RescheduleAllAlarmsUseCase` (15). Os demais fluxos (home, pesquisa, configurações, detalhes, compartilhar, cor/emoji, recorrência) usam repositório direto e helpers puros locais.

## Convenções comuns (valem para TODAS as specs; não repetidas nas tasks)

- **Steering obrigatório** (já adaptado a este projeto): `.kiro/steering/arquitetura.md` e `fluxo-de-trabalho.md`.
- **Verificação**: `./gradlew testDebugUnitTest :app:assembleDebug` `ktlintCheck detekt` → **BUILD SUCCESSFUL**. `build-logic`, `config/detekt`, `.editorconfig` e a CI já existem (gerados por `docs/project-foundation/bootstrap-foundation.sh`).
- **Testes**: JUnit4 + MockK + `kotlinx-coroutines-test` + Robolectric/Compose UI Test; **sem Turbine**; nomes ``fun `given … when … then …`()`` em inglês; `MainDispatcherRule` do `core.testing`. Cada spec lista os testes mínimos.
- **Presentation**: sem ViewModel base nem `setState`/`sendEffect`. `UiState` = `sealed class` (`Loading`/`Content`/…) em `StateFlow`; `UiEvent` = `sealed interface`; `UiEffect` one-shot = `sealed interface` em `Channel(BUFFERED).receiveAsFlow()`. Dispatcher injetado, conteúdo `@Composable` stateless `(state, onEvent)`.
- **Previews**: todo `@Composable` tem `@Preview` privada no mesmo arquivo, dentro de `AndroidEventSchedulerTheme`, um por estado relevante; telas usam o conteúdo stateless.
- **Acessibilidade**: todo elemento interativo com `testTag` (constantes em `core.ui.SemanticsTags`), `contentDescription`/`role`/`stateDescription` quando aplicável; alvos de toque ≥ 48dp.
- **Textos**: regra em `steering/arquitetura.md` §5.5 — texto fixo sempre de `strings.xml` (pt-BR), texto com variável via resource formatado (`%1$s`)/`plurals`; nada de string literal nem concatenação na UI. Erros de domínio viram `@StringRes` na apresentação, nunca texto cru de exceção.
- **Datas**: `java.time` (`LocalDate`, `LocalTime`, `ZoneId`); relógio e fuso **injetados** (`Clock`) para testes determinísticos.
- **Git**: uma spec → uma branch `feat/<nome-da-spec>` a partir de `main`, commits por camada (conventional commits pt-BR) → **UM PR** aberto só depois de todos os critérios de aceite marcados (autorizado; ver `steering/fluxo-de-trabalho.md` §0).
- **Critérios de aceite comuns (valem para toda spec, somados à seção `## Critérios de aceite` do `tasks.md`)**:
  - [ ] `./gradlew ktlintCheck detekt testDebugUnitTest :app:assembleDebug` → BUILD SUCCESSFUL.
  - [ ] Todas as tasks `[x]` e todos os critérios específicos `[x]`, cada um verificado.
  - [ ] Todo `@Composable` visual novo tem `@Preview` (obrigatório, `arquitetura.md` §5.3).
  - [ ] Nenhum texto literal na UI (§5.5); tags/semântica de acessibilidade presentes; nenhum import entre features.
  - [ ] Módulo Koin novo coberto pelo `KoinModulesVerificationTest`.
  - [ ] `ordem-de-execucao.md` §8 atualizado (e §1–§5 se algo diferiu).
- **Definição de pronto**: checklist da seção 7 de `arquitetura.md` + todas as tasks marcadas `[x]` + verificação acima verde. Relatar com franqueza o que não pôde ser verificado (ex.: comportamento real de alarme/tela cheia só em aparelho).
- **Skills do Kiro úteis**: `navigation-3`, `testing-setup`, `edge-to-edge`, `agp-9-upgrade` (se algo do AGP 9 atrapalhar o plugin do Room/KSP).
