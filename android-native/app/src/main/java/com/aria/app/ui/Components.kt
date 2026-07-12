package com.aria.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas

// ── Card ─────────────────────────────────────────────────────────────────────
@Composable
fun AriaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    val a = LocalAria.current
    var m = modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(a.surface)
        .border(1.dp, a.border, RoundedCornerShape(16.dp))
    if (onClick != null) m = m.clickable(onClick = onClick)
    Box(m.padding(padding)) { content() }
}

// ── Section header ─────────────────────────────────────────────────────────
@Composable
fun SectionHeader(title: String, right: String? = null, onClick: (() -> Unit)? = null) {
    val a = LocalAria.current
    Row(
        Modifier.fillMaxWidth().let { if (onClick != null) it.clickable(onClick = onClick) else it },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = a.text, modifier = Modifier.weight(1f))
        if (right != null) Text(right, style = MaterialTheme.typography.bodyMedium, color = a.textSecondary)
        if (onClick != null) {
            Spacer(Modifier.width(4.dp))
            Icon(Icons.Filled.ChevronRight, null, tint = a.textMuted, modifier = Modifier.size(18.dp))
        }
    }
}

// ── Label (uppercase caption) ─────────────────────────────────────────────
@Composable
fun FieldLabel(text: String) {
    Text(text, color = LocalAria.current.textMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
}

// ── Chip ─────────────────────────────────────────────────────────────────────
@Composable
fun Chip(label: String, color: Color? = null, icon: ImageVector? = null) {
    val a = LocalAria.current
    val fg = color ?: a.textSecondary
    val bg = if (color != null) color.copy(alpha = 0.14f) else a.surfaceAlt
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) Icon(icon, null, tint = fg, modifier = Modifier.size(13.dp))
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
    }
}

// ── ChipSelect (multi-option single choice) ──────────────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChipSelect(options: List<Pair<T, String>>, selected: T, colors: Map<T, Color> = emptyMap(), onSelect: (T) -> Unit) {
    val a = LocalAria.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            val active = value == selected
            val accent = colors[value] ?: a.primary
            Row(
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (active) accent.copy(alpha = 0.16f) else a.surfaceAlt)
                    .border(1.dp, if (active) accent else Color.Transparent, RoundedCornerShape(999.dp))
                    .clickable { onSelect(value) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, color = if (active) accent else a.textSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Segmented ────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Segmented(options: List<Pair<T, String>>, value: T, onChange: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (v, label) ->
            SegmentedButton(
                selected = v == value,
                onClick = { onChange(v) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
            ) { Text(label, fontSize = 13.sp, maxLines = 1) }
        }
    }
}

// ── Weekday picker ─────────────────────────────────────────────────────────
@Composable
fun WeekdayPicker(value: List<Int>, onChange: (List<Int>) -> Unit) {
    val a = LocalAria.current
    val labels = listOf("S", "M", "T", "W", "T", "F", "S")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        labels.forEachIndexed { i, l ->
            val active = i in value
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) a.primary else a.surfaceAlt)
                    .clickable {
                        onChange(if (active) value - i else (value + i).sorted())
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(l, color = if (active) a.onPrimary else a.textSecondary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Stepper ──────────────────────────────────────────────────────────────────
@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit, min: Int = 0, max: Int = 999, step: Int = 1, suffix: String? = null) {
    val a = LocalAria.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StepBtn(Icons.Filled.Remove, enabled = value > min) { onChange((value - step).coerceAtLeast(min)) }
        Text(
            if (suffix != null) "$value $suffix" else "$value",
            color = a.text, fontWeight = FontWeight.Bold, fontSize = 16.sp,
            modifier = Modifier.width(if (suffix != null) 84.dp else 40.dp), textAlign = TextAlign.Center,
        )
        StepBtn(Icons.Filled.Add, enabled = value < max) { onChange((value + step).coerceAtMost(max)) }
    }
}

@Composable
private fun StepBtn(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val a = LocalAria.current
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(a.surfaceAlt)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = if (enabled) a.primary else a.textMuted, modifier = Modifier.size(20.dp)) }
}

