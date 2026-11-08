package com.br.androideventscheduler.core.common.testing

import com.br.androideventscheduler.core.common.StringProvider

/**
 * [StringProvider] determinístico para testes na JVM: devolve `string(<id>)`/`plural(<id>,<quantity>)` seguidos
 * dos argumentos, o que permite comparar resultados sem carregar resources.
 */
class FakeStringProvider : StringProvider {
    override fun getString(
        id: Int,
        vararg args: Any,
    ): String = "string($id)" + args.toArgsSuffix()

    override fun getQuantityString(
        id: Int,
        quantity: Int,
        vararg args: Any,
    ): String = "plural($id,$quantity)" + args.toArgsSuffix()

    private fun Array<out Any>.toArgsSuffix(): String = if (isEmpty()) "" else joinToString(prefix = "[", postfix = "]")
}
