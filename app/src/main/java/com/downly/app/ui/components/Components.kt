package com.downly.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.downly.app.engine.Platform
import com.downly.app.ui.theme.DColors

/** Logo original de Downly: flecha de descarga sobre bandeja, en un cuadrado redondeado con degradado. */
@Composable
fun Logo(size: Dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(size * 0.28f)).background(DColors.Brand)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val s = w / 108f
            val arrow = Path().apply {
                moveTo(49 * s, 30 * s); lineTo(59 * s, 30 * s); lineTo(59 * s, 48 * s); lineTo(68.5f * s, 48 * s)
                lineTo(54 * s, 66 * s); lineTo(39.5f * s, 48 * s); lineTo(49 * s, 48 * s); close()
            }
            drawPath(arrow, Color.White)
            drawRoundRect(
                Color.White.copy(alpha = .85f), topLeft = Offset(31 * s, 70 * s),
                size = Size(46 * s, 6 * s), cornerRadius = CornerRadius(3 * s, 3 * s),
            )
        }
    }
}

@Composable
fun PlatformDot(p: Platform, size: Dp = 8.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(Color(p.colorHex)))
}

@Composable
fun GradientButton(
    text: String,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DColors.Brand)
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.let { Icon(it, null, tint = Color.White, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
            Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
}
