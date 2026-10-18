package com.example.shared.ui.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable

/**
 * Actual Android de [PlatformPermissions] — mesma lógica do `PermissionUtils` e
 * dos launchers do app nativo (`ui/permissions/*`).
 *
 * Usa `Context.checkSelfPermission` (API 23+; minSdk do projeto é 24), sem
 * depender de androidx.core.
 */

private var platformAppContext: Context? = null

actual object PlatformPermissions {

    actual fun init(appContext: Any?) {
        platformAppContext = appContext as? Context
    }

    actual fun hasLocationPermission(): Boolean {
        val context = platformAppContext ?: return false
        val fine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val coarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    actual fun hasNotificationPermission(): Boolean {
        val context = platformAppContext ?: return false
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
}

@Composable
actual fun rememberLocationPermissionLauncher(onResult: (Boolean) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
            PlatformPermissions.hasLocationPermission()
        onResult(granted)
    }
    return {
        launcher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }
}

@Composable
actual fun rememberNotificationPermissionLauncher(onResult: (Boolean) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> onResult(granted) }
    return { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
}
