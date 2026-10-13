package com.example.bangumioauthdemo

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import kotlin.math.roundToInt

/**
 * Kyant's library renders a real sampled backdrop, not a translucent fake panel.
 * The Local is scoped to the content captured by rememberLayerBackdrop/layerBackdrop.
 */
val LocalGlassBackdrop = compositionLocalOf<Backdrop?> { null }

private val Capsule = RoundedCornerShape(100)
private val GlassWhite = Color(0xB8FFFFFF)
private val GlassTint = Color(0x60FFFFFF)

@Composable
fun GlassPill(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    compact: Boolean = false,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    val backdrop = LocalGlassBackdrop.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (pressed) .94f else 1f,
        animationSpec = spring(dampingRatio = .57f, stiffness = 640f),
        label = "Liquid press"
    )
    val shine by animateFloatAsState(
        targetValue = if (pressed) 1f else if (selected) .38f else .15f,
        animationSpec = spring(dampingRatio = .66f, stiffness = 380f),
        label = "Liquid chromatic glow"
    )
    val base = modifier
        .heightIn(min = 42.dp)
        .then(if (backdrop != null) {
            Modifier.drawBackdrop(
                backdrop = backdrop,
                shape = { Capsule },
                effects = {
                    vibrancy()
                    blur((if (selected) 7f else 5f).dp.toPx())
                    lens(
                        (8f + shine * 7f).dp.toPx(),
                        (12f + shine * 8f).dp.toPx(),
                        chromaticAberration = true
                    )
                },
                highlight = { Highlight.Ambient.copy(alpha = .36f + shine * .45f) },
                shadow = { Shadow(radius = 10.dp, alpha = .52f) },
                innerShadow = { InnerShadow(radius = 6.dp, alpha = .18f + shine * .15f) },
                layerBlock = {
                    // Scale only foreground glass, not the pixels sampled from the backdrop.
                    scaleX = scale
                    scaleY = scale
                },
                onDrawSurface = {
                    drawRect(
                        if (selected) Color(0x9CE8EDFF) else GlassWhite,
                        blendMode = BlendMode.SrcOver
                    )
                    if (pressed) drawRect(Color.White.copy(alpha = .10f), blendMode = BlendMode.Plus)
                }
            )
        } else Modifier)
        .then(
            if (backdrop == null) {
                Modifier.background(
                    if (selected) Color(0xFFE7EDFF) else Color(0xEBFFFFFF), Capsule
                ).border(1.dp, Color(0xFFC6CCDC), Capsule)
            } else Modifier
        )
        .clickable(
            enabled = enabled,
            interactionSource = interaction,
            indication = null,
            role = Role.Button
        ) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }
        .padding(horizontal = if (compact) 9.dp else 15.dp, vertical = 9.dp)
    Row(
        base,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        content = content
    )
}

@Composable
fun GlassIconAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassPill(
        modifier = modifier.size(46.dp).semantics { contentDescription = description },
        compact = true,
        onClick = onClick
    ) {
        Icon(icon, contentDescription = description, tint = Ink, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun GlassAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    active: Boolean = false
) {
    GlassPill(modifier = modifier, selected = active, onClick = onClick) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = if (active) Blue else Ink,
                modifier = Modifier.size(19.dp))
        }
        Text(
            text,
            color = if (active) Blue else Ink,
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun GlassSelectionChip(text: String, selected: Boolean, onClick: () -> Unit) {
    GlassPill(selected = selected, onClick = onClick) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = if (selected) Blue else Ink
        )
    }
}

/**
 * Large sampled-glass surface for feature panels and summaries.
 * Not used on each list item; the app caps expensive GPU refraction to visible UI chrome.
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    radius: Dp = 26.dp,
    tint: Color = Color(0xACFFFFFF),
    content: @Composable BoxScope.() -> Unit
) {
    val backdrop = LocalGlassBackdrop.current
    val shape = RoundedCornerShape(radius)
    Box(
        modifier.then(if (backdrop != null) {
            Modifier.drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(10.dp.toPx())
                    lens(19.dp.toPx(), 23.dp.toPx(), chromaticAberration = true)
                },
                highlight = { Highlight.Default.copy(alpha = .70f) },
                shadow = { Shadow(radius = 18.dp, alpha = .65f) },
                innerShadow = { InnerShadow(radius = 10.dp, alpha = .16f) },
                onDrawSurface = { drawRect(tint) }
            )
        } else Modifier.background(tint, shape)),
        content = content
    )
}

/**
 * Dynamic liquid dock: spring-following selector, long-press/drag to change tabs,
 * transient chromatic dispersion, refractive capsule and haptic snap.
 * Modeled after the interaction structure of Kyant's LiquidBottomTabs sample.
 */
