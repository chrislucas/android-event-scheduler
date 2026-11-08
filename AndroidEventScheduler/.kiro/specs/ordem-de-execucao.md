# Ordem de execução das specs

> Documento vivo. Responde: **em que ordem executar**, **por quê**, **quem depende de quem** (por spec e por task) e **o que pode rodar em paralelo**. Deve ser **atualizado ao final de cada spec** (seção 8) e sempre que uma dependência nova for descoberta. Índice geral das specs: [`README.md`](./README.md).

## 1. Ordem recomendada (um desenvolvedor, em sequência)

| # | Spec | Depende de (duras) | Por que nesta posição |
|---|---|---|---|
| 1 | `00-fundacao-projeto` | — | Tudo precisa de Koin, tema, rotas, `AppNavHost`, `MainDispatcherRule`/`FakeClock`, `CollectEffects`, `SemanticsTags` e das bibliotecas |
| 2 | `02-splash` | 00 | Piloto barato: exercita o ciclo completo (branch → TDD → Koin → Navigation → verificação → PR) antes de investir no domínio. Só precisa de 00 |
| 3 | `01-nucleo-agendamentos` | 00 | Base de dados e domínio de **sete** specs (03, 04, 05, 07, 10, 13, 15). Quanto antes, mais cedo as demais saem do papel |
| 4 | `03-configuracoes` | 00, **01 (task 1: `ReminderOffset`)** | `UserSettingsRepository` é lido por alarmes, Upsert, Detail, Search e tela cheia; precisa existir antes deles |
| 5 | `04-alarmes-notificacoes` | 00, 01, 03 | Maior risco técnico (AlarmManager, permissões, notificação). Fornece `ScheduleNextAlarmUseCase`, `AlarmScheduler` e `NotificationPermissionEffect` |
| 6 | `05-home-calendario` | 00, 01, 04 (task 7) | Primeira tela com dados reais; só usa a spec 04 pela permissão de notificação |
| 7 | `07-criar-editar-agendamento` | 00, 01, 03, 04 | Fecha o 1º ciclo vertical: **criar → ver na Home**. `SaveScheduleUseCase` precisa do alarme |
| 8 | `10-detalhes-agendamento` | 00, 01, 03 (07 só para o "Editar" ponta a ponta) | Abre o item criado; habilita 11 e 12 e a navegação por notificação |
| 9 | `11-duplicar-excluir` | 10, 04, 01 | Mesmos arquivos do detalhe (ViewModel, barra de ações): manter em sequência com 10 |
| 10 | `12-compartilhar` | 10 | Idem; menor das três, fecha o CRUD |
| 11 | `14-alarme-tela-cheia-adiar` | 04, 03, 01 | Com itens já criáveis (07), dá para testar o alarme ponta a ponta no aparelho |
| 12 | `15-restauracao-pos-boot` | 04, 01 | Pequena; depende só do `ScheduleNextAlarmUseCase`. Vale após 14 para validar tudo de alarme junto |
| 13 | `08-recorrencia` | 07 (tasks 3–7) | Edita o formulário de 07; o motor já existe desde 01 e o alarme desde 04 |
| 14 | `06-home-gaveta-filtros` | 05, 03 | Edita o `HomeViewModel` de 05; usa `UserSettingsRepository` (e-mail da conta) |
| 15 | `09-seletor-cor-emoji` | 07, **06 (task 1: `HomePreferencesRepository`)** | Junta duas correntes: formulário (07/08) e Home (05/06). Por último nelas para não conflitar |
| 16 | `13-pesquisa` | 01, 03 (05 só para o atalho da lupa) | Folha do grafo: ninguém depende dela; a lupa da Home usa placeholder até aqui |
| 17 | `17-importar-exportar-agenda` | 01 (`upsertAll` novo), 03 (item em Configurações), 04 (`ScheduleNextAlarmUseCase`), 10 (`StringProvider`) | Funcionalidade isolada (feature própria); vem antes da 16 para que a autenticação já encontre o seletor/compartilhamento de arquivo e o "arquivo recebido" para integrar |
| 18 | `16-autenticacao-dispositivo` | 00, 03 (seção Segurança em Configurações); integra 04 (deep link), 12 (share sheet), 14 (ocultar local/notas) e 17 (seletor/arquivo recebido) | **Transversal**: é um gate acima do `AppNavHost` e toca várias telas. Por último, integra tudo de uma vez, sem ramos "se a spec X já existir" |

