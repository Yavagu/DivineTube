package com.yourtube.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourtube.app.AppState
import com.yourtube.app.AppViewModel
import com.yourtube.app.data.local.Category

private val Palette = lightColorScheme(primary = Color(0xFF395AB5), secondary = Color(0xFF277D71),
    tertiary = Color(0xFF9A5C1F), background = Color(0xFFF6F7FC), surface = Color.White)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun YourTubeApp(model: AppViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    val entries by model.entries.collectAsStateWithLifecycle()
    val categories by model.categories.collectAsStateWithLifecycle()
    var selected by rememberSaveable { mutableStateOf<Long?>(null) }
    var privacy by remember { mutableStateOf(false) }
    val online = rememberOnline()
    MaterialTheme(colorScheme = Palette) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).combinedClickable(onClick = {}, onLongClick = model::showGate)) {
                        Text("YourTube", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Palette.primary)
                        Text(if (state.parent) "Parent studio" else "A little library of big discoveries", style = MaterialTheme.typography.bodyMedium)
                    }
                    if (state.parent) TextButton(onClick = model::lock) { Text("Done & lock") }
                }
                if (!online) Text("You're offline. Your saved library is here; playback needs internet.", Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                if (!state.ready) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(state.message ?: "Opening your library…", Modifier.padding(24.dp))
                } else if (state.playback != null) {
                    PlayerScreen(state.playback!!, model, Modifier.weight(1f))
                } else if (state.parent) {
                    ParentScreen(model, state, categories, Modifier.weight(1f))
                } else {
                    if (categories.isNotEmpty()) {
                        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 20.dp)) {
                            item { FilterChip(selected == null, { selected = null }, label = { Text("All favorites") }) }
                            items(categories, key = { it.id }) { category ->
                                Column(Modifier.width(140.dp)) {
                                    Thumbnail(entries.firstOrNull { it.categoryId == category.id }?.videoIds?.substringBefore(','), Modifier.fillMaxWidth().height(72.dp))
                                    FilterChip(selected == category.id, { selected = category.id }, label = { Text(category.name) })
                                }
                            }
                        }
                    }
                    val visible = entries.filter { selected == null || it.categoryId == selected }
                    if (visible.isEmpty()) {
                        Column(Modifier.weight(1f).fillMaxWidth().padding(32.dp), Arrangement.Center, Alignment.CenterHorizontally) {
                            Text("Your next adventure awaits", style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.height(12.dp))
                            Text("Your saved favorites will appear here.")
                        }
                    } else {
                        LazyVerticalGrid(columns = GridCells.Adaptive(250.dp), modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            gridItems(visible, key = { it.id }) { entry ->
                                Card(onClick = { model.play(entry) }) {
                                    Thumbnail(entry.videoIds.substringBefore(','), Modifier.fillMaxWidth().height(155.dp))
                                    Column(Modifier.padding(16.dp)) {
                                        Text(entry.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text(if (entry.playlistId == null) "Watch on YouTube" else "${entry.videoIds.split(',').size} saved videos · YouTube",
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                    TextButton(onClick = { privacy = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Privacy & YouTube") }
                }
            }
            if (state.gate && !state.parent) PinDialog(state, model)
            if (privacy) AlertDialog(onDismissRequest = { privacy = false }, title = { Text("Privacy & YouTube") },
                text = { PrivacyNotice() }, confirmButton = { TextButton(onClick = { privacy = false }) { Text("Close") } })
        }
        BackHandler(state.parent || state.gate || state.playback != null || selected != null) {
            when { state.playback != null -> model.closePlayback(); state.parent || state.gate -> model.lock(); else -> selected = null }
        }
    }
}

@Composable
private fun PinDialog(state: AppState, model: AppViewModel) {
    var pin by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = model::lock, title = { Text(if (state.configured) "Parent access" else "Set your parent PIN") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Use 6–12 digits. Forgotten PINs require clearing all app data, including the library.")
                OutlinedTextField(pin, { if (it.length <= 12 && it.all(Char::isDigit)) pin = it }, label = { Text("PIN") }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                if (!state.configured) {
                    OutlinedTextField(confirmation, { if (it.length <= 12 && it.all(Char::isDigit)) confirmation = it }, label = { Text("Confirm PIN") },
                        singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
                    PrivacyNotice()
                    Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(consent, { consent = it }); Text("I accept YouTube's terms and understand these limitations.") }
                }
                state.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }, confirmButton = { TextButton(enabled = !state.busy, onClick = { model.authenticate(pin, confirmation, consent); pin = ""; confirmation = "" }) { Text(if (state.configured) "Unlock" else "Create PIN") } },
        dismissButton = { TextButton(onClick = model::lock) { Text("Cancel") } })
}

@Composable
fun PrivacyNotice() {
    val uri = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Approvals and PIN verification data stay on this device. There are no analytics or accounts. YouTube receives your IP address and device/app information when thumbnails, search, or its embedded player load.")
        Text("YouTube controls its player, ads, recommendations, and links. These can show content beyond your approvals. YourTube is not a kiosk or a guarantee of content safety. Review each video yourself.")
        Text("YouTube Terms: https://www.youtube.com/t/terms\nGoogle Privacy: https://policies.google.com/privacy", style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = { uri.openUri("https://www.youtube.com/t/terms") }) { Text("Read YouTube terms") }
        TextButton(onClick = { uri.openUri("https://policies.google.com/privacy") }) { Text("Read Google privacy policy") }
    }
}

@Composable
private fun ParentScreen(model: AppViewModel, state: AppState, categories: List<Category>, modifier: Modifier) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("Add", "Library", "Categories").forEachIndexed { i, title ->
                FilterChip(tab == i, { tab = i }, label = { Text(title) })
            }
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.message?.let { Text(it, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.primary) }
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
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Choose their next favorite", style = MaterialTheme.typography.headlineSmall)
            Text("Paste a YouTube link, share from YouTube, or search below. Approval is always your choice.")
        }
        item { OutlinedTextField(link, { link = it }, Modifier.fillMaxWidth(), label = { Text("YouTube video or playlist link") }) }
        item { OutlinedTextField(title, { title = it.take(200) }, Modifier.fillMaxWidth(), label = { Text("Library title (optional)") }) }
        item { OutlinedTextField(manual, { manual = it.take(64000) }, Modifier.fillMaxWidth(), label = { Text("Playlist snapshot links (optional)") }, supportingText = { Text("Without an API key, paste one reviewed video link per line for a playlist. Only these videos will be saved.") }, minLines = 2) }
        item { Button(onClick = { model.prepareLink(link, title, manual) }, enabled = !state.busy) { Text("Review link") } }
        state.review?.let { review ->
            item {
                Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(review.title, model::labelReview, label = { Text("Your library label") }, supportingText = { Text("A label you choose; not live YouTube metadata.") })
                    Text("Review all ${review.ids.size} video(s). Playlist updates require a new review and approval.")
                    CategorySelector(categories, category) { category = it }
                    Button(onClick = { model.approve(category) }, enabled = !state.busy) { Text("Approve these ${review.ids.size} video(s)") }
                    TextButton(onClick = model::clearReview) { Text("Cancel review") }
                } }
            }
            items(review.ids, key = { it }) { id ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(id, Modifier.weight(1f))
                    TextButton(onClick = { model.preview(id, review.title) }) { Text("Preview") }
                }
            }
        }
        item { HorizontalDivider(); Text("Search YouTube", style = MaterialTheme.typography.titleLarge) }
        item { Text("Search and playback checks need a configured YouTube Data API key. Search uses API quota; saved browsing works offline.") }
        item { OutlinedTextField(query, { query = it.take(200) }, Modifier.fillMaxWidth(), label = { Text("Search query") }) }
        item { Row(verticalAlignment = Alignment.CenterVertically) { Switch(playlists, { playlists = it }); Text("Search playlists", Modifier.padding(start = 12.dp)) } }
        item { Button(onClick = { model.search(query, playlists) }, enabled = !state.busy) { Text("Search") } }
        items(state.results, key = { it.id }) { result ->
            Card { Column(Modifier.padding(16.dp)) {
                Text(result.title, fontWeight = FontWeight.Bold)
                Row {
                    if (!result.isPlaylist) TextButton(onClick = { model.preview(result.id, result.title) }) { Text("Preview") }
                    TextButton(onClick = { model.reviewResult(result) }, enabled = !state.busy) { Text("Review for approval") }
                }
            } }
        }
        if (state.nextPage != null) item { TextButton(onClick = { model.search(query, playlists, true) }, enabled = !state.busy) { Text("Load more results") } }
        item { PrivacyNotice() }
    }
}

