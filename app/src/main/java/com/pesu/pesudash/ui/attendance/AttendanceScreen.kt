package com.pesu.pesudash.ui.attendance

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pesu.pesudash.data.model.AttendanceSubject
import com.pesu.pesudash.ui.components.ShadcnCard
import com.pesu.pesudash.ui.theme.AppTheme
import com.pesu.pesudash.ui.theme.Inter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: AttendanceViewModel,
    userId: String,
    modifier: Modifier = Modifier
) {
    val state         by viewModel.state.collectAsState()
    val targetPct     by viewModel.targetPct.collectAsState()
    val semEndDate    by viewModel.semEndDate.collectAsState()
    val futureClasses by viewModel.futureClasses.collectAsState()
    val c = AppTheme.colors

    var showDatePicker   by remember { mutableStateOf(false) }
    var sliderValue      by remember(targetPct) { mutableFloatStateOf(targetPct) }

    LaunchedEffect(userId) { viewModel.load(userId) }

    if (showDatePicker) {
        SemesterEndDatePicker(
            currentEpochMs = semEndDate,
            onConfirm      = { ms ->
                viewModel.setSemEndDate(ms)
                showDatePicker = false
            },
            onDismiss      = { showDatePicker = false }
        )
    }

    Column(modifier = modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Attendance",
                    fontSize   = 22.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    color      = c.foreground
                )
                if (!state.isLoading && state.subjects.isNotEmpty()) {
                    Text(
                        "Overall: ${state.overallPercentage.toInt()}%",
                        fontSize   = 12.sp,
                        fontFamily = Inter,
                        color      = c.mutedFg
                    )
                }
            }
            IconButton(onClick = { viewModel.reload(userId) }) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint               = c.mutedFg,
                    modifier           = Modifier.size(18.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(c.card)
                .border(1.dp, c.border, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Target",
                    fontSize   = 12.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    color      = c.mutedFg,
                    modifier   = Modifier.width(48.dp)
                )
                Slider(
                    value         = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = { viewModel.setTargetPct(sliderValue) },
                    valueRange    = 50f..100f,
                    steps         = 49,
                    modifier      = Modifier.weight(1f),
                    colors        = SliderDefaults.colors(
                        thumbColor          = c.foreground,
                        activeTrackColor    = c.foreground,
                        inactiveTrackColor  = c.border
                    )
                )
                Text(
                    "${sliderValue.toInt()}%",
                    fontSize   = 13.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    color      = c.foreground,
                    modifier   = Modifier
                        .width(40.dp)
                        .padding(start = 8.dp)
                )
            }

            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier              = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Semester ends",
                    fontSize   = 12.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    color      = c.mutedFg
                )
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(c.cardHover)
                        .border(1.dp, c.border, RoundedCornerShape(8.dp))
                        .clickable { showDatePicker = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Pick date",
                        tint               = c.mutedFg,
                        modifier           = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (semEndDate > 0L)
                            SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                                .format(Date(semEndDate))
                        else "Not set",
                        fontSize   = 12.sp,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium,
                        color      = if (semEndDate > 0L) c.foreground else c.dimFg
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        when {
            state.isLoading -> {
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color       = c.mutedFg,
                        strokeWidth = 2.dp,
                        modifier    = Modifier.size(24.dp)
                    )
                }
            }

            state.error != null -> {
                Box(
                    modifier         = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Failed to load",
                            fontFamily = Inter,
                            color      = c.foreground,
                            fontSize   = 14.sp
                        )
                        TextButton(onClick = { viewModel.reload(userId) }) {
                            Text("Retry", fontFamily = Inter, color = c.foreground)
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier       = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start  = 16.dp,
                        end    = 16.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.subjects) { subject ->
                        SubjectAttendanceCard(
                            subject        = subject,
                            targetPct      = targetPct,
                            semEndDate     = semEndDate,
                            futureSlots    = futureClasses[subject.subjectCode] ?: -1
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SemesterEndDatePicker(
    currentEpochMs: Long,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val c           = AppTheme.colors
    val initialMs   = if (currentEpochMs > 0L) currentEpochMs else System.currentTimeMillis()
    val dateState   = rememberDatePickerState(initialSelectedDateMillis = initialMs)

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(c.card)
                .border(1.dp, c.border, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(
                "Semester end date",
                fontSize   = 15.sp,
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                color      = c.foreground,
                modifier   = Modifier.padding(bottom = 8.dp)
            )

            DatePicker(
                state  = dateState,
                colors = DatePickerDefaults.colors(
                    containerColor              = c.card,
                    titleContentColor           = c.mutedFg,
                    headlineContentColor        = c.foreground,
                    weekdayContentColor         = c.dimFg,
                    subheadContentColor         = c.mutedFg,
                    navigationContentColor      = c.foreground,
                    yearContentColor            = c.foreground,
                    currentYearContentColor     = c.green,
                    selectedYearContentColor    = c.background,
                    selectedYearContainerColor  = c.foreground,
                    dayContentColor             = c.foreground,
                    selectedDayContentColor     = c.background,
                    selectedDayContainerColor   = c.foreground,
                    todayContentColor           = c.green,
                    todayDateBorderColor        = c.green
                )
            )

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(
                        "Cancel",
                        fontFamily = Inter,
                        color      = c.dimFg
                    )
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        val ms = dateState.selectedDateMillis
                        if (ms != null) onConfirm(ms)
                    }
                ) {
                    Text(
                        "Confirm",
                        fontFamily = Inter,
                        fontWeight = FontWeight.SemiBold,
                        color      = c.foreground
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectAttendanceCard(
    subject:     AttendanceSubject,
    targetPct:   Float,
    semEndDate:  Long,
    futureSlots: Int
) {
    val c        = AppTheme.colors
    val pct      = subject.percentage ?: 0f
    val attended = subject.attended?.toInt() ?: 0
    val total    = subject.total ?: 0

    val pctColor = when {
        pct < targetPct && pct < 75f -> c.red
        pct < targetPct              -> c.yellow
        else                         -> c.green
    }

    val (adviceText, adviceColor) = when {
        total == 0 ->
            null to c.dimFg

        pct >= targetPct -> {
            val canSkip = if (futureSlots >= 0)
                calculateCanSkipWithFuture(attended, total, futureSlots, targetPct)
            else
                calculateCanSkip(attended, total, targetPct)
            when {
                canSkip > 0 && futureSlots >= 0 ->
                    "Can skip $canSkip of $futureSlots remaining classes" to c.green
                canSkip > 0 ->
                    "Can skip $canSkip more classes" to c.green
                else ->
                    "Attendance is tight, attend all remaining classes" to c.yellow
            }
        }

        else -> {
            val needed = calculateClassesNeeded(attended, total, targetPct)
            when {
                needed <= 0 ->
                    null to c.green

                futureSlots >= 0 -> when {

                    attended + futureSlots < ((total + futureSlots) * targetPct / 100f).toInt() + 1 ->
                        "You're cooked — need $needed, only $futureSlots classes left" to c.red
                    else ->
                        "Need $needed of $futureSlots remaining classes" to c.yellow
                }

                else ->
                    "Need $needed more to hit ${targetPct.toInt()}%" to c.yellow
            }
        }
    }

    ShadcnCard {
        Row(verticalAlignment = Alignment.CenterVertically) {

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(pctColor.copy(alpha = 0.1f))
                    .border(0.5.dp, pctColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${pct.toInt()}%",
                    fontSize   = 14.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    color      = pctColor
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    subject.subjectName,
                    fontSize   = 14.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    color      = c.foreground,
                    maxLines   = 2
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    subject.subjectCode,
                    fontSize   = 11.sp,
                    fontFamily = Inter,
                    color      = c.dimFg
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress   = { (pct / 100f).coerceIn(0f, 1f) },
                        modifier   = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color      = pctColor,
                        trackColor = c.border
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "$attended / $total",
                        fontSize   = 11.sp,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium,
                        color      = c.mutedFg
                    )
                }

                if (adviceText != null) {
                    Text(
                        text     = adviceText,
                        fontSize = 11.sp,
                        fontFamily = Inter,
                        fontWeight = if (adviceColor == c.red) FontWeight.SemiBold
                                     else FontWeight.Normal,
                        color    = adviceColor,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }
        }
    }
}

private fun calculateClassesNeeded(attended: Int, total: Int, targetPct: Float): Int {
    if (targetPct >= 100f) return Int.MAX_VALUE
    val t       = targetPct / 100.0
    val rawNeed = (t * total - attended) / (1.0 - t)
    return if (rawNeed <= 0) 0 else Math.ceil(rawNeed).toInt()
}

fun calculateCanSkipWithFuture(
    attended:    Int,
    total:       Int,
    futureSlots: Int,
    targetPct:   Float
): Int {
    if (targetPct >= 100f) return 0
    val finalTotal       = total + futureSlots
    val requiredAttended = Math.ceil(finalTotal * targetPct / 100.0).toInt()
    val maxAbsences      = finalTotal - requiredAttended
    val alreadyAbsent    = total - attended
    return (maxAbsences - alreadyAbsent).coerceAtLeast(0)
}

private fun calculateCanSkip(attended: Int, total: Int, targetPct: Float): Int {
    return calculateCanSkipWithFuture(attended, total, 0, targetPct)
}