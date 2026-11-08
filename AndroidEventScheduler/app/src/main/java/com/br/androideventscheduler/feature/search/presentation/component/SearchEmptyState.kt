package com.br.androideventscheduler.feature.search.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.br.androideventscheduler.R
import com.br.androideventscheduler.core.designsystem.AndroidEventSchedulerTheme
import com.br.androideventscheduler.core.ui.SemanticsTags

private val HINT_ICON_SIZE = 48.dp

/** Estado inicial: texto em branco. */
@Composable
internal fun SearchHintState(modifier: Modifier = Modifier) {
    CenteredMessage(modifier = modifier.testTag(SemanticsTags.SEARCH_HINT)) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            modifier = Modifier.size(HINT_ICON_SIZE),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        MessageText(stringResource(R.string.search_hint))
    }
}

/** Texto consultado sem nenhum resultado. */
@Composable
internal fun SearchNoResultsState(
    query: String,
    modifier: Modifier = Modifier,
) {
    CenteredMessage(modifier = modifier.testTag(SemanticsTags.SEARCH_EMPTY_STATE)) {
        MessageText(stringResource(R.string.search_empty, query))
    }
}

/** Falha na consulta, com ação para tentar de novo. */
@Composable
internal fun SearchErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenteredMessage(modifier = modifier.testTag(SemanticsTags.SEARCH_ERROR_STATE)) {
        MessageText(stringResource(R.string.search_error))
        TextButton(onClick = onRetry, modifier = Modifier.testTag(SemanticsTags.SEARCH_RETRY_BUTTON)) {
            Text(stringResource(R.string.search_retry))
        }
    }
}

@Composable
private fun CenteredMessage(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        content()
    }
}

@Composable
private fun MessageText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Preview(showBackground = true, name = "Dica inicial")
@Composable
private fun SearchHintStatePreview() {
    AndroidEventSchedulerTheme { SearchHintState() }
}

@Preview(showBackground = true, name = "Sem resultados")
@Composable
private fun SearchNoResultsStatePreview() {
    AndroidEventSchedulerTheme { SearchNoResultsState(query = "dentista") }
}

@Preview(showBackground = true, name = "Erro")
@Composable
private fun SearchErrorStatePreview() {
    AndroidEventSchedulerTheme { SearchErrorState(onRetry = {}) }
}
