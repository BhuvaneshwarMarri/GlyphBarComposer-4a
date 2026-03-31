package com.smaarig.glyphbarcomposer.ui.viewmodel

import android.app.Application
import android.provider.ContactsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smaarig.glyphbarcomposer.controller.GlyphController
import com.smaarig.glyphbarcomposer.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ContactItem(val id: String, val name: String, val phoneNumber: String?)

data class ContactRingtoneUiState(
    val contacts: List<ContactItem> = emptyList(),
    val isLoadingContacts: Boolean = false,
    val selectedContact: ContactItem? = null,
    val isResetting: Boolean = false
)

class ContactRingtoneViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val playlistDao = db.playlistDao()
    private val glyphController = GlyphController.getInstance(application)

    private val _uiState = MutableStateFlow(ContactRingtoneUiState())
    val uiState: StateFlow<ContactRingtoneUiState> = _uiState.asStateFlow()

    val allPlaylists: Flow<List<PlaylistWithSteps>> = playlistDao.getAllPlaylists()
    val allContactBindings: Flow<List<ContactBindingWithPlaylist>> = playlistDao.getAllContactBindings()

    fun loadContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingContacts = true) }
            val contactList = withContext(Dispatchers.IO) {
                val list = mutableListOf<ContactItem>()
                val cursor = getApplication<Application>().contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    null, null, null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
                )
                cursor?.use {
                    val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val phoneIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    while (it.moveToNext()) {
                        val id = it.getString(idIdx)
                        val name = it.getString(nameIdx)
                        val phone = it.getString(phoneIdx)
                        if (list.none { item -> item.id == id }) {
                            list.add(ContactItem(id, name, phone))
                        }
                    }
                }
                list
            }
            _uiState.update { it.copy(contacts = contactList, isLoadingContacts = false) }
        }
    }

    fun openBindingDialog(contact: ContactItem) {
        _uiState.update { it.copy(selectedContact = contact) }
    }

    fun closeBindingDialog() {
        _uiState.update { it.copy(selectedContact = null) }
    }

    fun updateContactBinding(playlistId: Long) {
        val contact = _uiState.value.selectedContact ?: return
        viewModelScope.launch {
            playlistDao.insertContactBinding(ContactBinding(contact.id, contact.name, playlistId))
            closeBindingDialog()
        }
    }

    fun deleteBinding(binding: ContactBinding) {
        viewModelScope.launch {
            playlistDao.deleteContactBinding(binding)
        }
    }

    fun clearAllBindings() {
        viewModelScope.launch {
            val bindings = playlistDao.getContactBindingsList()
            bindings.forEach { playlistDao.deleteContactBinding(it.binding) }
        }
    }

    fun resetGlyphHardware() {
        viewModelScope.launch {
            _uiState.update { it.copy(isResetting = true) }
            glyphController.turnOffGlyphs()
            _uiState.update { it.copy(isResetting = false) }
        }
    }
}
