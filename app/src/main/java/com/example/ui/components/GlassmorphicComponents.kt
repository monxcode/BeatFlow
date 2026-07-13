package com.example.ui.components

import android.os.Build
import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GlassDarkBorder
import com.example.ui.theme.GlassDarkSurface
import com.example.ui.theme.GlassLightBorder
import com.example.ui.theme.GlassLightSurface

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    isGlassEnabled: Boolean = true,
    isDark: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val backgroundBrush = if (isGlassEnabled) {
        val baseColor = if (isDark) GlassDarkSurface else GlassLightSurface
        Brush.verticalGradient(
            colors = listOf(
                baseColor.copy(alpha = 0.15f),
                baseColor.copy(alpha = 0.05f)
            )
        )
    } else {
        val baseColor = if (isDark) Color(0xFF1E1E1E) else Color(0xFFF1F3F5)
        Brush.verticalGradient(listOf(baseColor, baseColor))
    }

    val borderBrush = if (isGlassEnabled) {
        val baseBorderColor = if (isDark) GlassDarkBorder else GlassLightBorder
        Brush.linearGradient(
            colors = listOf(
                baseBorderColor.copy(alpha = 0.3f),
                baseBorderColor.copy(alpha = 0.08f)
            )
        )
    } else {
        val baseBorderColor = if (isDark) Color(0xFF2C2C2C) else Color(0xFFE2E8F0)
        Brush.linearGradient(listOf(baseBorderColor, baseBorderColor))
    }

    val shape = RoundedCornerShape(cornerRadius)

    var currentModifier = modifier
        .clip(shape)

    if (onClick != null) {
        currentModifier = currentModifier.clickable(onClick = onClick)
    }

    Box(
        modifier = currentModifier,
        contentAlignment = Alignment.CenterStart
    ) {
        // Blur background layer to achieve true glassmorphic backdrop blur
        if (isGlassEnabled) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val blur = RenderEffect.createBlurEffect(
                                30f, 30f, Shader.TileMode.CLAMP
                            )
                            renderEffect = blur.asComposeRenderEffect()
                        }
                    }
                    .background(backgroundBrush)
                    .border(width = 1.dp, brush = borderBrush, shape = shape)
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(backgroundBrush)
                    .border(width = 1.dp, brush = borderBrush, shape = shape)
            )
        }

        Box(
            modifier = Modifier.padding(12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            content()
        }
    }
}

/**
 * Creates a beautiful neon glowing background behind components to simulate organic Gen-Z light blooms.
 */
fun Modifier.glow(
    color: Color,
    radius: Dp = 32.dp,
    alpha: Float = 0.25f
): Modifier = this.drawBehind {
    drawCircle(
        color = color.copy(alpha = alpha),
        radius = size.width / 2.5f + radius.toPx()
    )
}
