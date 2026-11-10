package com.yourtube.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourtube.app.AppState
import com.yourtube.app.AppViewModel
import com.yourtube.app.FeedItem
import com.yourtube.app.data.local.ApprovedEntry
import com.yourtube.app.data.local.Category
import com.yourtube.app.data.local.PlaybackProgress
import com.yourtube.app.data.local.WatchHistory

private val DarkPalette = darkColorScheme(
    primary = Color(0xFFFF0033),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF282828),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFCCCCCC),
    onSecondary = Color(0xFF0F0F0F),
    secondaryContainer = Color(0xFF222222),
    onSecondaryContainer = Color.White,
    tertiary = Color(0xFFFF334B),
    onTertiary = Color.White,
    background = Color(0xFF0F0F0F),
    onBackground = Color.White,
    surface = Color(0xFF181818),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF222222),
    onSurfaceVariant = Color(0xFFAAAAAA),
    outline = Color(0xFF333333),
    outlineVariant = Color(0xFF222222),
    error = Color(0xFFFF4D4D),
    onError = Color.White
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun YourTubeApp(model: AppViewModel, fullscreenHost: FullscreenHost? = LocalFullscreenHost.current) {
    val state by model.state.collectAsStateWithLifecycle()
    val entries by model.entries.collectAsStateWithLifecycle()
    val categories by model.categories.collectAsStateWithLifecycle()
    val history by model.histories.collectAsStateWithLifecycle()
    val progresses by model.progresses.collectAsStateWithLifecycle()
    val homeFeed by model.homeFeed.collectAsStateWithLifecycle()
    val resumeCandidate by model.resumeCandidate.collectAsStateWithLifecycle()
    var privacy by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf("home") }
    var categoriesExpanded by rememberSaveable { mutableStateOf(false) }
    val online = rememberOnline()

    MaterialTheme(colorScheme = DarkPalette) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                // Fixed top header with DivineTube branding
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(Color(0xFF0F0F0F))
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DivineTubeLogo(
                        isAdmin = state.parent,
                        onLongClick = model::showGate,
                        modifier = Modifier.weight(1f)
                    )
                    if (state.parent) {
                        TextButton(onClick = model::lock) {
                            Text("Done & lock", color = Color(0xFFFF0033), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (!online) {
                    Text(
                        "You're offline. Your saved library is here; playback needs internet.",
                        Modifier
                            .background(Color(0xFF222222))
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        color = Color(0xFFAAAAAA),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (!state.ready) {
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color(0xFFFF0033))
                    Text(state.message ?: "Opening your library…", Modifier.padding(24.dp), color = Color(0xFFAAAAAA))
                } else if (state.playback != null) {
                    PlayerScreen(state.playback!!, model, Modifier.weight(1f))
                } else if (state.parent) {
                    ParentScreen(model, state, categories, Modifier.weight(1f))
                } else {
                    // Viewer Mode: Horizontally swipeable tab bar + Feeds
                    val allCategoryTabs = categories.map { "cat_${it.id}" to it.name }
                    if (selectedTab.startsWith("cat_") && allCategoryTabs.none { it.first == selectedTab }) {
                        selectedTab = "home"
                    }

                    val displayCategoryTabs = if (!categoriesExpanded && allCategoryTabs.size > 3) {
                        allCategoryTabs.take(3)
                    } else {
                        allCategoryTabs
                    }
                    val hiddenCount = (allCategoryTabs.size - 3).coerceAtLeast(0)
                    val showMoreButton = !categoriesExpanded && hiddenCount > 0

                    val visibleTabs = listOf("home" to "Home", "playlists" to "Playlists") + displayCategoryTabs
                    val selectedTabIndex = visibleTabs.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0)

                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color(0xFF0F0F0F),
                        contentColor = Color.White,
                        edgePadding = 12.dp,
                        indicator = { tabPositions ->
                            if (selectedTabIndex in tabPositions.indices) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                    height = 2.5.dp,
                                    color = Color(0xFFFF0033)
                                )
                            }
                        },
                        divider = {
                            HorizontalDivider(color = Color(0xFF222222), thickness = 0.5.dp)
                        }
                    ) {
                        visibleTabs.forEach { (key, title) ->
                            val isSelected = selectedTab == key
                            Tab(
                                selected = isSelected,
                                onClick = { selectedTab = key },
                                text = {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Color(0xFFAAAAAA),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                        if (showMoreButton) {
                            Tab(
                                selected = false,
                                onClick = { categoriesExpanded = true },
                                text = {
                                    Text(
                                        text = "+$hiddenCount More",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFFFF0033),
                                        maxLines = 1
                                    )
                                }
                            )
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when {
                            selectedTab == "home" -> {
                                if (homeFeed.isEmpty()) {
                                    EmptyStateView(
                                        title = "Welcome to DivineTube",
                                        subtitle = "Approved videos and playlists will appear here.\nLong-press the DivineTube logo above to open Admin Mode and add content."
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(bottom = 24.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(homeFeed, key = { "${it.entryId}-${it.videoId}" }) { feedItem ->
                                            VideoFeedCard(
                                                item = feedItem,
                                                progresses = progresses,
                                                onClick = { model.playFeedItem(feedItem) }
                                            )
                                        }
                                        item(key = "home_privacy") {
                                            TextButton(
                                                onClick = { privacy = true },
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                            ) {
                                                Text("Privacy & YouTube", color = Color(0xFF888888))
                                            }
                                        }
                                    }
                                }
                            }
                            selectedTab == "playlists" -> {
                                val approvedPlaylists = entries.filter { it.playlistId != null }
                                if (approvedPlaylists.isEmpty()) {
                                    EmptyStateView(
                                        title = "No approved playlists",
                                        subtitle = "Playlists approved in Admin Mode will appear here."
                                    )
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(bottom = 24.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        items(approvedPlaylists, key = { "pl-${it.id}" }) { playlistEntry ->
                                            PlaylistFeedCard(
                                                entry = playlistEntry,
                                                history = history,
                                                onClick = { model.play(playlistEntry) }
                                            )
                                        }
                                        item(key = "playlists_privacy") {
                                            TextButton(
                                                onClick = { privacy = true },
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                            ) {
                                                Text("Privacy & YouTube", color = Color(0xFF888888))
                                            }
                                        }
                                    }
                                }
                            }
                            selectedTab.startsWith("cat_") -> {
                                val categoryId = selectedTab.removePrefix("cat_").toLongOrNull()
                                val category = categories.firstOrNull { it.id == categoryId }
                                if (category == null) {
                                    selectedTab = "home"
                                } else {
                                    val catEntries = entries.filter { it.categoryId == category.id }
                                    val catItems = remember(catEntries) {
                                        val items = mutableListOf<FeedItem>()
                                        val seen = mutableSetOf<Pair<Long, String>>()
                                        for (entry in catEntries) {
                                            val ids = entry.videoIds.split(',').map { it.trim() }.filter { it.isNotBlank() }
                                            if (entry.playlistId == null) {
                                                val vId = ids.firstOrNull() ?: continue
                                                val key = Pair(entry.id, vId)
                                                if (seen.add(key)) {
                                                    val actualTitle = model.getVideoTitle(vId) ?: entry.title
                                                    items.add(
                                                        FeedItem(
                                                            entryId = entry.id,
                                                            videoId = vId,
                                                            title = actualTitle,
                                                            isPlaylistEpisode = false,
                                                            playlistTitle = null,
                                                            episodeIndex = null,
                                                            totalEpisodes = null,
                                                            categoryId = entry.categoryId
                                                        )
                                                    )
                                                }
                                            } else {
                                                val uniqueIds = ids.distinct()
                                                uniqueIds.forEach { vId ->
                                                    val key = Pair(entry.id, vId)
                                                    if (seen.add(key)) {
                                                        val originalIndex = ids.indexOf(vId)
                                                        val actualTitle = model.getVideoTitle(vId)
                                                        items.add(
                                                            FeedItem(
                                                                entryId = entry.id,
                                                                videoId = vId,
                                                                title = actualTitle ?: entry.title,
                                                                isPlaylistEpisode = true,
                                                                playlistTitle = entry.title,
                                                                episodeIndex = originalIndex,
                                                                totalEpisodes = ids.size,
                                                                categoryId = entry.categoryId
                                                            )
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        items
                                    }
                                    if (catItems.isEmpty()) {
                                        EmptyStateView(
                                            title = "No videos in ${category.name}",
                                            subtitle = "Videos and playlists assigned to this category in Admin Mode will appear here."
                                        )
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(bottom = 24.dp),
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            items(catItems, key = { "${it.entryId}-${it.videoId}" }) { feedItem ->
                                                VideoFeedCard(
                                                    item = feedItem,
                                                    progresses = progresses,
                                                    onClick = { model.playFeedItem(feedItem) }
                                                )
                                            }
                                            item(key = "cat_privacy") {
                                                TextButton(
                                                    onClick = { privacy = true },
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                                                ) {
                                                    Text("Privacy & YouTube", color = Color(0xFF888888))
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

            if (state.gate && !state.parent) PinDialog(state, model)
            if (privacy) {
                AlertDialog(
                    onDismissRequest = { privacy = false },
                    containerColor = Color(0xFF181818),
                    title = { Text("Privacy & YouTube", color = Color.White) },
                    text = { PrivacyNotice() },
                    confirmButton = {
                        TextButton(onClick = { privacy = false }) {
                            Text("Close", color = Color(0xFFFF0033))
                        }
                    }
                )
            }
            resumeCandidate?.let { candidate ->
                AlertDialog(
                    onDismissRequest = model::dismissResumePrompt,
                    containerColor = Color(0xFF181818),
                    title = { Text("Continue watching?", color = Color.White) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(candidate.entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                            if (candidate.totalEpisodes > 1) {
                                Text("Episode ${candidate.episodeIndex + 1} of ${candidate.totalEpisodes}", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFAAAAAA))
                            }
                            Text(
                                "${formatProgressTime(candidate.positionSeconds)} / ${formatProgressTime(candidate.durationSeconds)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFF0033)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                model.dismissResumePrompt()
                                model.resumePlayback(candidate.entry.id, candidate.videoId, startOver = false)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0033))
                        ) {
                            Text("Resume", color = Color.White)
                        }
                    },
                    dismissButton = {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = model::dismissResumePrompt) {
                                Text("Not now", color = Color(0xFFAAAAAA))
                            }
                            OutlinedButton(
                                onClick = {
                                    model.dismissResumePrompt()
                                    model.resumePlayback(candidate.entry.id, candidate.videoId, startOver = true)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFF333333))
                            ) {
                                Text("Start over")
                            }
                        }
                    }
                )
            }
        }

        val isFullscreen = fullscreenHost?.isFullscreen == true
        BackHandler(isFullscreen || state.parent || state.gate || state.playback != null) {
            when {
                isFullscreen -> fullscreenHost.exitFullscreen()
                state.playback != null -> model.closePlayback()
                else -> model.lock()
            }
        }
    }
}

@Composable
private fun DivineTubeLogo(
    isAdmin: Boolean,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .combinedClickable(onClick = {}, onLongClick = onLongClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // YouTube-style red rounded badge with white play icon
        Box(
            modifier = Modifier
                .size(width = 28.dp, height = 20.dp)
                .background(Color(0xFFFF0033), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            PlayIcon(modifier = Modifier.size(10.dp), color = Color.White)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = "DivineTube",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = (-0.5).sp
        )
        if (isAdmin) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .background(Color(0xFF282828), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Admin",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF0033),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun PlayIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.2f, size.height * 0.15f)
            lineTo(size.width * 0.85f, size.height * 0.5f)
            lineTo(size.width * 0.2f, size.height * 0.85f)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
private fun VideoFeedCard(
    item: FeedItem,
    progresses: List<PlaybackProgress>,
    onClick: () -> Unit
) {
    val prog = progresses.firstOrNull { it.entryId == item.entryId && it.videoId == item.videoId }
    val hasProgress = prog != null && prog.positionSeconds > 0 && !prog.completed
    val durationSeconds = prog?.durationSeconds ?: 0L
    val positionSeconds = prog?.positionSeconds ?: 0L

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // Full-width 16:9 thumbnail with minimal padding
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            Thumbnail(
                videoId = item.videoId,
                modifier = Modifier.fillMaxSize()
            )

            // Duration or episode badge in bottom-right corner
            val badgeText = when {
                durationSeconds > 0 -> formatProgressTime(durationSeconds)
                item.isPlaylistEpisode && item.episodeIndex != null -> "EP ${item.episodeIndex + 1}"
                else -> null
            }
            if (badgeText != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = if (hasProgress && durationSeconds > 0) 8.dp else 8.dp, end = 8.dp)
                        .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Red progress bar at the bottom edge of the thumbnail
            if (hasProgress && durationSeconds > 0) {
                val progressFraction = (positionSeconds.toFloat() / durationSeconds.toFloat()).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color(0x66FFFFFF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressFraction)
                            .background(Color(0xFFFF0033))
                    )
                }
            }
        }

        // Below thumbnail: metadata matching YouTube styling
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF222222)),
                contentAlignment = Alignment.Center
            ) {
                PlayIcon(modifier = Modifier.size(14.dp), color = Color(0xFFFF0033))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(3.dp))

                val subtitle = buildString {
                    if (item.isPlaylistEpisode && item.playlistTitle != null) {
                        append(item.playlistTitle)
                        if (item.episodeIndex != null && item.totalEpisodes != null) {
                            append(" · Episode ${item.episodeIndex + 1} of ${item.totalEpisodes}")
                        }
                    } else {
                        append("Approved video")
                    }
                    if (hasProgress && positionSeconds > 0) {
                        append(" · Resume from ${formatProgressTime(positionSeconds)}")
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFAAAAAA),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun PlaylistFeedCard(
    entry: ApprovedEntry,
    history: List<WatchHistory>,
    onClick: () -> Unit
) {
    val ids = entry.videoIds.split(',').map { it.trim() }.filter { it.isNotBlank() }
    val firstVideoId = ids.firstOrNull()
    val lastWatched = history.firstOrNull { it.entryId == entry.id }
    val targetIndex = lastWatched?.videoId?.let(ids::indexOf)?.takeIf { it >= 0 }
        ?: lastWatched?.episodeIndex?.coerceIn(0, ids.lastIndex)
        ?: 0
    val targetVideoId = ids.getOrNull(targetIndex) ?: firstVideoId

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            Thumbnail(
                videoId = targetVideoId,
                modifier = Modifier.fillMaxSize()
            )

            // Playlist episode count badge in bottom-right corner
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color(0xCC000000), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${ids.size} EPISODES",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF222222)),
                contentAlignment = Alignment.Center
            ) {
                PlayIcon(modifier = Modifier.size(14.dp), color = Color(0xFFFF0033))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(3.dp))

                val subtitle = buildString {
                    append("Series · ${ids.size} saved episodes")
                    if (lastWatched != null && ids.size > 1) {
                        append(" · Resume Ep ${targetIndex + 1}")
                    }
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFAAAAAA),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyStateView(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFAAAAAA),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
private fun PinDialog(state: AppState, model: AppViewModel) {
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = model::lock,
        containerColor = Color(0xFF181818),
        title = { Text(if (state.configured) "Admin access" else "Set your Admin PIN", color = Color.White) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Use 6–12 digits. Forgotten PINs require clearing all app data, including the library.", color = Color(0xFFAAAAAA))
                OutlinedTextField(
                    pin,
                    { if (it.length <= 12 && it.all(Char::isDigit)) pin = it },
                    label = { Text("PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFFFF0033),
                        unfocusedBorderColor = Color(0xFF333333)
                    )
                )
                if (!state.configured) {
                    OutlinedTextField(
                        confirmation,
                        { if (it.length <= 12 && it.all(Char::isDigit)) confirmation = it },
                        label = { Text("Confirm PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFFF0033),
                            unfocusedBorderColor = Color(0xFF333333)
                        )
                    )
                    PrivacyNotice()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            consent,
                            { consent = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF0033))
                        )
                        Text("I accept YouTube's terms and understand these limitations.", color = Color(0xFFAAAAAA))
                    }
                }
                state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color(0xFFFF0033))
            }
        },
        confirmButton = {
            TextButton(
                enabled = !state.busy,
                onClick = { model.authenticate(pin, confirmation, consent); pin = ""; confirmation = "" }
            ) {
                Text(if (state.configured) "Unlock" else "Create PIN", color = Color(0xFFFF0033))
            }
        },
        dismissButton = {
            TextButton(onClick = model::lock) {
                Text("Cancel", color = Color(0xFFAAAAAA))
            }
        }
    )
}

@Composable
fun PrivacyNotice() {
    val uri = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Approvals and PIN verification data stay on this device. There are no analytics or accounts. YouTube receives your IP address and device/app information when thumbnails, search, or its embedded player load.",
            color = Color(0xFFAAAAAA)
        )
        Text(
            "YouTube controls its player, ads, recommendations, and links. These can show content beyond approved content. DivineTube is not a kiosk or a guarantee of content safety. Review each video yourself.",
            color = Color(0xFFAAAAAA)
        )
        Text(
            "YouTube Terms: https://www.youtube.com/t/terms\nGoogle Privacy: https://policies.google.com/privacy",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF777777)
        )
        TextButton(onClick = { uri.openUri("https://www.youtube.com/t/terms") }) {
            Text("Read YouTube terms", color = Color(0xFFFF0033))
        }
        TextButton(onClick = { uri.openUri("https://policies.google.com/privacy") }) {
            Text("Read Google privacy policy", color = Color(0xFFFF0033))
        }
    }
}

@Composable
private fun ParentScreen(model: AppViewModel, state: AppState, categories: List<Category>, modifier: Modifier) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier.background(Color(0xFF0F0F0F))) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf("Add", "Library", "Categories").forEachIndexed { i, title ->
                FilterChip(
                    selected = tab == i,
                    onClick = { tab = i },
                    label = { Text(title) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF0033),
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFF222222),
                        labelColor = Color(0xFFAAAAAA)
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = Color(0xFF333333),
                        selectedBorderColor = Color(0xFFFF0033),
                        enabled = true,
                        selected = tab == i
                    )
                )
            }
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Color(0xFFFF0033))
        state.message?.let {
            Text(it, Modifier.padding(16.dp), color = Color(0xFFFF0033))
        }
        when (tab) {
            0 -> AddPanel(model, state, categories)
            1 -> LibraryPanel(model, categories)
            else -> CategoriesPanel(model, categories)
        }
    }
}