@Composable
private fun LibraryPanel(model: AppViewModel, categories: List<Category>) {
    val entries by model.entries.collectAsStateWithLifecycle()
    var removing by remember { mutableStateOf<Long?>(null) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Approved library", style = MaterialTheme.typography.headlineSmall) }
        if (entries.isEmpty()) item { Text("Nothing approved yet. Use Add to start your library.") }
        items(entries, key = { it.id }) { entry ->
            Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(entry.title, fontWeight = FontWeight.Bold)
                Text("${entry.videoIds.split(',').size} saved video(s)")
                CategorySelector(categories, entry.categoryId) { model.assignCategory(entry.id, it) }
                Row {
                    TextButton(onClick = { model.play(entry) }) { Text("Play") }
                    TextButton(onClick = { model.moveEntry(entry.id, -1) }) { Text("Up") }
                    TextButton(onClick = { model.moveEntry(entry.id, 1) }) { Text("Down") }
                    TextButton(onClick = { removing = entry.id }) { Text("Remove") }
                }
                entry.playlistId?.let { playlist -> TextButton(onClick = { model.prepareLink("https://www.youtube.com/playlist?list=$playlist", entry.title); }) { Text("Fetch refreshed snapshot (review in Add)") } }
            } }
        }
    }
    if (removing != null) AlertDialog(onDismissRequest = { removing = null }, title = { Text("Remove approval?") },
        text = { Text("This item will disappear from Kids Mode.") }, confirmButton = { TextButton(onClick = { model.removeEntry(removing!!); removing = null }) { Text("Remove") } },
        dismissButton = { TextButton(onClick = { removing = null }) { Text("Cancel") } })
}

