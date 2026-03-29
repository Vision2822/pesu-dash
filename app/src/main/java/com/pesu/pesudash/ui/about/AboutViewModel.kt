package com.pesu.pesudash.ui.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pesu.pesudash.data.local.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

private const val META_URL =
    "https://raw.githubusercontent.com/Vision2822/pesu-dash/main/app_meta.json"

data class AppMeta(
    val appName: String               = "PesuDash",
    val tagline: String               = "",
    val creatorName: String           = "Prajval",
    val creatorGithub: String         = "",
    val creatorRole: String           = "",
    val contributors: List<Contributor> = emptyList(),
    val repoLink: String              = "",
    val releasesLink: String          = "",
    val bugReportLink: String         = "",
    val latestVersionName: String     = "",
    val latestVersionCode: Int        = 0,
    val changelog: String             = ""
)

data class Contributor(
    val name: String,
    val github: String,
    val role: String
)

sealed class AboutUiState {
    object Loading : AboutUiState()
    data class Success(
        val meta: AppMeta,
        val avatarUrls: Map<String, String> = emptyMap()
    ) : AboutUiState()
    data class Error(val message: String) : AboutUiState()
}

class AboutViewModel(
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _state = MutableStateFlow<AboutUiState>(AboutUiState.Loading)
    val state: StateFlow<AboutUiState> = _state

    private val client = OkHttpClient()

    init { fetchMeta() }

    fun fetchMeta() {
        _state.value = AboutUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request  = Request.Builder().url(META_URL).build()
                val response = client.newCall(request).execute()
                val body     = response.body?.string()

                if (!response.isSuccessful || body == null) {
                    _state.value = AboutUiState.Error("Failed to load app info")
                    return@launch
                }

                val json        = JSONObject(body)
                val creatorJson = json.getJSONObject("creator")

                val contribArray = json.getJSONArray("contributors")
                val contributors = mutableListOf<Contributor>()
                for (i in 0 until contribArray.length()) {
                    val obj = contribArray.getJSONObject(i)
                    contributors.add(
                        Contributor(
                            name   = obj.optString("name"),
                            github = obj.optString("github"),
                            role   = obj.optString("role")
                        )
                    )
                }

                val linksJson  = json.getJSONObject("links")
                val latestJson = json.getJSONObject("latest_version")

                val meta = AppMeta(
                    appName           = json.optString("app_name", "PesuDash"),
                    tagline           = json.optString("tagline", ""),
                    creatorName       = creatorJson.optString("name"),
                    creatorGithub     = creatorJson.optString("github"),
                    creatorRole       = creatorJson.optString("role"),
                    contributors      = contributors,
                    repoLink          = linksJson.optString("repo"),
                    releasesLink      = linksJson.optString("releases"),
                    bugReportLink     = linksJson.optString("report_bug"),
                    latestVersionName = latestJson.optString("name"),
                    latestVersionCode = latestJson.optInt("code", 1),
                    changelog         = latestJson.optString("changelog")
                )

                val storedAvatars = sessionStore.getAvatarCache().toMutableMap()

                val usernames = mutableListOf<String>()
                creatorJson.optString("github").githubUsername()?.let { usernames.add(it) }
                contributors.forEach { it.github.githubUsername()?.let { u -> usernames.add(u) } }

                usernames.forEach { username ->
                    if (!storedAvatars.containsKey(username)) {
                        storedAvatars[username] = "https://github.com/$username.png?size=128"
                    }
                }

                sessionStore.saveAvatarCache(storedAvatars)

                _state.value = AboutUiState.Success(
                    meta       = meta,
                    avatarUrls = storedAvatars
                )

            } catch (e: Exception) {
                _state.value = AboutUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun clearAvatarCache() {
        viewModelScope.launch(Dispatchers.IO) {
            sessionStore.clearAvatarCache()
        }
    }

    class Factory(private val sessionStore: SessionStore) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return AboutViewModel(sessionStore) as T
        }
    }
}

private fun String.githubUsername(): String? {
    if (isBlank()) return null
    return trimEnd('/').substringAfterLast("/").takeIf { it.isNotBlank() }
}