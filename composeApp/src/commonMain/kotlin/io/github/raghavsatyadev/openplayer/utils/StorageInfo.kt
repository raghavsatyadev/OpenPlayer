package io.github.raghavsatyadev.openplayer.utils

import java.io.File

/**
 * Represents the type of storage device
 */
enum class StorageType {
    INTERNAL,
    SD_CARD,
    USB
}

/**
 * Data class representing storage information
 */
data class StorageInfo(
    val path: File,
    val displayName: String,
    val storageType: StorageType,
    val isRemovable: Boolean
)
