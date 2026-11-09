# Critérios de aceite: quando uma spec está bem implementada

> Lista **geral**: vale para toda spec e soma-se à seção `## Critérios de aceite` do `tasks.md` de cada uma. Cada item é marcado `[x]` só **depois de verificado**: rodando o comando ou o teste, ou lendo o código, e nunca presumindo. Item que não puder ser verificado fica `[ ]` com `(não verificado: motivo)` e é declarado no PR. Isso só é aceitável quando a ressalva faz sentido (ex.: comportamento que só existe em aparelho).
>
> Origem: as regras do steering (`arquitetura.md`, `fluxo-de-trabalho.md`) e as lições das ondas A–G (`ordem-de-execucao.md`, histórico). Quando um critério nasceu de um problema real, o caso é citado entre parênteses.

Copie o checklist para o corpo do PR, na seção "Critérios gerais".

## 1. Escopo e requisitos

- [ ] Todo requisito `Rn.m` do `requirements.md` está implementado **e** coberto por pelo menos um teste. O PR lista o mapeamento requisito → teste.
- [ ] Todas as tasks do `tasks.md` e todos os critérios específicos estão `[x]`.
- [ ] As dependências da spec (`ordem-de-execucao.md` §1/§3) estavam **mergeadas** antes de começar.
- [ ] Desvios do `design.md` estão listados no PR, cada um com o motivo. Nenhum requisito foi cortado sem aviso.
- [ ] Nenhuma funcionalidade fora do escopo foi acrescentada ("enquanto estava ali").

## 2. Arquitetura (`arquitetura.md`)

- [ ] Pacotes no lugar certo: `app/`, `core/…`, `feature/<nome>/{data,domain,presentation,di}`. **Nenhuma feature importa outra**; o que é compartilhado sobe para `core`.
- [ ] `domain` sem Android (`android.*`, `R`, `Context`).
- [ ] UseCase só quando o `design.md` justifica (regra compartilhada, orquestração de 2+ fontes ou regra própria). Passa-direto de repositório não vira UseCase.
- [ ] Koin: módulo novo registrado em `AppModules` e coberto pelo `KoinModulesVerificationTest`. Tipos extras entram nas listas `androidFrameworkTypes`/`runtimeParameterTypes` existentes.
- [ ] Navegação: rota em `core.navigation`, `<feature>Entry` na feature, ligação **só** no `AppNavHost`. Nenhum placeholder esquecido; se sobrar algum, ele está listado no PR com a spec que vai removê-lo.

## 3. Presentation

- [ ] ViewModel estende `ViewModel` direto, sem classe base, `setState` ou `sendEffect`.
- [ ] `UiState` é `sealed class` em `StateFlow`; `UiEvent` é `sealed interface`; `UiEffect` é one-shot via `Channel(BUFFERED).receiveAsFlow()`, e efeitos nunca vão no estado.
- [ ] Telas com muitos eventos usam **famílias** `sealed` aninhadas, com handler exaustivo por família. **Proibido** `@Suppress("CyclomaticComplexMethod")` e `else ->` sobre `sealed` para contornar (caso do `onEvent` do Upsert, spec 08).
- [ ] Dispatcher `main` e relógio (`Clock`) **injetados**. Nada de `Dispatchers.*`, `LocalDate.now()` ou `System.currentTimeMillis()` dentro da regra (caso do Toast fora da main thread, spec 03).
- [ ] Estado que precisa sobreviver à morte do processo está no `SavedStateHandle`.
- [ ] Conteúdo Compose stateless `(state, onEvent)`. A tela coleta os efeitos e só ela toca `Context`/`Activity`.

## 4. UI, textos e acessibilidade

- [ ] **Todo** `@Composable` visual novo tem `@Preview` privada, dentro de `AndroidEventSchedulerTheme`, uma por estado relevante.
- [ ] Nenhum texto literal na UI: texto fixo vem de `strings.xml` pt-BR; texto com variável usa resource formatado (`%1$s`) ou `plurals`; nada de concatenação. Espaço no início ou no fim exige aspas no XML (caso do `" (Cópia)"`, spec 11).
- [ ] Erros de domínio viram `@StringRes` na apresentação, nunca texto cru de exceção.
- [ ] Todo elemento interativo tem `testTag` (constante em `SemanticsTags`) e, quando aplicável, `contentDescription`, `role`, `stateDescription` e `selected`.
- [ ] Alvos de toque ≥ 48 dp. Cor nunca é a única pista (há ícone ou rótulo).
- [ ] Gestos personalizados têm alternativa acessível (ação de acessibilidade personalizada), porque o TalkBack consome toque duplo e deslizes.
- [ ] Layout funciona com fonte grande e tela baixa (rolagem, `FlowRow`, `maxLines`/`Ellipsis`).

## 5. Testes

