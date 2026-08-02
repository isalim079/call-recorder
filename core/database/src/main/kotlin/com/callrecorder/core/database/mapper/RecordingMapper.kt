package com.callrecorder.core.database.mapper

import com.callrecorder.core.database.entity.RecordingEntity
import com.callrecorder.core.domain.model.AudioQuality
import com.callrecorder.core.domain.model.CallType
import com.callrecorder.core.domain.model.Recording

/**
 * Bidirectional mapper between [RecordingEntity] (database layer) and
 * [Recording] (domain layer).
 *
 * Intentionally stateless — all functions are top-level or extension functions
 * for clean, testable transformations without any Android framework dependency.
 *
 * Design note: Enums are serialized as their [name] strings in the database
 * for readability; they are parsed back with safe [enumValueOfOrDefault] to
 * handle forward-compatibility (new enum values in newer app versions).
 */

// ── Entity → Domain ────────────────────────────────────────────────────────

/**
 * Convert a [RecordingEntity] to a domain [Recording].
 */
fun RecordingEntity.toDomain(): Recording = Recording(
    id              = id,
    phoneNumber     = phoneNumber,
    contactName     = contactName,
    contactPhotoUri = contactPhotoUri,
    callType        = enumValueOfOrDefault(callType, CallType.UNKNOWN),
    durationMs      = durationMs,
    timestamp       = timestamp,
    filePath        = filePath,
    fileSizeBytes   = fileSizeBytes,
    quality         = enumValueOfOrDefault(quality, AudioQuality.MEDIUM),
    isFavorite      = isFavorite,
    isDeleted       = isDeleted,
    notes           = notes,
)

/**
 * Convert a list of [RecordingEntity] to domain [Recording] objects.
 */
fun List<RecordingEntity>.toDomain(): List<Recording> = map { it.toDomain() }

// ── Domain → Entity ────────────────────────────────────────────────────────

/**
 * Convert a domain [Recording] to a [RecordingEntity] for database storage.
 */
fun Recording.toEntity(): RecordingEntity = RecordingEntity(
    id              = id,
    phoneNumber     = phoneNumber,
    contactName     = contactName,
    contactPhotoUri = contactPhotoUri,
    callType        = callType.name,
    durationMs      = durationMs,
    timestamp       = timestamp,
    filePath        = filePath,
    fileSizeBytes   = fileSizeBytes,
    quality         = quality.name,
    isFavorite      = isFavorite,
    isDeleted       = isDeleted,
    notes           = notes,
)

// ── Utility ────────────────────────────────────────────────────────────────

/**
 * Parse an enum value by name, returning [default] if the name is unknown.
 * Handles forward-compatibility: if a new enum value is added in a future
 * version, old app installs won't crash — they'll fall back to [default].
 */
private inline fun <reified T : Enum<T>> enumValueOfOrDefault(name: String, default: T): T =
    runCatching { enumValueOf<T>(name) }.getOrDefault(default)