@Composable
fun LiquidDock(
    backdrop: Backdrop,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val titles = listOf("发现", "放送", "季度", "收藏", "我的")
    val icons = listOf(
        Icons.Outlined.Home, Icons.Outlined.CalendarMonth, Icons.Outlined.AutoAwesome,
        Icons.Outlined.Bookmarks, Icons.Outlined.PersonOutline
    )
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    var dragging by remember { mutableStateOf(false) }
    var dragIndex by remember { mutableFloatStateOf(0f) }
    val effectiveIndex = if (dragging) dragIndex else selected.toFloat()
    val position by animateFloatAsState(
        targetValue = effectiveIndex.coerceIn(0f, 4f),
        animationSpec = spring(dampingRatio = .68f, stiffness = 460f),
        label = "Liquid navigation spring"
    )
    val press by animateFloatAsState(
        targetValue = if (dragging) 1f else 0f,
        animationSpec = spring(dampingRatio = .58f, stiffness = 520f),
        label = "Liquid drag press"
    )
    BoxWithConstraints(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 9.dp)
            .fillMaxWidth()
            .height(73.dp)
    ) {
        val padding = 6.dp
        val tabWidth = (maxWidth - padding * 2) / titles.size
        val tabWidthPx = with(density) { tabWidth.toPx() }
        val extraPx = with(density) { padding.toPx() }
        val shape = RoundedCornerShape(35.dp)
        // Draw the glass rail from the backdrop captured by the entire screen.
        Box(
            Modifier.fillMaxSize().drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(9.dp.toPx())
                    lens(22.dp.toPx(), 23.dp.toPx(), chromaticAberration = true)
                },
                highlight = { Highlight.Default.copy(alpha = .64f) },
                shadow = { Shadow(radius = 20.dp, alpha = .85f) },
                innerShadow = { InnerShadow(radius = 8.dp, alpha = .23f) },
                onDrawSurface = { drawRect(Color(0xACFFFFFF)) }
            )
        )
        // Draw the moving refractive capsule BEFORE icon labels so text stays sharp.
        Box(
            Modifier
                .offset {
                    IntOffset((extraPx + position * tabWidthPx).roundToInt(),
                        with(density) { 7.dp.roundToPx() })
                }
                .width(tabWidth)
                .height(59.dp)
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RoundedCornerShape(30.dp) },
                    effects = {
                        vibrancy()
                        blur((4f + press * 5f).dp.toPx())
                        lens(
                            (8f + press * 14f).dp.toPx(),
                            (12f + press * 15f).dp.toPx(),
                            chromaticAberration = true
                        )
                    },
                    highlight = { Highlight.Ambient.copy(alpha = .34f + press * .65f) },
                    shadow = { Shadow(radius = 12.dp, alpha = .36f + press * .5f) },
                    innerShadow = { InnerShadow(radius = 7.dp, alpha = .22f + press * .28f) },
                    layerBlock = {
                        scaleX = 1f + .13f * press
                        scaleY = 1f + .13f * press
                    },
                    onDrawSurface = { drawRect(Color(0x42EDF3FF)) }
                )

        )

        Row(
            Modifier.fillMaxSize().padding(horizontal = padding, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            titles.forEachIndexed { index, title ->
                val isSelected = index == selected
                Column(
                    Modifier.weight(1f).fillMaxHeight().clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (index != selected) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelected(index)
                    }
                    .then(if (isSelected) Modifier.pointerInput(tabWidthPx, selected) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                dragging = true
                                dragIndex = selected.toFloat()
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            onDragEnd = {
                                val next = dragIndex.roundToInt().coerceIn(0, 4)
                                dragging = false
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelected(next)
                            },
                            onDragCancel = { dragging = false },
                            onDrag = { change, movement ->
                                change.consume()
                                dragIndex = (dragIndex + movement.x / tabWidthPx)
                                    .coerceIn(0f, 4f)
                            }
                        )
                    } else Modifier),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        icons[index], title,
                        tint = if (isSelected) Blue else Slate,
                        modifier = Modifier.size(if (isSelected) 24.dp else 22.dp)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        title,
                        color = if (isSelected) Blue else Slate,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

    }
}
