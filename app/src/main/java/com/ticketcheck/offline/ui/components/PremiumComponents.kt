package com.ticketcheck.offline.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import com.ticketcheck.offline.ui.theme.Accent
import com.ticketcheck.offline.ui.theme.BrandGradient
import com.ticketcheck.offline.ui.theme.Success
import com.ticketcheck.offline.utils.AppSound
import com.ticketcheck.offline.utils.SoundEffects
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─────────────────────────────────────────────────────────────────────────
// Animated aurora background - slow drifting radial glows over the deep
// base color. This is the canvas every screen sits on.
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val transition = rememberInfiniteTransition(label = "aurora")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing)),
        label = "drift"
    )

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val a = t * 2f * PI.toFloat()
            val violet = Color(0xFF8B5CF6)
            val cyan = Color(0xFF38BDF8)
            val emerald = Color(0xFF34D399)

            val a1 = if (dark) 0.30f else 0.14f
            val a2 = if (dark) 0.22f else 0.12f
            val a3 = if (dark) 0.16f else 0.08f

            drawCircle(
                brush = Brush.radialGradient(listOf(violet.copy(alpha = a1), Color.Transparent)),
                center = Offset(w * (0.25f + 0.14f * sin(a)), h * (0.16f + 0.10f * cos(a * 1.1f))),
                radius = w * 0.85f
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(cyan.copy(alpha = a2), Color.Transparent)),
                center = Offset(w * (0.86f - 0.12f * cos(a * 0.9f)), h * (0.34f + 0.10f * sin(a * 1.3f))),
                radius = w * 0.80f
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(emerald.copy(alpha = a3), Color.Transparent)),
                center = Offset(w * (0.50f + 0.16f * sin(a * 0.7f + 2f)), h * (0.94f - 0.06f * cos(a))),
                radius = w * 0.90f
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Glass card - translucent surface with a hairline border.
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    borderColor: Color = Color.White.copy(alpha = 0.10f),
    fill: Color = Color.White.copy(alpha = 0.05f),
    glowColor: Color? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val finalModifier = if (glowColor != null) {
        modifier.shadow(
            elevation = 16.dp,
            shape = shape,
            clip = false,
            ambientColor = glowColor.copy(alpha = 0.45f),
            spotColor = glowColor.copy(alpha = 0.55f)
        )
    } else modifier

    Surface(
        modifier = finalModifier,
        shape = shape,
        color = fill,
        border = BorderStroke(1.dp, borderColor),
        content = content
    )
}

