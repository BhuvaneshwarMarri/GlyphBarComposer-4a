package com.smaarig.glyphbarcomposer.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smaarig.glyphbarcomposer.data.ContactBindingWithPlaylist
import com.smaarig.glyphbarcomposer.data.PlaylistWithSteps
import com.smaarig.glyphbarcomposer.ui.viewmodel.ContactItem
import com.smaarig.glyphbarcomposer.ui.viewmodel.ContactRingtoneViewModel

@Composable
fun ContactRingtoneScreen(
    viewModel: ContactRingtoneViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val allPlaylists by viewModel.allPlaylists.collectAsStateWithLifecycle(initialValue = emptyList())
    val contactBindings by viewModel.allContactBindings.collectAsStateWithLifecycle(initialValue = emptyList())
    
    val context = LocalContext.current
    val permissions = listOf(
        Manifest.permission.READ_CONTACTS,
        Manifest.permission.READ_PHONE_STATE,
        Manifest.permission.READ_CALL_LOG
    )
    
    var hasPermissions by remember {
        mutableStateOf(
            permissions.all { 
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED 
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = results.values.all { it }
        if (hasPermissions) viewModel.loadContacts()
    }

    LaunchedEffect(hasPermissions) {
        if (hasPermissions) viewModel.loadContacts()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Contact Sync",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Button(
                onClick = viewModel::clearAllBindings,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Reset All", fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Assign custom light patterns to your contacts.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        if (!hasPermissions) {
            PermissionRequiredContent { permissionLauncher.launch(permissions.toTypedArray()) }
        } else {
            // Bindings Header
            if (contactBindings.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        "ACTIVE BINDINGS",
                        color = Color(0xFF555555),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(0.4f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(contactBindings) { binding ->
                        BindingCard(
                            binding = binding,
                            onDelete = { viewModel.deleteBinding(binding.binding) }
                        )
                    }
                }
                
                Spacer(Modifier.height(24.dp))
            }

            // Contact List Header
            var searchQuery by remember { mutableStateOf("") }
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search contacts...", color = Color.Gray, fontSize = 14.sp) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color(0xFF222222)
                )
            )

            Spacer(Modifier.height(12.dp))

            val filteredContacts = uiState.contacts.filter { 
                it.name.contains(searchQuery, ignoreCase = true) || 
                (it.phoneNumber?.contains(searchQuery) ?: false)
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filteredContacts) { contact ->
                    val isAlreadyBound = contactBindings.any { it.binding.contactId == contact.id }
                    ContactRow(
                        contact = contact,
                        isBound = isAlreadyBound,
                        onClick = { if (!isAlreadyBound) viewModel.openBindingDialog(contact) }
                    )
                }
            }
        }
    }

    if (uiState.selectedContact != null) {
        PlaylistSelectionDialog(
            contactName = uiState.selectedContact!!.name,
            playlists = allPlaylists,
            onDismiss = viewModel::closeBindingDialog,
            onSelect = { viewModel.updateContactBinding(it) }
        )
    }
}

@Composable
private fun PermissionRequiredContent(onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Contacts, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color(0xFF222222))
        Spacer(Modifier.height(16.dp))
        Text("Full Access Required", color = Color.White, fontWeight = FontWeight.Bold)
        Text(
            "We need Contacts, Phone, and Call Log permissions to reliably trigger light sequences when someone calls.",
            color = Color.Gray,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Grant Permissions")
        }
    }
}

@Composable
private fun BindingCard(
    binding: ContactBindingWithPlaylist,
    onDelete: () -> Unit
) {
    Surface(
        color = Color(0xFF161616),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00C853).copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(32.dp).background(Color(0xFF00C853).copy(alpha = 0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF00C853), modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(binding.binding.contactName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Loop: ${binding.playlist.name}", color = Color(0xFF00C853), fontSize = 11.sp)
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Reset to Default", tint = Color.Gray, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ContactRow(
    contact: ContactItem,
    isBound: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = Color(0xFF111111),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().clickable(enabled = !isBound) { onClick() },
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isBound) Color(0xFF222222) else Color(0xFF1A1A1A))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(40.dp).background(Color(0xFF1A1A1A), CircleShape), contentAlignment = Alignment.Center) {
                Text(contact.name.take(1).uppercase(), color = Color.Gray, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(contact.name, color = if (isBound) Color.Gray else Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                if (contact.phoneNumber != null) {
                    Text(contact.phoneNumber, color = Color(0xFF444444), fontSize = 12.sp)
                }
            }
            if (isBound) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF00C853), modifier = Modifier.size(16.dp))
            } else {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun PlaylistSelectionDialog(
    contactName: String,
    playlists: List<PlaylistWithSteps>,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Light Pattern for $contactName", color = Color.White, fontSize = 18.sp) },
        text = {
            if (playlists.isEmpty()) {
                Text("No saved sequences found. Go to Composer to create one!", color = Color.Gray)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(playlists) { item ->
                        Surface(
                            color = Color(0xFF252525),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().clickable { onSelect(item.playlist.id) }
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(12.dp))
                                Text(item.playlist.name, color = Color.White)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.White) }
        },
        containerColor = Color(0xFF161616)
    )
}
