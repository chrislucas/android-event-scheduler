package com.br.androideventscheduler.core.ui.format

import android.content.res.Resources
import com.br.androideventscheduler.core.common.StringProvider

/** Adaptador para as funções de formatação que ainda recebem [Resources] (Composables via `LocalContext`). */
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
