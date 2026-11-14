# Dependabot: dependências em dia sem build que muda com a data

> Regra resumida no steering: [`fluxo-de-trabalho.md` §4](../../.kiro/steering/fluxo-de-trabalho.md). Configuração: [`.github/dependabot.yml`](../../../.github/dependabot.yml), na raiz do repositório. Origem: spec 21 (`.kiro/specs/21-higiene-avisos-e-dependencias/`, R2.4), PR #39.

Este documento explica o problema que o Dependabot resolve no projeto, como ele está configurado e como encaixa no resto do processo. O processo inclui a CI e as travas de aviso, além do que fazer quando chega um PR dele.

## 1. O problema

### 1.1 Dependência parada envelhece em silêncio

Até a spec 21, as versões do `gradle/libs.versions.toml` e do Gradle wrapper só mudavam quando alguém lembrava de mudar. Em 2026-11-13, o Lint apontava sete atualizações pendentes:

| Item | Em uso | Disponível |
|---|---|---|
| Gradle (wrapper) | 9.6.0 | 9.9.1 |
| Compose BOM | 2026.02.01 | 2026.09.00 |
| Kotlin (plugins `compose`/`serialization`) | 2.2.10 | 2.4.21 |
| kotlinx-serialization-json | 1.9.0 | 1.13.0 |
| MockK | 1.14.11 | 1.14.12 |
| Robolectric | 4.17 | 4.18 |

Quanto mais tempo sem atualizar, maior e mais arriscado fica o salto. A atualização da spec 21 trouxe de uma vez:

- uma depreciação do Compose que tocou **41 arquivos de teste**: `createComposeRule` e as variantes passaram para `junit4.v2`;
- um aviso novo do Kotlin 2.4 no código de produção;
- a dúvida se KSP, Room e detekt 2.0 alpha aceitariam o Kotlin novo.

Atualizações pequenas e frequentes isolam cada problema num PR pequeno.

### 1.2 Por que não deixar o Lint reprovar o build

O Android Lint já sabe que existe versão nova (`NewerVersionAvailable`, `GradleDependency`, `AndroidGradlePluginVersion`). Transformar esses avisos em erro parece a solução óbvia, mas quebra uma propriedade básica do build: **o resultado depende só do código**.

Essas regras consultam a rede (Maven Central, Google Maven, `services.gradle.org`). O mesmo commit passaria hoje e falharia amanhã, sem ninguém ter mexido em nada, só porque alguém publicou uma versão nova. Isso:

- reprova PRs que não têm nada a ver com dependências;
- torna a CI irreproduzível (rodar de novo um commit antigo dá outro resultado);
- quebra o `git bisect` (`docs/decisoes/commits-compilaveis-e-git-bisect.md`), porque o "ruim" passa a ser a data, não o commit.

Por isso, na spec 21, o Lint passou a reprovar **qualquer** aviso (`warningsAsErrors = true`), **exceto** essas três regras, que foram desligadas no bloco `lint` de `app/build.gradle.kts`. A pergunta "tem versão nova?" saiu do build e virou processo, e esse processo é o Dependabot.

## 2. O que o Dependabot faz

O Dependabot é um serviço do GitHub. Quando o arquivo `.github/dependabot.yml` existe na branch padrão, ele roda no agendamento configurado, procura versões novas das dependências declaradas e **abre um PR** com a mudança de versão. Ele não faz merge sozinho e não corrige código.

