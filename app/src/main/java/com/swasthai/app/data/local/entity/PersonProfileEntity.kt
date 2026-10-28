package com.swasthai.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

const val DEFAULT_PERSON_PROFILE_ID = "local-default-profile"

@Entity(tableName = "person_profiles")
data class PersonProfileEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val relationship: String,
    val dateOfBirth: Long? = null,
    val sexAtBirth: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isOwner: Boolean = false,
    val avatarSeed: String = "avatar-01"
)
