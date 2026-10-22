package com.usipipo.vpn.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.usipipo.vpn.R
import com.usipipo.vpn.ui.theme.UsipipoTheme

private val BrandBlue = Color(0xFF004AAD)

/** El logo: la «u» blanca con guion bajo sobre el azul de marca. */
@Composable
fun AppLogo(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(BrandBlue),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_logo_u),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.85f),
        )
    }
}

@Preview(name = "Logo")
@Preview(name = "Logo oscuro", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewAppLogo() {
    UsipipoTheme {
        Surface {
            AppLogo(size = 56.dp, modifier = Modifier.padding(16.dp))
        }
    }
}
