@file:OptIn(UnstableApi::class)

package it.leonardo.mediaprojector.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import it.leonardo.mediaprojector.PlayerViewModel

@Composable
fun PlayerScreen(vm: PlayerViewModel, onExit: () -> Unit) {
    val snackbar = remember { SnackbarHostState() }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmExit by remember { mutableStateOf(false) }

    // Il tasto Indietro è disabilitato per evitare uscite accidentali durante lo show: usare EXIT.
    BackHandler(enabled = true) { }

    LaunchedEffect(vm.errorMessage) {
        vm.errorMessage?.let {
            snackbar.showSnackbar(it)
            vm.clearError()
        }
    }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris -> if (uris.isNotEmpty()) vm.addUris(uris) }

    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {
            if (maxWidth >= 720.dp) {
                // Tablet: lista a sinistra, pannello di controllo a destra
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlaylistPanel(
                        vm = vm,
                        onAdd = { picker.launch(arrayOf("video/*", "audio/*")) },
                        onClear = { confirmClear = true },
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight()
                    )
                    ControlPanel(
                        vm = vm,
                        onExitRequest = { confirmExit = true },
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ControlPanel(vm = vm, onExitRequest = { confirmExit = true })
                    PlaylistPanel(
                        vm = vm,
                        onAdd = { picker.launch(arrayOf("video/*", "audio/*")) },
                        onClear = { confirmClear = true },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Svuotare la lista?") },
            text = { Text("Tutti i file verranno rimossi dalla lista (i file originali non vengono cancellati).") },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; vm.clearAll() }) { Text("SVUOTA") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Annulla") }
            }
        )
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Uscire dall'app?") },
            text = { Text("La riproduzione verrà fermata e lo schermo esterno tornerà libero.") },
            confirmButton = {
                TextButton(onClick = { confirmExit = false; onExit() }) { Text("ESCI") }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false }) { Text("Annulla") }
            }
        )
    }
}

// ======================= PANNELLO DI CONTROLLO =======================

@Composable
private fun ControlPanel(
    vm: PlayerViewModel,
    onExitRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {

        DisplayStatusCard(vm.externalDisplayName)

        // File corrente / selezionato + stato
        val current = vm.playlist.firstOrNull { it.id == (vm.nowPlayingId ?: vm.selectedId) }
        val status = when {
            vm.isPlaying -> "IN RIPRODUZIONE"
            vm.nowPlayingId != null -> "IN PAUSA"
            else -> "FERMO"
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text(status, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(
                    current?.name ?: "Nessun file selezionato",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Anteprima sul tablet solo se NON c'è uno schermo esterno
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (vm.externalDisplayName == null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                            setShutterBackgroundColor(android.graphics.Color.BLACK)
                            player = vm.player
                        }
                    },
                    onRelease = { it.player = null },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("Proiezione sullo schermo esterno", color = Color(0xFFB0B0B0))
            }
        }

        ProgressSection(vm)

        // Bottoni principali
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PanelButton(
                "PLAY", Icons.Default.PlayArrow, { vm.play() },
                ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), contentColor = Color.White)
            )
            PanelButton(
                "PAUSE", Icons.Default.Pause, { vm.pause() },
                ButtonDefaults.buttonColors(containerColor = Color(0xFFF9A825), contentColor = Color.Black)
            )
            PanelButton(
                "STOP", Icons.Default.Stop, { vm.stop() },
                ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828), contentColor = Color.White)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PanelButton(
                "PREVIOUS", Icons.Default.SkipPrevious, { vm.previous() },
                ButtonDefaults.filledTonalButtonColors()
            )
            PanelButton(
                "NEXT", Icons.Default.SkipNext, { vm.next() },
                ButtonDefaults.filledTonalButtonColors()
            )
            PanelButton(
                "EXIT", Icons.AutoMirrored.Filled.ExitToApp, onExitRequest,
                ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F), contentColor = Color.White)
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Avanza automaticamente al file successivo", Modifier.weight(1f))
            Switch(checked = vm.autoAdvance, onCheckedChange = { vm.updateAutoAdvance(it) })
        }
    }
}

@Composable
private fun DisplayStatusCard(displayName: String?) {
    val connected = displayName != null
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (connected) Color(0xFF1B5E20) else Color(0xFF5D4037)
        )
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Cast, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(10.dp))
            Text(
                if (connected) "Schermo esterno collegato: $displayName"
                else "Nessuno schermo esterno rilevato: anteprima sul tablet",
                color = Color.White
            )
        }
    }
}

@Composable
private fun ProgressSection(vm: PlayerViewModel) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }

    val duration = vm.durationMs
    val fraction = if (duration > 0) (vm.positionMs.toFloat() / duration).coerceIn(0f, 1f) else 0f

    Column {
        Slider(
            value = if (dragging) dragValue else fraction,
            onValueChange = { dragging = true; dragValue = it },
            onValueChangeFinished = {
                vm.seekTo((dragValue * duration).toLong())
                dragging = false
            },
            enabled = duration > 0
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(vm.positionMs), style = MaterialTheme.typography.labelMedium)
            Text(formatTime(duration), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun RowScope.PanelButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    colors: ButtonColors
) {
    Button(
        onClick = onClick,
        colors = colors,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .weight(1f)
            .height(80.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = text, modifier = Modifier.size(30.dp))
            Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

// ======================= LISTA =======================

@Composable
private fun PlaylistPanel(
    vm: PlayerViewModel,
    onAdd: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Playlist (${vm.playlist.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Aggiungi")
                }
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onClear, enabled = vm.playlist.isNotEmpty()) {
                    Icon(Icons.Default.ClearAll, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Svuota")
                }
            }
            HorizontalDivider()

            if (vm.playlist.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Nessun file.\nPremi «Aggiungi» per caricare video MP4 e audio MP3.",
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(vm.playlist, key = { _, e -> e.id }) { index, entry ->
                        val selected = entry.id == vm.selectedId
                        val playingNow = entry.id == vm.nowPlayingId && vm.isPlaying
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .background(
                                    if (selected) MaterialTheme.colorScheme.secondaryContainer
                                    else Color.Transparent
                                )
                                .clickable { vm.select(entry.id) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (playingNow) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "In riproduzione",
                                    tint = Color(0xFF66BB6A),
                                    modifier = Modifier.width(32.dp)
                                )
                            } else {
                                Text(
                                    "${index + 1}",
                                    modifier = Modifier.width(32.dp),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Icon(
                                if (entry.isVideo) Icons.Default.Movie else Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 10.dp)
                            )
                            Column(Modifier.weight(1f)) {
                                Text(
                                    entry.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    if (entry.isVideo) "Video MP4" else "Audio MP3",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            IconButton(onClick = { vm.playItem(entry.id) }) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Riproduci questo")
                            }
                            IconButton(onClick = { vm.move(entry.id, -1) }, enabled = index > 0) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Sposta su")
                            }
                            IconButton(
                                onClick = { vm.move(entry.id, +1) },
                                enabled = index < vm.playlist.lastIndex
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = "Sposta giù")
                            }
                            IconButton(onClick = { vm.remove(entry.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Rimuovi")
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
