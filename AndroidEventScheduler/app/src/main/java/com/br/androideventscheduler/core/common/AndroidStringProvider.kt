package com.br.androideventscheduler.core.common

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/** [StringProvider] sobre o `Context` da aplicação; lê `resources` a cada chamada para respeitar mudanças de configuração. */
class AndroidStringProvider(
    private val context: Context,
) : StringProvider {
    override fun getString(
        @StringRes id: Int,
        vararg args: Any,
    ): String = context.resources.getString(id, *args)

    override fun getQuantityString(
        @PluralsRes id: Int,
        quantity: Int,
        vararg args: Any,
    ): String = context.resources.getQuantityString(id, quantity, *args)
}
