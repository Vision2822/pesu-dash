package com.pesu.pesudash.ui.results

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pesu.pesudash.data.model.IsaBreakdownItem
import com.pesu.pesudash.data.model.SubjectResultView
import com.pesu.pesudash.ui.components.ShadcnCard
import com.pesu.pesudash.ui.theme.AppTheme
import com.pesu.pesudash.ui.theme.Inter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    viewModel: ResultsViewModel,
    userId: String,
    usn: String,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val c = AppTheme.colors

    LaunchedEffect(userId) {
        viewModel.load(userId, usn)
    }

    Scaffold(
        modifier       = modifier,
        containerColor = c.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text       = "Results",
                            fontSize   = 22.sp,
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            color      = c.foreground
                        )
                        if (state.isProvisional) {
                            Text(
                                text       = "Provisional",
                                fontSize   = 11.sp,
                                fontFamily = Inter,
                                color      = c.orange
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.reload() }) {
                        Icon(
                            Icons.Default.Refresh,
                            "Refresh",
                            tint     = c.mutedFg,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = c.background)
            )
        }
    ) { padding ->

        when {
            state.isLoading -> {
                Box(
                    modifier         = Modifier
                        .fillMaxSize()
                        .padding(padding),
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
                    modifier         = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            state.error!!,
                            fontFamily = Inter,
                            color      = c.foreground,
                            fontSize   = 14.sp
                        )
                        TextButton(onClick = { viewModel.reload() }) {
                            Text("Retry", fontFamily = Inter, color = c.foreground)
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier       = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(
                        start  = 16.dp,
                        end    = 16.dp,
                        top    = 4.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        IsaEsaTabRow(
                            activeTab = state.activeTab,
                            onSwitch  = { viewModel.switchTab(it) }
                        )
                    }

                    when (state.activeTab) {

                        ResultsTab.ISA -> {
                            if (state.isaTabSemesters.isEmpty()) {
                                item {
                                    EmptyState(message = "No semesters found")
                                }
                            } else {
                                item {
                                    SemesterChips(
                                        labels        = state.isaTabSemesters.map { it.className },
                                        selectedIndex = state.isaSelectedIndex,
                                        onSelect      = { viewModel.selectIsaSemester(it) }
                                    )
                                }

                                val isaSt = state.isaSemesterState
                                when {
                                    isaSt == null || isaSt.isLoading -> {
                                        item { LoadingBox() }
                                    }
                                    isaSt.error != null -> {
                                        item {
                                            ErrorBox(
                                                message = isaSt.error!!,
                                                onRetry = { viewModel.retryIsa() }
                                            )
                                        }
                                    }
                                    isaSt.subjects.isEmpty() -> {
                                        item {
                                            EmptyState(message = "No ISA marks available yet")
                                        }
                                    }
                                    else -> {
                                        items(isaSt.subjects) { subject ->
                                            IsaSubjectCard(subject = subject)
                                        }
                                    }
                                }
                            }
                        }

                        ResultsTab.ESA -> {
                            if (state.esaTabSemesters.isEmpty()) {
                                item {
                                    EmptyState(message = "No ESA results available yet")
                                }
                            } else {
                                item {
                                    SemesterChips(
                                        labels        = state.esaTabSemesters.map { it.className },
                                        selectedIndex = state.esaSelectedIndex,
                                        onSelect      = { viewModel.selectEsaSemester(it) }
                                    )
                                }

                                val esaSt = state.esaSemesterState
                                when {
                                    esaSt == null || esaSt.isLoading -> {
                                        item { LoadingBox() }
                                    }
                                    esaSt.error != null -> {
                                        item {
                                            ErrorBox(
                                                message = esaSt.error!!,
                                                onRetry = { viewModel.retryIsa() }
                                            )
                                        }
                                    }
                                    else -> {
                                        item {
                                            SgpaCgpaCard(
                                                sgpa          = esaSt.sgpa,
                                                cgpa          = esaSt.cgpa,
                                                totalCredits  = esaSt.totalCredits,
                                                earnedCredits = esaSt.earnedCredits
                                            )
                                        }
                                        items(esaSt.subjects) { subject ->
                                            EsaSubjectCard(subject = subject)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IsaEsaTabRow(
    activeTab: ResultsTab,
    onSwitch: (ResultsTab) -> Unit
) {
    val c = AppTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.card)
            .border(1.dp, c.border, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(ResultsTab.ISA, ResultsTab.ESA).forEach { tab ->
            val selected = activeTab == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) c.foreground else Color.Transparent)
                    .clickable { onSwitch(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = tab.name,
                    fontSize   = 14.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (selected) c.background else c.mutedFg
                )
            }
        }
    }
}

@Composable
private fun SemesterChips(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val c = AppTheme.colors

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) c.foreground else c.card)
                    .border(
                        1.dp,
                        if (isSelected) c.foreground else c.border,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(index) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text       = label,
                    fontSize   = 13.sp,
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    color      = if (isSelected) c.background else c.foreground
                )
            }
        }
    }
}

@Composable
private fun SgpaCgpaCard(
    sgpa: String?,
    cgpa: String?,
    totalCredits: String?,
    earnedCredits: String?
) {
    val c = AppTheme.colors

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.card)
            .border(1.dp, c.border, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            if (sgpa != null) StatBlock(label = "SGPA", value = sgpa, color = c.green)
            if (cgpa != null) StatBlock(label = "CGPA", value = cgpa, color = c.blue)
            if (totalCredits != null) {
                StatBlock(
                    label = "Credits",
                    value = "$earnedCredits/$totalCredits",
                    color = c.foreground
                )
            }
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String, color: Color) {
    val c = AppTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = value,
            fontSize   = 24.sp,
            fontFamily = Inter,
            fontWeight = FontWeight.Bold,
            color      = color
        )
        Text(
            text     = label,
            fontSize = 11.sp,
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            color    = c.dimFg,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun IsaSubjectCard(subject: IsaSubjectView) {
    val c = AppTheme.colors
    var expanded by remember { mutableStateOf(false) }

    val finalIsa = subject.breakdown.firstOrNull {
        it.assessmentName.contains("final", ignoreCase = true)
    }

    val finalPct = finalIsa?.let { isa ->
        val m = isa.marks?.toFloatOrNull()
        val x = isa.maxMarks
        if (m != null && x != null && x > 0) (m / x.toFloat()) * 100f else null
    }

    val headerColor = when {
        finalPct == null -> c.foreground
        finalPct < 40f   -> c.red
        finalPct < 60f   -> c.orange
        finalPct < 80f   -> c.yellow
        else             -> c.green
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.card)
            .border(1.dp, c.border, RoundedCornerShape(14.dp))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(headerColor.copy(alpha = 0.1f))
                        .border(
                            0.5.dp,
                            headerColor.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (finalIsa != null) {
                        val m = finalIsa.marks?.toFloatOrNull()
                        val x = finalIsa.maxMarks
                        Text(
                            text = if (m != null && m == m.toInt().toFloat())
                                m.toInt().toString() else finalIsa.marks ?: "-",
                            fontSize   = 15.sp,
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            color      = headerColor
                        )
                        if (x != null) {
                            Text(
                                text     = "/${x.toInt()}",
                                fontSize = 9.sp,
                                fontFamily = Inter,
                                color    = headerColor.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        Text(
                            text       = "-",
                            fontSize   = 18.sp,
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            color      = headerColor
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = subject.subjectName,
                        fontSize   = 14.sp,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium,
                        color      = c.foreground,
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text       = subject.subjectCode,
                            fontSize   = 11.sp,
                            fontFamily = Inter,
                            color      = c.dimFg
                        )
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(c.border)
                        )
                        Text(
                            text       = "${subject.credits.toInt()} credits",
                            fontSize   = 11.sp,
                            fontFamily = Inter,
                            color      = c.dimFg
                        )
                    }
                }

                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint               = c.dimFg,
                    modifier           = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.cardHover)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text       = "Breakdown",
                        fontSize   = 12.sp,
                        fontFamily = Inter,
                        fontWeight = FontWeight.SemiBold,
                        color      = c.mutedFg,
                        modifier   = Modifier.padding(bottom = 2.dp)
                    )
                    for (isa in subject.breakdown) {
                        IsaRow(isa = isa)
                    }
                }
            }
        }
    }
}

