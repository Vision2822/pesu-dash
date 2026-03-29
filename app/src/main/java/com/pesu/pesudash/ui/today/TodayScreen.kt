package com.pesu.pesudash.ui.today

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pesu.pesudash.data.model.ClassStatus
import com.pesu.pesudash.data.model.TodayClass
import com.pesu.pesudash.ui.components.ShadcnBadge
import com.pesu.pesudash.ui.components.ShadcnCard
import com.pesu.pesudash.ui.theme.AppTheme
import com.pesu.pesudash.ui.theme.Inter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    viewModel: TodayViewModel,
    userId: String,
    modifier: Modifier = Modifier
) {
    val state        by viewModel.state.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val c = AppTheme.colors

    val dates = remember {
        val list = mutableListOf<Calendar>()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -13)
        }
        repeat(14) {
            list.add(cal.clone() as Calendar)
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        list
    }

    val todayIndex   = dates.size - 1
    val listState    = rememberLazyListState()
    val snapBehavior = rememberSnapFlingBehavior(listState)
    val scope        = rememberCoroutineScope()

    var selectedIndex by remember { mutableIntStateOf(todayIndex) }

    LaunchedEffect(Unit) {
        listState.scrollToItem(todayIndex)
        viewModel.loadForDate(userId, dates[todayIndex], debounce = false)
    }

    val centeredIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo
                .minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }
                ?.index ?: selectedIndex
        }
    }

    LaunchedEffect(centeredIndex) {
        if (centeredIndex != selectedIndex) {
            selectedIndex = centeredIndex
            viewModel.loadForDate(userId, dates[centeredIndex], debounce = true)
        }
    }

    val contentAlpha by animateFloatAsState(
        targetValue   = if (isRefreshing) 0.35f else 1f,
        animationSpec = tween(300),
        label         = "contentAlpha"
    )

    val dayDate = remember(selectedIndex) {
        val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(dates[selectedIndex].time)
        val dateStr = SimpleDateFormat("MMMM d", Locale.getDefault()).format(dates[selectedIndex].time)
        "$dayName, $dateStr"
    }

    Scaffold(
        modifier       = modifier,
        containerColor = c.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text       = "Timetable",
                            fontSize   = 22.sp,
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            color      = c.foreground
                        )
                        Text(
                            text       = dayDate,
                            fontSize   = 12.sp,
                            fontFamily = Inter,
                            color      = c.dimFg
                        )
                    }
                },
                actions = {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            color       = c.mutedFg,
                            modifier    = Modifier.size(18.dp),
                            strokeWidth = 1.5.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    } else {
                        IconButton(onClick = {
                            viewModel.refresh(userId, dates[selectedIndex])
                        }) {
                            Icon(
                                Icons.Default.Refresh,
                                "Refresh",
                                tint     = c.mutedFg,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.background)
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start  = 16.dp,
                end    = 16.dp,
                top    = 4.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                DateCarousel(
                    dates         = dates,
                    selectedIndex = selectedIndex,
                    listState     = listState,
                    snapBehavior  = snapBehavior,
                    onCardClick   = { index ->
                        scope.launch { listState.animateScrollToItem(index) }
                    }
                )
                Spacer(Modifier.height(4.dp))
            }

            when (val s = state) {

                is TodayUiState.Idle -> {}

                is TodayUiState.Weekend -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp)
                                .alpha(contentAlpha),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No classes",
                                fontSize   = 18.sp,
                                fontFamily = Inter,
                                fontWeight = FontWeight.SemiBold,
                                color      = c.foreground
                            )
                            Text(
                                "Weekend or holiday",
                                fontFamily = Inter,
                                color      = c.dimFg,
                                fontSize   = 13.sp,
                                modifier   = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

                is TodayUiState.Holiday -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp)
                                .alpha(contentAlpha),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape  = RoundedCornerShape(8.dp),
                                color  = c.red.copy(alpha = 0.12f),
                                border = BorderStroke(0.5.dp, c.red.copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text       = s.eventType,
                                    color      = c.red,
                                    fontSize   = 11.sp,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Medium,
                                    modifier   = Modifier.padding(
                                        horizontal = 10.dp,
                                        vertical   = 4.dp
                                    )
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text       = s.name,
                                fontSize   = 20.sp,
                                fontFamily = Inter,
                                fontWeight = FontWeight.SemiBold,
                                color      = c.foreground
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text       = "No classes today",
                                fontFamily = Inter,
                                color      = c.dimFg,
                                fontSize   = 13.sp
                            )
                        }
                    }
                }

                is TodayUiState.Error -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 48.dp)
                                .alpha(contentAlpha),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Something went wrong",
                                color      = c.foreground,
                                fontFamily = Inter,
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                s.message,
                                color      = c.dimFg,
                                fontFamily = Inter,
                                fontSize   = 12.sp,
                                modifier   = Modifier.padding(top = 4.dp)
                            )
                            TextButton(onClick = {
                                viewModel.refresh(userId, dates[selectedIndex])
                            }) {
                                Text(
                                    "Retry",
                                    color      = c.foreground,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                is TodayUiState.Success -> {
                    val classes   = s.classes
                    val attended  = classes.count { it.status == ClassStatus.ATTENDED }
                    val upcoming  = classes.count {
                        it.status == ClassStatus.UPCOMING || it.status == ClassStatus.ONGOING
                    }
                    val bunked    = classes.count { it.status == ClassStatus.BUNKED }
                    val notMarked = classes.count { it.status == ClassStatus.NOT_MARKED }

                    item {
                        SummaryRow(
                            attended  = attended,
                            upcoming  = upcoming,
                            bunked    = bunked,
                            notMarked = notMarked,
                            modifier  = Modifier.alpha(contentAlpha)
                        )
                        Spacer(Modifier.height(4.dp))
                    }

                    items(classes) { cls ->
                        ClassCard(
                            cls      = cls,
                            modifier = Modifier.alpha(contentAlpha)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DateCarousel(
    dates: List<Calendar>,
    selectedIndex: Int,
    listState: androidx.compose.foundation.lazy.LazyListState,
    snapBehavior: androidx.compose.foundation.gestures.FlingBehavior,
    onCardClick: (Int) -> Unit
) {
    val c = AppTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
    ) {
        LazyRow(
            state                 = listState,
            flingBehavior         = snapBehavior,
            modifier              = Modifier.fillMaxSize(),
            contentPadding        = PaddingValues(horizontal = 148.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            itemsIndexed(dates) { index, cal ->
                DateCard(
                    calendar   = cal,
                    isSelected = index == selectedIndex,
                    onClick    = { onCardClick(index) }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(120.dp)
                .align(Alignment.CenterStart)
                .background(
                    Brush.horizontalGradient(listOf(c.background, Color.Transparent))
                )
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(120.dp)
                .align(Alignment.CenterEnd)
                .background(
                    Brush.horizontalGradient(listOf(Color.Transparent, c.background))
                )
        )
    }
}

@Composable
fun DateCard(
    calendar: Calendar,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val c = AppTheme.colors

    val scale by animateFloatAsState(
        targetValue   = if (isSelected) 1f else 0.78f,
        animationSpec = tween(200),
        label         = "scale"
    )
    val alpha by animateFloatAsState(
        targetValue   = if (isSelected) 1f else 0.3f,
        animationSpec = tween(200),
        label         = "alpha"
    )

    val month = SimpleDateFormat("MMM", Locale.getDefault()).format(calendar.time).uppercase()
    val day   = SimpleDateFormat("d",   Locale.getDefault()).format(calendar.time)
    val dow   = SimpleDateFormat("EEE", Locale.getDefault()).format(calendar.time).uppercase()

    val isToday = run {
        val today = Calendar.getInstance()
        calendar.get(Calendar.YEAR)        == today.get(Calendar.YEAR) &&
        calendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    Card(
        onClick  = onClick,
        modifier = Modifier
            .width(62.dp)
            .height(78.dp)
            .scale(scale)
            .alpha(alpha),
        shape  = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) c.card else c.background
        ),
        border = if (isSelected) BorderStroke(1.dp, c.border) else null
    ) {
        Column(
            modifier            = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text       = dow,
                fontSize   = 9.sp,
                fontFamily = Inter,
                color      = if (isSelected) c.mutedFg else c.dimFg,
                fontWeight = FontWeight.Medium
            )
            Text(
                text       = day,
                fontSize   = if (isSelected) 26.sp else 20.sp,
                fontFamily = Inter,
                fontWeight = FontWeight.Black,
                color      = when {
                    isSelected && isToday -> c.green
                    else                  -> c.foreground
                }
            )
            Text(
                text       = month,
                fontSize   = 9.sp,
                fontFamily = Inter,
                color      = if (isSelected) c.mutedFg else c.dimFg,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun SummaryRow(
    attended: Int,
    upcoming: Int,
    bunked: Int,
    notMarked: Int,
    modifier: Modifier = Modifier
) {
    val c = AppTheme.colors

    Row(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard("Present",  attended,  c.green,  Modifier.weight(1f))
        StatCard("Upcoming", upcoming,  c.yellow, Modifier.weight(1f))
        StatCard("Absent",   bunked,    c.red,    Modifier.weight(1f))
        if (notMarked > 0) {
            StatCard("Pending", notMarked, c.orange, Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(label: String, count: Int, color: Color, modifier: Modifier) {
    val c = AppTheme.colors

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(c.card)
            .border(1.dp, c.border, RoundedCornerShape(10.dp))
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                count.toString(),
                fontSize   = 22.sp,
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                color      = color
            )
            Text(
                label,
                fontSize   = 10.sp,
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                color      = c.dimFg,
                modifier   = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun ClassCard(cls: TodayClass, modifier: Modifier = Modifier) {
    val c = AppTheme.colors

    val (dotColor, badgeLabel) = when (cls.status) {
        ClassStatus.ATTENDED   -> c.green  to "Present"
        ClassStatus.BUNKED     -> c.red    to "Absent"
        ClassStatus.UPCOMING   -> c.yellow to "Upcoming"
        ClassStatus.ONGOING    -> c.blue   to "Ongoing"
        ClassStatus.NOT_MARKED -> c.orange to "Pending"
    }

    ShadcnCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(dotColor.copy(alpha = 0.7f))
            )

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    cls.subjectName,
                    fontSize   = 14.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    color      = c.foreground
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${formatTime(cls.startTime)} – ${formatTime(cls.endTime)}  ·  ${cls.roomName}",
                    fontSize   = 12.sp,
                    fontFamily = Inter,
                    color      = c.dimFg
                )
                if (cls.totalCount > 0) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress   = { (cls.percentage / 100f).coerceIn(0f, 1f) },
                            modifier   = Modifier
                                .width(48.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color      = if (cls.percentage < 75f) c.red else c.green,
                            trackColor = c.border
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${cls.attendedCount}/${cls.totalCount} (${cls.percentage.toInt()}%)",
                            fontSize   = 11.sp,
                            fontFamily = Inter,
                            color      = if (cls.percentage < 75f) c.red else c.mutedFg
                        )
                    }
                }
            }

            ShadcnBadge(text = badgeLabel, color = dotColor)
        }
    }
}

private fun formatTime(time: String): String {
    return try {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val out = SimpleDateFormat("h:mm a", Locale.getDefault())
        out.format(sdf.parse(time)!!)
    } catch (e: Exception) {
        time
    }
}