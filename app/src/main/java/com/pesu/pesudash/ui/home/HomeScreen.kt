package com.pesu.pesudash.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pesu.pesudash.R
import com.pesu.pesudash.data.model.AttendanceSubject
import com.pesu.pesudash.data.model.ClassStatus
import com.pesu.pesudash.data.model.SeatingInfo
import com.pesu.pesudash.data.model.TodayClass
import com.pesu.pesudash.data.model.UserProfile
import com.pesu.pesudash.data.model.isOngoing
import com.pesu.pesudash.data.model.isToday
import com.pesu.pesudash.data.model.isTomorrow
import com.pesu.pesudash.ui.components.ShadcnBadge
import com.pesu.pesudash.ui.components.ShadcnCard
import com.pesu.pesudash.ui.theme.AppTheme
import com.pesu.pesudash.ui.theme.Inter
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    viewModel:              HomeViewModel,
    profile:                UserProfile,
    onNavigateToTimetable:  () -> Unit,
    onNavigateToAttendance: () -> Unit,
    onNavigateToSettings:   () -> Unit,
    modifier:               Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val c = AppTheme.colors

    LaunchedEffect(profile.userId) {
        viewModel.loadHome(profile.userId)
    }

    val todayDate = remember {
        SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
            .format(Calendar.getInstance().time)
    }

    val initials = remember(profile.name) {
        profile.name
            .split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")
            .ifEmpty { "?" }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(c.background),
        contentPadding = PaddingValues(
            start  = 20.dp,
            end    = 20.dp,
            top    = 20.dp,
            bottom = 32.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter            = painterResource(id = R.drawable.logo),
                    contentDescription = "PesuDash",
                    modifier           = Modifier.height(28.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = "PesuDash",
                    fontSize   = 20.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    color      = c.foreground,
                    modifier   = Modifier.weight(1f)
                )
                IconButton(
                    onClick  = { viewModel.reload(profile.userId) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint               = c.mutedFg,
                        modifier           = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick  = onNavigateToSettings,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint               = c.mutedFg,
                        modifier           = Modifier.size(18.dp)
                    )
                }
            }
        }

        item {
            ProfileCard(
                initials  = initials,
                name      = profile.name,
                srn       = profile.srn,
                className = profile.className,
                branch    = profile.branch,
                program   = profile.program,
                date      = todayDate
            )
        }

        if (state.isLoading) {
            item {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color       = c.mutedFg,
                        strokeWidth = 2.dp,
                        modifier    = Modifier.size(28.dp)
                    )
                }
            }
            return@LazyColumn
        }

        if (state.error != null) {
            item {
                ShadcnCard {
                    Column(
                        modifier            = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Something went wrong",
                            fontFamily = Inter,
                            fontWeight = FontWeight.Medium,
                            color      = c.foreground,
                            fontSize   = 14.sp
                        )
                        Text(
                            state.error!!,
                            fontFamily = Inter,
                            color      = c.dimFg,
                            fontSize   = 12.sp,
                            modifier   = Modifier.padding(top = 4.dp)
                        )
                        TextButton(onClick = { viewModel.reload(profile.userId) }) {
                            Text("Retry", fontFamily = Inter, color = c.foreground)
                        }
                    }
                }
            }
            return@LazyColumn
        }

        if (state.seatingInfo.isNotEmpty()) {
            item {
                SectionHeader(
                    title      = "Upcoming Exams",
                    actionText = null,
                    onAction   = {}
                )
            }
            items(state.seatingInfo) { exam ->
                SeatingCard(exam = exam)
            }
        }

        val nextClass = state.todayClasses.firstOrNull {
            it.status == ClassStatus.ONGOING
        } ?: state.todayClasses.firstOrNull {
            it.status == ClassStatus.UPCOMING
        }

        when {
            nextClass != null -> {
                item { NextClassCard(cls = nextClass, onClick = onNavigateToTimetable) }
            }
            state.holidayName != null -> {
                item { HolidayCard(name = state.holidayName!!) }
            }
            state.isWeekend -> {
                item { WeekendCard() }
            }
            state.todayClasses.isNotEmpty() -> {
                item { AllDoneCard() }
            }
        }

        if (state.todayClasses.isNotEmpty()) {
            item {
                SectionHeader(
                    title      = "Today's Classes",
                    actionText = "View timetable",
                    onAction   = onNavigateToTimetable
                )
            }
            items(state.todayClasses) { cls ->
                CompactClassRow(cls = cls)
            }
        }

        if (state.attendanceSubjects.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                SectionHeader(
                    title      = "Attendance",
                    actionText = "View all",
                    onAction   = onNavigateToAttendance
                )
            }

            val lowAttendance = state.attendanceSubjects
                .filter { (it.percentage ?: 100f) < 85f }
                .sortedBy { it.percentage }

            if (lowAttendance.isNotEmpty()) {
                items(lowAttendance.take(3)) { subject ->
                    AttendanceRow(subject = subject)
                }
            } else {
                item {
                    ShadcnCard {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "All subjects above 85%",
                                fontFamily = Inter,
                                fontSize   = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color      = c.foreground
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            SectionHeader(title = "Announcements", actionText = null, onAction = {})
        }
        item {
            ComingSoonCard(label = "Announcements")
        }
    }
}

