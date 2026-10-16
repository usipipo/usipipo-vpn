package com.usipipo.vpn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.usipipo.vpn.R
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.domain.isActive
import com.usipipo.vpn.ui.theme.LocalStatusColors
import com.usipipo.vpn.ui.theme.rememberReduceMotion

@Composable
fun ConnectButton(
    state: TunnelState,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val status = LocalStatusColors.current
    val container by animateColorAsState(
        targetValue = when (state) {
            is TunnelState.Connected -> status.connected
            is TunnelState.NoResponse, is TunnelState.Failed -> status.error
            else -> status.idle
        },
        animationSpec = tween(durationMillis = 300),
        label = "connectContainer",
    )
    val actionLabel = stringResource(if (state.isActive) R.string.disconnect else R.string.connect)
    val reduceMotion = rememberReduceMotion()

    Box(modifier = modifier.size(200.dp), contentAlignment = Alignment.Center) {
        if (state is TunnelState.Connecting) {
            PulseRing(color = container, animate = !reduceMotion)
        }
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = CircleShape,
            color = container,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 4.dp,
            modifier = Modifier
                .size(168.dp)
                .alpha(if (enabled) 1f else 0.5f),
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(R.drawable.ic_power),
                    contentDescription = actionLabel,
                    modifier = Modifier.size(72.dp),
                )
            }
        }
    }
}

/** Anillo que "respira" mientras conecta; estático si el sistema pide reducir el movimiento. */
@Composable
private fun PulseRing(color: Color, animate: Boolean) {
    val factor = if (animate) {
        val transition = rememberInfiniteTransition(label = "pulse")
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.14f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulseScale",
        ).value
    } else {
        1.07f
    }
    Box(
        modifier = Modifier
            .size(168.dp)
            .graphicsLayer {
                scaleX = factor
                scaleY = factor
            }
            .background(color.copy(alpha = 0.35f), CircleShape),
    )
}