// ── Checkbox (circular, colored) ──────────────────────────────────────────────
@Composable
fun AriaCheckbox(checked: Boolean, color: Color? = null, onToggle: () -> Unit) {
    val a = LocalAria.current
    val tint = color ?: a.primary
    Box(
        Modifier.size(26.dp).clip(CircleShape)
            .background(if (checked) tint else Color.Transparent)
            .border(2.dp, if (checked) tint else a.borderStrong, CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) { if (checked) Icon(Icons.Filled.Check, null, tint = a.onPrimary, modifier = Modifier.size(16.dp)) }
}

// ── Ring (circular progress) ───────────────────────────────────────────────
@Composable
fun Ring(progress: Float, size: Dp, stroke: Dp, color: Color, content: @Composable () -> Unit = {}) {
    val a = LocalAria.current
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val sw = stroke.toPx()
            val inset = sw / 2
            val arcSize = androidx.compose.ui.geometry.Size(this.size.width - sw, this.size.height - sw)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(color = a.track, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width = sw, cap = StrokeCap.Round))
            drawArc(color = color, startAngle = -90f, sweepAngle = 360f * progress.coerceIn(0f, 1f), useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width = sw, cap = StrokeCap.Round))
        }
        content()
    }
}

// ── Progress bar ─────────────────────────────────────────────────────────────
@Composable
fun AriaProgressBar(progress: Float, color: Color? = null, modifier: Modifier = Modifier) {
    val a = LocalAria.current
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(999.dp)),
        color = color ?: a.primary,
        trackColor = a.track,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

// ── Stat tile ────────────────────────────────────────────────────────────────
@Composable
fun StatTile(label: String, value: String, icon: ImageVector, color: Color, sublabel: String? = null) {
    val a = LocalAria.current
    AriaCard(padding = 14.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Text(value, color = a.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(label, color = a.textSecondary, fontSize = 13.sp)
            if (sublabel != null) Text(sublabel, color = a.textMuted, fontSize = 12.sp)
        }
    }
}

// ── Bar chart ────────────────────────────────────────────────────────────────
data class ChartBar(val label: String, val value: Float, val highlight: Boolean = false)

@Composable
fun BarChart(bars: List<ChartBar>, color: Color, goal: Float = 0f, height: Dp = 130.dp) {
    val a = LocalAria.current
    val max = maxOf(goal, bars.maxOfOrNull { it.value } ?: 0f, 1f)
    Column {
        Box(Modifier.fillMaxWidth().height(height)) {
            if (goal > 0f) {
                val frac = (goal / max).coerceIn(0f, 1f)
                Box(
                    Modifier.fillMaxWidth().offset(y = height * (1f - frac)).height(1.dp).background(a.borderStrong),
                )
            }
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                bars.forEach { b ->
                    val frac = (b.value / max).coerceIn(0f, 1f)
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            Modifier.fillMaxWidth(0.64f).fillMaxHeight(if (frac <= 0f) 0.001f else frac)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (b.highlight) color else color.copy(alpha = 0.33f)),
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            bars.forEach { b ->
                Text(b.label, color = a.textMuted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
    }
}

// ── Empty state ──────────────────────────────────────────────────────────────
@Composable
fun EmptyState(icon: ImageVector, title: String, message: String? = null) {
    val a = LocalAria.current
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(a.surfaceAlt), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = a.textMuted, modifier = Modifier.size(34.dp))
        }
        Text(title, color = a.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        if (message != null) Text(message, color = a.textSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

// ── Buttons ──────────────────────────────────────────────────────────────
@Composable
fun PrimaryButton(label: String, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val a = LocalAria.current
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).background(if (enabled) a.primary else a.borderStrong)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 18.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, tint = a.onPrimary, modifier = Modifier.size(18.dp))
        Text(label, color = a.onPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
fun SecondaryButton(label: String, modifier: Modifier = Modifier, icon: ImageVector? = null, onClick: () -> Unit) {
    val a = LocalAria.current
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).background(a.primarySoft)
            .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, tint = a.primary, modifier = Modifier.size(18.dp))
        Text(label, color = a.primary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

@Composable
fun GhostButton(label: String, modifier: Modifier = Modifier, icon: ImageVector? = null, tint: Color? = null, onClick: () -> Unit) {
    val a = LocalAria.current
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, null, tint = tint ?: a.textSecondary, modifier = Modifier.size(18.dp))
        Text(label, color = tint ?: a.textSecondary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}

// ── Pill button used inside forms/bars ────────────────────────────────────
@Composable
fun SoftIconTile(icon: ImageVector, color: Color, size: Dp = 56.dp, iconSize: Dp = 24.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.13f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = color, modifier = Modifier.size(iconSize))
    }
}