@Composable
private fun AddPanel(model: AppViewModel, state: AppState, categories: List<Category>) {
    var link by remember(state.importText) { mutableStateOf(state.importText) }
    var title by remember { mutableStateOf("") }
    var manual by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var playlists by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf<Long?>(null) }
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color(0xFFFF0033),
        unfocusedBorderColor = Color(0xFF333333),
        focusedLabelColor = Color(0xFFFF0033),
        unfocusedLabelColor = Color(0xFFAAAAAA)
    )

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Add approved content", style = MaterialTheme.typography.headlineSmall, color = Color.White)
            Text("Paste a YouTube link, share from YouTube, or search below. Approval is always your choice.", color = Color(0xFFAAAAAA))
        }
        item { OutlinedTextField(link, { link = it }, Modifier.fillMaxWidth(), label = { Text("YouTube video or playlist link") }, colors = textFieldColors) }
        item { OutlinedTextField(title, { title = it.take(200) }, Modifier.fillMaxWidth(), label = { Text("Library title (optional)") }, colors = textFieldColors) }
        item {
            OutlinedTextField(
                manual,
                { manual = it.take(64000) },
                Modifier.fillMaxWidth(),
                label = { Text("Playlist snapshot links (optional)") },
                supportingText = { Text("Without an API key, paste one reviewed video link per line for a playlist. Only these videos will be saved.", color = Color(0xFF777777)) },
                minLines = 2,
                colors = textFieldColors
            )
        }
        item {
            Button(
                onClick = { model.prepareLink(link, title, manual) },
                enabled = !state.busy,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0033))
            ) {
                Text("Review link", color = Color.White)
            }
        }
        state.review?.let { review ->
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)), border = BorderStroke(1.dp, Color(0xFF282828))) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            review.title,
                            model::labelReview,
                            label = { Text("Your library label") },
                            supportingText = { Text("A label you choose; not live YouTube metadata.", color = Color(0xFF777777)) },
                            colors = textFieldColors
                        )
                        Text("Review all ${review.ids.size} video(s). Playlist updates require a new review and approval.", color = Color(0xFFAAAAAA))
                        CategorySelector(categories, category) { category = it }
                        Button(
                            onClick = { model.approve(category) },
                            enabled = !state.busy,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0033))
                        ) {
                            Text("Approve these ${review.ids.size} video(s)", color = Color.White)
                        }
                        TextButton(onClick = model::clearReview) { Text("Cancel review", color = Color(0xFFAAAAAA)) }
                    }
                }
            }
            items(review.ids, key = { it }) { id ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(id, Modifier.weight(1f), color = Color.White)
                    TextButton(onClick = { model.preview(id, review.title) }) { Text("Preview", color = Color(0xFFFF0033)) }
                }
            }
        }
        item { HorizontalDivider(color = Color(0xFF222222)); Text("Search YouTube", style = MaterialTheme.typography.titleLarge, color = Color.White) }
        item { Text("Search and playback checks need a configured YouTube Data API key. Search uses API quota; saved browsing works offline.", color = Color(0xFFAAAAAA)) }
        item { OutlinedTextField(query, { query = it.take(200) }, Modifier.fillMaxWidth(), label = { Text("Search query") }, colors = textFieldColors) }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    playlists,
                    { playlists = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFFFF0033))
                )
                Text("Search playlists", Modifier.padding(start = 12.dp), color = Color.White)
            }
        }
        item {
            Button(
                onClick = { model.search(query, playlists) },
                enabled = !state.busy,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0033))
            ) {
                Text("Search", color = Color.White)
            }
        }
        items(state.results, key = { it.id }) { result ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)), border = BorderStroke(1.dp, Color(0xFF282828))) {
                Column(Modifier.padding(16.dp)) {
                    Text(result.title, fontWeight = FontWeight.Bold, color = Color.White)
                    Row {
                        if (!result.isPlaylist) TextButton(onClick = { model.preview(result.id, result.title) }) { Text("Preview", color = Color(0xFFFF0033)) }
                        TextButton(onClick = { model.reviewResult(result) }, enabled = !state.busy) { Text("Review for approval", color = Color.White) }
                    }
                }
            }
        }
        if (state.nextPage != null) {
            item {
                TextButton(onClick = { model.search(query, playlists, true) }, enabled = !state.busy) {
                    Text("Load more results", color = Color(0xFFFF0033))
                }
            }
        }
        item { PrivacyNotice() }
    }
}