**Critérios usados, em ordem de peso:** (1) dependência dura de código; (2) risco técnico (alarmes cedo, para descobrir problemas de plataforma antes de empilhar UI); (3) feedback vertical rápido (criar → listar → abrir → editar antes dos "extras"); (4) conflito de arquivos (specs que editam o mesmo ViewModel ficam em sequência).

**Por que 17 e 16 no fim:** são funcionalidades novas, sem ninguém dependendo delas. A 16 altera o `AppNavHost`, a `MainActivity`, Configurações e três outras features; fazê-la por último evita reabrir esses arquivos depois. Se a proteção for prioridade de negócio, ela pode subir para logo após a 03 — nesse caso, as tasks 9 da 16 (integrações) passam a ser feitas pelas specs 12, 14 e 17 quando forem implementadas.

**Marcos de validação manual (aparelho/emulador):**
- **M1 — após a 7:** criar um lembrete para daqui a 2 min e conferir notificação (spec 04) e permissões de Android 13+/14+.
- **M2 — após a 10:** tocar a notificação abre o detalhe (deep link, spec 04 R5).
- **M3 — após a 12:** CRUD completo.
- **M5 — após a 17:** exportar e enviar por e-mail; salvar no seletor; abrir o anexo com o RememberMe e importar.
- **M6 — após a 16:** ligar a proteção, sair e voltar após o tempo limite, desbloquear com PIN/padrão em emulador API 30+ e API ≤ 29.
- **M4 — após a posição 12 da lista (specs 14 e 15):** reiniciar o emulador/`adb shell am broadcast -a android.intent.action.BOOT_COMPLETED` e conferir `adb shell dumpsys alarm | grep <pacote>`; tela cheia sobre o bloqueio (spec 14).

## 2. Grafo de dependências

```
00 ─┬─► 02
    └─► 01 ─► 03 ─┬─► 04 ─┬─► 05 ─► 06 ───────────┐
                  │       ├─► 07 ─► 08 ───────────┼─► 09
                  │       ├─► 14                  │   (09 exige 06 e 07)
                  │       ├─► 15
                  │       └─► 11 ◄─┐
                  ├─────────────► 10 ─┴─► 12       (10 exige 01+03; 11 exige 10+04)
                  └─────────────► 13               (exige 01+03)
```
Novas specs: `17` depende de 01, 03, 04 e 10; `16` depende de 00 e 03 e integra 04, 12, 14 e 17.

Três **correntes de arquivos** (cada uma edita sempre o mesmo ViewModel/tela → sequenciais dentro da corrente, independentes entre si):

| Corrente | Specs | Arquivos compartilhados |
|---|---|---|
| Home | 05 → 06 → 09 (parte Home) | `HomeViewModel`, `HomeUiState`, `HomeScreen` |
| Upsert | 07 → 08 → 09 (parte formulário) | `UpsertScheduleViewModel`, `UpsertScheduleUiState`, `TitleRow`, `UpsertScheduleScreen` |
| Detail | 10 → 11 → 12 | `ScheduleDetailViewModel`, `ScheduleDetailUiState`, `ScheduleActionBar`, `ScheduleDetailScreen` |
| Configurações / app shell | 03 → 17 → 16 | `SettingsViewModel`, `SettingsScreen`, `AppNavHost`, `MainActivity` (intent-filters, `FLAG_SECURE`) |

A spec **09 é o ponto de junção**: tasks 1–2 (formulário) exigem 07/08 prontas; tasks 3–5 (Home) exigem 06 pronta.

## 3. Dependências entre tasks (cruzam specs)

"Pode começar após" indica o menor pedaço da spec anterior que destrava a task.

