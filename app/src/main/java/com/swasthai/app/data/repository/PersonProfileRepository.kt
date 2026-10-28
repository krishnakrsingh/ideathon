package com.swasthai.app.data.repository

import android.content.Context
import com.swasthai.app.data.local.dao.PersonProfileDao
import com.swasthai.app.data.local.entity.DEFAULT_PERSON_PROFILE_ID
import com.swasthai.app.data.local.entity.PersonProfileEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class PersonProfileRepository(context: Context, private val profileDao: PersonProfileDao) {
    private val preferences = context.getSharedPreferences("person_profile_session", Context.MODE_PRIVATE)
    private val _activeProfileId = MutableStateFlow(
        preferences.getString(ACTIVE_PROFILE_KEY, DEFAULT_PERSON_PROFILE_ID) ?: DEFAULT_PERSON_PROFILE_ID
    )
    val activeProfileId: StateFlow<String> = _activeProfileId.asStateFlow()
    val profiles: Flow<List<PersonProfileEntity>> = profileDao.observeProfiles()

    suspend fun getProfile(profileId: String): PersonProfileEntity? = profileDao.getProfile(profileId)

    suspend fun createProfile(
        displayName: String,
        relationship: String,
        dateOfBirth: Long? = null,
        sexAtBirth: String? = null,
        avatarSeed: String = "avatar-01"
    ): String {
        val name = displayName.trim()
        require(name.isNotEmpty()) { "Enter a name for this profile" }
        require(dateOfBirth == null || dateOfBirth <= System.currentTimeMillis()) { "Date of birth cannot be in the future" }
        require(sexAtBirth == null || sexAtBirth in ALLOWED_SEX_AT_BIRTH) { "Choose a listed sex-at-birth option or leave it blank" }
        require(avatarSeed in AVATAR_SEEDS) { "Choose one of the available avatars" }
        val profileId = UUID.randomUUID().toString()
        profileDao.insert(
            PersonProfileEntity(
                id = profileId,
                displayName = name,
                relationship = relationship.trim().ifEmpty { "family" },
                dateOfBirth = dateOfBirth,
                sexAtBirth = sexAtBirth,
                avatarSeed = avatarSeed
            )
        )
        selectProfile(profileId)
        return profileId
    }

    suspend fun selectProfile(profileId: String) {
        require(profileDao.getProfile(profileId) != null) { "Profile not found on this device" }
        check(preferences.edit().putString(ACTIVE_PROFILE_KEY, profileId).commit()) {
            "Could not save the active profile"
        }
        _activeProfileId.value = profileId
    }

    companion object {
        private const val ACTIVE_PROFILE_KEY = "active_profile_id"
        private val ALLOWED_SEX_AT_BIRTH = setOf("Female", "Male", "Intersex", "Prefer not to say")
        val AVATAR_SEEDS = (1..12).map { "avatar-%02d".format(it) }.toSet()
    }
}
