package io.github.raghavsatyadev.openplayer.data.database

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

@Dao
interface AlbumArtThemeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTheme(theme: AlbumArtThemeEntity)

    @Query(
        "SELECT * FROM album_art_themes WHERE albumArtUriString = :uriString AND paletteStyle = :paletteStyle"
    )
    suspend fun getThemeByUriAndStyle(uriString: String, paletteStyle: String): AlbumArtThemeEntity?

    @Query("DELETE FROM album_art_themes WHERE albumArtUriString IN (:uriStrings)")
    suspend fun deleteThemesByUris(uriStrings: List<String>)

    @Query("""
        DELETE FROM album_art_themes
        WHERE albumArtUriString GLOB 'openplayer_local_art://*'
           OR albumArtUriString GLOB 'pixelplay_local_art://*'
    """)
    suspend fun deleteLocalArtworkThemes()
}
