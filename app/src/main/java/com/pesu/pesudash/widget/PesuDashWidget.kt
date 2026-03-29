package com.pesu.pesudash.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.pesu.pesudash.MainActivity
import com.pesu.pesudash.data.model.ClassStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class PesuDashWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(180.dp, 110.dp),
            DpSize(250.dp, 110.dp),
            DpSize(350.dp, 200.dp),
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = withContext(Dispatchers.IO) {
            WidgetStateStore.load(context)
        }

        provideContent {
            val size    = LocalSize.current
            val isSmall = size.width < 250.dp

            if (isSmall) SmallWidget(data, context)
            else         LargeWidget(data, context)
        }
    }
}

private val Bg          = ColorProvider(Color(0xFF09090B))
private val CardBg      = ColorProvider(Color(0xFF0F0F12))
private val Border      = ColorProvider(Color(0xFF27272A))
private val Foreground  = ColorProvider(Color(0xFFFAFAFA))
private val MutedFg     = ColorProvider(Color(0xFFA1A1AA))
private val DimFg       = ColorProvider(Color(0xFF71717A))
private val GreenColor  = ColorProvider(Color(0xFF22C55E))
private val RedColor    = ColorProvider(Color(0xFFEF4444))
private val YellowColor = ColorProvider(Color(0xFFEAB308))
private val OrangeColor = ColorProvider(Color(0xFFF97316))
private val BlueColor   = ColorProvider(Color(0xFF3B82F6))

private fun statusColor(status: ClassStatus): ColorProvider = when (status) {
    ClassStatus.ATTENDED   -> GreenColor
    ClassStatus.BUNKED     -> RedColor
    ClassStatus.UPCOMING   -> YellowColor
    ClassStatus.ONGOING    -> BlueColor
    ClassStatus.NOT_MARKED -> OrangeColor
    ClassStatus.UNMARKED   -> DimFg
}

private fun statusLabel(status: ClassStatus): String = when (status) {
    ClassStatus.ATTENDED   -> "Present"
    ClassStatus.BUNKED     -> "Absent"
    ClassStatus.UPCOMING   -> "Upcoming"
    ClassStatus.ONGOING    -> "Ongoing"
    ClassStatus.NOT_MARKED -> "Pending"
    ClassStatus.UNMARKED   -> "Unmarked"
}

private fun formatTime(time: String): String {
    return try {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val out = SimpleDateFormat("h:mm a", Locale.getDefault())
        out.format(sdf.parse(time)!!)
    } catch (e: Exception) { time }
}

private fun openAppAction(context: Context) =
    actionStartActivity(
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    )