@Composable
private fun LibraryPanel(model: AppViewModel, categories: List<Category>) {
    val entries by model.entries.collectAsStateWithLifecycle()
    var removing by remember { mutableStateOf<Long?>(null) }
    var trusting by remember { mutableStateOf<Long?>(null) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Approved library", style = MaterialTheme.typography.headlineSmall, color = Color.White) }
        if (entries.isEmpty()) item { Text("Nothing approved yet. Use Add to start your library.", color = Color(0xFFAAAAAA)) }
        items(entries, key = { it.id }) { entry ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)), border = BorderStroke(1.dp, Color(0xFF282828))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(entry.title, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${entry.videoIds.split(',').size} saved video(s)", color = Color(0xFFAAAAAA))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (entry.featured) Text("Featured on Home", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFF0033))
                        else OutlinedButton(
                            onClick = { model.setFeatured(entry.id) },
                            modifier = Modifier.heightIn(min = 48.dp),
                            border = BorderStroke(1.dp, Color(0xFF333333))
                        ) {
                            Text("Feature on Home", color = Color.White)
                        }
                    }
                    CategorySelector(categories, entry.categoryId) { model.assignCategory(entry.id, it) }
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                        TextButton(onClick = { model.play(entry) }) { Text("Play", color = Color(0xFFFF0033)) }
                        TextButton(onClick = { model.moveEntry(entry.id, -1) }) { Text("Up", color = Color.White) }
                        TextButton(onClick = { model.moveEntry(entry.id, 1) }) { Text("Down", color = Color.White) }
                        TextButton(onClick = { removing = entry.id }) { Text("Remove", color = Color(0xFFFF4D4D)) }
                    }
                    entry.playlistId?.let { _ ->
                        Text("Playlist updates: ${if (entry.refreshMode == "AUTOMATIC") "Automatic (trusted)" else "Manual review"}", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFAAAAAA))
                        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                            TextButton(onClick = { model.setRefreshMode(entry.id, "MANUAL") }) { Text("Manual refresh", color = Color.White) }
                            TextButton(onClick = { trusting = entry.id }) { Text("Enable automatic updates", color = Color(0xFFFF0033)) }
                            TextButton(onClick = { model.refreshPlaylist(entry.id) }) { Text("Refresh now", color = Color.White) }
                        }
                    }
                }
            }
        }
    }
    if (removing != null) {
        AlertDialog(
            onDismissRequest = { removing = null },
            containerColor = Color(0xFF181818),
            title = { Text("Remove approval?", color = Color.White) },
            text = { Text("This item will disappear from Home.", color = Color(0xFFAAAAAA)) },
            confirmButton = { TextButton(onClick = { model.removeEntry(removing!!); removing = null }) { Text("Remove", color = Color(0xFFFF4D4D)) } },
            dismissButton = { TextButton(onClick = { removing = null }) { Text("Cancel", color = Color(0xFFAAAAAA)) } }
        )
    }
    if (trusting != null) {
        AlertDialog(
            onDismissRequest = { trusting = null },
            containerColor = Color(0xFF181818),
            title = { Text("Trust automatic playlist updates?", color = Color.White) },
            text = { Text("New videos from this YouTube playlist can appear on Home when Admin Mode opens. This uses API quota and skips manual review until you switch back to Manual refresh.", color = Color(0xFFAAAAAA)) },
            confirmButton = { TextButton(onClick = { model.setRefreshMode(trusting!!, "AUTOMATIC"); trusting = null }) { Text("Enable updates", color = Color(0xFFFF0033)) } },
            dismissButton = { TextButton(onClick = { trusting = null }) { Text("Keep manual review", color = Color(0xFFAAAAAA)) } }
        )
    }
}

