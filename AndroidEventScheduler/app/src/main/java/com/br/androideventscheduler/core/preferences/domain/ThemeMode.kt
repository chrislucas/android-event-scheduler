package com.br.androideventscheduler.core.preferences.domain

/**
 * Escolha de tema do usuário (spec 19). [storageId] é o valor gravado no DataStore: estável e independente do `name`
 * do enum, para que renomear a constante não perca a escolha já salva.
 */
enum class ThemeMode(
    val storageId: String,
) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    ;

    companion object {
        /** Escuro, o comportamento anterior à spec 19: atualizar o app não muda o visual de ninguém. */
        val DEFAULT = DARK

        /** Id ausente, desconhecido ou com outra caixa resulta em [DEFAULT]. */
        fun fromStorageId(id: String?): ThemeMode = entries.firstOrNull { it.storageId == id } ?: DEFAULT
    }
}
