package com.usipipo.vpn.ui.details

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

/** Tiempo conectado, recalculado cada segundo mientras el composable está en pantalla. */
@Composable
fun rememberUptimeText(sinceMs: Long): String {
    var nowMs by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(sinceMs) {
        while (true) {
            nowMs = SystemClock.elapsedRealtime()
            delay(1_000L)
        }
    }
    return uptimeText(sinceMs = sinceMs, nowMs = nowMs)
}
