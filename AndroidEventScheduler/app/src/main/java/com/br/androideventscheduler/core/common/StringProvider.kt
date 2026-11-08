package com.br.androideventscheduler.core.common

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * Acesso a textos de resource fora de Composables (mappers, ViewModels), sem expor `Context`.
 * Permite testar a formatação na JVM com um fake e mantém todo texto de UI em `strings.xml`.
 */
interface StringProvider {
    fun getString(
        @StringRes id: Int,
        vararg args: Any,
    ): String

    fun getQuantityString(
        @PluralsRes id: Int,
        quantity: Int,
        vararg args: Any,
    ): String
}