| Task | Precisa de | Detalhe |
|---|---|---|
| **todas as specs**, última task | 00.t6 | `KoinModulesVerificationTest` cresce a cada módulo |
| **todas as telas** | 00.t2, t3, t7 | tema, `CollectEffects`/`SemanticsTags`, rotas + `AppNavHost` |
| **todos os testes** | 00.t4, 00.t9 | `MainDispatcherRule`, `FakeClock`; testes Robolectric dependem de `TestRememberMeApplication` |
| 00.t5 | 00.t1 | Koin e App Startup dependem das bibliotecas |
| 01.t4 (Room) | 00.t1 | KSP/Room validados no build (risco AGP 9) |
| 01.t5–t6 | 01.t1b, t2, t3, t4 | mapper usa o normalizador; repositório usa engine + DAO + mapper |
| 03.t2 | **01.t1** | `UserSettings.defaultReminderOffset` é `ReminderOffset` |
| 03.t3 | 03.t1 (`core.datastore`) | DataStore único |
| 04.t2 | 01.t1–t3 | `ScheduleNextAlarmUseCase` usa `ScheduleItem`, `RecurrenceEngine` |
| 04.t5 (receiver) | 03.t2–t3, 01.t6 | lê `UserSettingsRepository` e `ScheduleRepository` |
| 04.t7 | — | `NotificationPermissionEffect`, consumido por **05.t6** |
| 04.t8 (deep link) | 10 (entry de `ScheduleDetail`) | funciona com placeholder; ponta a ponta só após a spec 10 |
| 05.t3 | 01.t6 | `observeOccurrences` real |
| 05.t6 | 04.t7 | `NotificationPermissionEffect` |
| 06.t1 | 05 (módulo `HomeModule`) | repositório próprio da Home |
| 06.t3 | 03.t3, 05.t3 | `UserSettingsRepository` + pipeline do Home |
| 07.t1 | 04.t2 | `SaveScheduleUseCase` chama `ScheduleNextAlarmUseCase` |
| 07.t4 | 03.t3, 01.t6 | padrão de lembrete + `getById`/`upsert` |
| 07.t5 | 01.t7 | rótulos de alerta/repetição |
| 08.t2–t5 | 07.t3–t7 | estado, VM e tela do formulário |
| 08.t6 (integração) | 04.t2, 01.t6 | alarme + `observeOccurrences` |
| 09.t1 | 00 (tema), 01 (`StickerEmoji`/`EventColorOption`) | diálogos em `core.ui` — pode começar cedo |
| 09.t2 | 07.t4–t5 (e 08 por conflito) | `TitleRow` e VM do formulário |
| 09.t3–t5 | **06.t1** | `HomePreferencesRepository` nasce na 06 |
| 10.t1 | 01.t6 | `observeById` |
| 10.t2–t3 | 01.t7, 03.t3, **StringProvider** (ver §4) | mapper e e-mail da conta |
| 11.t2–t3 | 04.t2–t3, 10.t3 | `ScheduleNextAlarmUseCase`, `AlarmScheduler.cancel` |
| 11.t4–t6 | 10.t3–t4 | VM e `ScheduleActionBar` |
| 12.t1–t4 | 10.t2–t4 (11 por conflito) | `ScheduleDetailUi`, VM, barra |
| 13.t3 | 01.t6, 03.t3 | `search` e e-mail da conta |
| 14.t1 | 04.t4–t5 | `AlarmIntents` (`notificationId`, action, extra) |
| 14.t2 | 04.t3 | `AlarmScheduler` |
| 14.t3 | 03.t3 | `defaultSnoozeMinutes` |
| 15.t1 | 04.t2 | `ScheduleNextAlarmUseCase` |
| 15.t3–t4 | 15.t1, 04 (manifest) | receiver no manifest |
| 17.t1 | 01.t4–t6 | `upsertAll` com `@Transaction` no DAO/repositório do núcleo |
| 17.t4 | 04.t2, 10.t2 | `ScheduleNextAlarmUseCase`; `StringProvider` para o sufixo " (Importado)" |
| 17.t8 | 03.t5 | item em Configurações |
| 17.t9 | 04.t8 | mesmo padrão do `DeepLinkState` (`IncomingFileState`) |
| 16.t3, 16.t8 | 03.t2–t5 | `UserSettings` e tela de Configurações |
| 16.t7 | 04.t8, 17.t9 | gate só consome deep link e arquivo recebido quando desbloqueado |
| 16.t9 | 12.t4, 14.t3, 17.t7 | `suppressNextLock()` no share sheet e no SAF; ocultar local/notas no alarme |

## 4. Artefatos compartilhados: quem cria

| Artefato | Criado em | Usado por | Observação |
|---|---|---|---|
| `ScheduleNotFoundException` | **01** (task 1) | 11, 14 | antes só "onde for primeiro necessária" — ficou explícito na 01 |
| `StringProvider` (`core.common`) | **10** (task 2) | 10, 11, 12 | antes só aparecia em 11; passa a nascer na 10 |
| `AlarmIntents`, `AlertPresentationMode.forType` | 04 | 14 | constantes públicas de `core.alarm` |
| `ScheduleNextAlarmUseCase` | 04 | 07, 11, 15 e o receiver | |
| `UserSettingsRepository` | 03 | 04, 06, 07, 10, 13, 14 | |
| `HomePreferencesRepository` | 06 | 09 | |
| `core.ui.component` (diálogos cor/emoji) | 09 | 07 (via 09), Home | |
| `ScheduleRepository.upsertAll` | 17 | 17 | transação no núcleo |
| `AppLockController` (`core.security`) | 16 | app, auth, settings, alarm, backup | gate + `suppressNextLock()` |
| `IncomingFileState` | 17 | app | arquivo recebido por `VIEW`/`SEND` |
| `SemanticsTags`, `strings.xml`, `AppModules`, `AppNavHost`, `AndroidManifest` | todas | todas | conflitos **triviais** (linhas adjacentes) |

