# Testar a restauração de alarmes pós-boot via `adb`

> Guia manual da **spec 15** (`.kiro/specs/15-restauracao-pos-boot/`). Os testes JVM (Robolectric) cobrem a lógica: `BootCompletedReceiverRobolectricTest`, `RescheduleAllAlarmsUseCaseTest` e `BootRestoreIntegrationTest`. Só um aparelho ou emulador confirma duas coisas:
> 1. que o **sistema entrega** o broadcast ao receiver (`exported="false"`);
> 2. que, depois de reiniciar, os alarmes **voltam ao `AlarmManager`**.

Pacote: `com.br.androideventscheduler` · Receiver: `.core.alarm.data.BootCompletedReceiver` · Tag de log: `BootCompletedReceiver`

## 1. Pré-requisitos

| Item | Por quê |
|---|---|
| Emulador **"Google APIs"** (sem Play Store) ou aparelho | Imagens "Google Play" não aceitam `adb root`, e a simulação por broadcast (§4) depende dele |
| `adb` no `PATH` (`$ANDROID_HOME/platform-tools`) | Todos os comandos abaixo |
| App instalado **e aberto ao menos uma vez** | Apps recém-instalados ficam em *stopped state* e o Android não entrega `BOOT_COMPLETED` a eles |
| Ao menos 2 agendamentos **futuros** com alerta, um deles recorrente | Para ver alarmes antes e depois. Crie pela UI (spec 07); um item no passado sem recorrência deve ficar **sem** alarme |
| Emulador sem uso concorrente | Instalar sobrescreve o app de outra pessoa ou agente que use o mesmo `applicationId` |

```bash
adb devices                                   # confirme o serial; use -s <serial> se houver mais de um
./gradlew :app:installDebug                   # a partir de AndroidEventScheduler/
adb shell monkey -p com.br.androideventscheduler -c android.intent.category.LAUNCHER 1   # abre o app (sai do stopped state)
```

Com mais de um dispositivo conectado, prefixe todos os comandos com `adb -s <serial>`, ou exporte `ANDROID_SERIAL=<serial>`.

## 2. Comandos de apoio

```bash
# Alarmes pendentes do app: compare a contagem antes e depois de cada cenário
adb shell dumpsys alarm | grep -c "com.br.androideventscheduler"
adb shell dumpsys alarm | grep -B1 -A6 "com.br.androideventscheduler"   # detalhes (tipo RTC_WAKEUP, horário, PendingIntent)

# Logs do reagendamento: imprime "Alarms rescheduled by <ação>: RescheduleSummary(scheduled=…, skipped=…, failed=…)"
adb logcat -c                                 # limpa o buffer antes do cenário
adb logcat -s BootCompletedReceiver:I AlarmScheduler:W ScheduleAlarmReceiver:W

# Estado do boot
adb shell getprop sys.boot_completed          # "1" quando o boot terminou
```

**Como ler o `dumpsys alarm`:** cada alarme do app aparece com o pacote, o tipo (`RTC_WAKEUP`), o horário de disparo e o `PendingIntent` do `ScheduleAlarmReceiver`. O formato exato varia entre versões do Android. Por isso o mais confiável é **contar** as linhas do pacote e conferir que o número bate com os itens futuros com alerta.

## 3. Cenário principal: reinício real (recomendado)

É o único teste que reproduz o caso real: o `AlarmManager` perde os alarmes e o sistema envia `BOOT_COMPLETED`.

```bash
adb shell dumpsys alarm | grep -c "com.br.androideventscheduler"   # 1) anote N (antes)
adb reboot                                                        # 2) reinicia
adb wait-for-device
until [ "$(adb shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
```

3. **Desbloqueie a tela** se houver PIN, padrão ou senha. O app usa `BOOT_COMPLETED` (não `LOCKED_BOOT_COMPLETED`) porque o banco Room fica em armazenamento protegido por credencial; o broadcast só chega **depois do primeiro desbloqueio**.

```bash
adb logcat -d -s BootCompletedReceiver:I                          # 4) deve haver "Alarms rescheduled by android.intent.action.BOOT_COMPLETED"
adb shell dumpsys alarm | grep -c "com.br.androideventscheduler"  # 5) deve voltar a N
```

**Esperado:** a contagem depois é igual à de antes; o log mostra `scheduled=N`. Itens passados sem recorrência entram em `skipped`. Itens recorrentes cuja ocorrência passou enquanto o aparelho estava desligado são reagendados para a **próxima** ocorrência, sem disparo retroativo (spec 15 R2.4).

**Variações do reinício:**
- No emulador, `adb emu kill` seguido de iniciar com `emulator -avd <nome> -no-snapshot-load` força um *cold boot*. Um snapshot pode restaurar o estado sem passar por um boot e não envia `BOOT_COMPLETED`.
- No aparelho físico, reiniciar pelo botão liga/desliga tem o mesmo efeito do `adb reboot`.

## 4. Simular o broadcast sem reiniciar (requer `adb root`)

Mais rápido para iterar. Duas limitações:
- `BOOT_COMPLETED` é um **broadcast protegido**: o shell comum (uid 2000) recebe `SecurityException: Permission Denial: not allowed to send broadcast android.intent.action.BOOT_COMPLETED`.
- O receiver tem `exported="false"`, então só o sistema ou o root conseguem entregá-lo.

