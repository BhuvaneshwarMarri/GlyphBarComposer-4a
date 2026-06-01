package com.smaarig.glyphbarcomposer.ui.hooks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.smaarig.glyphbarcomposer.data.NotificationHookWithPlaylist
import com.smaarig.glyphbarcomposer.ui.hooks.components.EmptyHooksView
import com.smaarig.glyphbarcomposer.ui.hooks.components.HookItem
import com.smaarig.glyphbarcomposer.ui.hooks.components.HooksHeader
import com.smaarig.glyphbarcomposer.ui.hooks.components.PermissionBanner
import com.smaarig.glyphbarcomposer.ui.screens.HookAddSheet
import com.smaarig.glyphbarcomposer.ui.theme.nothingFont
import com.smaarig.glyphbarcomposer.ui.viewmodel.AppInfo
import com.smaarig.glyphbarcomposer.ui.viewmodel.HooksViewModel
import kotlinx.coroutines.launch

// ─── Sheet navigation state ──────────────────────────────────────────────────

private sealed interface SheetState {
    data object AppPicker : SheetState
    data class HookConfig(val app: AppInfo) : SheetState
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HooksScreen(viewModel: HooksViewModel) {
    val hooks by viewModel.allHooks.collectAsState(initial = emptyList())
    val isPermissionGranted by viewModel.isPermissionGranted.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val playlists by viewModel.allPlaylists.collectAsState()
    val channels by viewModel.selectedAppChannels.collectAsState()
    val isLoadingChannels by viewModel.isLoadingChannels.collectAsState()
    val testResult by viewModel.testHookResult.collectAsState()
    val isBgServiceEnabled by viewModel.isBackgroundServiceEnabled.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sheetState by remember { mutableStateOf<SheetState?>(null) }
    var hookToDelete by remember { mutableStateOf<NotificationHookWithPlaylist?>(null) }

    val pickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Show test result snackbar via global host
    val snackbarHostState = com.smaarig.glyphbarcomposer.ui.LocalSnackbarHostState.current
    LaunchedEffect(testResult) {
        val msg = testResult ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(
            message = msg,
            duration = SnackbarDuration.Short
        )
        viewModel.clearTestResult()
    }

    fun dismissPickerSheet() {
        scope.launch { pickerSheetState.hide() }.invokeOnCompletion {
            if (!pickerSheetState.isVisible) sheetState = null
        }
    }

    fun dismissConfigSheet() {
        scope.launch { configSheetState.hide() }.invokeOnCompletion {
            if (!configSheetState.isVisible) {
                viewModel.clearSelectedChannels()
                sheetState = null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 16.dp, vertical = 24.dp)
    ) {
        HooksHeader(onAddClick = { sheetState = SheetState.AppPicker })

        AnimatedVisibility(
            visible = !isPermissionGranted,
            enter = fadeIn(tween(300)) + slideInVertically { -it },
            exit = fadeOut(tween(200))
        ) {
            Column {
                PermissionBanner { viewModel.openPermissionSettings(context) }
                Spacer(Modifier.height(16.dp))
            }
        }

        if (hooks.isEmpty() && isPermissionGranted) {
            EmptyHooksView()
        } else {
            HooksList(
                hooks = hooks,
                isBgServiceEnabled = isBgServiceEnabled,
                onBgServiceToggle = { viewModel.toggleBackgroundService(it) },
                onDelete = { hookToDelete = it },
                onToggle = { hook, enabled -> viewModel.toggleHook(hook.hook, enabled) },
                onTest = { viewModel.testHook(it, context) }
            )
        }
    }

    // Sheet 1: App Picker
    if (sheetState == SheetState.AppPicker) {
        ModalBottomSheet(
            onDismissRequest = { sheetState = null },
            sheetState = pickerSheetState,
            containerColor = Color(0xFF141414),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF444444)) }
        ) {
            AppPickerSheet(
                apps = installedApps,
                onAppSelected = { app ->
                    scope.launch { pickerSheetState.hide() }.invokeOnCompletion {
                        viewModel.loadChannelsForApp(app.packageName)
                        sheetState = SheetState.HookConfig(app)
                    }
                },
                onDismiss = ::dismissPickerSheet
            )
        }
    }

    // Sheet 2: Hook Config
    val configApp = (sheetState as? SheetState.HookConfig)?.app
    if (configApp != null) {
        ModalBottomSheet(
            onDismissRequest = {
                viewModel.clearSelectedChannels()
                sheetState = null
            },
            sheetState = configSheetState,
            containerColor = Color(0xFF141414),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF444444)) }
        ) {
            HookAddSheet(
                selectedApp = configApp,
                channels = channels,
                isLoadingChannels = isLoadingChannels,
                playlists = playlists,
                onConfirm = { channelId, channelName, playlistId, presetName, isProgressSync ->
                    viewModel.addHook(
                        packageName = configApp.packageName,
                        appName = configApp.appName,
                        playlistId = playlistId,
                        presetName = presetName,
                        isProgressSync = isProgressSync,
                        notificationChannelId = channelId,
                        notificationChannelName = channelName
                    )
                    dismissConfigSheet()
                },
                onDismiss = ::dismissConfigSheet
            )
        }
    }

    // Lifecycle: re-check permission on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.checkPermission()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Confirmation Dialog
    if (hookToDelete != null) {
        AlertDialog(
            onDismissRequest = { hookToDelete = null },
            title = { Text("Delete Hook?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will stop Glyph effects for ${hookToDelete?.hook?.appName}.",
                    color = Color.Gray
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    hookToDelete?.let { viewModel.deleteHook(it.hook) }
                    hookToDelete = null
                }) {
                    Text("DELETE", color = Color(0xFFFF5252), fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { hookToDelete = null }) {
                    Text("CANCEL", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF111111),
            shape = RoundedCornerShape(28.dp)
        )
    }
}

// ─── Hooks list with stable keys and no re-composition stutter ───────────────

@Composable
private fun HooksList(
    hooks: List<NotificationHookWithPlaylist>,
    isBgServiceEnabled: Boolean,
    onBgServiceToggle: (Boolean) -> Unit,
    onDelete: (NotificationHookWithPlaylist) -> Unit,
    onToggle: (NotificationHookWithPlaylist, Boolean) -> Unit,
    onTest: (NotificationHookWithPlaylist) -> Unit
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        item {
            BackgroundServiceCard(
                isEnabled = isBgServiceEnabled,
                onToggle = onBgServiceToggle,
                modifier = Modifier.animateItem()
            )
        }

        items(
            items = hooks,
            key = { it.hook.id }
        ) { hookWithPlaylist ->
            HookItem(
                hookWithPlaylist = hookWithPlaylist,
                onDelete = { onDelete(hookWithPlaylist) },
                onToggle = { enabled -> onToggle(hookWithPlaylist, enabled) },
                onTest = { onTest(hookWithPlaylist) },
                modifier = Modifier.animateItem(tween(250))
            )
        }
    }
}