**Placeholders no `AppNavHost`** (a spec que cria a tela remove o placeholder):

| Destino | Placeholder criado em | Removido em |
|---|---|---|
| `Splash` | 00 | 02 ✅ |
| `Home` (destino da Splash) | 02 | 05 |
| `Search`, `Upsert`, `Detail`, `Settings` (saídas da Home) | 05 | 13, 07, 10 e 03 (entry) / 06 (ligação pela gaveta) |
| `Detail` (deep link da notificação) | 04 | 10 |

## 5. Paralelização (git worktrees, `steering/fluxo-de-trabalho.md` §3)

Com mais de um executor, em **ondas**; dentro de cada onda as specs tocam pacotes distintos (conflito só trivial):

| Onda | Specs em paralelo | Condição para começar |
|---|---|---|
| A | 00 | — |
| B | 01 ∥ 02 | 00 mergeada |
| C | 03 | 01.t1 pronta (de preferência 01 mergeada) |
| D | 04 ∥ 10 ∥ 13 | 03 mergeada (10 e 13 não precisam de 04) |
| E | 05 ∥ 07 ∥ 11 ∥ 14 ∥ 15 | 04 mergeada (11 também exige 10) |
| F | 06 ∥ 08 ∥ 12 | 05 / 07 / 10(+11) respectivamente |
| G | 09 ∥ 17 | 09: 06 e 08 mergeadas · 17: 03, 04 e 10 mergeadas (pode adiantar para a onda E/F) |
| H | 16 | 17 mergeada (e 12, 14) |

**Nunca em paralelo:** specs da mesma corrente (§2) — conflito **não-trivial** (mesmo ViewModel/barra de ações). Em cada onda, rebase mantendo as linhas de agregação de ambos os lados.
A ordem linear da §1 e as ondas são compatíveis; a linear prioriza feedback vertical, as ondas priorizam profundidade do grafo.

## 6. Spec por spec: o que pode atrasar

| Spec | Risco para o cronograma | Mitigação |
|---|---|---|
| 00 | AGP 9 + KSP/Room, Navigation 3 | validar o build na task 1 antes de seguir |
| 04 | permissões e comportamento por versão do Android | M1 em aparelho logo após a 07 |
| 14 / 15 | só verificáveis em aparelho | marcos M2/M4; relatar o que não foi verificado |
| 09 | junção de duas correntes | executar por último nas duas |
| 10 | concentra `StringProvider` e a fundação do detalhe | não paralelizar 11/12 |
| 16 | transversal; diálogo do sistema só verificável em aparelho; API < 30 usa caminho diferente | executar por último; marco M6 em duas versões do Android |
| 17 | arquivo não confiável; integração com apps de e-mail | validação completa antes de gravar; marco M5 |

## 7. Regras de uso

- Antes de iniciar uma spec, conferir na §1/§3 que suas dependências estão **concluídas e mergeadas** (ou usar o placeholder indicado). Se não estiverem, parar e avisar.
- Dentro de uma spec as tasks são sequenciais, salvo dependência explícita acima.
- Dependência nova descoberta durante a execução → registrar aqui (§3/§4) **e** corrigir a tabela de dependências do `README.md`.

## 8. Status de execução (atualizar ao concluir cada spec)

