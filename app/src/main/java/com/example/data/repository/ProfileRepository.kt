package com.example.data.repository

import com.example.data.local.ProfileDao
import com.example.data.local.ProfileEntity
import kotlinx.coroutines.flow.Flow

class ProfileRepository(private val profileDao: ProfileDao) {
    val allProfiles: Flow<List<ProfileEntity>> = profileDao.getAllProfiles()
    val totalCount: Flow<Int> = profileDao.getProfileCount()

    fun getProfilesByCategory(category: String): Flow<List<ProfileEntity>> {
        return if (category == "Todos") {
            profileDao.getAllProfiles()
        } else {
            profileDao.getProfilesByCategory(category)
        }
    }

    suspend fun getProfileById(id: Int): ProfileEntity? = profileDao.getProfileById(id)

    fun observeProfileById(id: Int): Flow<ProfileEntity?> = profileDao.observeProfileById(id)

    suspend fun insertProfile(profile: ProfileEntity): Long = profileDao.insertProfile(profile)

    suspend fun updateProfile(profile: ProfileEntity) = profileDao.updateProfile(profile)

    suspend fun deleteProfile(profile: ProfileEntity) = profileDao.deleteProfile(profile)

    suspend fun deleteProfileById(id: Int) = profileDao.deleteProfileById(id)

    suspend fun updateLastAccessed(id: Int) = profileDao.updateLastAccessed(id)

    suspend fun toggleFavorite(id: Int, currentFav: Boolean) = profileDao.updateFavorite(id, !currentFav)

    suspend fun updateStats(id: Int, cookieCount: Int, dataBytes: Long) =
        profileDao.updateStats(id, cookieCount, dataBytes)

    suspend fun updateCookiesSnapshot(id: Int, cookies: String) =
        profileDao.updateCookiesSnapshot(id, cookies)
}
