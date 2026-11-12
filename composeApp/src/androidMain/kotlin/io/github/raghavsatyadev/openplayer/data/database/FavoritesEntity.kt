package io.github.raghavsatyadev.openplayer.data.database

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(
    tableName = "favorites",
    indices = [
        Index(value = ["timestamp"], unique = false)
    ]
)
data class FavoritesEntity(
    @PrimaryKey
    @SerializedName(value = "songId", alternate = ["song_id"])
    val songId: Long,
    @SerializedName(value = "isFavorite", alternate = ["is_favorite"])
    val isFavorite: Boolean = true,
    @SerializedName(value = "timestamp", alternate = ["addedAt", "added_at"])
    val timestamp: Long = System.currentTimeMillis()
)