@Composable
private fun EsaSubjectCard(subject: SubjectResultView) {
    val c = AppTheme.colors
    var expanded by remember { mutableStateOf(false) }

    val gradeColor = when (subject.grade) {
        "S"  -> c.green
        "A"  -> Color(0xFF22C55E)
        "B"  -> c.blue
        "C"  -> c.yellow
        "D"  -> c.orange
        "E", "F" -> c.red
        else -> c.foreground
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.card)
            .border(1.dp, c.border, RoundedCornerShape(14.dp))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { if (subject.isaBreakdown.isNotEmpty()) expanded = !expanded }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(gradeColor.copy(alpha = 0.1f))
                        .border(
                            0.5.dp,
                            gradeColor.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = subject.grade ?: "-",
                        fontSize   = 18.sp,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        color      = gradeColor
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = subject.subjectName,
                        fontSize   = 14.sp,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium,
                        color      = c.foreground,
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text       = subject.subjectCode,
                            fontSize   = 11.sp,
                            fontFamily = Inter,
                            color      = c.dimFg
                        )
                        Box(
                            modifier = Modifier
                                .size(3.dp)
                                .clip(CircleShape)
                                .background(c.border)
                        )
                        Text(
                            text       = "${subject.credits.toInt()} credits",
                            fontSize   = 11.sp,
                            fontFamily = Inter,
                            color      = c.dimFg
                        )
                    }
                }

                if (subject.isaBreakdown.isNotEmpty()) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp
                        else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint               = c.dimFg,
                        modifier           = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically(),
                exit    = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(c.cardHover)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text       = "ISA Breakdown",
                        fontSize   = 12.sp,
                        fontFamily = Inter,
                        fontWeight = FontWeight.SemiBold,
                        color      = c.mutedFg,
                        modifier   = Modifier.padding(bottom = 2.dp)
                    )
                    for (isa in subject.isaBreakdown) {
                        IsaRow(isa = isa)
                    }
                }
            }
        }
    }
}