@Composable
fun ComingSoonCard(label: String) {
    val c = AppTheme.colors
    ShadcnCard {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text       = label,
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 14.sp,
                color      = c.foreground
            )
            Text(
                text       = "Coming soon",
                fontFamily = Inter,
                fontSize   = 12.sp,
                color      = c.dimFg
            )
        }
    }
}

@Composable
private fun SeatingCard(exam: SeatingInfo) {
    val c = AppTheme.colors

    val isOngoing  = exam.isOngoing()
    val isToday    = exam.isToday()
    val isTomorrow = exam.isTomorrow()

    val accentColor = when {
        isOngoing  -> c.red
        isToday    -> c.orange
        isTomorrow -> c.yellow
        else       -> c.blue
    }

    val tagText = when {
        isOngoing  -> "Ongoing"
        isToday    -> "Today"
        isTomorrow -> "Tomorrow"
        else       -> "Upcoming"
    }

    val examDate  = remember(exam.testStartTime) {
        SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(exam.testStartTime))
    }
    val startTime = remember(exam.testStartTime) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(exam.testStartTime))
    }
    val endTime   = remember(exam.testEndTime) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(exam.testEndTime))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.card)
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShadcnBadge(text = tagText, color = accentColor)
                    Text(
                        text     = exam.assessmentName,
                        fontSize = 11.sp,
                        fontFamily = Inter,
                        color    = c.dimFg,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text       = exam.subjectName,
                    fontSize   = 16.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    color      = c.foreground,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    text     = exam.subjectCode,
                    fontSize = 11.sp,
                    fontFamily = Inter,
                    color    = c.dimFg,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    ExamInfoChip(label = examDate,                 color = c.mutedFg)
                    ExamInfoChip(label = "$startTime - $endTime",  color = c.mutedFg)
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    ExamInfoChip(label = "Room ${exam.roomName}",     color = accentColor)
                    ExamInfoChip(label = "Seat ${exam.terminalName}", color = accentColor)
                }
            }
        }
    }
}

@Composable
private fun ExamInfoChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text       = label,
            fontSize   = 11.sp,
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            color      = color
        )
    }
}

@Composable
private fun ProfileCard(
    initials:  String,
    name:      String,
    srn:       String,
    className: String,
    branch:    String,
    program:   String,
    date:      String
) {
    val c = AppTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.card)
            .border(1.dp, c.border, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier         = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(c.cardHover)
                    .border(1.dp, c.border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = initials,
                    fontSize   = 18.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    color      = c.foreground
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = name,
                    fontSize   = 16.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    color      = c.foreground
                )
                Text(
                    text     = srn,
                    fontSize = 12.sp,
                    fontFamily = Inter,
                    color    = c.mutedFg,
                    modifier = Modifier.padding(top = 1.dp)
                )
                val infoLine = buildString {
                    if (className.isNotBlank()) append(className)
                    if (branch.isNotBlank()) {
                        if (isNotEmpty()) append("  ·  ")
                        append(branch)
                    }
                }
                if (infoLine.isNotBlank()) {
                    Text(
                        text     = infoLine,
                        fontSize = 11.sp,
                        fontFamily = Inter,
                        color    = c.dimFg,
                        modifier = Modifier.padding(top = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier          = Modifier.padding(top = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(c.green)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text     = date,
                        fontSize = 11.sp,
                        fontFamily = Inter,
                        color    = c.dimFg
                    )
                }
            }
        }
    }
}

