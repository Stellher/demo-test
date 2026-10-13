package com.example.bangumioauthdemo

import android.os.Build
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens

object G {
    val ink = Color(0xFFF7F8FC)
    val faint = Color(0xFFAAB4CD)
    val subdued = Color(0xFF6E7A97)
    val night = Color(0xFF090D1B)
    val panel = Color(0xFF171D32)
    val accent = Color(0xFF91B8FF)
    val lavender = Color(0xFFC3AAFF)
    val mint = Color(0xFF89E4D5)
    val gold = Color(0xFFF7CE9B)
    val border = Color.White.copy(alpha = .15f)
    val scrim = Color(0xFF0B1125)
}

val GlassTypography = Typography(
    headlineLarge = androidx.compose.ui.text.TextStyle(
        color = G.ink, fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp
    ),
    headlineMedium = androidx.compose.ui.text.TextStyle(
        color = G.ink, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp
    ),
    titleLarge = androidx.compose.ui.text.TextStyle(
        color = G.ink, fontSize = 21.sp, fontWeight = FontWeight.SemiBold
    ),
    titleMedium = androidx.compose.ui.text.TextStyle(
        color = G.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold
    ),
    bodyLarge = androidx.compose.ui.text.TextStyle(color = G.ink, fontSize = 15.sp),
    bodyMedium = androidx.compose.ui.text.TextStyle(color = G.ink, fontSize = 14.sp),
    bodySmall = androidx.compose.ui.text.TextStyle(color = G.faint, fontSize = 12.sp),
    labelLarge = androidx.compose.ui.text.TextStyle(color = G.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = androidx.compose.ui.text.TextStyle(color = G.faint, fontSize = 11.sp)
)

private val GlassScheme = darkColorScheme(
    primary = G.accent,
    secondary = G.lavender,
    tertiary = G.mint,
    background = G.night,
    surface = G.panel,
    surfaceVariant = Color(0xFF212940),
    onSurface = G.ink,
    onBackground = G.ink,
    onSurfaceVariant = G.faint,
    outline = G.subdued
)

@Composable
fun GlassTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GlassScheme, typography = GlassTypography, content = content)
}

@Composable
fun AuroraBackground(modifier: Modifier = Modifier) {
    Box(
        modifier.background(
            Brush.verticalGradient(
                listOf(Color(0xFF1A2444), Color(0xFF101A32), G.night, Color(0xFF080C17))
            )
        )
    ) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(G.lavender.copy(alpha = .16f), Color.Transparent),
                    center = Offset(size.width * .9f, size.height * .08f),
                    radius = size.width * .84f
                ),
                center = Offset(size.width * .9f, size.height * .08f),
                radius = size.width * .84f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(G.accent.copy(alpha = .12f), Color.Transparent),
                    center = Offset(size.width * .06f, size.height * .55f),
                    radius = size.width * .85f
                ),
                center = Offset(size.width * .06f, size.height * .55f),
                radius = size.width * .85f
            )
        }
    }
}

fun Modifier.glassPanel(
    shape: Shape = RoundedCornerShape(24.dp),
    opacity: Float = .11f
): Modifier = this
    .clip(shape)
    .background(
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = opacity + .065f),
                Color.White.copy(alpha = opacity * .45f),
                Color(0xFF9CAFE5).copy(alpha = opacity * .4f)
            )
        )
    )
    .border(1.dp, Brush.verticalGradient(
        listOf(Color.White.copy(alpha = .27f), Color.White.copy(alpha = .08f)),
    ), shape)

@Composable
fun KyantGlass(
    backdrop: Backdrop,
    shape: Shape = RoundedCornerShape(32.dp),
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.drawBackdrop(
                        backdrop = backdrop,
                        shape = { shape },
                        effects = {
                            blur(14.dp.toPx())
                            lens(8.dp.toPx(), 7.dp.toPx())
                        },
                        onDrawSurface = {
                            drawRect(G.panel.copy(alpha = .40f))
                        }
                    )
                } else {
                    Modifier.background(G.panel.copy(alpha = .94f), shape)
                }
            )
            .border(1.dp, Brush.verticalGradient(
                listOf(Color.White.copy(alpha = .35f), Color.White.copy(alpha = .07f))
            ), shape)
            .clip(shape),
        content = content
    )
}