@Composable
private fun CategoriesPanel(model: AppViewModel, categories: List<Category>) {
    var name by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<Long?>(null) }
    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = Color(0xFFFF0033),
        unfocusedBorderColor = Color(0xFF333333),
        focusedLabelColor = Color(0xFFFF0033),
        unfocusedLabelColor = Color(0xFFAAAAAA)
    )

    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Manage Categories", style = MaterialTheme.typography.headlineSmall, color = Color.White) }
        item { OutlinedTextField(name, { name = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("New category") }, colors = textFieldColors) }
        item {
            Button(
                onClick = { model.createCategory(name); name = "" },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0033))
            ) {
                Text("Create category", color = Color.White)
            }
        }
        items(categories, key = { it.id }) { category ->
            var edited by remember(category.name) { mutableStateOf(category.name) }
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF181818)), border = BorderStroke(1.dp, Color(0xFF282828))) {
                Column(Modifier.padding(16.dp)) {
                    OutlinedTextField(edited, { edited = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("Category name") }, colors = textFieldColors)
                    Row {
                        TextButton(onClick = { model.renameCategory(category.id, edited) }, enabled = edited.isNotBlank()) { Text("Rename", color = Color(0xFFFF0033)) }
                        TextButton(onClick = { model.moveCategory(category.id, -1) }) { Text("Up", color = Color.White) }
                        TextButton(onClick = { model.moveCategory(category.id, 1) }) { Text("Down", color = Color.White) }
                        TextButton(onClick = { deleting = category.id }) { Text("Delete", color = Color(0xFFFF4D4D)) }
                    }
                }
            }
        }
    }
    if (deleting != null) {
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = Color(0xFF181818),
            title = { Text("Delete category?", color = Color.White) },
            text = { Text("Approved videos remain available under Family Favorites and the main feed.", color = Color(0xFFAAAAAA)) },
            confirmButton = { TextButton(onClick = { model.deleteCategory(deleting!!); deleting = null }) { Text("Delete", color = Color(0xFFFF4D4D)) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel", color = Color(0xFFAAAAAA)) } }
        )
    }
}

@Composable
private fun CategorySelector(categories: List<Category>, selected: Long?, choose: (Long?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            border = BorderStroke(1.dp, Color(0xFF333333)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Text(categories.firstOrNull { it.id == selected }?.name ?: "No category")
        }
        DropdownMenu(
            expanded,
            { expanded = false },
            modifier = Modifier.background(Color(0xFF222222))
        ) {
            DropdownMenuItem(
                text = { Text("No category", color = Color.White) },
                onClick = { choose(null); expanded = false }
            )
            categories.forEach { c ->
                DropdownMenuItem(
                    text = { Text(c.name, color = Color.White) },
                    onClick = { choose(c.id); expanded = false }
                )
            }
        }
    }
}

private fun formatProgressTime(seconds: Long): String {
    val s = seconds.coerceAtLeast(0)
    val m = s / 60
    val remS = s % 60
    val h = m / 60
    val remM = m % 60
    return if (h > 0) {
        String.format("%d:%02d:%02d", h, remM, remS)
    } else {
        String.format("%d:%02d", remM, remS)
    }
}
