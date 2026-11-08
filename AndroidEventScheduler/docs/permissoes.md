# Permissões do RememberMe

> Documento obrigatório: **toda** permissão que o APK solicita — declarada por nós ou injetada por uma biblioteca no merge do manifest — está listada aqui com a justificativa e o comportamento quando ela não é concedida.
> Uma permissão sem boa justificativa é removida, desde que a remoção não quebre o app.
> A regra está no steering (`.kiro/steering/arquitetura.md` §5.6). O teste `RequestedPermissionsGuardRobolectricTest` falha se a lista do APK mudar sem a lista desse teste ser atualizada.

**Estado:** permissões introduzidas pela spec 04 (PR #8) e planejadas para as specs 14 a 17. Revisado em 2026-11-08.

## 1. Resumo

| Permissão | Tipo | Android | Introduzida por | Por que o app precisa | Se for negada ou não existir |
|---|---|---|---|---|---|
| `POST_NOTIFICATIONS` | runtime (pedida ao usuário) | 13+ | spec 04 | Mostrar os lembretes e alertas de eventos | Os alarmes continuam agendados, mas nenhuma notificação aparece; nada quebra |
| `USE_EXACT_ALARM` | normal, **restrita pelo Google Play** | 13+ | spec 04 | Disparar o lembrete no minuto configurado, mesmo com o aparelho em repouso (Doze) | Ver §2.2. Sem ela, no Android 14+ os alarmes seriam inexatos e poderiam atrasar |
| `SCHEDULE_EXACT_ALARM` (`maxSdkVersion=32`) | acesso especial | 12 e 12L | spec 04 | Igual à anterior, nas versões em que `USE_EXACT_ALARM` não existe | O app cai para alarme inexato (`setAndAllowWhileIdle`) |
| `USE_FULL_SCREEN_INTENT` | normal no 13−; acesso especial no 14+, **restrita pelo Google Play** | 10+ | spec 04 (usada na spec 14) | Mostrar o alerta de **evento** em tela cheia, como um despertador | O app mostra uma notificação de alta prioridade em vez da tela cheia |
| `RECEIVE_BOOT_COMPLETED` | normal (concedida na instalação) | todas | spec 15 | Reagendar os alarmes após reiniciar o aparelho: o `AlarmManager` perde todos ao desligar | Ver §2.6. Sem ela, os alarmes só voltam quando o usuário abrir o app e salvar cada item de novo |
| `<pacote>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` | signature (só o próprio app) | — | `androidx.core` (automática) | Usada pela `ContextCompat.registerReceiver` para proteger receivers dinâmicos não exportados em Android < 13 | Não se aplica: não aparece para o usuário e não dá acesso a nada fora do app |

O app **não** declara `INTERNET`: é 100% offline, e nenhuma biblioteca atual a injeta. Também não declara permissões de armazenamento, localização, contatos, câmera ou microfone.

## 2. Detalhe por permissão

### 2.1 `POST_NOTIFICATIONS`
- **Para quê:** os lembretes (`NOTIFICATION_ONLY`) e os alertas de evento são notificações. Sem elas o app perde a função principal.
- **Como é pedida:** pelo `NotificationPermissionEffect` (`core.permissions`), no máximo uma vez por sessão e sem bloquear a interface. A Home faz o pedido na primeira abertura (spec 05).
- **Se negada:** o `ScheduleNotifier` verifica a permissão antes de notificar e não lança exceção. O alarme continua sendo agendado e reagendado.
- **Google Play:** sem restrição.

### 2.2 `USE_EXACT_ALARM`: por que é usada se o Google Play a restringe?

**Resposta curta:** porque é a permissão que a documentação oficial do Android indica para apps de calendário e lembretes, e o RememberMe é exatamente isso. A restrição do Play não é uma proibição. Ela limita o uso a apps cuja função principal precisa de alarmes exatos, e esse é o nosso caso. O custo é preencher uma declaração na Play Console e passar pela revisão.

**Contexto técnico:**
- Desde o Android 14, a `SCHEDULE_EXACT_ALARM` **não é mais concedida automaticamente** a apps recém-instalados que tenham como alvo o Android 13 ou superior: ela começa negada e o usuário precisa liberá-la manualmente nas configurações ([Android 14 — alarmes exatos negados por padrão](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)).
- A mesma página orienta que **apps de calendário ou de despertador** declarem `USE_EXACT_ALARM`, que é concedida na instalação. Ela avisa também que só é possível publicar com essa permissão se o app se enquadrar na política do Google Play.
- Sem alarme exato, o sistema pode adiar o disparo em repouso (Doze): o lembrete "15 minutos antes" pode chegar depois do horário do compromisso. Isso compromete a função central do app.

**Alternativas avaliadas:**

| Opção | Como funciona | Prós | Contras |
|---|---|---|---|
| **A. `USE_EXACT_ALARM` (escolhida)** | Concedida na instalação no Android 13+ | Lembretes no horário sem nenhum passo do usuário; uso previsto pela documentação para calendários | Declaração na Play Console; risco de o revisor discordar do enquadramento |
| B. Só `SCHEDULE_EXACT_ALARM` | O usuário libera "Alarmes e lembretes" nas configurações (`ACTION_REQUEST_SCHEDULE_EXACT_ALARM`) | Sem restrição de política | No Android 14+ começa **negada**: todo usuário novo teria alarmes atrasados até achar e ligar a opção. Exige tela e fluxo novos |
| C. Só alarmes inexatos | `setAndAllowWhileIdle`/`setWindow` | Zero permissões de alarme | Atrasos de minutos em repouso; inaceitável para um lembrete com hora marcada |

**Por que não removemos:** a regra do projeto remove uma permissão quando ela não tem boa justificativa e a remoção não quebra o app. Aqui a justificativa existe e é a oficial, e removê-la degradaria a função principal (opção C) no Android 14+.

**Plano B (se o Google Play recusar a declaração):**
1. Remover `USE_EXACT_ALARM` e retirar o `maxSdkVersion` da `SCHEDULE_EXACT_ALARM`.
2. Criar, em Configurações, o item "Lembretes no horário exato", que abre `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` quando `canScheduleExactAlarms()` for falso.
3. A spec 15 já reagenda tudo ao receber `ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`, então os alarmes viram exatos assim que o usuário liberar.
4. Nada mais muda: o `AndroidAlarmScheduler` já consulta `canScheduleExactAlarms()` e cai para alarme inexato quando não pode.

**Declaração na Play Console:** em "Conteúdo do app → Permissões de alarmes exatos", informar que o app é de calendário e lembretes e que a função principal é avisar o usuário no horário que ele escolheu.

### 2.3 `SCHEDULE_EXACT_ALARM` (`maxSdkVersion="32"`)
- **Para quê:** no Android 12 e 12L, alarmes exatos exigem esta permissão (`setExactAndAllowWhileIdle` lança `SecurityException` sem ela), e `USE_EXACT_ALARM` só existe a partir do Android 13.
- **Por que `maxSdkVersion=32`:** no Android 13+ quem cobre o caso é a `USE_EXACT_ALARM`. Limitar a versão evita pedir as duas.
- **Se revogada pelo usuário:** `canScheduleExactAlarms()` retorna `false` e o app usa alarme inexato, sem crash.
- **Google Play:** sem restrição específica, mas a política pede que alarmes exatos sejam usados só para ações intencionais do usuário, que é o nosso caso.

### 2.4 `USE_FULL_SCREEN_INTENT`
- **Para quê:** o alerta de **evento** abre uma tela cheia por cima do bloqueio, com "Adiar" e "Dispensar" (spec 14, `FullScreenAlarmActivity`). O usuário pode desligar isso em Configurações (`fullScreenEventAlertEnabled`, spec 03).
- **Como a tela cheia é aberta (uso real, spec 14):** o `ScheduleNotifier` publica a notificação com `setFullScreenIntent(...)`, e o sistema inicia a `FullScreenAlarmActivity` (`exported="false"`, sem `<data>`) com a ação `SHOW_FULL_SCREEN_ALARM` e o extra `scheduleId`. Só esta permissão autoriza o *full-screen intent*; a atividade em si não precisa de mais nada.
- **Sobre o bloqueio e a tela apagada: sem permissão.** A atividade usa `setShowWhenLocked(true)` e `setTurnScreenOn(true)` (API 27+; atributos `showWhenLocked`/`turnScreenOn` no manifest) e, no Android 7.0 a 8.0 (API 24–26), os flags de janela equivalentes `FLAG_SHOW_WHEN_LOCKED` e `FLAG_TURN_SCREEN_ON`. Manter a tela acesa enquanto o alerta está visível usa `FLAG_KEEP_SCREEN_ON`. Nenhum deles exige permissão, por isso **não** foram adicionadas `WAKE_LOCK` nem `DISABLE_KEYGUARD` (e `SYSTEM_ALERT_WINDOW` continua descartada, ver §4). A atividade não pede para dispensar o bloqueio: um aparelho com PIN, padrão ou senha continua protegido.
- **Android 14+:** a permissão passou a ser um acesso especial. A Play Store revoga a permissão de apps que não têm funções de chamada ou de alarme ([limites de intents em tela cheia — AOSP](https://source.android.com/docs/core/permissions/fsi-limits)).
- **Se negada:** o `ScheduleNotifier` consulta `NotificationManager.canUseFullScreenIntent()` (API 34+) e usa uma notificação de alta prioridade, sem o `fullScreenIntent`. O evento continua sendo avisado, com toque para abrir o detalhe. O mesmo acontece se a atividade não estiver instalada (testado) ou se o usuário desligar a opção nas Configurações. Em todos esses casos a tela cheia simplesmente não aparece e nada quebra.
- **Notificação ao fechar:** a tela cancela a notificação do item com `NotificationManagerCompat.cancel` ao dispensar ou adiar (nenhuma permissão envolvida); o botão voltar fecha a tela e mantém a notificação.
- **Google Play:** exige declaração de uso de intent em tela cheia. A justificativa é o alerta de evento no estilo despertador. Se a declaração for recusada, o app continua correto: só perde a tela cheia no Android 14+.

### 2.5 `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` (automática)
- **Origem:** manifest da `androidx.core` (`core-1.19.1`), mesclado no build.
- **Para quê:** a `ContextCompat.registerReceiver(..., RECEIVER_NOT_EXPORTED)` usa essa permissão própria do app para proteger receivers registrados em tempo de execução nas versões do Android anteriores ao 13.
- **Por que mantemos:** tem nível *signature*, então só o próprio app a detém. Não aparece para o usuário e não dá acesso a dados. Removê-la com `tools:node="remove"` poderia quebrar bibliotecas que dependem do registro seguro.

### 2.6 `RECEIVE_BOOT_COMPLETED`
- **Para quê:** o `AlarmManager` perde todos os alarmes quando o aparelho reinicia. O `BootCompletedReceiver` (`core.alarm.data`) recebe `BOOT_COMPLETED` e chama o `RescheduleAllAlarmsUseCase`, que reagenda o próximo alarme de cada item. O mesmo receiver trata `MY_PACKAGE_REPLACED`, `TIMEZONE_CHANGED`, `TIME_SET` e `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`; só o `BOOT_COMPLETED` depende da permissão.
- **Como é pedida:** é uma permissão normal, concedida na instalação; não há diálogo nem tela.
- **Se ausente:** o receiver não é acionado no boot e os lembretes ficam sem disparar até o usuário abrir o app e salvar cada item de novo. Isso compromete a função principal, por isso ela fica.
- **Limites deliberados:** o receiver **não** trata `LOCKED_BOOT_COMPLETED` e **não** é `directBootAware`, porque o Room fica em armazenamento protegido por credencial e só abre depois do primeiro desbloqueio. Alertas que venceram com o aparelho desligado não são disparados depois: só as próximas ocorrências são reagendadas. Um alerta adiado na tela cheia (spec 14) também se perde no reboot: o adiamento não é salvo no item, então volta a valer a próxima ocorrência normal. O receiver não abre tela nem notificação.
- **`exported`:** o receiver usa `android:exported="false"`. A [documentação do `<receiver>`](https://developer.android.com/guide/topics/manifest/receiver-element) define `exported` como a capacidade de receber mensagens de fontes **não pertencentes ao sistema**; com `false`, o receiver continua recebendo as mensagens enviadas pelo sistema. Assim, outros apps não conseguem acioná-lo. Não verificado em aparelho (ver PR da spec 15).
- **Google Play:** sem restrição de política (permissão normal).

### 2.7 Importar e exportar a agenda (spec 17): nenhuma permissão nova

- **Permissões:** a spec 17 **não adiciona permissão**. `READ/WRITE_EXTERNAL_STORAGE` e `READ_MEDIA_*` não são declaradas nem entram por biblioteca; o `RequestedPermissionsGuardRobolectricTest` continua com a mesma lista.
- **Como o arquivo sai e entra sem armazenamento:** *Salvar em arquivo* usa o seletor do sistema (SAF, `ACTION_CREATE_DOCUMENT`) e *Escolher arquivo* usa `ACTION_OPEN_DOCUMENT`: o usuário escolhe o local e o sistema entrega ao app uma URI com acesso apenas àquele documento.
- **`FileProvider` (`androidx.core.content.FileProvider`, authority `<applicationId>.fileprovider`):** entrega o arquivo temporário ao share sheet (e-mail, Drive, WhatsApp...). Configuração mínima, de propósito:
  - `android:exported="false"`: nenhum app consegue abrir a URI por conta própria;
  - `android:grantUriPermissions="true"`: o app concede **leitura por URI** (`FLAG_GRANT_READ_URI_PERMISSION`) apenas ao destinatário escolhido, e a concessão vale só para aquele arquivo;
  - `res/xml/file_paths.xml` expõe **somente** `cacheDir/exports/` (`<cache-path path="exports/">`); o banco, o DataStore e o resto do `filesDir`/`cacheDir` ficam inalcançáveis. Os arquivos anteriores são apagados a cada nova exportação.
- **O que o usuário vê:** nada além do seletor e do share sheet do sistema; não há diálogo de permissão.
- **Google Play:** sem restrição de política (nenhuma permissão sensível; o `FileProvider` é o mecanismo recomendado para compartilhar arquivos).

## 3. Permissões planejadas pelas próximas specs

| Permissão | Spec | Justificativa | Observação |
|---|---|---|---|
| `USE_BIOMETRIC` | 16 | O `BiometricPrompt` do Android 11+ exige essa permissão mesmo quando só a credencial do aparelho (PIN, padrão ou senha) é aceita | Injetada pela `androidx.biometric`; não aparece para o usuário |
| `USE_FINGERPRINT` | 16 | **Nenhuma**: o app não usa digital, e no Android ≤ 10 usa o `KeyguardManager` | Injetada pela `androidx.biometric`. **Remover** com `tools:node="remove"` e cobrir na trava |
| — | 14 | **Implementada, sem permissão nova:** a tela cheia usa `setShowWhenLocked`/`setTurnScreenOn` e `FLAG_KEEP_SCREEN_ON`, que **não** exigem permissão (ver §2.4) | `WAKE_LOCK` e `DISABLE_KEYGUARD` **não** foram adicionadas |
| — | 17 | **Implementada, sem permissão nova:** exportar e importar usam o seletor do sistema (SAF) e o `FileProvider` (ver §2.7) | `READ/WRITE_EXTERNAL_STORAGE` **não** foram adicionadas |

Cada uma dessas specs deve atualizar este documento e a lista do `RequestedPermissionsGuardRobolectricTest` no mesmo PR.

## 4. Avaliadas e não usadas

| Permissão | Por que não |
|---|---|
| `INTERNET` / `ACCESS_NETWORK_STATE` | O app é 100% offline; nada é enviado a servidores |
| `READ/WRITE_EXTERNAL_STORAGE`, `READ_MEDIA_*` | O SAF e o `FileProvider` (spec 17) dispensam essas permissões |
| `WAKE_LOCK` | O `AlarmManager` acorda o aparelho para o receiver; `goAsync()` cobre o trabalho curto |
| `FOREGROUND_SERVICE` | Não há trabalho contínuo em segundo plano |
| `SYSTEM_ALERT_WINDOW` | A tela cheia usa *full-screen intent*, não sobreposição |
| `VIBRATE` | A vibração das notificações é controlada pelo canal; não precisa de permissão |
| `READ_CALENDAR` / `WRITE_CALENDAR` | A agenda é própria (Room); não sincronizamos com o calendário do sistema |

## 5. Como adicionar ou remover uma permissão

1. Justificar aqui, na tabela do §1 e com uma seção própria: para que serve, como é pedida, o que acontece se for negada e a política do Google Play.
2. Comentar a permissão no `AndroidManifest.xml` com uma linha de justificativa.
3. Atualizar a lista esperada no `RequestedPermissionsGuardRobolectricTest`.
4. Se a permissão vier de uma biblioteca e não for necessária, removê-la com `<uses-permission android:name="..." tools:node="remove" />` e registrar aqui.
5. Descrever a mudança no PR, na seção "Permissões".

## 6. Fontes

- [Elemento `<receiver>` do manifest (`android:exported`, `android:directBootAware`)](https://developer.android.com/guide/topics/manifest/receiver-element)
- [Android 14: alarmes exatos negados por padrão (`SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`)](https://developer.android.com/about/versions/14/changes/schedule-exact-alarms)
- [Mudanças de comportamento do Android 14 para todos os apps](https://developer.android.com/about/versions/14/behavior-changes-all)
- [Limites de intents em tela cheia (AOSP)](https://source.android.com/docs/core/permissions/fsi-limits)
- [Requisitos de serviço em primeiro plano e de intent em tela cheia (Ajuda do Play Console)](https://support.google.com/googleplay/android-developer/answer/13392821)
- [Política do Google Play para a API de alarme exato](https://support.google.com/googleplay/android-developer/answer/12253906#exact_alarm_preview)

*O conteúdo das fontes foi parafraseado para o contexto do projeto.*