Para o Gradle, ele edita o `gradle/libs.versions.toml` **sem rodar o Gradle**. Para o wrapper, ele roda o Gradle e atualiza `gradle-wrapper.properties`, `gradlew`, `gradlew.bat` e `gradle-wrapper.jar` ([ecossistemas suportados](https://docs.github.com/en/code-security/reference/supply-chain-security/supported-ecosystems-and-repositories)). Daí a consequência central: **o Dependabot não sabe se a versão nova compila.** Quem descobre isso é a CI do PR (seção 4).

## 3. Como está configurado

```yaml
version: 2
updates:
  - package-ecosystem: gradle
    directory: /AndroidEventScheduler
    schedule: { interval: weekly, day: monday, time: "07:00", timezone: America/Sao_Paulo }
    commit-message: { prefix: "chore(deps)" }
    groups:
      kotlin:   { patterns: ["org.jetbrains.kotlin*", "com.google.devtools.ksp*"] }
      androidx: { patterns: ["androidx.*"] }
      test:     { patterns: ["io.mockk*", "org.robolectric*", "junit*"] }
  - package-ecosystem: github-actions
    directory: /
    schedule: { interval: weekly, day: monday, time: "07:00", timezone: America/Sao_Paulo }
    commit-message: { prefix: "ci(deps)" }
    groups:
      actions: { patterns: ["*"] }
```

(Forma compacta. O arquivo real usa a forma expandida, com comentários.)

### 3.1 Escolha por escolha

| Opção | Valor | Por quê |
|---|---|---|
| `package-ecosystem: gradle` | `directory: /AndroidEventScheduler` | O repositório git tem o projeto Gradle numa subpasta (`AndroidEventScheduler/`); o `directory` aponta para onde ficam `gradle/libs.versions.toml` e o wrapper. O `build-logic` lê o **mesmo** catálogo (`build-logic/settings.gradle.kts`), então uma atualização vale para os dois. |
| `package-ecosystem: github-actions` | `directory: /` | As Actions (`actions/checkout`, `actions/setup-java`, `gradle/actions/*`, `actions/upload-artifact`) ficam em `.github/workflows/`, na raiz. Action velha também envelhece: muda o runtime do Node, as APIs são deprecadas. |
| `schedule` | semanal, segunda 07:00 (São Paulo) | Semanal agrupa o que saiu na semana num lote pequeno; diário geraria ruído num projeto de uma pessoa. Sem `time`, o GitHub sorteia o horário, e o `timezone` sozinho não teria efeito. Às 07:00 de segunda, os PRs estão prontos no início da semana. |
| `commit-message.prefix` | `chore(deps)` / `ci(deps)` | Mantém os conventional commits do projeto (`fluxo-de-trabalho.md` §1). O GitHub acrescenta os dois-pontos depois do prefixo. |
| `groups.kotlin` | `org.jetbrains.kotlin*`, `com.google.devtools.ksp*` | Kotlin, os plugins `compose`/`serialization`, kotlinx-* (serialization, coroutines) e KSP **precisam subir juntos**. Na spec 21, a kotlinx-serialization 1.13 só funcionou com o compilador 2.4, e o KSP tem de aceitar o Kotlin do projeto. Separados, cada PR quebraria a CI esperando o outro. O padrão `org.jetbrains.kotlin*` cobre também `org.jetbrains.kotlinx`. |
| `groups.androidx` | `androidx.*` | Compose BOM, Room, Lifecycle, Navigation 3, DataStore, Activity… costumam ser lançados em conjunto e testados entre si. |
| `groups.test` | `io.mockk*`, `org.robolectric*`, `junit*` | Só afetam testes, o menor risco. Um PR único fica fácil de aprovar. |
| `groups.actions` | `*` | Todas as Actions num PR só. |
| `open-pull-requests-limit` | padrão (5) | No máximo cinco PRs de versão abertos ao mesmo tempo. Com os grupos, isso basta. |
| `target-branch` | padrão (branch padrão: `main`) | O PR vai contra a `main`, onde a CI (`ci-validation.yml`) roda em `pull_request`. |

### 3.2 Como os grupos se resolvem

- Se uma dependência casa com mais de um grupo, **fica no primeiro grupo que casa**, na ordem do arquivo. Por isso, `org.jetbrains.kotlinx:kotlinx-coroutines-test` vai para `kotlin`, e não para `test`. Isso é intencional, porque a coroutines também acompanha o Kotlin.
- Dependência que **não casa com nenhum grupo** ganha um **PR individual**. Hoje isso vale para AGP (`com.android.application`), Koin, detekt, o plugin do ktlint, `desugar_jdk_libs` e o Gradle wrapper. Para o AGP e o detekt 2.0 alpha, é bom ser assim: são as atualizações de maior risco, e ficam isoladas.
- Os grupos valem para **atualizações de versão** (o padrão de `applies-to`).
- Como o Dependabot edita a entrada de `[versions]`, tudo que usa o mesmo `version.ref` sobe junto. Exemplo: `kotlin` alimenta os dois plugins e a `kotlin-gradle-plugin-api` do `build-logic`.

Fonte: [referência de opções do `dependabot.yml`](https://docs.github.com/en/code-security/dependabot/dependabot-version-updates/configuration-options-for-the-dependabot.yml-file).

## 4. Como ele resolve o problema: o ciclo de um PR

```
segunda 07:00  Dependabot encontra versão nova
      │
      ▼
PR "chore(deps): bump the kotlin group …" contra a main
      │
      ▼
CI (ci-validation.yml): ktlint + detekt │ testes + assembleDebug + lintDebug
      │
      ├── verde ─────────────► revisar o changelog da biblioteca → merge
      │
      └── vermelha ──► a versão nova trouxe algo:
                        • depreciação nova → aviso → allWarningsAsErrors reprova a compilação
                        • aviso novo do Lint → warningsAsErrors reprova o lintDebug
                        • incompatibilidade (KSP/Room/detekt não aceitam) → erro de build
                        │
                        ▼
                     corrigir no próprio PR (ou fechar e registrar o motivo)
```

O Dependabot sozinho só **avisa** que existe versão nova. O que torna o aviso seguro de aceitar são as travas da spec 21. Elas transformam o "isso compila e continua limpo?" numa resposta automática:

- **`allWarningsAsErrors`** (convention plugin `androideventscheduler.code.quality`, produção e testes): uma depreciação trazida pela versão nova aparece na CI do PR do Dependabot, e não semanas depois como mais um `w:` ignorado. Foi o que aconteceu, à mão, com o Compose BOM na spec 21: 41 avisos de uma vez.
- **Lint com `warningsAsErrors`**: idem para recursos, manifest e APIs Android.
- **CI completa em `pull_request`**: a mesma verificação de qualquer PR, inclusive a validação do checksum do Gradle wrapper (`gradle/actions/wrapper-validation`). Isso importa justamente nos PRs que trocam o `gradle-wrapper.jar`.

Juntas, as peças separam as responsabilidades:

| Pergunta | Quem responde | Quando |
|---|---|---|
| Existe versão nova? | Dependabot | Toda segunda, fora do build |
| A versão nova compila, passa nos testes e não traz aviso? | CI do PR (travas da spec 21) | No PR do Dependabot |
| Vale adotar? | Quem revisa o PR | Antes do merge |

O build volta a ser determinístico: o mesmo commit sempre dá o mesmo resultado. Mesmo assim, as dependências não envelhecem.

### 4.1 Segurança da CI nos PRs do Dependabot

Workflows disparados por PR do Dependabot rodam com o `GITHUB_TOKEN` **somente leitura** e **sem os secrets do Actions**: só os "Dependabot secrets" ficam disponíveis ([Dependabot on GitHub Actions](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-on-actions)). O `ci-validation.yml` não usa secret nenhum e já declara `permissions: contents: read`, então roda igual a qualquer PR. Se um dia a CI precisar de secret (ex.: assinatura), ele terá de ser cadastrado também como Dependabot secret.

## 5. O que fazer com um PR do Dependabot

1. **CI verde**: leia o changelog ou as release notes linkadas no corpo do PR, com atenção a mudança de comportamento em runtime, que os testes podem não pegar. Se estiver tudo certo, faça o merge. A regra de sempre vale: CI verde no **último** commit.
2. **CI vermelha por depreciação ou aviso**: corrija **no próprio PR**. Faça um commit na branch do Dependabot ou abra um PR substituto a partir dela, usando o substituto não deprecado. Se não houver substituto, use o helper isolado com `@Suppress("DEPRECATION")` justificado (`arquitetura.md` §5.7; exemplo: `core.testing.pendingOperation()`).
3. **Incompatibilidade real** (ex.: o detekt 2.0 alpha não aceita o Kotlin novo): feche o PR e fique na mais recente compatível. Registre o motivo com um `ignore` no `dependabot.yml` (comentário dizendo o motivo e quando reavaliar) e uma linha no histórico de `ordem-de-execucao.md`.
4. **Nunca** "passe" o PR desligando a trava, rebaixando outra dependência ou desligando regra do detekt, do ktlint ou do Lint.
5. **Wrapper**: o PR troca o `.jar`, o `.properties` e os scripts juntos, e a CI valida o checksum. Não edite esses arquivos à mão no PR.

### 5.1 Comandos úteis (comentário no PR)

| Comando | Efeito |
|---|---|
| `@dependabot rebase` | Rebaseia o PR sobre a `main` atual. |
| `@dependabot recreate` | Recria o PR do zero, descartando edições feitas nele. **Cuidado:** apaga a correção de depreciação que você tiver commitado. |
| `@dependabot ignore this major version` | Fecha o PR e não propõe mais essa versão *major*. Existem variantes para `minor`, `patch` e `this dependency`. |
| `@dependabot ignore <dependência> major version` | O mesmo, dentro de um PR agrupado, para uma dependência do grupo. |
| `@dependabot show <dependência> ignore conditions` | Lista o que está sendo ignorado para aquela dependência. |

Referência: [comandos de comentário do Dependabot](https://docs.github.com/en/code-security/reference/supply-chain-security/dependabot-pull-request-comment-commands). Prefira `ignore` no `dependabot.yml`, com comentário, aos comandos de `ignore` no PR: no arquivo, o motivo fica versionado e visível no review.

## 6. O que ele **não** resolve

- **Versão do ktlint (engine).** A chave `ktlint = "1.8.0"` do catálogo não está ligada a nenhuma coordenada de biblioteca ou plugin: o convention plugin a lê com `findVersion("ktlint")`. Sem coordenada, o Dependabot não sabe o que ela é, então **ela não é atualizada automaticamente**. Confira à mão quando atualizar o plugin `ktlintGradle`.
- **Compatibilidade.** O Dependabot propõe a versão mais nova publicada, não a mais nova compatível. Quem filtra é a CI (seção 4) e, se preciso, o `ignore`.
- **Alertas de segurança do Gradle.** Esta configuração cuida de **atualizações de versão**. Os alertas e as atualizações de segurança do Dependabot para Gradle dependem do grafo de dependências enviado pela *dependency submission API*, que não está configurada aqui ([ecossistemas suportados](https://docs.github.com/en/code-security/reference/supply-chain-security/supported-ecosystems-and-repositories)).
- **Dependências transitivas.** Por padrão, ele só atualiza as declaradas diretamente no catálogo.
- **Comportamento em runtime.** Teste verde não prova que nada mudou no aparelho. Mudanças grandes (AGP, Compose, Room) merecem uma rodada manual no emulador.

## 7. Como validar e alterar a configuração

- **Antes do merge**: o `dependabot.yml` desta spec foi conferido contra o schema da SchemaStore (`https://json.schemastore.org/dependabot-2.0.json`): chaves, ecossistemas, `interval`, `day`, `timezone` e grupos.
- **Depois do merge na `main`**: o GitHub mostra erros de configuração e o histórico de execuções em *Insights → Dependency graph → Dependabot*. Por lá também dá para disparar uma verificação na hora ("Check for updates").
- **Mudou o layout do projeto** (ex.: novo módulo com catálogo próprio, ou o projeto saiu da subpasta)? Ajuste o `directory`. Grupo novo entra **antes** de um grupo mais genérico que o capturaria (regra do "primeiro grupo que casa").