@Composable
private fun NextClassCard(cls: TodayClass, onClick: () -> Unit) {
    val c           = AppTheme.colors
    val isOngoing   = cls.status == ClassStatus.ONGOING
    val accentColor = if (isOngoing) c.blue else c.yellow

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.card)
            .border(1.dp, c.border, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(64.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShadcnBadge(
                        text  = if (isOngoing) "Ongoing" else "Up next",
                        color = accentColor
                    )
                    Text(
                        text       = "${formatTime(cls.startTime)} - ${formatTime(cls.endTime)}",
                        fontSize   = 12.sp,
                        fontFamily = Inter,
                        color      = c.dimFg
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text       = cls.subjectName,
                    fontSize   = 17.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    color      = c.foreground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text       = cls.roomName,
                    fontSize   = 12.sp,
                    fontFamily = Inter,
                    color      = c.dimFg
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint               = c.dimFg,
                modifier           = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun HolidayCard(name: String) {
    val c = AppTheme.colors
    ShadcnCard {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Holiday",
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 14.sp,
                color      = c.foreground
            )
            Text(
                name,
                fontFamily = Inter,
                fontSize   = 12.sp,
                color      = c.mutedFg
            )
        }
    }
}

@Composable
private fun WeekendCard() {
    val c = AppTheme.colors
    ShadcnCard {
        Text(
            "No classes today",
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            fontSize   = 14.sp,
            color      = c.foreground
        )
    }
}

@Composable
private fun AllDoneCard() {
    val c = AppTheme.colors
    ShadcnCard {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "All done for today!",
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 14.sp,
                color      = c.foreground
            )
            Text(
                "No more classes remaining",
                fontFamily = Inter,
                fontSize   = 12.sp,
                color      = c.mutedFg
            )
        }
    }
}

@Composable
private fun CompactClassRow(cls: TodayClass) {
    val c = AppTheme.colors
    val (dotColor, label) = when (cls.status) {
        ClassStatus.ATTENDED   -> c.green  to "Present"
        ClassStatus.BUNKED     -> c.red    to "Absent"
        ClassStatus.UPCOMING   -> c.yellow to "Upcoming"
        ClassStatus.ONGOING    -> c.blue   to "Ongoing"
        ClassStatus.NOT_MARKED -> c.orange to "Pending"
        ClassStatus.UNMARKED   -> c.dimFg  to "Unmarked"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.card)
            .border(1.dp, c.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = cls.subjectName,
                fontSize   = 14.sp,
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                color      = c.foreground,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text       = "${formatTime(cls.startTime)} - ${formatTime(cls.endTime)}  ·  ${cls.roomName}",
                fontSize   = 12.sp,
                fontFamily = Inter,
                color      = c.dimFg
            )
        }
        Spacer(Modifier.width(8.dp))
        ShadcnBadge(text = label, color = dotColor)
    }
}

@Composable
private fun AttendanceRow(subject: AttendanceSubject) {
    val c        = AppTheme.colors
    val pct      = subject.percentage ?: 0f
    val attended = subject.attended?.toInt() ?: 0
    val total    = subject.total ?: 0
    val pctColor = when {
        pct < 75f -> c.red
        pct < 85f -> c.yellow
        else      -> c.green
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.card)
            .border(1.dp, c.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = subject.subjectName,
                fontSize   = 13.sp,
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                color      = c.foreground,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress   = { (pct / 100f).coerceIn(0f, 1f) },
                    modifier   = Modifier
                        .width(80.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color      = pctColor,
                    trackColor = c.border
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "$attended/$total",
                    fontSize   = 11.sp,
                    fontFamily = Inter,
                    color      = c.dimFg
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text       = "${pct.toInt()}%",
            fontSize   = 18.sp,
            fontFamily = Inter,
            fontWeight = FontWeight.Bold,
            color      = pctColor
        )
    }
}

@Composable
private fun SectionHeader(title: String, actionText: String?, onAction: () -> Unit) {
    val c = AppTheme.colors
    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text       = title,
            fontSize   = 17.sp,
            fontFamily = Inter,
            fontWeight = FontWeight.SemiBold,
            color      = c.foreground,
            modifier   = Modifier.weight(1f)
        )
        if (actionText != null) {
            TextButton(
                onClick        = onAction,
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text       = actionText,
                    fontSize   = 13.sp,
                    fontFamily = Inter,
                    color      = c.mutedFg
                )
            }
        }
    }
}

private fun formatTime(time: String): String {
    return try {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val out = SimpleDateFormat("h:mm a", Locale.getDefault())
        out.format(sdf.parse(time)!!)
    } catch (e: Exception) { time }
}