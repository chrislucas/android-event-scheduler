# Todo commit compila: commits atômicos e `git bisect`

> Regra resumida no steering: [`fluxo-de-trabalho.md` §1.1](../../.kiro/steering/fluxo-de-trabalho.md). Este documento explica o porquê, registra o caso que motivou a regra (PR #16, spec 12) e mostra como usar o `git bisect` no projeto.

## 1. A regra

Na `main`, **cada commit compila e passa nos testes**, não só o último commit do PR. Organizar os commits por camada (domínio → ViewModel → tela) continua sendo a regra. Mas uma camada que só compila junto com a seguinte não é um commit à parte: ela vai no mesmo commit da camada que a completa.

A verificação completa (`ktlintCheck detekt testDebugUnitTest :app:assembleDebug`) continua sendo exigida só no topo da branch. Em cada commit intermediário, o mínimo é a compilação de produção **e** de testes.

## 2. Por que importa: `git bisect`

O `git bisect` encontra o commit que introduziu um bug por busca binária no histórico. Entre um commit "bom" e um "ruim", ele faz o checkout do commit do meio, roda um teste, decide em qual metade o bug está e repete. Com 64 commits, bastam ~6 passos.

A busca só funciona se o teste de cada commit responder à pergunta "o bug está aqui?". Um commit que **não compila** não responde nada:

- No `git bisect run`, a falha de compilação vira "ruim" (código de saída ≠ 0) e o bisect **aponta o culpado errado**.
- Ou é preciso marcar o commit com `git bisect skip`. Se o commit que não compila for justamente o vizinho do culpado, o bisect termina com "não foi possível determinar" e uma lista de candidatos.

Commits que não compilam também atrapalham `git revert` de um commit isolado, `git cherry-pick` para outra branch e a leitura do histórico com `git log -p`.

## 3. O caso que motivou a regra: PR #16 (spec 12, compartilhar)

O PR saiu com cinco commits, organizados por camada:

| Commit | Conteúdo | Compila? |
|---|---|---|
| `173ceab` | `ScheduleShareTextBuilder` + strings + testes | sim |
| `5034289` | evento `OnShareClicked` e efeito `ShareText` no ViewModel | **não** |
| `211c464` | `ShareLauncher` (dispara o seletor) | **não** |
| `dd2dd55` | botão Compartilhar + tratamento do efeito na tela + ponta a ponta | sim |
| `b6a65c9` | tasks e critérios marcados | sim |

**Causa:** o `ScheduleDetailScreen` trata os efeitos num `when` exaustivo sobre a `sealed interface ScheduleDetailUiEffect`. Ao acrescentar `ShareText` ao contrato (commit 2), esse `when` passou a não cobrir todos os casos, o que é erro de compilação em Kotlin:

```
e: .../ScheduleDetailScreen.kt:88:9 'when' expression must be exhaustive. Add the 'is ShareText' branch or an 'else' branch instead.
```

O ramo `is ShareText` só foi escrito no commit 4, junto com o botão. A CI só compila o topo do PR, por isso passou.

**Como foi detectado:** depois de o PR estar aberto, cada commit foi compilado isoladamente, como o `git bisect run` faria (seção 5). Os commits 2 e 3 falharam com o erro acima.

**O que aconteceu depois:** os commits 2, 3 e 4 foram unidos localmente num só (`fixup`), e a árvore final ficou idêntica. Mas o PR foi mergeado (merge commit `92209ee`) antes de o histórico corrigido subir. Os cinco commits originais estão na `main`, inclusive os dois que não compilam (`5034289` e `211c464`).

**Por que a `main` não foi reescrita:** consertar exigiria `rebase` + `push --force` na `main`. Isso muda o SHA de todos os commits seguintes e quebra os clones e as branches abertas (#17, #18). Para dois commits num intervalo conhecido, o custo não compensa. Num bisect, basta marcá-los como não testáveis:

```bash
git bisect skip 5034289 211c464   # ou use o script com exit 125 da seção 6
```

Com `--first-parent`, o bisect anda só pelos merge commits da `main`, que compilam, e nunca para nesses dois.

**Lição:** a verificação por commit tem de rodar **antes** de abrir o PR, não depois. Uma vez mergeado, o histórico fica. Por isso a seção 5 faz parte do fluxo (steering §1.1).

**Por que não um `else ->` no `when`:** ele faria o commit 2 compilar, mas engoliria em silêncio qualquer efeito novo no futuro. O `when` exaustivo sobre `sealed` é justamente a garantia de que nenhum efeito fica sem tratamento.

**Alternativa válida para quem quer manter commits pequenos:** reordenar, de modo que o tipo novo entre no contrato no mesmo commit em que todos os `when` que o consomem ganham o ramo. A regra é sobre compilar, não sobre o tamanho do commit.

## 4. Padrões do projeto que costumam causar isso

- **Novo membro numa `sealed` consumida por `when` exaustivo**: efeitos, eventos ou estados de UI. O tipo e todos os ramos que o tratam entram no mesmo commit.
- **Parâmetro obrigatório novo** em `HomeScreen`/`homeEntry`/Composable: o call site (`AppNavHost`, testes) muda no mesmo commit.
- **Construtor de ViewModel com dependência nova**: o módulo Koin e o `KoinModulesVerificationTest` mudam no mesmo commit.
- **Resource referenciado antes de existir** (`R.string.x`): a string entra no mesmo commit que a usa.

## 5. Como verificar antes do PR

Compila produção e testes em **cada** commit da branch e para no primeiro que falhar:

```bash
# o --exec roda na raiz do repositório (não no diretório atual), por isso o "cd"
git rebase --exec "cd AndroidEventScheduler && ./gradlew :app:compileDebugUnitTestKotlin -q" origin/main
```

O `compileDebugUnitTestKotlin` depende do `compileDebugKotlin`, então cobre os dois. Se um commit falhar, o rebase para nele: corrija, `git add`, `git commit --amend` e `git rebase --continue`. Para checar sem reescrever nada, use uma worktree descartável:

```bash
# a partir da raiz do repositório
git worktree add --detach ../wt-check HEAD && cp AndroidEventScheduler/local.properties ../wt-check/AndroidEventScheduler/
cd ../wt-check/AndroidEventScheduler
for c in $(git rev-list --reverse origin/main..HEAD); do
  git checkout -q "$c"
  ./gradlew :app:compileDebugUnitTestKotlin -q >/dev/null 2>&1 && r=ok || r=FALHOU
  echo "$(git log --oneline -1) => $r"
done
cd - && git worktree remove --force ../wt-check
```

## 6. Usando o `git bisect` no projeto

Manual:

```bash
git bisect start
git bisect bad                 # o commit atual tem o bug
git bisect good <sha-ou-tag>   # um commit antigo sem o bug
# o git faz o checkout do commit do meio: teste e responda
git bisect good   # ou: git bisect bad   (ou: git bisect skip, se não der para testar)
# ... repetir até ele imprimir "<sha> is the first bad commit"
git bisect reset               # volta para onde estava
```

Automático, com um teste que reproduz o bug (saída 0 = bom; 1–124 e 126–127 = ruim; **125 = pular**):

```bash
git bisect start HEAD <sha-bom>
# o gradlew fica em AndroidEventScheduler/, não na raiz
git bisect run sh -c "cd AndroidEventScheduler && ./gradlew testDebugUnitTest --tests '*NomeDoTesteQueReproduz*' -q"
git bisect reset
```

Quando o teste que reproduz o bug ainda não existe nos commits antigos, use um script que separa "não compila" (pular) de "teste falhou" (ruim):

```bash
#!/usr/bin/env bash
# bisect.sh: 125 = commit não testável (o bisect pula); 1 = ruim; 0 = bom
cd AndroidEventScheduler || exit 125
./gradlew :app:compileDebugUnitTestKotlin -q >/dev/null 2>&1 || exit 125
./gradlew testDebugUnitTest --tests '*NomeDoTesteQueReproduz*' -q >/dev/null 2>&1 || exit 1
exit 0
```

```bash
cp bisect.sh /tmp/bisect.sh && chmod +x /tmp/bisect.sh   # fora do repo: o checkout de cada passo não o altera
git bisect run /tmp/bisect.sh                            # o run executa na raiz do repositório
```

O `exit 125` reduz o estrago de um commit que não compila, mas não o elimina: o bisect só consegue apontar um intervalo de suspeitos. Daí a regra da seção 1.

**Cuidados no projeto:**

- Rode o bisect numa **worktree** (`git worktree add --detach ../wt-bisect <sha-ruim>`) para não mexer na branch em que você trabalha. Copie o `local.properties`.
- Os commits de merge da `main` (`Merge pull request #N`) também entram na busca. Para olhar só a linha principal, use `git bisect start --first-parent`. Ele aponta o PR culpado, e depois dá para bisectar dentro dele.
- Testes que dependem de `LocalDate.now()` podem oscilar perto da meia-noite. Prefira reproduzir o bug com `FakeClock`.
- Se o bug só aparece em aparelho, o "teste" do bisect pode ser manual (instalar o APK de cada passo com `./gradlew :app:installDebug`). Cada passo fica mais caro, e a regra de compilar faz ainda mais diferença.

## 7. Fontes

- [`git bisect` — documentação oficial](https://git-scm.com/docs/git-bisect) (códigos de saída do `run`, `skip`, `--first-parent`)
- [`git rebase --exec` — documentação oficial](https://git-scm.com/docs/git-rebase#Documentation/git-rebase.txt---execltcmdgt)