@Composable
fun GlassTag(
    text: String,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val color = if (selected) G.accent else G.ink
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .clip(shape)
            .background(if (selected) G.accent.copy(alpha = .23f) else Color.White.copy(alpha = .075f))
            .border(1.dp, if (selected) G.accent.copy(alpha = .60f) else G.border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelLarge, maxLines = 1)
    }
}

@Composable
fun PrimaryGlassButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(G.accent.copy(alpha = if (enabled) .96f else .30f),
                        G.lavender.copy(alpha = if (enabled) .94f else .30f))
                )
            )
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color(0xFF0E1424), fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun SectionTitle(
    heading: String, caption: String = "",
    action: String? = null, onAction: (() -> Unit)? = null
) {
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(heading, style = MaterialTheme.typography.titleLarge)
            if (caption.isNotBlank()) Text(caption, style = MaterialTheme.typography.bodySmall)
        }
        if (action != null && onAction != null) {
            Text(action, color = G.accent, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clickable(onClick = onAction).padding(6.dp))
        }
    }
}

@Composable
fun ScreenTitle(eyebrow: String, title: String, description: String = "") {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(eyebrow.uppercase(), color = G.mint, fontSize = 11.sp,
            letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        Text(title, style = MaterialTheme.typography.headlineLarge)
        if (description.isNotBlank()) Text(description, color = G.faint, fontSize = 13.sp)
    }
}

@Composable
fun Poster(
    subject: AnimeSubject,
    modifier: Modifier = Modifier,
    rounded: Shape = RoundedCornerShape(17.dp)
) {
    Box(
        modifier
            .clip(rounded)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF3B4A73), Color(0xFF21283F), Color(0xFF67729B))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (subject.image.isNotBlank()) {
            AsyncImage(
                model = subject.image,
                contentDescription = subject.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text("B.", color = G.faint, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun SmallPoster(
    src: String, title: String, modifier: Modifier = Modifier
) {
    Box(
        modifier.clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF48516A), G.panel))),
        contentAlignment = Alignment.Center
    ) {
        if (src.isNotBlank()) {
            AsyncImage(model = src, contentDescription = title,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else Text("B.", color = G.faint, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun AnimePosterTile(
    subject: AnimeSubject, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Column(
        modifier.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box {
            Poster(subject, Modifier.fillMaxWidth().aspectRatio(.71f))
            if (subject.score > 0) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(7.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(G.night.copy(alpha = .82f))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text("★ %.1f".format(subject.score), color = G.gold,
                        style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Text(subject.displayName, maxLines = 2, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium, fontSize = 13.sp, lineHeight = 18.sp)
        Text(subject.date.ifBlank { if (subject.rank > 0) "排名 #${subject.rank}" else "Bangumi" },
            style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}

@Composable
fun AnimeListRow(
    subject: AnimeSubject, modifier: Modifier = Modifier,
    subtitle: String = "", trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Poster(subject, Modifier.width(74.dp).height(98.dp), RoundedCornerShape(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(subject.displayName, style = MaterialTheme.typography.titleMedium,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle.ifBlank { subject.date },
                color = G.faint, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subject.score > 0) {
                Text("★ %.1f".format(subject.score) +
                        if (subject.rank > 0) "   ·   #${subject.rank}" else "",
                    color = G.gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        trailing?.invoke() ?: Text("›", color = G.faint, fontSize = 24.sp)
    }
}

@Composable
fun ErrorInline(text: String?, retry: (() -> Unit)? = null) {
    if (text.isNullOrBlank()) return
    Row(
        Modifier.fillMaxWidth().glassPanel().padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text, Modifier.weight(1f), fontSize = 12.sp, color = G.gold)
        if (retry != null) GlassTag("重试", onClick = retry)
    }
}

@Composable
fun BusyRow(label: String = "正在载入 Bangumi 数据…") {
    Row(Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
            color = G.accent)
        Spacer(Modifier.width(12.dp))
        Text(label, color = G.faint, fontSize = 12.sp)
    }
}

@Composable
fun VerticalDividerLine() {
    HorizontalDivider(color = Color.White.copy(alpha = .09f), thickness = .6.dp)
}

@Composable
fun GlassEmpty(title: String, detail: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Column(
        Modifier.fillMaxWidth().glassPanel().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("✧", color = G.accent, fontSize = 33.sp)
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(detail, color = G.faint, fontSize = 12.sp)
        if (action != null && onAction != null) PrimaryGlassButton(action, onClick = onAction)
    }
}

@Composable
fun StateLabel(title: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = G.faint, fontSize = 11.sp)
        Text(value, color = G.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