// ─────────────────────────────────────────────────────────────────────────
// Gradient button - the primary CTA. Presses in with a spring.
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = 56.dp,
    cornerRadius: Dp = 18.dp,
    colors: List<Color> = BrandGradient,
    sound: AppSound? = AppSound.CLICK
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
    val disabledColors = listOf(Color(0xFF3A4258), Color(0xFF283041))

    Box(
        modifier
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.5f
            }
            .clip(RoundedCornerShape(cornerRadius))
            .background(
                Brush.linearGradient(if (enabled) colors else disabledColors)
            )
            .clickable(interactionSource = interaction, indication = null, enabled = enabled) {
                sound?.let { SoundEffects.play(it) }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
            }
            Text(
                text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 1.2.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Glass circle button - back arrows, close buttons, icon actions.
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun GlassCircleButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    tint: Color = Color.White,
    sound: AppSound? = AppSound.CLICK,
    background: Color = Color.White.copy(alpha = 0.10f)
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
    Box(
        modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(50))
            .background(background)
            .clickable(interactionSource = interaction, indication = null) {
                sound?.let { SoundEffects.play(it) }
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint)
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Outlined glass button - secondary actions (e.g. "SCAN NEXT").
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun GlassOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    height: Dp = 52.dp,
    enabled: Boolean = true,
    sound: AppSound? = AppSound.CLICK
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
    Box(
        modifier
            .height(height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.45f
            }
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(interactionSource = interaction, indication = null, enabled = enabled) {
                sound?.let { SoundEffects.play(it) }
                onClick()
            }
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
            }
            Text(text, color = Color.White, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Status pill - small rounded badge with tinted fill + border.
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun StatusPill(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 11.sp
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Text(
            label,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Gradient icon chip - the little colored square behind action icons.
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun GradientIconChip(
    icon: ImageVector,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    cornerRadius: Dp = 14.dp,
    iconTint: Color = Color.White
) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = iconTint)
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Animated counter - numbers roll up when the dashboard refreshes.
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun AnimatedCounter(
    value: Int,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = 26.sp
) {
    val animated by animateFloatAsState(
        targetValue = value.toFloat(),
        animationSpec = tween(750, easing = FastOutSlowInEasing),
        label = "counter"
    )
    Text(
        animated.toInt().toString(),
        modifier = modifier,
        color = color,
        fontWeight = FontWeight.ExtraBold,
        fontSize = fontSize,
        letterSpacing = (-0.5).sp
    )
}

// ─────────────────────────────────────────────────────────────────────────
// Staggered entrance - content fades in and slides up, delayed by index.
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun StaggeredAppear(
    index: Int,
    modifier: Modifier = Modifier,
    stepMs: Long = 55,
    content: @Composable () -> Unit
) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index.coerceAtMost(12) * stepMs)
        shown = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(360, easing = FastOutSlowInEasing),
        label = "entranceAlpha"
    )
    val translationY by animateFloatAsState(
        targetValue = if (shown) 0f else 36f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "entranceY"
    )
    Box(
        modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = translationY
        }
    ) {
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Pulsing dot - the "live" indicator dot.
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun PulsingDot(color: Color = Success, modifier: Modifier = Modifier, dotSize: Dp = 8.dp) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "phase"
    )
    Box(modifier.size(dotSize * 2.4f), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(dotSize * 2.4f)
                .graphicsLayer {
                    scaleX = 0.5f + phase
                    scaleY = 0.5f + phase
                    alpha = (1f - phase) * 0.45f
                }
                .clip(RoundedCornerShape(50))
                .background(color)
        )
        Box(
            Modifier
                .size(dotSize)
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Gradient progress bar - check-in progress on the dashboard.
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun GradientProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(850, easing = FastOutSlowInEasing),
        label = "progress"
    )
    Box(
        modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.08f))
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(animated)
                .clip(RoundedCornerShape(50))
                .background(Brush.linearGradient(listOf(Success, Accent)))
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Premium top bar - transparent, floating over the aurora background.
// ─────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                title,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                letterSpacing = (-0.3).sp
            )
        },
        navigationIcon = {
            GlassCircleButton(Icons.AutoMirrored.Filled.ArrowBack, onClick = onBack)
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = modifier
    )
}

// ─────────────────────────────────────────────────────────────────────────
// Glass text field - rounded, translucent, brand-colored focus ring.
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val fill = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f)
    val unfocusedBorder = if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.12f)

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        singleLine = singleLine,
        leadingIcon = leadingIcon?.let {
            { Icon(it, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = fill,
            unfocusedContainerColor = fill,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = unfocusedBorder,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────
// Small caption used for section labels.
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurfaceVariant else color,
        fontWeight = FontWeight.Bold
    )
}

// ─────────────────────────────────────────────────────────────────────────
// Spring-in icon wrapper - used by the scan result overlay.
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun SpringInIcon(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 104.dp
) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = 380f
            )
        )
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Icon(
            icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .size(iconSize)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Expanding pulse rings - behind the scan result icon.
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun PulseRings(color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "rings")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1900, easing = LinearEasing)),
        label = "phase"
    )
    Canvas(modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val maxR = size.minDimension / 2
        for (i in 0 until 3) {
            val p = (phase + i / 3f) % 1f
            val r = maxR * (0.45f + 0.55f * p)
            drawCircle(
                color = color.copy(alpha = (1f - p) * 0.35f),
                radius = r,
                center = Offset(cx, cy),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────
// Fade-in wrapper for whole screens (played once on composition).
// ─────────────────────────────────────────────────────────────────────────

@Composable
fun FadeIn(content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = androidx.compose.animation.fadeIn(tween(350))
    ) {
        content()
    }
}
