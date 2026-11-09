# Persistência do tema: Preferences DataStore ou SharedPreferences?

> Decisão para a spec 19 (tema claro e escuro persistente). **Recomendação: Preferences DataStore**, no arquivo único que o app já usa (`remember_me_prefs`), numa chave nova `theme_mode`. A leitura síncrona do `SharedPreferences`, única vantagem dele para este caso, é resolvida de outra forma (seção 4).

## 1. O que precisa ser guardado

Um único valor: `system`, `light` ou `dark`. Ele é escrito raramente (quando o usuário troca o tema), lido em toda partida, observado enquanto o app está aberto (a troca precisa refletir na hora) e precisa sobreviver à morte do processo e à atualização do app.

É chave e valor, sem relação nem consulta. Banco relacional (Room) e esquema tipado (Proto DataStore) ficam fora de cogitação já de saída (seção 3). A dúvida real é entre as duas opções de chave e valor.

## 2. As duas opções

### Preferences DataStore

Biblioteca Jetpack de chave e valor sobre corrotinas e `Flow`. Neste projeto já é a persistência de `UserSettings` (spec 03) e de `HomePreferences` (spec 06), num único `DataStore<Preferences>` injetado pelo Koin (`core/datastore`).

| Vantagens | Desvantagens |
|---|---|
| **Já está no projeto**: mesmo arquivo, mesmo módulo Koin, mesmo padrão de repositório e de teste. Custo de adoção zero | **Sem leitura síncrona**: a primeira leitura é `suspend`/`Flow`. Na partida o app precisa esperar o valor antes de desenhar (resolvido na seção 4) |
| Leitura e escrita fora da main thread por construção. Não há como travar a UI por engano | Mais código de infraestrutura que um `getString`: `Flow`, `catch`, `map` (já existe no projeto, então o custo aqui é nulo) |
| `data: Flow<Preferences>` **observável**: a tela recebe a troca de tema na hora, sem listener manual | Um arquivo por instância; duas instâncias no mesmo arquivo dão erro (o projeto já trata com o `single` do Koin; nos testes, um escopo por vez) |
| Escrita **transacional** (`edit {}` atômico), com erro propagado como exceção. O valor só é visto como gravado depois de ir para o disco | Não é multiprocesso no modo padrão (o app não precisa) |
| Corrupção tratada: o projeto usa `ReplaceFileCorruptionHandler`, que volta aos padrões em vez de quebrar | |
| É a recomendação atual da documentação Android para dados pequenos | |
| Testável na JVM com arquivo temporário (padrão já usado nas specs 03 e 06) | |

### SharedPreferences

API do framework, presente desde a API 1.

| Vantagens | Desvantagens |
|---|---|
| **Leitura síncrona**: `getString("theme_mode", …)` no `onCreate` devolve o valor antes do primeiro quadro, sem Splash nem estado de "carregando" | A leitura síncrona **é I/O na main thread** na primeira vez (carrega o XML do disco); viola o StrictMode e pode causar jank na partida, justamente o momento crítico |
| Simples de ler e escrever; nenhum `Flow` ou corrotina | `apply()` não informa erro; `commit()` só devolve `boolean`. Segundo a documentação, o valor aparece em memória **antes** de ir para o disco, então dá para perder a escolha se o processo morrer logo depois |
| Sem dependência (faz parte do Android) | `apply()` pendentes **bloqueiam a main thread** em transições de ciclo de vida (início e parada de Activity/Service), fonte conhecida de ANR |
| | Sem observação reativa: precisa de `OnSharedPreferenceChangeListener`, com referência fraca (armadilha conhecida) e ciclo de vida manual |
| | **Segundo mecanismo de persistência** no projeto: outro arquivo, outro padrão de teste, outra regra no steering, para guardar um único valor |
| | A própria documentação Android **recomenda não usar** em código novo e aponta o DataStore |

## 3. Opções descartadas

