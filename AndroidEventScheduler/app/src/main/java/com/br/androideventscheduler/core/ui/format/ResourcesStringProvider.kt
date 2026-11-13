package com.br.androideventscheduler.core.ui.format

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import com.br.androideventscheduler.core.common.StringProvider

/**
 * Adaptador para as funções de formatação que recebem [StringProvider] a partir de [Resources]. Em Compose, use
 * [rememberResourcesStringProvider] (lê `LocalResources`, nunca `LocalContext.current.resources`).
 */
internal class ResourcesStringProvider(
    private val resources: Resources,
) : StringProvider {
    override fun getString(
        id: Int,
        vararg args: Any,
    ): String = resources.getString(id, *args)

    override fun getQuantityString(
        id: Int,
        quantity: Int,
        vararg args: Any,
    ): String = resources.getQuantityString(id, quantity, *args)
}

/**
 * [StringProvider] para Composables. Lê [LocalResources] (e não `LocalContext.current.resources`) para recompor quando a
 * configuração muda, e só recria o adaptador quando o [Resources] muda.
 */
@Composable
internal fun rememberResourcesStringProvider(): StringProvider {
    val resources = LocalResources.current
    return remember(resources) { ResourcesStringProvider(resources) }
}
