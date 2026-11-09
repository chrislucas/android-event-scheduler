---
inclusion: always
---

# Fluxo de trabalho: PRs, review e paralelização

> Regras de **processo de execução** (branches, PRs, review e paralelização de tarefas). As regras de **arquitetura de código** ficam em [`arquitetura.md`](./arquitetura.md); o plano de funcionalidades fica em `.kiro/specs/` (índice em `.kiro/specs/README.md`).

## 0. Critérios de aceite e PR por spec

- **Cada spec tem uma seção `## Critérios de aceite`** no fim do seu `tasks.md` (checklist verificável) e os **critérios gerais** de [`.kiro/specs/criterios-de-aceite.md`](../specs/criterios-de-aceite.md) (lista canônica, resumida no `.kiro/specs/README.md`, com o modelo de corpo de PR). São eles — e não a sensação de "terminei" — que definem se a spec foi concluída com êxito.
- Marque `- [x]` em cada critério **somente depois de verificá-lo** (rodando o teste/comando, não presumindo). Critério que não puder ser verificado (ex.: depende de aparelho) fica `[ ]` com a nota `(não verificado: motivo)` e deve ser dito no PR; só é aceitável se o próprio critério prever essa ressalva.
- **Fluxo por spec: uma branch → UM PR.** Com todos os critérios marcados e a verificação completa verde: push da branch `feat/<spec>` e abertura do PR contra `main`. O PR contém o checklist dos critérios de aceite (marcado) e o resultado da verificação. Autorização do Christopher: a abertura de **um PR por spec** está autorizada; push em `main`, `--force`, merge e PRs adicionais continuam exigindo confirmação.
- Uma spec dependente só começa depois que a anterior estiver **mergeada** (ver `ordem-de-execucao.md`).

## 1. Branch e PR por tarefa

- Uma spec (ou tarefa) → uma branch a partir de `origin/main` (`feat/<nome-da-spec>`) → commits organizados **por camada** (conventional commits em pt-BR: `feat`/`fix`/`test`/`docs`/`chore(<escopo>)`) → PR contra `main`.
- Antes de abrir o PR, rodar a verificação completa **localmente**:
  `./gradlew testDebugUnitTest ktlintCheck detekt :app:assembleDebug` → **BUILD SUCCESSFUL**.
  O projeto **não tem flavors**. ktlint e detekt vêm do convention plugin `androideventscheduler.code.quality` (`build-logic`) e **reprovam** o build; a CI (`.github/workflows/ci-validation.yml`) roda exatamente estas verificações em push/PR para `main`, então um PR só deve ser aberto com o comando acima verde. O `:app:assembleDebug` é obrigatório porque o merge de recursos/manifest do APK só aparece no `assemble`, não nos testes.
- Marcar `- [x]` em `tasks.md` apenas depois da tarefa implementada **e** testada. Antes de iniciar uma spec, seguir `.kiro/specs/ordem-de-execucao.md` (ordem e dependências); ao concluí-la, atualizar o status e as dependências descobertas nesse documento (seção 8) como parte do PR.
- Nunca commitar/push direto em `main`. O push da branch da spec e o PR da spec (seção 0) estão autorizados; qualquer outro push/PR (incluindo PR empilhado) exige a confirmação do Christopher.
- Comportamentos que só valem em aparelho/emulador (alarme real, tela cheia sobre o bloqueio, broadcast de boot, permissões em runtime) devem ser declarados no PR como **não verificados em dispositivo**, quando for o caso.

## 1.1. Todo commit compila (`git bisect`)