- **Proto DataStore:** tipado e com esquema (protobuf), ótimo para objetos estruturados. Para um valor só, exige plugin de protobuf, um `.proto` e um `Serializer`, sem ganho sobre a chave de string.
- **Room:** banco relacional para os agendamentos. Uma tabela de uma linha para o tema seria abuso: migração de esquema e acesso assíncrono, sem nenhuma consulta.
- **`UiModeManager.setApplicationNightMode` (API 31+):** não é um armazenamento do app, é o sistema guardando o modo noturno **da app**. É interessante como **complemento**, porque faz a SplashScreen e o qualificador `-night` seguirem a escolha desde a partida. Mas:
  - causa mudança de configuração a cada troca;
  - só existe a partir da API 31 (o `minSdk` do projeto é 24);
  - não achamos na documentação um valor que signifique "seguir o sistema".

  Ficou como melhoria futura no `design.md` da spec 19.

## 4. O ponto fraco do DataStore: a partida

O risco real de guardar o tema de forma assíncrona é **piscar o tema errado**: o app desenha no escuro, o valor `light` chega 30 ms depois e a tela troca. A spec 19 resolve sem I/O na main thread, assim:

1. A `MainActivity` já usa a SplashScreen do sistema (`installSplashScreen`, spec 02). Ela passa a segurar a Splash até o tema ser lido: `setKeepOnScreenCondition { themeMode.value == null }`.
2. O `Flow` do repositório vira um `StateFlow<ThemeMode?>` com `stateIn(…, Eagerly, null)` desde a criação da Activity. A leitura começa antes do `setContent`.
3. O primeiro quadro do Compose já sai com o tema certo. A Splash cobre os poucos milissegundos da primeira leitura do processo. Depois dela, o DataStore serve da memória.

Com isso, a vantagem do `SharedPreferences` deixa de existir para este caso. As desvantagens dele continuam.

## 5. Opinião

**Usar o Preferences DataStore que o projeto já tem**, por quatro motivos, em ordem de peso:

1. **Consistência:** o projeto tem uma regra de DataStore único e dois repositórios que já o usam. Um `ThemePreferenceRepository` no mesmo arquivo segue o padrão da spec 06 sem criar nada novo. Um `SharedPreferences` só para o tema seria uma exceção sem motivo forte, e exceções viram dívida.
2. **Reatividade de graça:** o tema precisa mudar na hora em todas as telas abertas. O `Flow` do DataStore entrega isso; com `SharedPreferences`, seria um listener manual.
3. **Segurança da main thread e durabilidade:** sem StrictMode, sem ANR de `apply()` e sem perder a escolha se o processo morrer logo após a troca.
4. **Direção da plataforma:** a documentação Android desaconselha `SharedPreferences` em código novo.

`SharedPreferences` faria sentido num app **sem** DataStore e **sem** SplashScreen, em que a leitura síncrona na partida fosse o único jeito simples de evitar o flash. Mesmo assim, eu recomendaria introduzir o DataStore e a Splash em vez de depender de I/O síncrono. Não é o caso deste projeto.

**Detalhes que a spec 19 adota:**
- A chave tem valores **estáveis** (`system`/`light`/`dark`), nunca o `name` do enum, porque renomear quebraria o que já está salvo.
- O repositório é próprio, e não um campo novo em `UserSettings`. O `UserSettingsRepository.save()` regrava o formulário inteiro, e o tema precisa ser gravado sozinho, na hora.

## 6. Fontes

- [Referência de `SharedPreferences`](https://developer.android.com/reference/android/content/SharedPreferences): a equipe Android recomenda DataStore para dados pequenos em código novo e lista as desvantagens (bloqueio da UI e ANR em `apply()` pendente, erro não sinalizado, valor em memória antes de persistir, corrupção silenciosa).
- [Prefer Storing Data with Jetpack DataStore (Android Developers Blog)](https://android-developers.googleblog.com/2020/09/prefer-storing-data-with-jetpack.html): motivação do DataStore e comparação com `SharedPreferences`.
- [Codelab: Working with Preferences DataStore](https://developer.android.com/codelabs/android-preferences-datastore): Preferences DataStore × Proto DataStore.
- [Referência de `DataStore`](https://developer.android.com/reference/kotlin/androidx/datastore/core/DataStore).
- [Referência de `UiModeManager.setApplicationNightMode`](https://developer.android.com/reference/android/app/UiModeManager#setApplicationNightMode(int)): API 31+; o modo é persistido para a app e a troca gera mudança de configuração.

O conteúdo das fontes foi parafraseado.
