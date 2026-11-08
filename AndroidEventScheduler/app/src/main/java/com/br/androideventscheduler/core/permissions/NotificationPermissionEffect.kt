package com.br.androideventscheduler.core.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

// Sem preview: efeito sem UI própria (só dispara o pedido de permissão do sistema).

/**
 * Pede a permissão `POST_NOTIFICATIONS` (API 33+) **uma única vez** enquanto estiver na composição, sem
 * bloquear a UI: o diálogo é do sistema e a tela segue utilizável. Pública para ser incluída por qualquer
 * tela (a Home, na spec 05).
 *
 * - Antes do API 33 ou com a permissão já concedida, não faz nada.
 * - A tentativa fica registrada em `rememberSaveable`: rotação e recriação de processo **não** repetem o
 *   pedido, então uma recusa não é insistida a cada recomposição. (O próprio Android deixa de exibir o
 *   diálogo após recusas repetidas.)
 *
 * @param onResult chamado com `true` se o usuário concedeu e `false` se recusou.
 */
@Composable
fun NotificationPermissionEffect(onResult: (granted: Boolean) -> Unit = {}) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        RequestPostNotificationsOnce(onResult)
    }
}

@Composable
private fun RequestPostNotificationsOnce(onResult: (Boolean) -> Unit) {
    val context = LocalContext.current
    var alreadyRequested by rememberSaveable { mutableStateOf(false) }
    val currentOnResult by rememberUpdatedState(onResult)
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            currentOnResult(isGranted)
        }

    LaunchedEffect(Unit) {
        val isAlreadyGranted =
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!alreadyRequested && !isAlreadyGranted) {
            alreadyRequested = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
