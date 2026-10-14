package com.example.bangumioauthdemo

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.backdrop.shadow.InnerShadow
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Bangumi Glass material system: Kyant0/AndroidLiquidGlass (Apache-2.0).
 *
 * Parent screen captures the content once using Modifier.layerBackdrop.
 * Floating glass controls sample that captured layer; children of glass sheets
 * sample rememberCombinedBackdrop(parentBackdrop, exportedSheetBackdrop).
 * Never put layerBackdrop AFTER drawBackdrop on the same sheet: recursive
 * backdrop capture can cause RenderThread crashes.
 */
val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }
val LocalAnimePreviewHandler = staticCompositionLocalOf<((AnimeCard) -> Unit)?> { null }

@Composable
fun LiquidAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    radius: Dp = 24.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 15.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit
) {
    val backdrop = LocalGlassBackdrop.current
    val source = remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val pressed by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 360f),
        label = "liquidActionPress"
    )
    val shape = RoundedCornerShape(radius)
    val material = if (backdrop == null) {
        Modifier.background(
            if (selected) Color(0xE2D9E7FF) else Color(0xDDFDFDFF), shape
        )
    } else {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur((3f + pressed * 2f).dp.toPx())
                lens(
                    (12f + pressed * 8f).dp.toPx(),
                    (18f + pressed * 8f).dp.toPx(),
                    chromaticAberration = pressed > .03f
                )
            },
            highlight = {
                Highlight.Default.copy(alpha = .23f + pressed * .62f)
            },
            shadow = { Shadow(radius = 5.dp, alpha = .28f) },
            innerShadow = { InnerShadow(radius = 7.dp * pressed, alpha = pressed * .45f) },
            layerBlock = {
                // Critically: use drawBackdrop.layerBlock, NOT graphicsLayer
                // outside it, otherwise backdrop coordinates scale incorrectly.
                scaleX = 1f - pressed * .055f
                scaleY = 1f - pressed * .055f
            },
            onDrawSurface = {
                drawRect(if (selected) Color(0xACE6EDFF) else Color(0x91FFFFFF))
                if (pressed > .01f) drawRect(Color.White.copy(alpha = .08f * pressed))
            }
        )
    }
    Row(
        modifier
            .shadow(if (selected) 7.dp else 2.dp, shape, ambientColor = Blue.copy(alpha = .12f))
            .then(material)
            .border(1.dp, Color.White.copy(alpha = .83f), shape)
            .clip(shape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = source,
                indication = null,
                onClick = onClick
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        content = content
    )
}

@Composable
fun LiquidIconAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    LiquidAction(
        onClick = onClick,
        modifier = modifier.size(46.dp),
        selected = selected,
        radius = 23.dp,
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(icon, description, tint = if (selected) Blue else Ink,
            modifier = Modifier.size(22.dp))
    }
}