- [ ] Testes mínimos do `design.md` existem, com nomes ``fun `given … when … then …`()`` em inglês, JUnit4 + MockK + `kotlinx-coroutines-test` + Robolectric/Compose UI Test, **sem Turbine**.
- [ ] Regras puras com TDD (teste antes do código, como nas tasks).
- [ ] Testes de tela Robolectric com tela alta: `@Config(qualifiers = "pt-rBR-w411dp-h891dp")`, e `h2400dp` para formulários longos. O padrão do Robolectric (320×470 dp) esconde itens da `LazyColumn`.
- [ ] Integração com grafo real drena o looper dentro do `waitUntil` (`shadowOf(Looper.getMainLooper()).idle()`). Flow do Room é coletado em `backgroundScope` + `Channel`.
- [ ] **Pelo menos um teste ponta a ponta com a `MainActivity`** cobrindo a integração da spec com o app real (Splash → Home → …). Foi assim que as sincronizações das ondas E–G pegaram o que a spec isolada não alcançava.
- [ ] Nenhum teste existente foi enfraquecido para passar. Se um teste teve de mudar, o PR explica por quê (caso dos testes de degradação da spec 04 ajustados na spec 14).
- [ ] Testes determinísticos: nada depende de `now()` sem `FakeClock`, nem de ordem entre testes. Rodam verdes duas vezes seguidas.

## 6. Qualidade e build

- [ ] `./gradlew ktlintFormat` e depois `./gradlew ktlintCheck detekt testDebugUnitTest :app:assembleDebug --rerun-tasks` → **BUILD SUCCESSFUL, duas vezes**. O PR traz o número total de testes e de falhas.
- [ ] Nenhum `@Suppress` novo sem justificativa no código e no PR. Nenhuma regra do detekt desligada para a spec passar.
- [ ] **Todo commit compila** (`fluxo-de-trabalho.md` §1.1). Rodar da raiz do repositório:
  `git rebase --exec "cd AndroidEventScheduler && ./gradlew :app:compileDebugUnitTestKotlin -q" origin/main`. Commit que falhar é unido ao que o completa **antes** do PR, porque depois do merge o histórico fica (caso do #16; ver `docs/decisoes/commits-compilaveis-e-git-bisect.md`).
- [ ] Dependência nova tem versão **exata** em `libs.versions.toml`, estável quando existir, compatível com AGP/Kotlin do projeto. O PR justifica a escolha.

## 7. Permissões, segurança e plataforma

- [ ] Permissão nova ou removida: documentada em `docs/permissoes.md` (para quê, como é pedida, se ausente, Google Play), comentada no manifest e refletida no `RequestedPermissionsGuardRobolectricTest`. **Manifest mesclado conferido**, porque bibliotecas injetam permissões (caso do `USE_FINGERPRINT` da `androidx.biometric`, spec 16).
- [ ] Componentes do manifest com o menor `exported` possível. Intents recebidos são validados (ação, MIME, esquema) e extras são tratados como não confiáveis.
- [ ] Entrada externa (arquivo, intent, deep link) tem limites e validação **antes** de gravar, com gravação atômica (caso da spec 17).
- [ ] Comportamento que só existe em aparelho (alarme real, tela cheia sobre o bloqueio, boot, diálogo do sistema, share sheet, TalkBack) está declarado como **não verificado em dispositivo**, com roteiro manual quando couber.

## 8. Documentação

- [ ] `ordem-de-execucao.md` §8 atualizado com status, data, branch/PR e desvios. Dependência nova ou diferente corrigida em §1–§5 e no histórico. Em execução paralela, quem coordena consolida isso num PR de docs.
- [ ] `README.md` das specs (tabela de dependências) e o `README.md` do projeto atualizados se a spec mudou algo listado neles.
- [ ] Decisão de processo ou arquitetura nova registrada no steering e, se instrutiva, em `docs/decisoes/`.

## 9. PR e integração

- [ ] Uma branch `feat/<spec>` a partir de `origin/main` → **um** PR. Push só na branch da spec, nunca na `main`, sem merge.
- [ ] Antes do PR: `git fetch && git rebase origin/main`. Conflitos em arquivos de agregação (`AppModules`, `AppNavHost`, `SemanticsTags`, `strings.xml`, `AndroidManifest`, `docs/permissoes.md`) resolvidos mantendo os dois lados, com linha em branco entre os blocos.
- [ ] Depois do rebase: nenhum marcador de conflito em **nenhum** commit (`for c in $(git rev-list origin/main..HEAD); do git grep -n '^<<<<<<<' $c -- .; done`) e nenhuma string ou tag duplicada.
- [ ] Placeholders vizinhos apagados pelas duas specs são removidos **por inteiro** (função, preview, imports, tag, string), sem ressuscitar o que o outro lado apagou (caso das specs 05 e 07).
- [ ] CI do PR verde, conferida no **último** commit.
- [ ] Review do Copilot pedido. Apontamentos avaliados um a um na seção "Revisão do Copilot e correções", ou registro de que não houve revisão (ex.: sem cota).
- [ ] Ao sincronizar depois de outro merge: verificação refeita duas vezes, total de testes conferido (o da `main` mais os novos do PR) e seção "Sincronização" no corpo do PR.

## Modelo de corpo de PR

```markdown
## Resumo (por requisito)
- R1 … → arquivos/testes
## Critérios de aceite da spec
(checklist do tasks.md, marcado)
## Critérios gerais
(checklist deste documento, marcado; itens não verificados com o motivo)
## Verificação
- ktlintCheck detekt testDebugUnitTest :app:assembleDebug --rerun-tasks: 2× verde, N testes, 0 falhas
- Checagem por commit: K commits compilam isoladamente
## Desvios do design
## Descobertas para a ordem de execução
## Não verificado (aparelho) + roteiro manual
## Revisão do Copilot e correções
```