@Composable
private fun IsaRow(isa: IsaBreakdownItem) {
    val c = AppTheme.colors

    val marksFloat = isa.marks?.toFloatOrNull()
    val maxMarks   = isa.maxMarks
    val pct        = if (marksFloat != null && maxMarks != null && maxMarks > 0)
        (marksFloat / maxMarks.toFloat()) * 100f else null

    val pctColor = when {
        pct == null -> c.dimFg
        pct < 40f   -> c.red
        pct < 60f   -> c.orange
        pct < 80f   -> c.yellow
        else        -> c.green
    }

    val isFinal = isa.assessmentName.contains("final", ignoreCase = true)

    Row(
        modifier          = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text       = isa.assessmentName,
            fontSize   = if (isFinal) 13.sp else 12.sp,
            fontFamily = Inter,
            fontWeight = if (isFinal) FontWeight.SemiBold else FontWeight.Normal,
            color      = if (isFinal) c.foreground else c.mutedFg,
            modifier   = Modifier.weight(1f),
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis
        )

        val displayMarks = if (marksFloat != null) {
            if (marksFloat == marksFloat.toInt().toFloat())
                marksFloat.toInt().toString()
            else isa.marks ?: "-"
        } else isa.marks ?: "-"

        Text(
            text       = "$displayMarks / ${maxMarks?.toInt() ?: "-"}",
            fontSize   = if (isFinal) 13.sp else 12.sp,
            fontFamily = Inter,
            fontWeight = if (isFinal) FontWeight.SemiBold else FontWeight.Normal,
            color      = pctColor
        )

        if (pct != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text       = "${pct.toInt()}%",
                fontSize   = 11.sp,
                fontFamily = Inter,
                color      = pctColor.copy(alpha = 0.7f),
                modifier   = Modifier.width(36.dp)
            )
        }
    }
}

@Composable
private fun LoadingBox() {
    val c = AppTheme.colors
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color       = c.mutedFg,
            strokeWidth = 2.dp,
            modifier    = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun ErrorBox(message: String, onRetry: () -> Unit) {
    val c = AppTheme.colors
    ShadcnCard {
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                message,
                fontFamily = Inter,
                color      = c.foreground,
                fontSize   = 13.sp
            )
            TextButton(onClick = onRetry) {
                Text("Retry", fontFamily = Inter, color = c.foreground)
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    val c = AppTheme.colors
    Box(
        modifier         = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = message,
            fontFamily = Inter,
            color      = c.dimFg,
            fontSize   = 14.sp
        )
    }
}