@Composable
fun LiquidTextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    selected: Boolean = false,
    enabled: Boolean = true
) {
    LiquidAction(onClick, modifier, selected, enabled, 23.dp) {
        if (icon != null) {
            Icon(icon, null, tint = if (selected) Blue else Ink,
                modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(7.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold,
            color = if (selected) Blue else Ink,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Moving refractive indicator, drag-to-switch, overshoot spring and
 * chromatic-aberration-on-press. Only one glass indicator is drawn for the rail
 * regardless of how many segments are present.
 */
@Composable
fun LiquidSegmented(
    titles: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    if (titles.isEmpty()) return
    val backdrop = LocalGlassBackdrop.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val safeIndex = selectedIndex.coerceIn(0, titles.lastIndex)
    val capsule = RoundedCornerShape(24.dp)
    val railBackdrop = rememberLayerBackdrop()
    var dragPx by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val dragPress by animateFloatAsState(
        if (dragging) 1f else 0f,
        spring(dampingRatio = .62f, stiffness = 340f),
        label = "segmentDrag"
    )
    val height = if (compact) 48.dp else 54.dp
    BoxWithConstraints(modifier.fillMaxWidth().height(height)) {
        val segmentWidth = (maxWidth - 8.dp) / titles.size
        val segmentPx = with(density) { segmentWidth.toPx() }
        val position by animateDpAsState(
            segmentWidth * safeIndex,
            spring(dampingRatio = .72f, stiffness = 400f),
            label = "segmentSelectedPosition"
        )
        // Rail material, captured separately to provide the refractive thumb a
        // combined backdrop instead of recursively sampling its own content.
        Box(
            Modifier.fillMaxSize().clip(capsule)
                .drawBackdropIfAvailable(backdrop, capsule, Color(0xADFFFFFF))
        )
        Box(Modifier.matchParentSize().layerBackdrop(railBackdrop).alpha(0f)) {
            Box(Modifier.fillMaxSize().background(Color(0x55E8EEFD), capsule))
        }
        val thumbBackdrop = if (backdrop != null) {
            rememberCombinedBackdrop(backdrop, railBackdrop)
        } else null
        Box(
            Modifier.padding(4.dp)
                .offset(x = position + with(density) { dragPx.toDp() })
                .width(segmentWidth)
                .fillMaxHeight()
                .then(if (thumbBackdrop == null) {
                    Modifier.background(Color(0xD9E1E9FF), RoundedCornerShape(21.dp))
                } else {
                    Modifier.drawBackdrop(
                        backdrop = thumbBackdrop,
                        shape = { RoundedCornerShape(21.dp) },
                        effects = {
                            vibrancy()
                            blur((2f + dragPress * 2f).dp.toPx())
                            lens(
                                (9f + dragPress * 4f).dp.toPx(),
                                (14f + dragPress * 7f).dp.toPx(),
                                chromaticAberration = dragPress > .05f
                            )
                        },
                        highlight = { Highlight.Default.copy(alpha = .35f + .55f * dragPress) },
                        shadow = { Shadow(radius = 6.dp, alpha = .35f) },
                        innerShadow = {
                            InnerShadow(radius = 7.dp * dragPress, alpha = dragPress)
                        },
                        layerBlock = {
                            scaleX = 1f + dragPress * .15f
                            scaleY = 1f - dragPress * .08f
                        },
                        onDrawSurface = {
                            drawRect(Color(0xB7DFE9FF))
                        }
                    )
                })
                .border(1.dp, Color.White.copy(alpha = .86f), RoundedCornerShape(21.dp))
        )
        Row(
            Modifier.fillMaxSize().padding(4.dp).pointerInput(safeIndex, segmentPx) {
                detectHorizontalDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = {
                        val next = (safeIndex + (dragPx / segmentPx).roundToInt())
                            .coerceIn(0, titles.lastIndex)
                        dragPx = 0f
                        dragging = false
                        if (next != safeIndex) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelected(next)
                        }
                    },
                    onDragCancel = { dragPx = 0f; dragging = false }
                ) { change, amount ->
                    change.consume()
                    dragPx = (dragPx + amount)
                        .coerceIn(-safeIndex * segmentPx,
                            (titles.lastIndex - safeIndex) * segmentPx)
                }
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            titles.forEachIndexed { index, name ->
                Box(
                    Modifier.weight(1f).fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(
                            role = Role.Tab,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (safeIndex != index) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelected(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        name,
                        color = if (safeIndex == index) Blue else Slate,
                        fontWeight = if (safeIndex == index) FontWeight.Bold else FontWeight.Medium,
                        style = if (compact) MaterialTheme.typography.labelSmall
                            else MaterialTheme.typography.labelMedium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun Modifier.drawBackdropIfAvailable(
    backdrop: Backdrop?,
    shape: RoundedCornerShape,
    tint: Color
): Modifier {
    return if (backdrop == null) background(tint, shape)
    else drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            blur(6.dp.toPx())
            lens(16.dp.toPx(), 17.dp.toPx())
        },
        onDrawSurface = { drawRect(tint) }
    )
}

/** Shared premium glass panel for key summaries, not every scrolling list row. */
@Composable
fun LiquidGlassPanel(
    modifier: Modifier = Modifier,
    radius: Dp = 25.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val backdrop = LocalGlassBackdrop.current
    val shape = RoundedCornerShape(radius)
    Column(
        modifier
            .then(if (backdrop == null) {
                Modifier.background(Color(0xEAFBFCFF), shape)
            } else {
                Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        vibrancy()
                        blur(9.dp.toPx())
                        lens(13.dp.toPx(), 17.dp.toPx())
                    },
                    highlight = { Highlight.Default.copy(alpha = .33f) },
                    shadow = { Shadow(radius = 9.dp, alpha = .25f) },
                    onDrawSurface = { drawRect(Color(0xBFFAFBFF)) }
                )
            })
            .border(1.dp, Color.White.copy(alpha = .88f), shape)
            .padding(17.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

/** Compact translucent toolbar; not part of the recorded page backdrop. */
@Composable
fun LiquidTopBar(
    title: String,
    leadingIcon: ImageVector? = null,
    onLeading: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    modifier: Modifier = Modifier
) {
    val backdrop = LocalGlassBackdrop.current
    val barBackdrop = rememberLayerBackdrop()
    val glassShape = RoundedCornerShape(30.dp)
    val shell = if (backdrop == null) {
        Modifier.background(Color(0xE6FFFFFF), glassShape)
    } else {
        Modifier.drawBackdrop(
            backdrop = backdrop,
            exportedBackdrop = barBackdrop,
            shape = { glassShape },
            effects = {
                vibrancy()
                blur(10.dp.toPx())
                lens(16.dp.toPx(), 19.dp.toPx())
            },
            highlight = { Highlight.Default.copy(alpha = .42f) },
            shadow = { Shadow(radius = 9.dp, alpha = .20f) },
            onDrawSurface = { drawRect(Color(0x9CF8FAFF)) }
        )
    }
    Row(
        modifier.fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(8.dp, glassShape)
            .then(shell)
            .border(1.dp, Color.White.copy(.9f), glassShape)
            .padding(horizontal = 13.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val composite = if (backdrop != null) {
            rememberCombinedBackdrop(backdrop, barBackdrop)
        } else null
        CompositionLocalProvider(LocalGlassBackdrop provides composite) {
            if (leadingIcon != null && onLeading != null) {
                LiquidIconAction(leadingIcon, "返回", onLeading, modifier = Modifier.size(38.dp))
            } else {
                Box(Modifier.size(10.dp).clip(RoundedCornerShape(50))
                    .background(Blue))
            }
            Text(
                title, Modifier.weight(1f), fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium, color = Ink, maxLines = 1
            )
            actions()
        }
    }
}

/**
 * Real same-window bottom sheet; records glass with exportedBackdrop to avoid
 * the recursive-backdrop crash described in Kyant's Glass Bottom Sheet guide.
 * The drag handle is the only draggable area, so text inputs can scroll safely.
 */
@Composable
fun LiquidBottomSheet(
    visible: Boolean,
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val backdrop = LocalGlassBackdrop.current
    val sheetBackdrop = rememberLayerBackdrop()
    var dragY by remember { mutableFloatStateOf(0f) }
    val drag by animateFloatAsState(
        dragY, spring(dampingRatio = .85f, stiffness = 420f),
        label = "sheetDrag"
    )
    BackHandler(visible) { onDismiss() }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(
            initialOffsetY = { it }, animationSpec = spring(dampingRatio = .85f)
        ),
        exit = fadeOut() + slideOutVertically(
            targetOffsetY = { it }, animationSpec = spring(dampingRatio = 1f)
        ),
        modifier = modifier.fillMaxSize()
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize()
                    .background(Color(0x4213243E))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onDismiss() }
            )
            val shape = RoundedCornerShape(topStart = 33.dp, topEnd = 33.dp)
            val surface = if (backdrop == null) {
                Modifier.background(Color(0xFFF0F4FF), shape)
            } else {
                Modifier.drawBackdrop(
                    backdrop = backdrop,
                    exportedBackdrop = sheetBackdrop,
                    shape = { shape },
                    effects = {
                        vibrancy()
                        blur(16.dp.toPx())
                        lens(18.dp.toPx(), 24.dp.toPx(), chromaticAberration = true)
                    },
                    highlight = { Highlight.Default.copy(alpha = .6f) },
                    shadow = { Shadow(radius = 20.dp, alpha = .25f) },
                    onDrawSurface = { drawRect(Color(0xDFFBFCFF)) }
                )
            }
            Column(
                Modifier.align(Alignment.BottomCenter)
                    .offset { IntOffset(0, drag.roundToInt()) }
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .heightIn(max = 720.dp)
                    .shadow(20.dp, shape)
                    .then(surface)
                    .border(1.dp, Color.White.copy(.96f), shape)
                    .padding(horizontal = 21.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.fillMaxWidth().height(31.dp)
                        .pointerInput(onDismiss) {
                            detectVerticalDragGestures(
                                onVerticalDrag = { change, amount ->
                                    change.consume()
                                    dragY = (dragY + amount).coerceIn(0f, 320f)
                                },
                                onDragEnd = {
                                    if (dragY > 95f) onDismiss()
                                    dragY = 0f
                                },
                                onDragCancel = { dragY = 0f }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.width(42.dp).height(5.dp)
                        .clip(RoundedCornerShape(100))
                        .background(Color(0x66909DB5)))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f),
                        color = Ink, style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    LiquidIconAction(Icons.Outlined.Close, "关闭弹层", onDismiss)
                }
                Spacer(Modifier.height(17.dp))
                val composite = if (backdrop != null) {
                    rememberCombinedBackdrop(backdrop, sheetBackdrop)
                } else null
                CompositionLocalProvider(LocalGlassBackdrop provides composite) {
                    content()
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}