@Composable
private fun BackgroundServiceCard(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF111111),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (isEnabled) Color(0xFF00C853) else Color(0xFF222222)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isEnabled) Color(0xFF00C853).copy(0.15f) else Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isEnabled) Icons.Default.CloudSync else Icons.Default.CloudOff,
                    contentDescription = null,
                    tint = if (isEnabled) Color(0xFF00C853) else Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Background Persistence",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp
                )
                Text(
                    if (isEnabled) "Active: Hooks run while app is closed" else "Inactive: Hooks stop if app is closed",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color(0xFF00C853),
                    uncheckedThumbColor = Color.Gray,
                    uncheckedTrackColor = Color(0xFF1A1A1A),
                    uncheckedBorderColor = Color(0xFF333333)
                ),
                modifier = Modifier.scale(0.8f)
            )
        }
    }
}

// ─── App Picker Sheet ────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppPickerSheet(
    apps: List<AppInfo>,
    onAppSelected: (AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    val filtered = remember(query, apps) {
        if (query.isBlank()) apps
        else apps.filter { it.appName.contains(query, ignoreCase = true) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Select App",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF888888))
            }
        }

        Spacer(Modifier.height(8.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search apps…", color = Color(0xFF666666)) },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = Color(0xFF888888)
                )
            },
            trailingIcon = if (query.isNotEmpty()) {
                {
                    IconButton(onClick = { query = "" }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color(0xFF888888)
                        )
                    }
                }
            } else null,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF444444),
                unfocusedBorderColor = Color(0xFF2A2A2A),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White,
                focusedContainerColor = Color(0xFF1E1E1E),
                unfocusedContainerColor = Color(0xFF1A1A1A)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(6.dp))

        Text(
            "Tap an app to set up its notification hook",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF555555),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (filtered.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No apps found", color = Color(0xFF444444))
            }
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    AppPickerRow(
                        app = app,
                        onInfoClick = { onAppSelected(app) },
                        modifier = Modifier.animateItem(tween(200))
                    )
                }
            }
        }
    }
}

// ─── App Picker Row ──────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppPickerRow(
    app: AppInfo,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(onClick = onInfoClick, onLongClick = onInfoClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppIcon(app = app, size = 42.dp)

        Column(Modifier.weight(1f)) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (app.isProgressOnly) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Progress sync only",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF00BFA5)
                )
            }
        }

        if (app.isProgressOnly) {
            Surface(shape = CircleShape, color = Color(0xFF00BFA5).copy(alpha = 0.12f)) {
                Text(
                    text = "Media",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF00BFA5),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        IconButton(onClick = onInfoClick, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = "Configure ${app.appName}",
                tint = Color(0xFF666666),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─── App Icon ────────────────────────────────────────────────────────────────

@Composable
private fun AppIcon(app: AppInfo, size: androidx.compose.ui.unit.Dp) {
    val bitmap = remember(app.packageName) {
        runCatching { app.icon?.toBitmap()?.asImageBitmap() }.getOrNull()
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(0xFF111111))
            .border(1.dp, Color(0xFF222222), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap = bitmap,
                contentDescription = app.appName,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        } else {
            Text(
                text = app.appName.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF888888),
                fontWeight = FontWeight.Black,
                fontFamily = nothingFont
            )
        }
    }
}