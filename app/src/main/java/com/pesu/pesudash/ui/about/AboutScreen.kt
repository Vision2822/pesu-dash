package com.pesu.pesudash.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.pesu.pesudash.R
import com.pesu.pesudash.data.local.SessionStore
import com.pesu.pesudash.ui.components.ButtonVariant
import com.pesu.pesudash.ui.components.ShadcnBadge
import com.pesu.pesudash.ui.components.ShadcnButton
import com.pesu.pesudash.ui.components.ShadcnCard
import com.pesu.pesudash.ui.components.ShadcnSeparator
import com.pesu.pesudash.ui.theme.AppTheme
import com.pesu.pesudash.ui.theme.Inter

@Composable
fun AboutScreen(
    sessionStore:       SessionStore,
    currentVersionName: String   = "1.0",
    currentVersionCode: Int      = 1,
    modifier:           Modifier = Modifier
) {
    val vm: AboutViewModel = viewModel(
        factory = AboutViewModel.Factory(
            sessionStore        = sessionStore,
            currentVersionCode  = currentVersionCode
        )
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val c     = AppTheme.colors
    val ctx   = LocalContext.current

    fun openUrl(url: String) {
        if (url.isBlank()) return
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                painter            = painterResource(id = R.drawable.logo),
                contentDescription = "PesuDash",
                modifier           = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
            Text(
                text       = "PesuDash",
                fontFamily = Inter,
                fontWeight = FontWeight.Bold,
                fontSize   = 22.sp,
                color      = c.foreground
            )
            ShadcnBadge(
                text  = "v$currentVersionName",
                color = c.blue
            )
        }

        when (val s = state) {

            is AboutUiState.Loading -> {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color       = c.foreground,
                        modifier    = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            is AboutUiState.Error -> {
                ShadcnCard {
                    Text(
                        text       = "Couldn't load info: ${s.message}",
                        fontFamily = Inter,
                        fontSize   = 13.sp,
                        color      = c.red,
                        textAlign  = TextAlign.Center,
                        modifier   = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    ShadcnButton(
                        text     = "Retry",
                        onClick  = { vm.fetchMeta() },
                        variant  = ButtonVariant.Outline,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            is AboutUiState.Success -> {
                val meta        = s.meta
                val avatarUrls  = s.avatarUrls
                val updateState = s.updateState

                if (meta.tagline.isNotBlank()) {
                    Text(
                        text       = meta.tagline,
                        fontFamily = Inter,
                        fontSize   = 13.sp,
                        color      = c.mutedFg,
                        textAlign  = TextAlign.Center,
                        modifier   = Modifier.fillMaxWidth()
                    )
                }

                if (updateState.hasUpdate) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(c.green.copy(alpha = 0.08f))
                            .border(1.dp, c.green.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .clickable { openUrl(updateState.releasesLink) }
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Text(
                                    text       = "Update available",
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize   = 14.sp,
                                    color      = c.green
                                )
                                ShadcnBadge(text = "v${updateState.latestVersion}", color = c.green)
                            }
                            if (updateState.changelog.isNotBlank()) {
                                Text(
                                    text       = updateState.changelog,
                                    fontFamily = Inter,
                                    fontSize   = 12.sp,
                                    color      = c.mutedFg,
                                    lineHeight = 18.sp,
                                    maxLines   = 4,
                                    overflow   = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text       = "Tap to download →",
                                fontFamily = Inter,
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color      = c.green
                            )
                        }
                    }
                }

                if (meta.changelog.isNotBlank() && !updateState.hasUpdate) {
                    ShadcnCard {
                        SectionLabel(text = "WHAT'S NEW IN V${meta.latestVersionName}")
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text       = meta.changelog,
                            fontFamily = Inter,
                            fontSize   = 13.sp,
                            color      = c.foreground,
                            lineHeight = 20.sp
                        )
                    }
                }

                ShadcnCard {
                    SectionLabel(text = "CREATED BY")
                    Spacer(Modifier.height(12.dp))
                    PersonRow(
                        name      = meta.creatorName,
                        role      = meta.creatorRole,
                        github    = meta.creatorGithub,
                        avatarUrl = meta.creatorGithub
                            .githubUsername()
                            ?.let { avatarUrls[it] },
                        onClick   = { openUrl(meta.creatorGithub) }
                    )
                }

                if (meta.contributors.isNotEmpty()) {
                    ShadcnCard {
                        SectionLabel(text = "CONTRIBUTORS")
                        Spacer(Modifier.height(12.dp))
                        meta.contributors.forEachIndexed { index, contributor ->
                            PersonRow(
                                name      = contributor.name,
                                role      = contributor.role,
                                github    = contributor.github,
                                avatarUrl = contributor.github
                                    .githubUsername()
                                    ?.let { avatarUrls[it] },
                                onClick   = { openUrl(contributor.github) }
                            )
                            if (index < meta.contributors.lastIndex) {
                                Spacer(Modifier.height(8.dp))
                                ShadcnSeparator()
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }

                ShadcnCard {
                    SectionLabel(text = "LINKS")
                    Spacer(Modifier.height(12.dp))
                    LinkRow(
                        label   = "GitHub Repository",
                        onClick = { openUrl(meta.repoLink) }
                    )
                    ShadcnSeparator(modifier = Modifier.padding(vertical = 8.dp))
                    LinkRow(
                        label   = "Releases & Changelog",
                        onClick = { openUrl(meta.releasesLink) }
                    )
                    ShadcnSeparator(modifier = Modifier.padding(vertical = 8.dp))
                    LinkRow(
                        label   = "Report a Bug",
                        onClick = { openUrl(meta.bugReportLink) }
                    )
                }
            }
        }

        Text(
            text       = "Version $currentVersionName (build $currentVersionCode)",
            fontFamily = Inter,
            fontSize   = 11.sp,
            color      = c.dimFg,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text          = text,
        fontFamily    = Inter,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 12.sp,
        color         = AppTheme.colors.mutedFg,
        letterSpacing = 0.8.sp
    )
}

@Composable
private fun PersonRow(
    name:      String,
    role:      String,
    github:    String,
    avatarUrl: String?,
    onClick:   () -> Unit
) {
    val c = AppTheme.colors

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier              = Modifier.weight(1f)
        ) {
            Box(
                modifier         = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(c.cardHover)
                    .border(1.dp, c.border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model              = avatarUrl,
                        contentDescription = name,
                        modifier           = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale       = ContentScale.Crop
                    )
                } else {
                    Text(
                        text       = name.first().uppercase(),
                        fontFamily = Inter,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 15.sp,
                        color      = c.foreground
                    )
                }
            }

            Column {
                Text(
                    text       = name,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    fontSize   = 14.sp,
                    color      = c.foreground
                )
                if (role.isNotBlank()) {
                    Text(
                        text       = role,
                        fontFamily = Inter,
                        fontSize   = 12.sp,
                        color      = c.mutedFg
                    )
                }
                if (github.isNotBlank()) {
                    Text(
                        text       = "@${github.githubUsername()}",
                        fontFamily = Inter,
                        fontSize   = 11.sp,
                        color      = c.dimFg
                    )
                }
            }
        }

        if (github.isNotBlank()) {
            Text(
                text     = "↗",
                fontSize = 16.sp,
                color    = c.dimFg
            )
        }
    }
}

@Composable
private fun LinkRow(
    label:   String,
    onClick: () -> Unit
) {
    val c = AppTheme.colors

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Text(
            text       = label,
            fontFamily = Inter,
            fontSize   = 14.sp,
            color      = c.foreground
        )
        Text(
            text     = "↗",
            fontSize = 16.sp,
            color    = c.dimFg
        )
    }
}

private fun String.githubUsername(): String? {
    if (isBlank()) return null
    return trimEnd('/').substringAfterLast("/").takeIf { it.isNotBlank() }
}