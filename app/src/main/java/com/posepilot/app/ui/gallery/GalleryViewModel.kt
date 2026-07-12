package com.posepilot.app.ui.gallery

import android.app.RecoverableSecurityException
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.posepilot.app.data.db.SessionRecord
import com.posepilot.app.data.db.SessionRecordDao
import com.posepilot.app.data.repository.GalleryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    private val galleryRepository: GalleryRepository,
    private val sessionRecordDao: SessionRecordDao
) : ViewModel() {

    private val _photos = MutableStateFlow<List<Uri>>(emptyList())
    val photos: StateFlow<List<Uri>> = _photos.asStateFlow()

    val sessionRecords: StateFlow<List<SessionRecord>> = sessionRecordDao.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _pendingDeleteUri = MutableStateFlow<Uri?>(null)
    val pendingDeleteUri: StateFlow<Uri?> = _pendingDeleteUri.asStateFlow()

    private val _securityException = MutableStateFlow<RecoverableSecurityException?>(null)
    val securityException: StateFlow<RecoverableSecurityException?> = _securityException.asStateFlow()

    init {
        loadPhotos()
    }

    fun loadPhotos() {
        viewModelScope.launch {
            val list = galleryRepository.getPhotos()
            _photos.value = list
        }
    }

    fun deletePhoto(uri: Uri) {
        viewModelScope.launch {
            try {
                val success = galleryRepository.deletePhoto(uri)
                if (success) {
                    _photos.value = _photos.value.filter { it != uri }
                }
            } catch (securityException: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && securityException is RecoverableSecurityException) {
                    _securityException.value = securityException
                    _pendingDeleteUri.value = uri
                }
            }
        }
    }

    fun clearPendingDelete() {
        _pendingDeleteUri.value = null
        _securityException.value = null
    }

    fun handleDeletedPending() {
        val uri = _pendingDeleteUri.value
        if (uri != null) {
            _photos.value = _photos.value.filter { it != uri }
            clearPendingDelete()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            sessionRecordDao.clearAllRecords()
        }
    }
}