```bash
adb root                                      # só em imagens sem Play Store; reconecta o adbd como root
adb wait-for-device

# 4.1 Apaga os alarmes do app, simulando a perda do reboot. Atenção: force-stop também deixa o app em "stopped state".
adb shell am force-stop com.br.androideventscheduler
adb shell dumpsys alarm | grep -c "com.br.androideventscheduler"   # deve ser 0

# 4.2 Entrega o BOOT_COMPLETED explicitamente ao receiver, incluindo pacotes parados
adb shell am broadcast \
  -a android.intent.action.BOOT_COMPLETED \
  -n com.br.androideventscheduler/.core.alarm.data.BootCompletedReceiver \
  --include-stopped-packages

adb logcat -d -s BootCompletedReceiver:I
adb shell dumpsys alarm | grep -c "com.br.androideventscheduler"   # deve voltar a N

adb unroot                                    # volta o adbd ao modo normal
```

**Por que `-n` e `--include-stopped-packages`:** depois do `force-stop` o app está parado, e o Android não entrega broadcasts a apps parados sem essa flag. O `-n` dirige o intent ao receiver, que não é exportado, e funciona porque o remetente é root.

**Sem root**, o shell não consegue entregar nenhuma ação a este receiver: o broadcast é recusado ou ignorado. Use o reinício real (§3) ou as variações da §5, que o próprio sistema dispara.

## 5. Variações: outras ações tratadas pelo receiver

| Ação | Como provocar | Observações |
|---|---|---|
| `MY_PACKAGE_REPLACED` | Reinstalar por cima: `./gradlew :app:installDebug` ou `adb install -r app/build/outputs/apk/debug/app-debug.apk` | O próprio sistema envia ao app atualizado; não precisa de root. Alarmes não somem numa atualização, mas o reagendamento garante que reflitam o código novo |
| `TIMEZONE_CHANGED` | Configurações → Sistema → Data e hora → desligar "Definir automaticamente" e trocar o fuso. Por `adb`, com root: `adb shell cmd alarm set-timezone America/Manaus` | Os alarmes são recalculados no fuso novo (o horário local do compromisso é mantido) |
| `TIME_SET` | Configurações → Data e hora → desligar "Hora automática" e mudar a hora. Por `adb`, com root: `adb shell cmd alarm set-time <epoch_ms>` | Se a hora pular para depois de um alarme, o item recorrente vai para a próxima ocorrência |
| `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` | **Só Android 12/12L (API 31–32)**: `adb shell appops set com.br.androideventscheduler SCHEDULE_EXACT_ALARM deny`, depois `... allow`; ou a tela "Alarmes e lembretes" | O sistema envia a ação quando a permissão é **concedida**. No Android 13+ o app usa `USE_EXACT_ALARM` (concedida na instalação), então este cenário não se aplica |

Para cada variação: `adb logcat -c`, provoque a ação e confira o log (`Alarms rescheduled by <ação>`) e a contagem no `dumpsys alarm`.

Os subcomandos `cmd alarm set-time`/`set-timezone` existem no shell do `AlarmManagerService` das versões recentes do Android, mas exigem privilégio de sistema. Confira com `adb shell cmd alarm help`; se não aparecerem na sua versão, use a tela de configurações.

## 6. Casos negativos (o que **não** deve acontecer)

| Cenário | Esperado |
|---|---|
| Ação desconhecida (com root: `adb shell am broadcast -a com.exemplo.QUALQUER -n com.br.androideventscheduler/.core.alarm.data.BootCompletedReceiver`) | Nenhum log `Alarms rescheduled`; contagem inalterada |
| Banco vazio (sem agendamentos) | Log com `scheduled=0, skipped=0, failed=0`, sem crash |
| Item passado sem recorrência | Contado em `skipped`; nenhum alarme para ele |
| Notificações negadas (`adb shell pm revoke com.br.androideventscheduler android.permission.POST_NOTIFICATIONS`) | O reagendamento acontece igual; só a notificação deixa de aparecer no disparo |
| Bloqueio de tela ativo após `adb reboot` sem desbloquear | **Nenhum** reagendamento até o desbloqueio (comportamento intencional, sem `directBootAware`) |

## 7. Doze / repouso (complementar)

Confirma que os alarmes reagendados disparam com o aparelho ocioso, porque são exatos (`setExactAndAllowWhileIdle`):

```bash
adb shell dumpsys battery unplug
adb shell dumpsys deviceidle force-idle       # entra em Doze
# aguarde o horário de um alarme próximo (crie um item para daqui a ~8 min com alerta "5 minutos antes": o alarme toca em ~3 min)
adb shell dumpsys deviceidle unforce
adb shell dumpsys battery reset
```

## 8. Checklist para o PR

- [ ] Reinício real (§3): contagem antes = depois; log `BOOT_COMPLETED` com `scheduled` correto.
- [ ] Simulação com root (§4), se a imagem permitir.
- [ ] `MY_PACKAGE_REPLACED` (§5).
- [ ] Ao menos uma entre `TIMEZONE_CHANGED` e `TIME_SET` (§5).
- [ ] Casos negativos da §6: item passado e banco vazio.
- [ ] Anotar no PR: dispositivo/emulador, versão do Android, imagem (Google APIs/Play) e o que **não** foi executado.

## Referências

- [Elemento `<receiver>` e o atributo `exported`](https://developer.android.com/guide/topics/manifest/receiver-element)
- [`dumpsys`](https://developer.android.com/tools/dumpsys) e [Android Debug Bridge (`adb`)](https://developer.android.com/tools/adb)
- [Agendar alarmes](https://developer.android.com/develop/background-work/services/alarms)
- [Doze e App Standby (testes com `deviceidle`)](https://developer.android.com/training/monitoring-device-state/doze-standby)

*Conteúdo das fontes parafraseado para o contexto do projeto.*
