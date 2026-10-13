package com.example.bangumioauthdemo

import androidx.compose.foundation.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import org.json.JSONArray
import org.json.JSONObject

/**
 * Structured fallback presentation for the full public OpenAPI catalog:
 * always show meaningful cards and fields, not just a raw JSON textbox.
 * Domain-specific data is presented in the primary client screens.
 */
@Composable
fun SmartApiResult(raw: String) {
    var rawMode by remember(raw) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("可视化数据", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            GlassTag(if (rawMode) "返回可视化" else "原始 JSON", onClick = { rawMode = !rawMode })
        }
        if (rawMode) {
            Box(Modifier.fillMaxWidth().glassPanel().padding(12.dp)) {
                SelectionContainer { Text(raw, style = MaterialTheme.typography.bodySmall) }
            }
        } else {
            val trimmed = raw.trim()
            when {
                trimmed.startsWith("{") -> {
                    val json = try { JSONObject(trimmed) } catch (_: Exception) { null }
                    if (json != null) StructuredObject(json, 0)
                    else Text(raw, color = G.faint)
                }
                trimmed.startsWith("[") -> {
                    val arr = try { JSONArray(trimmed) } catch (_: Exception) { null }
                    if (arr != null) StructuredArray(arr, 0)
                    else Text(raw, color = G.faint)
                }
                else -> Text(raw.ifBlank { "无响应内容" }, color = G.faint)
            }
        }
    }
}

@Composable
private fun StructuredObject(obj: JSONObject, depth: Int) {
    if (depth >= 4) return
    val title = listOf("name_cn", "name", "title", "nickname", "username")
        .firstNotNullOfOrNull { key -> obj.str(key).takeIf { it.isNotBlank() } }
    val cover = (obj.optJSONObject("images") ?: obj.optJSONObject("avatar"))
        ?.let { it.str("large").ifBlank { it.str("medium") } }.orEmpty()
    Column(
        Modifier.fillMaxWidth().glassPanel(RoundedCornerShape(18.dp)).padding(13.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        if (title != null) Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        if (cover.isNotBlank() && depth == 0) {
            AsyncImage(
                model = cover.replace("http://", "https://"),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.height(160.dp).fillMaxWidth()
            )
        }
        val names = obj.keys().asSequence().toList()
        names.take(45).forEach { key ->
            val value = obj.opt(key)
            when (value) {
                is JSONObject -> {
                    if (key == "images" || key == "avatar") {
                        val image = value.str("large").ifBlank { value.str("medium") }
                        if (cover.isBlank() && image.isNotBlank()) AsyncImage(
                            image.replace("http://", "https://"), key,
                            Modifier.height(100.dp).fillMaxWidth(), contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(key, color = G.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        StructuredObject(value, depth + 1)
                    }
                }
                is JSONArray -> {
                    Text("$key · ${value.length()} 项", color = G.accent, fontWeight = FontWeight.Bold)
                    StructuredArray(value, depth + 1)
                }
                else -> {
                    if (value != null && value != JSONObject.NULL) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(key, Modifier.width(90.dp), color = G.faint, fontSize = 11.sp)
                            SelectionContainer {
                                Text(value.toString(), Modifier.weight(1f), color = G.ink,
                                    fontSize = 12.sp, maxLines = 9, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }
        if (names.size > 45) Text("另有 ${names.size - 45} 个字段，请切换原始 JSON 查看。",
            color = G.faint, fontSize = 11.sp)
    }
}

@Composable
private fun StructuredArray(arr: JSONArray, depth: Int) {
    if (depth >= 4) return
    if (arr.length() == 0) {
        Text("当前没有记录", color = G.faint)
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        for (i in 0 until minOf(arr.length(), 25)) {
            when (val child = arr.opt(i)) {
                is JSONObject -> StructuredObject(child, depth + 1)
                is JSONArray -> StructuredArray(child, depth + 1)
                else -> GlassTag(child?.toString().orEmpty())
            }
        }
        if (arr.length() > 25) {
            Text("这里先展示前 25 项，共 ${arr.length()} 项；完整数据可切换 JSON 查看。",
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
