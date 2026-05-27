package com.smaarig.glyphbarcomposer.ui.hooks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val installedApps       by viewModel.installedApps.collectAsState()
    val playlists           by viewModel.allPlaylists.collectAsState()
    val channels            by viewModel.selectedAppChannels.collectAsState()
    val isLoadingChannels   by viewModel.isLoadingChannels.collectAsState()
    val testResult          by viewModel.testHookResult.collectAsState()

    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var sheetState by remember { mutableStateOf<SheetState?>(null) }

    val pickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val configSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Show test result snackbar
    val snackbarHostState = remember { SnackbarHostState() }
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

    Scaffold(
        containerColor = Color(0xFF0A0A0A),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color(0xFF1E1E1E),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
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

            if (hooks.isEmpty()) {
                EmptyHooksView()
            } else {
                HooksList(
                    hooks = hooks,
                    onDelete = { viewModel.deleteHook(it.hook) },
                    onToggle = { hook, enabled -> viewModel.toggleHook(hook.hook, enabled) },
                    onTest   = { viewModel.testHook(it, context) }
                )
            }
        }
    }

    // Sheet 1: App Picker
    if (sheetState == SheetState.AppPicker) {
        ModalBottomSheet(
            onDismissRequest = { sheetState = null },
            sheetState       = pickerSheetState,
            containerColor   = Color(0xFF141414),
            dragHandle       = { BottomSheetDefaults.DragHandle(color = Color(0xFF444444)) }
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
            sheetState     = configSheetState,
            containerColor = Color(0xFF141414),
            dragHandle     = { BottomSheetDefaults.DragHandle(color = Color(0xFF444444)) }
        ) {
            HookAddSheet(
                selectedApp       = configApp,
                channels          = channels,
                isLoadingChannels = isLoadingChannels,
                playlists         = playlists,
                onConfirm         = { channelId, channelName, playlistId, isProgressSync ->
                    viewModel.addHook(
                        packageName             = configApp.packageName,
                        appName                 = configApp.appName,
                        playlistId              = playlistId,
                        isProgressSync          = isProgressSync,
                        notificationChannelId   = channelId,
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
}

// ─── Hooks list with stable keys and no re-composition stutter ───────────────

@Composable
private fun HooksList(
    hooks: List<NotificationHookWithPlaylist>,
    onDelete: (NotificationHookWithPlaylist) -> Unit,
    onToggle: (NotificationHookWithPlaylist, Boolean) -> Unit,
    onTest:   (NotificationHookWithPlaylist) -> Unit
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        items(
            items = hooks,
            key   = { it.hook.id }
        ) { hookWithPlaylist ->
            HookItem(
                hookWithPlaylist = hookWithPlaylist,
                onDelete  = { onDelete(hookWithPlaylist) },
                onToggle  = { enabled -> onToggle(hookWithPlaylist, enabled) },
                onTest    = { onTest(hookWithPlaylist) },
                modifier  = Modifier.animateItem(tween(250))
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
            value         = query,
            onValueChange = { query = it },
            placeholder   = { Text("Search apps…", color = Color(0xFF666666)) },
            leadingIcon   = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF888888)) },
            trailingIcon  = if (query.isNotEmpty()) {
                { IconButton(onClick = { query = "" }) { Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF888888)) } }
            } else null,
            singleLine      = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            shape  = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor      = Color(0xFF444444),
                unfocusedBorderColor    = Color(0xFF2A2A2A),
                focusedTextColor        = Color.White,
                unfocusedTextColor      = Color.White,
                cursorColor             = Color.White,
                focusedContainerColor   = Color(0xFF1E1E1E),
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
                        app         = app,
                        onInfoClick = { onAppSelected(app) },
                        modifier    = Modifier.animateItem(tween(200))
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
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppIcon(app = app, size = 42.dp)

        Column(Modifier.weight(1f)) {
            Text(
                text       = app.appName,
                style      = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color      = Color.White,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            if (app.isProgressOnly) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = "Progress sync only",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF00BFA5)
                )
            }
        }

        if (app.isProgressOnly) {
            Surface(shape = CircleShape, color = Color(0xFF00BFA5).copy(alpha = 0.12f)) {
                Text(
                    text     = "Media",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = Color(0xFF00BFA5),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        IconButton(onClick = onInfoClick, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector        = Icons.Outlined.Info,
                contentDescription = "Configure ${app.appName}",
                tint               = Color(0xFF666666),
                modifier           = Modifier.size(20.dp)
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
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF222222)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            androidx.compose.foundation.Image(
                bitmap             = bitmap,
                contentDescription = app.appName,
                modifier           = Modifier.size(size)
            )
        } else {
            Text(
                text       = app.appName.firstOrNull()?.uppercase() ?: "?",
                style      = MaterialTheme.typography.bodyLarge,
                color      = Color(0xFF888888),
                fontWeight = FontWeight.Bold
            )
        }
    }
}