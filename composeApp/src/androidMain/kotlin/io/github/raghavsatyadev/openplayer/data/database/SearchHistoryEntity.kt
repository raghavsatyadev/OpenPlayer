package io.github.raghavsatyadev.openplayer.data.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.google.gson.annotations.SerializedName
import io.github.raghavsatyadev.openplayer.data.model.SearchHistoryItem

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    @SerializedName("id")
    val id: Long = 0,
    @ColumnInfo(name = "query")
    @SerializedName("query")
    val query: String,
    @ColumnInfo(name = "timestamp")
    @SerializedName("timestamp")
    val timestamp: Long
)

fun SearchHistoryEntity.toSearchHistoryItem(): SearchHistoryItem {
    return SearchHistoryItem(
        id = this.id,
        query = this.query,
        timestamp = this.timestamp
    )
}

fun SearchHistoryItem.toEntity(): SearchHistoryEntity {
    return SearchHistoryEntity(
        id = this.id ?: 0,
        query = this.query,
        timestamp = this.timestamp
    )
}
