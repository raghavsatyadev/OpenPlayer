package io.github.raghavsatyadev.openplayer.data.database

import androidx.room3.Embedded
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.google.gson.annotations.SerializedName
import io.github.raghavsatyadev.openplayer.data.model.TransitionSettings

@Entity(
    tableName = "transition_rules",
    indices = [Index(value = ["playlistId", "fromTrackId", "toTrackId"], unique = true)]
)
data class TransitionRuleEntity(
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id")
    val id: Long = 0,
    @SerializedName("playlistId")
    val playlistId: String,
    @SerializedName(value = "fromTrackId", alternate = ["fromSongId", "from_song_id"])
    val fromTrackId: String?,
    @SerializedName(value = "toTrackId", alternate = ["toSongId", "to_song_id"])
    val toTrackId: String?,
    @Embedded
    @SerializedName("settings")
    val settings: TransitionSettings
)
