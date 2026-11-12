package io.github.raghavsatyadev.openplayer.data.playlist.rules

import io.github.raghavsatyadev.openplayer.data.database.EngagementDao
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.data.repository.MusicRepository
import io.github.raghavsatyadev.openplayer.data.repository.SongUserMetaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Feeds [SmartRuleEvaluator] with the library, play stats, user meta and favorites, once or as
 * a flow that re-evaluates whenever any of them changes.
 */
@Singleton
class SmartRuleResolver @Inject constructor(
    private val musicRepository: MusicRepository,
    private val engagementDao: EngagementDao,
    private val songUserMetaRepository: SongUserMetaRepository,
) {
    /**
     * The songs matching [definition], re-emitted when the library, stats, meta or favorites
     * change. [seed] keeps a random sort stable between those changes.
     */
    fun observe(definition: SmartRuleDefinition, seed: Long): Flow<List<Song>> = combine(
        musicRepository.getAudioFiles(),
        engagementDao.getAllEngagementsFlow(),
        songUserMetaRepository.observeAll(),
        musicRepository.getFavoriteSongIdsFlow(),
    ) { songs, engagements, meta, favorites ->
        val context = SmartRuleContext(
            playCounts = engagements.associate { it.songId to it.playCount },
            lastPlayed = engagements.associate { it.songId to it.lastPlayedTimestamp },
            userMeta = meta,
            favoriteIds = favorites,
            now = System.currentTimeMillis(),
            random = Random(seed),
        )
        SmartRuleEvaluator.evaluate(definition, songs, context)
    }.flowOn(Dispatchers.Default)

    companion object {
        /** A rule playlist stores at most this many songs, whatever the rule matches. */
        const val MAX_SONGS = 5000
    }

    suspend fun resolve(definition: SmartRuleDefinition, seed: Long): List<Song> =
        observe(definition, seed).first()
}