@Composable
private fun CategoriesPanel(model: AppViewModel, categories: List<Category>) {
    var name by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<Long?>(null) }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Make room for their interests", style = MaterialTheme.typography.headlineSmall) }
        item { OutlinedTextField(name, { name = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("New category") }) }
        item { Button(onClick = { model.createCategory(name); name = "" }, enabled = name.isNotBlank()) { Text("Create category") } }
        items(categories, key = { it.id }) { category ->
            var edited by remember(category.name) { mutableStateOf(category.name) }
            Card { Column(Modifier.padding(16.dp)) {
                OutlinedTextField(edited, { edited = it.take(80) }, Modifier.fillMaxWidth(), label = { Text("Category name") })
                Row {
                    TextButton(onClick = { model.renameCategory(category.id, edited) }, enabled = edited.isNotBlank()) { Text("Rename") }
                    TextButton(onClick = { model.moveCategory(category.id, -1) }) { Text("Up") }
                    TextButton(onClick = { model.moveCategory(category.id, 1) }) { Text("Down") }
                    TextButton(onClick = { deleting = category.id }) { Text("Delete") }
                }
            } }
        }
    }
    if (deleting != null) AlertDialog(onDismissRequest = { deleting = null }, title = { Text("Delete category?") },
        text = { Text("Approved videos remain available under All favorites.") }, confirmButton = { TextButton(onClick = { model.deleteCategory(deleting!!); deleting = null }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } })
}

@Composable
private fun CategorySelector(categories: List<Category>, selected: Long?, choose: (Long?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(categories.firstOrNull { it.id == selected }?.name ?: "No category") }
        DropdownMenu(expanded, { expanded = false }) {
            DropdownMenuItem(text = { Text("No category") }, onClick = { choose(null); expanded = false })
            categories.forEach { c -> DropdownMenuItem(text = { Text(c.name) }, onClick = { choose(c.id); expanded = false }) }
        }
    }
}