| Spec | Status | Concluída em | Branch / PR | Desvios e descobertas |
|---|---|---|---|---|
| 00 | **concluída e mergeada** | 2026-11-07 | `feat/00-fundacao-projeto` / PR [#1](https://github.com/chrislucas/android-event-scheduler/pull/1) | detekt 2.0 alpha (JDK 25); `kotlinx-serialization` 1.9.0 (compilador Kotlin 2.2); testes Robolectric via `TestRememberMeApplication`; `core-splashscreen` movido para a 02 |
| 01 | **concluída e mergeada** | 2026-11-07 | `feat/01-nucleo-agendamentos` / PR [#4](https://github.com/chrislucas/android-event-scheduler/pull/4) | rótulos de emoji/cor/calendário saem do domínio (vão para resources nas specs 09 e 06); ordenação "dia inteiro primeiro" já no núcleo; formatos pt-BR conferidos só na JVM |
| 02 | **concluída e mergeada** | 2026-11-07 | `feat/02-splash` / PR [#2](https://github.com/chrislucas/android-event-scheduler/pull/2) | placeholder de **Home** no `AppNavHost` (removido na 05); verificação Koin do grafo agregado |
| 03 | **concluída** (aguardando merge) | 2026-11-07 | `feat/03-configuracoes` / PR [#5](https://github.com/chrislucas/android-event-scheduler/pull/5) | ViewModels passam a usar o dispatcher `main` (Splash ajustada); `TestRememberMeApplication` recarrega módulos a cada teste; efeitos Toast × Snackbar |
| 04 | **concluída** (aguardando merge) | 2026-11-07 | `feat/04-alarmes-notificacoes` / PR (link no commit seguinte) | sem placeholder de `ScheduleDetail` (a entry vem da spec 10); `ScheduleNotifier`/canais via App Startup próprio; `NotificationPermissionEffect` só `rememberSaveable`; verificação Koin com `extraTypes` do framework |
| 05 | pendente | | | |
| 06 | pendente | | | |
| 07 | pendente | | | |
| 08 | pendente | | | |
| 09 | pendente | | | |
| 10 | **concluída** (aguardando merge) | 2026-11-07 | `feat/10-detalhes-agendamento` / PR [#7](https://github.com/chrislucas/android-event-scheduler/pull/7) | `StringProvider` nasceu em `core.common` e os formatadores de `core.ui.format` ganharam overloads com ele (mapper testável na JVM); placeholder de **Upsert** no `AppNavHost` (removido na 07); `CalendarSource.label` em `core.ui.format`; saídas Home/pesquisa/notificação → Detail ficam para 04/05/13 |
| 11 | pendente | | | |
| 12 | pendente | | | |
| 13 | pendente | | | |
| 14 | pendente | | | |
| 15 | pendente | | | |
| 16 | pendente | | | |
| 17 | pendente | | | |

### Como manter este documento

Ao **final de cada spec** (parte do relatório final e do PR):
1. Atualizar a linha da spec em §8: status (`pendente` → `em andamento` → `concluída`), data, branch/PR e desvios.
2. Se alguma dependência real **diferiu** do previsto (tarefa que passou a precisar de outra, artefato que nasceu em outra spec, placeholder que sobrou), corrigir §1–§5 e anotar no histórico abaixo.
3. Se a ordem recomendada mudar, reescrever §1 e §5 e dizer o motivo.
4. Atualizar também a tabela de dependências do `README.md` se ela ficar diferente.

### Histórico de mudanças

| Data | Mudança | Motivo |
|---|---|---|
| 2026-11-07 | Spec 03 concluída. **Todas** as specs com ViewModel passam a injetar o dispatcher `main` e os testes de integração de tela seguem o padrão de drenar o looper (steering §5.1/§5.2). Ordem inalterada; próxima onda paralela: **04 ∥ 10 ∥ 13** | Toast fora da main e `single`s com arquivos de testes anteriores |
| 2026-11-07 | Spec 01 concluída. A **09** passa a criar os rótulos de acessibilidade de `StickerEmoji`/`EventColorOption` em `strings.xml` e a **06** os de `CalendarSource` (antes previstos no domínio). Ordem inalterada | Domínio não conhece `R` (`arquitetura.md` §5.5) |
| 2026-11-07 | Novas specs **16 (autenticação com o bloqueio do dispositivo)** e **17 (importar/exportar a agenda)**, posicionadas no fim (17 → 16); nova corrente Configurações/app shell (03 → 17 → 16) e ondas G/H | Pedido de novas funcionalidades; 16 é transversal e integra 12, 14 e 17 |
| 2026-11-07 | Spec 02 concluída. A 05 passa a remover também o placeholder de `Home` criado pela 02. Ordem inalterada | Home ainda não existe quando a Splash navega |
| 2026-11-07 | Spec 00 concluída. Nova dependência implícita de **todas** as specs com testes Robolectric: `TestRememberMeApplication` + `robolectric.properties` (spec 00). Ordem inalterada | Robolectric não executa o `InitializationProvider` |
| 2026-11-07 | Versão inicial. Corrigidas no `README.md` as dependências que estavam incompletas: 03 → 01; 05 não exige 03; 09 → 06; 10 → 03; 13 → 03 (não exige 05); 14 → 03. Atribuídos dono de `ScheduleNotFoundException` (01) e de `StringProvider` (10) | Análise das tasks entre specs |