- **Cada commit** da branch compila produção e testes, não só o último commit. Sem isso, o `git bisect` aponta o culpado errado ou para sem resposta, e `revert`/`cherry-pick` de um commit isolado quebram.
- Camada que só compila junto com a seguinte vai **no mesmo commit** dela. Casos típicos: membro novo numa `sealed` consumida por `when` exaustivo (o tipo e os ramos entram juntos); parâmetro obrigatório novo (os call sites mudam juntos); dependência nova no ViewModel (o módulo Koin muda junto); string nova (entra com o código que a usa). Não use `else ->` para "fazer compilar": ele esconde efeitos futuros sem tratamento.
- **Antes de abrir o PR** (depois do merge o histórico fica): `git rebase --exec "cd AndroidEventScheduler && ./gradlew :app:compileDebugUnitTestKotlin -q" origin/main`. O `--exec` roda na raiz do repositório. Commit que falhar é unido ao que o completa (`fixup`).
- Caso real (PR #16, spec 12: dois commits que não compilam ficaram na `main` e devem ser pulados num bisect), guia de `git bisect` e script com `exit 125`: [`docs/decisoes/commits-compilaveis-e-git-bisect.md`](../../docs/decisoes/commits-compilaveis-e-git-bisect.md).

## 2. Review do Copilot em todo PR

Ao abrir um PR, **peça o review do Copilot** (botão "Copilot" em *Reviewers*), desde que ainda haja cota. Depois que o review chegar:

1. **Analise cada apontamento** e decida se procede (nem todo apontamento faz sentido no contexto).
2. Para os que procederem, **faça a correção** sem quebrar o que já existe (testes + `assembleDebug` verdes) e, de preferência, **adicione um teste** que cubra o problema apontado.
3. **Documente na descrição do PR** uma seção "Revisão do Copilot e correções" listando: o apontamento, se foi aceito ou recusado (com o motivo), e a mudança feita.
4. Se o mesmo padrão aparecer em outras partes do código, corrija junto quando for do escopo do PR; se for de outra spec já mergeada, registre e trate em **PR de follow-up** para manter o PR focado.

## 2.1. PRs de correção de bug são sempre bem documentados

Todo PR que corrige um **bug** (crash, comportamento errado, regressão) é documentado em dois momentos:

1. **Na descrição do PR**, obrigatoriamente, com estas seções em pt-BR:
   - **Sintoma** — o que o usuário/dev via (crash na partida, alarme não dispara, tela X não abre etc.), em que **versão do Android/dispositivo ou ambiente de teste** e em que ação (ex.: "após reiniciar o aparelho, alarmes recorrentes não voltam"). Inclua a exceção/trecho-chave do stacktrace, não o log inteiro.
   - **Causa raiz** — *por que* acontecia, não só *o que* mudou. Descreva a cadeia até a origem (ex.: ordem de inicialização do Koin, banco indisponível antes do desbloqueio, estado não reavaliado). Explique também **por que só se manifestava naquela condição** (versão de API, permissão, fuso, rotação), quando for o caso.
   - **Correção** — o que foi alterado e por que resolve a causa raiz, deixando claro que o comportamento esperado é preservado (ou, se mudou, o quê).
   - **Verificação** — o que rodou verde (`testDebugUnitTest`, `ktlintCheck`, `detekt`, `:app:assembleDebug`) e, com franqueza, **o que não foi possível verificar** (ex.: comportamento real só em aparelho).
   - **Revisão do Copilot e correções** — como em qualquer PR (seção 2).
2. **Depois de abrir o PR**, pergunte ao Christopher **se ele quer um documento `.md`** registrando a correção (em `docs/`, com diagnóstico aprofundado e lições). Só gere o `.md` se ele confirmar; a descrição do PR é sempre obrigatória, o `.md` é sob demanda.

> Por quê: bug sem causa raiz documentada volta. A descrição do PR é o registro mínimo e inseparável da correção; o `.md` em `docs/` é o registro durável e didático, usado quando a armadilha é recorrente ou instrutiva.

## 3. Paralelizar tarefas quando o risco de conflito for baixo

Sempre que possível, **paralelize** as tarefas independentes, avaliando antes o **risco de conflito de arquivos** (não só a dependência lógica). A ordem e as dependências entre specs estão no `.kiro/specs/README.md`.

1. **Mapeie os arquivos que cada tarefa toca.** Tarefas em **pacotes de feature distintos** (`feature.<a>` × `feature.<b>`) praticamente não colidem — a regra "uma feature não importa outra" garante isolamento. Os pontos de contato comuns costumam ser: `app/AppModules.kt` (lista de módulos Koin), `app/AppNavHost.kt` (entries), `core/ui/SemanticsTags.kt`, `res/values/strings.xml`, `AndroidManifest.xml`, `KoinModulesVerificationTest` e `gradle/libs.versions.toml`.
2. **Classifique o conflito.** *Trivial* = linhas adjacentes em pontos de agregação (lista `modules(...)`, novo `entry<>`, nova tag, nova string, nova permissão), resolvível mantendo os dois lados. *Não-trivial* = duas tarefas editam a mesma função/arquivo de lógica, ou uma depende do código da outra (ex.: 06 e 09 editam `HomeViewModel`; 10, 11 e 12 editam o detalhe — fazer em sequência).
3. **Se o conflito for trivial, execute sub-agentes em paralelo — um por tarefa — em _git worktrees_ isoladas** (`git worktree add ../<dir> -b <branch> origin/main`). Cada agente tem seu próprio checkout, então **não se atropelam** (sub-agentes no mesmo checkout misturam arquivos e deixam mudanças não-commitadas umas das outras). Cada agente roda a verificação completa e commita por camada; push/PR só com confirmação do Christopher. Os conflitos triviais remanescentes são resolvidos no rebase de cada PR, mantendo todos os módulos/entries. Ao terminar, remover a worktree (`git worktree remove <dir>`).
4. **Se o conflito for não-trivial, faça sequencialmente** (uma branch/PR por vez) — o custo de coordenação supera o ganho.
