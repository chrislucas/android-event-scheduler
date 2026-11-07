# 12 — Compartilhar agendamento — Design

## Abordagem Android-only
O PDF usava `ShareManager` (`expect/actual`) + `ShareScheduleUseCase`. Em Android puro isso é indireção sem ganho: compartilhar é um **efeito de UI** (precisa da `Activity`), então segue o padrão MVI da arquitetura: ViewModel emite `UiEffect.ShareText(subject, body)` e a tela o executa.

## Mudanças em `feature.detail`
```
presentation/mapper/ScheduleShareTextBuilder.kt   (novo: ScheduleDetailUi + strings → ShareContent)
presentation/effect/ScheduleDetailUiEffect.kt     (+ ShareText)
presentation/event/ScheduleDetailUiEvent.kt       (+ OnShareClicked)
presentation/screen/ShareLauncher.kt              (novo: função que dispara o chooser)
presentation/viewmodel/ScheduleDetailViewModel.kt (alterado)
```

## Contratos
```kotlin
data class ShareContent(val subject: String, val body: String)

class ScheduleShareTextBuilder(private val stringProvider: StringProvider) {
    fun build(schedule: ScheduleDetailUi): ShareContent
}
// linhas: "📅 ${title}", "🕒 ${dateRangeText}", "🔔 ${alert_prefix} ${reminderText}", "🔁 ${repeat_prefix} ${recurrenceText}",
//         "📍 ${location_prefix} ${location}" (se != null), "📝 ${notes_prefix} ${notes}" (se != null)
// subject: "RememberMe: ${title}"

sealed interface ScheduleDetailUiEffect { … data class ShareText(val subject: String, val body: String) }
```
`ShareContent` e o builder dependem de `ScheduleDetailUi` (já formatado pela spec 10), garantindo o mesmo texto da tela. `StringProvider` (spec 11 / `core.common`).

## ViewModel
`OnShareClicked` → `(uiState.value as? ScheduleDetailUiState.Content)?.let { viewModelScope.launch { _uiEffect.send(ShareText(builder.build(it.schedule))) } }`; sem I/O. Falha de lançamento é tratada na tela.

## Tela
```kotlin
// ShareLauncher.kt
fun launchShareChooser(context: Context, subject: String, body: String): Boolean {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, subject); putExtra(Intent.EXTRA_TEXT, body)
    }
    return try { context.startActivity(Intent.createChooser(sendIntent, subject)); true }
           catch (e: ActivityNotFoundException) { false }
}
```
- Em `ScheduleDetailScreen`: `CollectEffects` → `ShareText -> if (!launchShareChooser(LocalContext.current, …)) snackbar(share_error)`. `LocalContext.current` numa `Activity` (Compose Activity) é a própria `Activity`; não guardar referência fora da composição.
- Botão: `ScheduleAction(label = R.string.action_share, description = "Compartilhar agendamento", icon = Share, tag = DETAIL_SHARE_BUTTON, enabled = uiState is ScheduleDetailUiState.Content)`; adicionado à lista na posição 3.
- Para o rótulo curto em telas estreitas usar `maxLines = 1`, `overflow = Ellipsis` no rótulo.

## Decisões de UseCase
Nenhum: não há regra de negócio nem segundo ponto de entrada; formatar texto é apresentação (`ScheduleShareTextBuilder`), o envio é efeito de UI. (Removidos `ShareManager`, `ShareScheduleUseCase` do PDF.)

## Testes
- `ScheduleShareTextBuilderTest`: texto completo; sem local; sem notas; sem ambos; recorrente (`Toda semana em ter., qui. (5000 vezes)`); assunto; ausência de `id`/e-mail.
- `ScheduleDetailViewModelTest` (acréscimos): `OnShareClicked` com item → `ShareText` com o conteúdo do builder (mockado); sem item carregado → nenhum efeito.
- `ShareLauncherRobolectricTest`: `shadowOf(application).nextStartedActivity` é um chooser cujo `EXTRA_INTENT` tem `ACTION_SEND`, `text/plain`, subject e texto; sem handler (`ShadowPackageManager` / forçar exceção) → retorna `false`.
- `ScheduleDetailScreenRobolectricTest` (acréscimo): botão Compartilhar com descrição e desabilitado carregando; clique envia `OnShareClicked`.
