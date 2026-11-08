package com.br.androideventscheduler.app

import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Trava das permissões do APK (manifest **mesclado**, incluindo as injetadas por bibliotecas).
 *
 * Se este teste falhar, uma permissão entrou ou saiu. Antes de atualizar a lista abaixo, documente a
 * permissão com justificativa em `docs/permissoes.md` (regra do steering, `arquitetura.md` §5.6).
 */
@RunWith(RobolectricTestRunner::class)
class RequestedPermissionsGuardRobolectricTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun requestedPermissions(): Set<String> =
        context.packageManager
            .getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            .orEmpty()
            .toSet()

    private val alwaysRequested =
        setOf(
            "android.permission.POST_NOTIFICATIONS",
            "android.permission.USE_FULL_SCREEN_INTENT",
            // Pós-boot (spec 15)
            "android.permission.RECEIVE_BOOT_COMPLETED",
            // Injetada pela androidx.core (nível signature; não aparece para o usuário).
            "${context.packageName}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION",
        )

    @Test
    fun `given android 13 or newer when reading the merged manifest then only documented permissions are requested`() {
        assertEquals(
            "Permissões mudaram: atualize docs/permissoes.md antes de ajustar este teste",
            alwaysRequested + "android.permission.USE_EXACT_ALARM",
            requestedPermissions(),
        )
    }

    @Test
    @Config(sdk = [32])
    fun `given android 12L when reading the merged manifest then schedule exact alarm is requested too`() {
        assertEquals(
            "Permissões mudaram: atualize docs/permissoes.md antes de ajustar este teste",
            alwaysRequested + "android.permission.USE_EXACT_ALARM" + "android.permission.SCHEDULE_EXACT_ALARM",
            requestedPermissions(),
        )
    }
}
