package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY isFavorite DESC, lastAccessedAt DESC")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE spaceCategory = :category ORDER BY isFavorite DESC, lastAccessedAt DESC")
    fun getProfilesByCategory(category: String): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: Int): ProfileEntity?

    @Query("SELECT * FROM profiles WHERE id = :id")
    fun observeProfileById(id: Int): Flow<ProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Int)

    @Query("UPDATE profiles SET lastAccessedAt = :timestamp WHERE id = :id")
    suspend fun updateLastAccessed(id: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE profiles SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavorite(id: Int, isFav: Boolean)

    @Query("UPDATE profiles SET cookieCount = :count, dataUsageBytes = :bytes WHERE id = :id")
    suspend fun updateStats(id: Int, count: Int, bytes: Long)

    @Query("UPDATE profiles SET cookiesSnapshotJson = :cookies WHERE id = :id")
    suspend fun updateCookiesSnapshot(id: Int, cookies: String)

    @Query("DELETE FROM profiles")
    suspend fun deleteAllProfiles()

    @Query("SELECT COUNT(*) FROM profiles")
    fun getProfileCount(): Flow<Int>
}
