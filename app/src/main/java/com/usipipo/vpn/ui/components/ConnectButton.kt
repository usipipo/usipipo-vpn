package com.usipipo.vpn.ui.components

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.usipipo.vpn.R
import com.usipipo.vpn.domain.TunnelState
import com.usipipo.vpn.domain.isActive
import com.usipipo.vpn.ui.theme.AtmospherePhase
import com.usipipo.vpn.ui.theme.LocalReduceMotion
import com.usipipo.vpn.ui.theme.LocalStatusColors
import com.usipipo.vpn.ui.theme.UsipipoTheme
import com.usipipo.vpn.ui.theme.atmospherePhase
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
    val phase = atmospherePhase(state)
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current

    // Temblor: una sola vez al entrar en Alert (y nunca con movimiento reducido).
    // `rememberSaveable` con clave por estado de error evita repetirlo al rotar a mitad del fallo.
    val offsetX = remember { Animatable(0f) }
    var shook by rememberSaveable(state is TunnelState.NoResponse || state is TunnelState.Failed) {
        mutableStateOf(false)
    }
    LaunchedEffect(phase) {
        if (phase == AtmospherePhase.Alert && !shook && !reduceMotion) {
            shook = true
            val amplitude = with(density) { 10.dp.toPx() }
            offsetX.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 360
                    0f at 0
                    -amplitude at 60
                    amplitude at 120
                    -amplitude * 0.7f at 180
                    amplitude * 0.7f at 240
                    -amplitude * 0.3f at 300
                    0f at 360
                },
            )
        }
        if (phase != AtmospherePhase.Alert) {
            shook = false
            // Salir de Alert a mitad del temblor cancela `animateTo` y deja el offset
            // congelado; devolverlo al centro garantiza que el botón no quede descentrado.
            offsetX.snapTo(0f)
        }
    }

    Box(
        modifier = modifier
            .size(200.dp)
            .graphicsLayer { translationX = offsetX.value },
        contentAlignment = Alignment.Center,
    ) {
        Halo(color = container, phase = phase, reduceMotion = reduceMotion)
        if (phase == AtmospherePhase.Drifting && !reduceMotion) {
            ExpandingRing(color = container)
        }
        Surface(
            onClick = {
                if (state.isActive) {
                    haptics.performHapticFeedback(HapticFeedbackType.ToggleOff)
                } else {
                    haptics.performHapticFeedback(HapticFeedbackType.ToggleOn)
                }
                onClick()
            },
            enabled = enabled,
            shape = CircleShape,
            color = container,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .size(168.dp)
                .alpha(if (enabled) 1f else 0.5f),
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = state is TunnelState.Connected,
                    transitionSpec = {
                        if (reduceMotion) {
                            EnterTransition.None togetherWith ExitTransition.None
                        } else {
                            (fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.8f))
                                .togetherWith(
                                    fadeOut(tween(200)) + scaleOut(tween(200), targetScale = 0.8f),
                                )
                        }
                    },
                    label = "connectIcon",
                ) { connected ->
                    Icon(
                        painter = painterResource(
                            if (connected) R.drawable.ic_shield_check else R.drawable.ic_power,
                        ),
                        contentDescription = actionLabel,
                        modifier = Modifier.size(72.dp),
                    )
                }
            }
        }
    }
}

/** Halo que respira tras el botón; con `reduceMotion` queda estático y no se crea ninguna transición infinita. */
@Composable
private fun Halo(color: Color, phase: AtmospherePhase, reduceMotion: Boolean) {
    // Valores estáticos: los de Calm, salvo Flowing (respiración detenida en su punto alto).
    val staticScale = if (phase == AtmospherePhase.Flowing) 1.04f else 1f
    val staticAlpha = if (phase == AtmospherePhase.Flowing) 0.65f else 0.35f

    val scale: Float
    val alpha: Float
    if (reduceMotion || phase == AtmospherePhase.Calm || phase == AtmospherePhase.Alert) {
        scale = staticScale
        alpha = staticAlpha
    } else if (phase == AtmospherePhase.Drifting) {
        val transition = rememberInfiniteTransition(label = "halo")
        scale = transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.14f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "haloScale",
        ).value
        alpha = 0.35f
    } else {
        // Flowing: respiración lenta de escala y opacidad.
        val transition = rememberInfiniteTransition(label = "halo")
        scale = transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "haloScale",
        ).value
        alpha = transition.animateFloat(
            initialValue = 0.50f,
            targetValue = 0.65f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "haloAlpha",
        ).value
    }

    Box(
        modifier = Modifier
            .size(188.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .background(color.copy(alpha = alpha), CircleShape),
    )
}

/** Aro que se expande y se desvanece mientras conecta; solo con `Drifting` y sin `reduceMotion`. */
@Composable
private fun ExpandingRing(color: Color) {
    val progress by rememberInfiniteTransition(label = "ring").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "ringProgress",
    )
    Box(
        modifier = Modifier
            .size(168.dp)
            .graphicsLayer {
                val factor = 1f + 0.30f * progress
                scaleX = factor
                scaleY = factor
            }
            .background(color.copy(alpha = 0.35f * (1f - progress)), CircleShape),
    )
}

@Preview(name = "Desconectado")
@Preview(name = "Desconectado oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewDesconectado() {
    UsipipoTheme {
        Surface {
            ConnectButton(state = TunnelState.Disconnected, enabled = true, onClick = {})
        }
    }
}

@Preview(name = "Conectando")
@Preview(name = "Conectando oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewConectando() {
    UsipipoTheme {
        Surface {
            ConnectButton(state = TunnelState.Connecting("preview"), enabled = true, onClick = {})
        }
    }
}

@Preview(name = "Conectado")
@Preview(name = "Conectado oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewConectado() {
    UsipipoTheme {
        Surface {
            ConnectButton(state = TunnelState.Connected("preview"), enabled = true, onClick = {})
        }
    }
}

@Preview(name = "Sin respuesta")
@Preview(name = "Sin respuesta oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewSinRespuesta() {
    UsipipoTheme {
        Surface {
            ConnectButton(state = TunnelState.NoResponse("preview"), enabled = true, onClick = {})
        }
    }
}

@Preview(name = "Fallo")
@Preview(name = "Fallo oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewFallo() {
    UsipipoTheme {
        Surface {
            ConnectButton(state = TunnelState.Failed("preview"), enabled = true, onClick = {})
        }
    }
}

@Preview(name = "Conectando (movimiento reducido)")
@Preview(
    name = "Conectando (movimiento reducido) oscuro",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PreviewConectandoReducido() {
    UsipipoTheme {
        CompositionLocalProvider(LocalReduceMotion provides true) {
            Surface {
                ConnectButton(state = TunnelState.Connecting("preview"), enabled = true, onClick = {})
            }
        }
    }
}