@Composable
private fun SmallWidget(data: WidgetData, context: Context) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Bg)
            .clickable(openAppAction(context))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        when {
            data.isLoggedOut -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Pesu Dash",
                        style = TextStyle(
                            color      = Foreground,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        "Tap to login",
                        style = TextStyle(color = DimFg, fontSize = 11.sp)
                    )
                }
            }

            data.isLoading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Pesu Dash",
                        style = TextStyle(
                            color      = Foreground,
                            fontSize   = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    Text(
                        "Loading...",
                        style = TextStyle(color = DimFg, fontSize = 11.sp)
                    )
                }
            }

            else -> {
                Column(
                    modifier            = GlanceModifier.fillMaxSize(),
                    verticalAlignment   = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Today",
                        style = TextStyle(
                            color      = MutedFg,
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )

                    Spacer(GlanceModifier.height(8.dp))

                    Row(horizontalAlignment = Alignment.CenterHorizontally) {
                        SmallStat(data.attended,  GreenColor,  "Present")
                        Spacer(GlanceModifier.width(12.dp))
                        SmallStat(data.upcoming,  YellowColor, "Soon")
                        Spacer(GlanceModifier.width(12.dp))
                        SmallStat(data.bunked,    RedColor,    "Absent")
                        if (data.notMarked > 0) {
                            Spacer(GlanceModifier.width(12.dp))
                            SmallStat(data.notMarked, OrangeColor, "Pending")
                        }
                    }

                    Spacer(GlanceModifier.height(10.dp))

                    Row(
                        modifier            = GlanceModifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalAlignment   = Alignment.CenterVertically
                    ) {
                        Text(
                            data.lastUpdated.ifEmpty { "Not synced" },
                            style = TextStyle(color = DimFg, fontSize = 9.sp)
                        )
                        Spacer(GlanceModifier.width(6.dp))
                        Image(
                            provider           = ImageProvider(android.R.drawable.ic_popup_sync),
                            contentDescription = "Refresh",
                            modifier           = GlanceModifier
                                .size(12.dp)
                                .clickable(actionRunCallback<RefreshWidgetCallback>())
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallStat(count: Int, color: ColorProvider, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            count.toString(),
            style = TextStyle(
                color      = color,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            label,
            style = TextStyle(
                color    = DimFg,
                fontSize = 9.sp
            )
        )
    }
}

@Composable
private fun LargeWidget(data: WidgetData, context: Context) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Bg)
            .padding(14.dp)
    ) {
        Row(
            modifier          = GlanceModifier
                .fillMaxWidth()
                .clickable(openAppAction(context)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Pesu Dash",
                style = TextStyle(
                    color      = Foreground,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                data.lastUpdated.ifEmpty { "--:--" },
                style = TextStyle(color = DimFg, fontSize = 9.sp)
            )
            Spacer(GlanceModifier.width(6.dp))
            Image(
                provider           = ImageProvider(android.R.drawable.ic_popup_sync),
                contentDescription = "Refresh",
                modifier           = GlanceModifier
                    .size(14.dp)
                    .clickable(actionRunCallback<RefreshWidgetCallback>())
            )
        }

        Spacer(GlanceModifier.height(2.dp))

        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Border)
        ) {}

        Spacer(GlanceModifier.height(10.dp))

        when {
            data.isLoggedOut -> {
                Box(
                    modifier         = GlanceModifier
                        .fillMaxSize()
                        .clickable(openAppAction(context)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Tap to login",
                        style = TextStyle(color = DimFg, fontSize = 13.sp)
                    )
                }
            }

            data.isLoading -> {
                Box(
                    modifier         = GlanceModifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Loading...",
                        style = TextStyle(color = DimFg, fontSize = 13.sp)
                    )
                }
            }

            data.classes.isEmpty() -> {
                Box(
                    modifier         = GlanceModifier
                        .fillMaxSize()
                        .clickable(openAppAction(context)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No classes today",
                        style = TextStyle(color = MutedFg, fontSize = 13.sp)
                    )
                }
            }

            else -> {
                Row(
                    modifier          = GlanceModifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SummaryPill(data.attended,  GreenColor,  "Present")
                    Spacer(GlanceModifier.width(10.dp))
                    SummaryPill(data.upcoming,  YellowColor, "Soon")
                    Spacer(GlanceModifier.width(10.dp))
                    SummaryPill(data.bunked,    RedColor,    "Absent")
                    if (data.notMarked > 0) {
                        Spacer(GlanceModifier.width(10.dp))
                        SummaryPill(data.notMarked, OrangeColor, "Pending")
                    }
                }

                Spacer(GlanceModifier.height(10.dp))

                LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                    items(data.classes) { cls ->
                        ClassRow(cls, context)
                        Spacer(GlanceModifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryPill(count: Int, color: ColorProvider, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            count.toString(),
            style = TextStyle(
                color      = color,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(GlanceModifier.width(3.dp))
        Text(
            label,
            style = TextStyle(
                color    = DimFg,
                fontSize = 11.sp
            )
        )
    }
}

@Composable
private fun ClassRow(cls: WidgetClassItem, context: Context) {
    val color = statusColor(cls.status)

    Row(
        modifier          = GlanceModifier
            .fillMaxWidth()
            .clickable(openAppAction(context)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier
                .width(3.dp)
                .height(32.dp)
                .background(color)
        ) {}

        Spacer(GlanceModifier.width(10.dp))

        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                cls.subjectName.take(26),
                style    = TextStyle(
                    color      = Foreground,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1
            )
            Text(
                "${formatTime(cls.startTime)}  ·  ${cls.roomName}",
                style = TextStyle(color = DimFg, fontSize = 9.sp)
            )
        }

        Text(
            statusLabel(cls.status),
            style = TextStyle(
                color      = color,
                fontSize   = 10.sp,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

class PesuDashWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = PesuDashWidget()
}

class RefreshWidgetCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        WidgetStateStore.save(
            context,
            WidgetStateStore.load(context).copy(isLoading = true)
        )
        PesuDashWidget().update(context, glanceId)

        val intent = Intent(context, WidgetRefreshReceiver::class.java).apply {
            action = WidgetRefreshReceiver.ACTION_REFRESH
        }
        context.sendBroadcast(intent)
    }
}