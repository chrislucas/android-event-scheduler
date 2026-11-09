package com.br.androideventscheduler.feature.backup.testing

import androidx.core.content.FileProvider

/**
 * O `FileProvider` guarda em um cache **estático** as pastas configuradas na primeira vez que a authority é usada. O
 * Robolectric cria um diretório temporário novo por teste, então sem limpar o cache os testes seguintes falham com
 * "Failed to find configured root". Chame no `@Before` de testes que geram URIs do `FileProvider`.
 */
fun resetFileProviderCache() {
    val cache = FileProvider::class.java.getDeclaredField("sCache")
    cache.isAccessible = true
    (cache.get(null) as MutableMap<*, *>).clear()
}
