package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ProfileEntity
import com.example.data.repository.ProfileRepository
import com.example.security.SecurityPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class ScreenState {
    object Dashboard : ScreenState()
    data class SingleSandbox(val profile: ProfileEntity) : ScreenState()
    data class SplitSandbox(val topProfile: ProfileEntity, val bottomProfile: ProfileEntity) : ScreenState()
    object SecurityVault : ScreenState()
}

class MultiSpaceViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    val repository = ProfileRepository(database.profileDao())
    val securityPreferences = SecurityPreferences(application)

    // Security Unlock State
    private val _isVaultUnlocked = MutableStateFlow(!securityPreferences.hasPinConfigured())
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _isDecoyMode = MutableStateFlow(false)
    val isDecoyMode: StateFlow<Boolean> = _isDecoyMode.asStateFlow()

    // Navigation & Screen State
    private val _screenState = MutableStateFlow<ScreenState>(ScreenState.Dashboard)
    val screenState: StateFlow<ScreenState> = _screenState.asStateFlow()

    // Filtering & Search
    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Active Profile IDs
    private val _activeProfileId = MutableStateFlow<Int?>(null)
    val activeProfileId: StateFlow<Int?> = _activeProfileId.asStateFlow()

    // All Profiles
    val allProfiles: StateFlow<List<ProfileEntity>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Profiles
    val filteredProfiles: StateFlow<List<ProfileEntity>> = combine(
        allProfiles,
        _selectedCategory,
        _searchQuery,
        _isDecoyMode
    ) { profiles, category, query, isDecoy ->
        var list = if (isDecoy) {
            // In decoy mode, hide private / secret categories
            profiles.filter { it.spaceCategory != "Privado" && it.spaceCategory != "Finanzas" }
        } else {
            profiles
        }

        if (category != "Todos") {
            list = list.filter { it.spaceCategory.equals(category, ignoreCase = true) }
        }

        if (query.isNotBlank()) {
            list = list.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.appName.contains(query, ignoreCase = true) ||
                it.targetUrl.contains(query, ignoreCase = true)
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun unlockVault(isDecoy: Boolean) {
        _isDecoyMode.value = isDecoy
        _isVaultUnlocked.value = true
    }

    fun lockVault() {
        if (securityPreferences.hasPinConfigured()) {
            _isVaultUnlocked.value = false
            _screenState.value = ScreenState.Dashboard
        }
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun openSandbox(profile: ProfileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateLastAccessed(profile.id)
        }
        _activeProfileId.value = profile.id
        _screenState.value = ScreenState.SingleSandbox(profile)
    }

    fun openSplitSandbox(profile1: ProfileEntity, profile2: ProfileEntity) {
        _screenState.value = ScreenState.SplitSandbox(profile1, profile2)
    }

    fun closeSandboxToDashboard() {
        _screenState.value = ScreenState.Dashboard
    }

    fun openSecurityVaultSettings() {
        _screenState.value = ScreenState.SecurityVault
    }

    fun addProfile(profile: ProfileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val newId = repository.insertProfile(profile)
            val inserted = repository.getProfileById(newId.toInt())
            inserted?.let {
                _activeProfileId.value = it.id
                _screenState.value = ScreenState.SingleSandbox(it)
            }
        }
    }

    fun cloneAgain(sourceProfile: ProfileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val currentList = allProfiles.value
            val baseName = sourceProfile.appName.ifBlank {
                sourceProfile.name.replace(Regex(" (Cuenta|Clon) \\d+.*", RegexOption.IGNORE_CASE), "").trim()
            }
            val existingMatches = currentList.filter {
                it.appName.equals(baseName, ignoreCase = true) ||
                it.name.startsWith(baseName, ignoreCase = true) ||
                it.targetUrl == sourceProfile.targetUrl
            }
            val nextNumber = existingMatches.size + 1

            val newProfile = sourceProfile.copy(
                id = 0,
                name = "$baseName Cuenta $nextNumber",
                appName = baseName,
                createdAt = System.currentTimeMillis(),
                lastAccessedAt = System.currentTimeMillis(),
                cookieCount = 0,
                dataUsageBytes = 0,
                cookiesSnapshotJson = ""
            )
            val newId = repository.insertProfile(newProfile)
            val inserted = repository.getProfileById(newId.toInt())
            inserted?.let {
                _activeProfileId.value = it.id
                _screenState.value = ScreenState.SingleSandbox(it)
            }
        }
    }

    fun updateProfile(profile: ProfileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateProfile(profile)
        }
    }

    fun deleteProfile(profile: ProfileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteProfile(profile)
            if (_activeProfileId.value == profile.id) {
                _activeProfileId.value = null
                _screenState.value = ScreenState.Dashboard
            }
        }
    }

    fun toggleFavorite(profile: ProfileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleFavorite(profile.id, profile.isFavorite)
        }
    }

    fun updateProfileStats(id: Int, cookieCount: Int, dataBytes: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateStats(id, cookieCount, dataBytes)
        }
    }

    fun saveEncryptedNotes(profile: ProfileEntity, notes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = profile.copy(encryptedNotes = notes)
            repository.updateProfile(updated)
        }
    }

    fun deleteAllProfiles() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAllProfiles()
            _activeProfileId.value = null
            _screenState.value = ScreenState.Dashboard
        }
    }

    fun clearProfileData(profile: ProfileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = profile.copy(cookieCount = 0, dataUsageBytes = 0L)
            repository.updateProfile(updated)
        }
    }
}
