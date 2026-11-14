@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package io.github.raghavsatyadev.openplayer.presentation.screens

import io.github.raghavsatyadev.openplayer.presentation.navigation.navigateSafely
import io.github.raghavsatyadev.openplayer.presentation.navigation.navigateSafelyReplacing
import io.github.raghavsatyadev.openplayer.presentation.components.DetailHeroFloatingLayer
import io.github.raghavsatyadev.openplayer.presentation.components.DetailHeroPinnedActionsHeight
import io.github.raghavsatyadev.openplayer.presentation.components.DetailSongsSummary
import io.github.raghavsatyadev.openplayer.presentation.components.HeroPlayState
import io.github.raghavsatyadev.openplayer.presentation.components.MediaDetailHero
import io.github.raghavsatyadev.openplayer.presentation.components.DetailHeroMenuAction
import io.github.raghavsatyadev.openplayer.presentation.components.ExpressiveDropdownMenu
import io.github.raghavsatyadev.openplayer.presentation.components.rememberDetailHeroDock
import io.github.raghavsatyadev.openplayer.presentation.components.rememberSongCollectionMenuActions
import io.github.raghavsatyadev.openplayer.presentation.utils.getSegmentedItemShape
import io.github.raghavsatyadev.openplayer.presentation.utils.SegmentedItemShapeDefaults
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import io.github.raghavsatyadev.openplayer.ui.theme.LocalOpenPlayerDarkTheme
import io.github.raghavsatyadev.openplayer.ui.theme.RoundedSans
import io.github.raghavsatyadev.openplayer.ui.theme.LocalShowScrollbar
import io.github.raghavsatyadev.openplayer.ui.theme.OpenPlayerStatusBarStyle
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import org.koin.compose.viewmodel.koinViewModel
import androidx.navigation.NavController
import io.github.raghavsatyadev.openplayer.data.model.Artist
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.presentation.components.CollapsibleCommonTopBar
import io.github.raghavsatyadev.openplayer.presentation.components.ExpressiveScrollBar
import io.github.raghavsatyadev.openplayer.presentation.components.MiniPlayerHeight
import io.github.raghavsatyadev.openplayer.presentation.components.PlaylistBottomSheet
import io.github.raghavsatyadev.openplayer.presentation.components.SmartImageCompactListTargetSize
import io.github.raghavsatyadev.openplayer.presentation.components.SmartImage
import io.github.raghavsatyadev.openplayer.presentation.components.SongInfoBottomSheet
import io.github.raghavsatyadev.openplayer.presentation.components.resolveNavBarOccupiedHeight
import io.github.raghavsatyadev.openplayer.presentation.navigation.Screen
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.ArtistDetailViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.ArtistAlbumSection
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlayerViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.collapseKey
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.isAlbumSectionExpanded
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.isAnyAlbumSectionExpanded
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlaylistViewModel
import io.github.raghavsatyadev.openplayer.utils.formatSongCount
import io.github.raghavsatyadev.openplayer.utils.shapes.RoundedStarShape
import kotlinx.coroutines.launch
import io.github.raghavsatyadev.openplayer.presentation.components.subcomps.EnhancedSongListItem
import kotlin.math.roundToInt
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.size.Size
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.ImmutableList
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

private const val UseSharedCollapsibleTopBarProbe = true

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ArtistDetailScreen(
    artistId: String,
    navController: NavController,
    playerViewModel: PlayerViewModel,
    viewModel: ArtistDetailViewModel = koinViewModel(),
    playlistViewModel: PlaylistViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Kept as State and read only by the album list and the expand-all button, so a header tap
    // does not recompose the whole screen.
    val albumExpansionOverrides = viewModel.albumExpansionOverrides.collectAsStateWithLifecycle()
    val stablePlayerState by playerViewModel.stablePlayerState.collectAsStateWithLifecycle()
    
    var isTransitionFinished by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(300)
        isTransitionFinished = true
    }

    val lazyListState = rememberLazyListState()
    val favoriteIds by playerViewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val navBarCompactMode by playerViewModel.navBarCompactMode.collectAsStateWithLifecycle()
    var showSongInfoBottomSheet by remember { mutableStateOf(false) }
    val selectedSongForInfo by playerViewModel.selectedSongForInfo.collectAsStateWithLifecycle()
    val systemNavBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomBarHeightDp = resolveNavBarOccupiedHeight(systemNavBarInset, navBarCompactMode)
    var showPlaylistBottomSheet by remember { mutableStateOf(false) }
    var showArtistPlaylistSheet by remember { mutableStateOf(false) }
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val isDarkTheme = LocalOpenPlayerDarkTheme.current
    val baseColorScheme = MaterialTheme.colorScheme

    val artistColorSchemePair by viewModel.artistColorScheme.collectAsStateWithLifecycle()
    val artistColorScheme = remember(artistColorSchemePair, isDarkTheme) {
        artistColorSchemePair?.let { pair -> if (isDarkTheme) pair.dark else pair.light }
            ?: baseColorScheme
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.setCustomImage(it) }
    }

    LaunchedEffect(Unit) {
        playerViewModel.collapsePlayerSheet()
    }

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val minTopBarHeight = 64.dp + statusBarHeight
    val maxTopBarHeight = 300.dp

    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }
    val headerImageRequestSize = remember(
        configuration.screenWidthDp,
        density.density,
        maxTopBarHeightPx
    ) {
        Size(
            width = with(density) { configuration.screenWidthDp.dp.roundToPx() },
            height = maxTopBarHeightPx.roundToInt()
        )
    }

    val topBarHeight = remember { Animatable(maxTopBarHeightPx) }

    // Hiding the system bars drops the status-bar inset to zero, which shrinks
    // minTopBarHeightPx. Pull the animated height back into range when the inset returns,
    // otherwise the collapsed bar stays too short.
    LaunchedEffect(minTopBarHeightPx, maxTopBarHeightPx) {
        val clamped = topBarHeight.value.coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
        if (clamped != topBarHeight.value) topBarHeight.snapTo(clamped)
    }
    val collapseFraction by remember(minTopBarHeightPx, maxTopBarHeightPx) {
        derivedStateOf {
            1f - ((topBarHeight.value - minTopBarHeightPx) / (maxTopBarHeightPx - minTopBarHeightPx)).coerceIn(0f, 1f)
        }
    }

    // Direction of the latest scroll delta. A gesture that ends while heading back to the top
    // (a fling landing there included) must leave the header fully expanded.
    var lastScrollTowardsTop by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember(minTopBarHeightPx, maxTopBarHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val isScrollingDown = delta < 0
                if (delta != 0f) lastScrollTowardsTop = !isScrollingDown

                if (!isScrollingDown && (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0)) {
                    return Offset.Zero
                }

                val previousHeight = topBarHeight.value
                val newHeight = (previousHeight + delta).coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
                val consumed = newHeight - previousHeight

                if (consumed.roundToInt() != 0) {
                    coroutineScope.launch {
                        topBarHeight.snapTo(newHeight)
                    }
                }

                val canConsumeScroll = !(isScrollingDown && newHeight == minTopBarHeightPx)
                return if (canConsumeScroll) Offset(0f, consumed) else Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                return super.onPostFling(consumed, available)
            }
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val canExpand = lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset == 0
            val shouldExpand = lastScrollTowardsTop ||
                topBarHeight.value > (minTopBarHeightPx + maxTopBarHeightPx) / 2

            val targetValue = if (shouldExpand && canExpand) {
                maxTopBarHeightPx
            } else {
                minTopBarHeightPx
            }

            if (topBarHeight.value != targetValue) {
                coroutineScope.launch {
                    topBarHeight.animateTo(targetValue, spring(stiffness = Spring.StiffnessMedium))
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = artistColorScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes
    ) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box(modifier = Modifier.nestedScroll(nestedScrollConnection)) {
            when {
                uiState.isLoading && uiState.artist == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        ContainedLoadingIndicator()
                    }
                }
                uiState.error != null && uiState.artist == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = uiState.error!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            FilledTonalButton(onClick = { viewModel.retry() }) {
                                Text(stringResource(Res.string.library_retry))
                            }
                        }
                    }
                }
                uiState.artist != null -> {
                    val artist = uiState.artist!!
                    val songs = uiState.songs
                    val artistDurationMs = remember(songs) { songs.sumOf { it.duration } }
                    val currentTopBarHeightDp = with(density) { topBarHeight.value.toDp() }

                    val albumSections = uiState.albumSections
                    val heroOverlapPx = with(density) { ArtistHeroHeaderOverlap.toPx() }
                    val listTopPadding = minTopBarHeight + 8.dp
                    val listTopPaddingPx = with(density) { listTopPadding.toPx() }
                    // The list rides down with the header, overlapping its artwork by the hero overlap.
                    val listOffsetPx: () -> Int = {
                        (topBarHeight.value - minTopBarHeightPx - heroOverlapPx * (1f - collapseFraction))
                            .roundToInt()
                            .coerceAtLeast(0)
                    }
                    val heroDock = rememberDetailHeroDock(
                        heroTopPx = {
                            if (lazyListState.firstVisibleItemIndex == 0) {
                                listOffsetPx() + listTopPaddingPx - lazyListState.firstVisibleItemScrollOffset
                            } else {
                                null
                            }
                        },
                        // Docking starts once the header is collapsed and the list itself scrolls, so a
                        // list too short to scroll never leaves the title half-way.
                        dockStartHeroTopPx = listTopPaddingPx,
                        heroStartPadding = 16.dp,
                        collapsedBarBottom = minTopBarHeight,
                        hasBarSubtitle = true,
                        // The bar keeps the image menu on the right.
                        collapsedTitleEndPadding = 88.dp
                    )
                    val artistMenuActions = rememberSongCollectionMenuActions(
                        onAddToPlaylist = { showArtistPlaylistSheet = true },
                        onPlayNext = { if (songs.isNotEmpty()) playerViewModel.selectionActions.addSelectedAsNext(songs) },
                        onAddToQueue = { if (songs.isNotEmpty()) playerViewModel.selectionActions.addSelectedToQueue(songs) }
                    )
                    val songIdsInScope = remember(songs) { songs.mapTo(HashSet()) { it.id } }
                    val currentSongId = stablePlayerState.currentSong?.id
                    val heroPlayState = when {
                        currentSongId == null || currentSongId !in songIdsInScope -> HeroPlayState.Idle
                        stablePlayerState.isPlaying -> HeroPlayState.Playing
                        else -> HeroPlayState.Paused
                    }
                    val onHeroShuffle: () -> Unit = {
                        if (songs.isNotEmpty()) {
                            playerViewModel.playSongsShuffled(songs, artist.name, startAtZero = true)
                        }
                    }
                    val onHeroPlay: () -> Unit = {
                        if (heroPlayState == HeroPlayState.Idle) {
                            songs.firstOrNull()?.let { first -> playerViewModel.showAndPlaySong(first, songs) }
                        } else {
                            playerViewModel.playPause()
                        }
                    }
                    val barScrimFadePx = with(density) { 24.dp.toPx() }
                    val barContainerAlpha by remember(barScrimFadePx) {
                        derivedStateOf {
                            if (lazyListState.firstVisibleItemIndex > 0) 1f
                            else (lazyListState.firstVisibleItemScrollOffset / barScrimFadePx).coerceIn(0f, 1f)
                        }
                    }
                    val visibleSections = remember(albumSections) { albumSections.filter { it.songs.isNotEmpty() } }
                    val visibleSectionKeys = remember(visibleSections) { visibleSections.map { it.collapseKey() } }
                    val albumCount = visibleSections.size
                    val albumCountLabel = if (albumCount == 1) {
                        stringResource(Res.string.detail_hero_album_count_one)
                    } else {
                        stringResource(Res.string.detail_hero_album_count_many, albumCount)
                    }
                    val albumsCollapsedByDefault = uiState.albumsCollapsedByDefault
                    val isSectionExpanded: (String) -> Boolean = { sectionKey ->
                        isAlbumSectionExpanded(
                            key = sectionKey,
                            overrides = albumExpansionOverrides.value,
                            collapsedByDefault = albumsCollapsedByDefault,
                            sectionCount = albumCount
                        )
                    }
                    // Derived and read only by the summary row: this scope recomposes on every
                    // frame of the header collapse, which should not re-check every album.
                    val anySectionExpanded = remember(
                        albumExpansionOverrides,
                        visibleSectionKeys,
                        albumsCollapsedByDefault
                    ) {
                        derivedStateOf {
                            isAnyAlbumSectionExpanded(
                                sectionKeys = visibleSectionKeys,
                                overrides = albumExpansionOverrides.value,
                                collapsedByDefault = albumsCollapsedByDefault
                            )
                        }
                    }

                    val isScrollbarEnabled = LocalShowScrollbar.current
                    val showScrollBar by remember(isScrollbarEnabled) {
                        derivedStateOf {
                            isScrollbarEnabled &&
                            collapseFraction > 0.95f &&
                                (lazyListState.canScrollForward || lazyListState.canScrollBackward)
                        }
                    }
                    val scrollbarInset by animateDpAsState(
                        targetValue = if (showScrollBar) 8.dp else 0.dp,
                        label = "artistScrollbarInset"
                    )

                    // Artwork behind the list so the hero can ride up over its lower part.
                    if (UseSharedCollapsibleTopBarProbe) {
                        ArtistHeaderBackdrop(
                            artistName = artist.name,
                            effectiveImageUrl = uiState.effectiveImageUrl,
                            collapseFraction = collapseFraction,
                            headerHeight = currentTopBarHeightDp,
                            headerImageRequestSize = headerImageRequestSize
                        )
                    }

                    LazyColumn(
                        state = lazyListState,
                        modifier = Modifier
                            .fillMaxSize()
                            .offset { IntOffset(0, listOffsetPx()) },
                        contentPadding = PaddingValues(
                            top = listTopPadding,
                            start = 16.dp,
                            // Fixed, so the hero never shifts; only album cards make room for
                            // the scrollbar (scrollbarInset).
                            end = 16.dp,
                            bottom = MiniPlayerHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp
                        )
                    ) {
                        item(key = "artist_hero", contentType = "artist_hero") {
                            MediaDetailHero(
                                title = artist.name,
                                // Song count and total length live in the summary above the list.
                                metaLine = albumCountLabel.takeIf { albumCount > 0 },
                                onShuffle = onHeroShuffle,
                                onPlay = onHeroPlay,
                                playState = heroPlayState,
                                menuActions = artistMenuActions,
                                enabled = songs.isNotEmpty(),
                                modifier = Modifier.padding(bottom = 8.dp),
                                dock = heroDock
                            )
                        }
                        if (songs.isNotEmpty()) {
                            item(key = "artist_songs_summary", contentType = "detail_songs_summary") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp, bottom = 12.dp)
                                        .padding(end = scrollbarInset),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DetailSongsSummary(
                                        songCount = songs.size,
                                        durationMs = artistDurationMs,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (albumCount > 1) {
                                        val anyExpanded = anySectionExpanded.value
                                        ArtistAlbumsExpandAllButton(
                                            anyExpanded = anyExpanded,
                                            onClick = { viewModel.setAllAlbumSectionsExpanded(!anyExpanded) }
                                        )
                                    }
                                }
                            }
                        }
                        visibleSections.forEachIndexed { index, section ->
                            val sectionKey = visibleSectionKeys[index]
                            val isFirstSection = index == 0
                            val isLastSection = index == visibleSections.lastIndex
                            val isExpanded = isSectionExpanded(sectionKey)
                            val sectionSongs = if (isTransitionFinished) section.songs else section.songs.take(5)

                            item(
                                key = "${sectionKey}_header",
                                contentType = "artist_section_header"
                            ) {
                                CollapsibleAlbumSectionHeader(
                                    modifier = Modifier.padding(end = scrollbarInset),
                                    section = section,
                                    isExpanded = isExpanded,
                                    isFirstSection = isFirstSection,
                                    isLastSection = isLastSection,
                                    onToggleExpanded = {
                                        viewModel.setAlbumSectionExpanded(sectionKey, !isExpanded)
                                    },
                                    onPlayAlbum = {
                                        section.songs.firstOrNull()?.let { firstSong ->
                                            playerViewModel.showAndPlaySong(firstSong, section.songs)
                                        }
                                    }
                                )
                            }

                            if (isExpanded) {
                                item(
                                    key = "${sectionKey}_song_group_spacer",
                                    contentType = "artist_section_spacer"
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .animateItem(
                                                fadeInSpec = tween(durationMillis = 160),
                                                fadeOutSpec = tween(durationMillis = 120),
                                                placementSpec = tween(durationMillis = 180)
                                            )
                                            .padding(end = scrollbarInset)
                                            .fillMaxWidth()
                                            .height(10.dp)
                                            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f))
                                    )
                                }

                                itemsIndexed(
                                    items = sectionSongs,
                                    key = { songIndex, song -> "${sectionKey}_song_${song.id}_$songIndex" },
                                    contentType = { _, _ -> "artist_section_song" }
                                ) { songIndex, song ->
                                    ArtistAlbumSectionSongItem(
                                        modifier = Modifier
                                            .animateItem(
                                                fadeInSpec = tween(durationMillis = 180),
                                                fadeOutSpec = tween(durationMillis = 120),
                                                placementSpec = tween(durationMillis = 200)
                                            )
                                            .padding(end = scrollbarInset),
                                        song = song,
                                        songIndex = songIndex,
                                        songCount = section.songs.size,
                                        isLastSection = isLastSection,
                                        isCurrentSong = stablePlayerState.currentSong?.id == song.id,
                                        isPlaying = stablePlayerState.isPlaying,
                                        onSongClick = {
                                            playerViewModel.showAndPlaySong(song, section.songs)
                                        },
                                        onMoreOptionsClick = {
                                            playerViewModel.selectSongForInfo(song)
                                            showSongInfoBottomSheet = true
                                        }
                                    )
                                }
                            }

                            item(
                                key = "${sectionKey}_footer",
                                contentType = "artist_section_footer"
                            ) {
                                // Album cards sit 4dp apart, as one segmented group.
                                Spacer(modifier = Modifier.height(if (isLastSection) 24.dp else 4.dp))
                            }
                        }
                    }

                    if (showScrollBar) {
                        ExpressiveScrollBar(
                            listState = lazyListState,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(
                                    top = minTopBarHeight + DetailHeroPinnedActionsHeight + 4.dp,
                                    bottom = MiniPlayerHeight + systemNavBarInset + 8.dp
                                )
                        )
                    }

                    if (UseSharedCollapsibleTopBarProbe) {
                        SharedArtistTopBarProbe(
                            artist = artist,
                            songsCount = songs.size,
                            collapseFraction = collapseFraction,
                            titleAlpha = heroDock.barTitleAlpha,
                            subtitleAlpha = heroDock.barSubtitleAlpha,
                            containerAlpha = maxOf(barContainerAlpha, heroDock.barContainerAlpha),
                            headerHeight = currentTopBarHeightDp,
                            hasCustomImage = !artist.customImageUri.isNullOrBlank(),
                            onBackPressed = { navController.popBackStack() },
                            onChangeImage = { imagePickerLauncher.launch("image/*") },
                            onClearCustomImage = { viewModel.clearCustomImage() }
                        )
                    } else {
                        CustomCollapsingTopBar(
                            artist = artist,
                            effectiveImageUrl = uiState.effectiveImageUrl,
                            hasCustomImage = !artist.customImageUri.isNullOrBlank(),
                            songsCount = songs.size,
                            collapseFraction = collapseFraction,
                            headerHeight = currentTopBarHeightDp,
                            headerImageRequestSize = headerImageRequestSize,
                            onBackPressed = { navController.popBackStack() },
                            onPlayClick = {
                                if (songs.isNotEmpty()) {
                                    playerViewModel.playSongsShuffled(
                                        songs,
                                        artist.name,
                                        startAtZero = true
                                    )
                                }
                            },
                            onChangeImage = { imagePickerLauncher.launch("image/*") },
                            onClearCustomImage = { viewModel.clearCustomImage() }
                        )
                    }

                    DetailHeroFloatingLayer(
                        title = artist.name,
                        dock = heroDock,
                        listState = lazyListState,
                        onShuffle = onHeroShuffle,
                        onPlay = onHeroPlay,
                        menuActions = artistMenuActions,
                        playState = heroPlayState,
                        enabled = songs.isNotEmpty(),
                        metaLine = albumCountLabel.takeIf { albumCount > 0 }
                    )
                }
            }
        }
    }

    if (showArtistPlaylistSheet && uiState.songs.isNotEmpty()) {
        val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()
        val artistSongs = remember(uiState.songs) { uiState.songs.toImmutableList() }
        PlaylistBottomSheet(
            playlistUiState = playlistUiState,
            songs = artistSongs,
            onDismiss = { showArtistPlaylistSheet = false },
            bottomBarHeight = bottomBarHeightDp,
            playerViewModel = playerViewModel,
        )
    }

    if (showSongInfoBottomSheet && selectedSongForInfo != null) {
        val currentSong = selectedSongForInfo
        val isFavorite = remember(currentSong?.id, favoriteIds) {
            currentSong?.let { favoriteIds.contains(it.id) } ?: false
        }

        if (currentSong != null) {
            val removeFromListTrigger = remember(uiState.songs) {
                {
                    viewModel.removeSongFromAlbumSection(currentSong.id)
                }
            }
            SongInfoBottomSheet(
                song = currentSong,
                isFavorite = isFavorite,
                onToggleFavorite = {
                    playerViewModel.toggleFavoriteSpecificSong(currentSong)
                },
                onDismiss = { showSongInfoBottomSheet = false },
                onPlaySong = {
                    playerViewModel.showAndPlaySong(currentSong)
                    showSongInfoBottomSheet = false
                },
                onAddToQueue = {
                    playerViewModel.addSongToQueue(currentSong)
                    showSongInfoBottomSheet = false
                },
                onAddNextToQueue = {
                    playerViewModel.addSongNextToQueue(currentSong)
                    showSongInfoBottomSheet = false
                },
                onAddToPlayList = {
                    showPlaylistBottomSheet = true
                },
                onDeleteFromDevice = playerViewModel.deleteActions::deleteFromDevice,
                onNavigateToAlbum = {
                    navController.navigateSafelyReplacing(
                        route = Screen.AlbumDetail.createRoute(currentSong.albumId),
                        patternToPop = Screen.AlbumDetail.route
                    )
                    showSongInfoBottomSheet = false
                },
                onNavigateToArtist = {
                    navController.navigateSafelyReplacing(
                        route = Screen.ArtistDetail.createRoute(currentSong.artistId),
                        patternToPop = Screen.ArtistDetail.route
                    )
                    showSongInfoBottomSheet = false
                },
                onNavigateToArtistById = { artistId ->
                    navController.navigateSafelyReplacing(
                        route = Screen.ArtistDetail.createRoute(artistId),
                        patternToPop = Screen.ArtistDetail.route
                    )
                    showSongInfoBottomSheet = false
                },
                onNavigateToGenre = {
                    currentSong.genre?.let {
                        navController.navigateSafelyReplacing(
                            route = Screen.GenreDetail.createRoute(java.net.URLEncoder.encode(it, "UTF-8")),
                            patternToPop = Screen.GenreDetail.route
                        )
                    }
                    showSongInfoBottomSheet = false
                },
                onEditSong = { newTitle, newArtist, newAlbum, newAlbumArtist, newComposer, newGenre, newLyrics, newTrackNumber, newDiscNumber, replayGainTrackGainDb, replayGainAlbumGainDb, coverArtUpdate, customMetadataChanges ->
                    playerViewModel.metadataEditor.editSongMetadata(
                        currentSong,
                        newTitle,
                        newArtist,
                        newAlbum,
                        newAlbumArtist,
                        newComposer,
                        newGenre,
                        newLyrics,
                        newTrackNumber,
                        newDiscNumber,
                        replayGainTrackGainDb,
                        replayGainAlbumGainDb,
                        coverArtUpdate,
                        customMetadataChanges
                    )
                },
                removeFromListTrigger = removeFromListTrigger
            )
            if (showPlaylistBottomSheet) {
                val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()

                PlaylistBottomSheet(
                    playlistUiState = playlistUiState,
                    songs = persistentListOf(currentSong),
                    onDismiss = { showPlaylistBottomSheet = false },
                    bottomBarHeight = bottomBarHeightDp,
                    playerViewModel = playerViewModel,
                )
            }
        }
    }
    }
}

private val SectionOuterCorner = 24.dp
private val SectionInnerCorner = 8.dp

/** "Collapse all" while any album shows its songs, "Expand all" once every album is collapsed. */
@Composable
private fun ArtistAlbumsExpandAllButton(
    anyExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val height = ButtonDefaults.ExtraSmallContainerHeight
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = height),
        contentPadding = ButtonDefaults.contentPaddingFor(height, hasStartIcon = true)
    ) {
        Icon(
            imageVector = if (anyExpanded) vectorResource(Res.drawable.rounded_unfold_less_24) else vectorResource(Res.drawable.rounded_unfold_more_24),
            contentDescription = null,
            modifier = Modifier.size(ButtonDefaults.iconSizeFor(height))
        )
        Spacer(modifier = Modifier.width(ButtonDefaults.iconSpacingFor(height)))
        Text(
            text = stringResource(
                if (anyExpanded) Res.string.artist_page_collapse_all_albums else Res.string.artist_page_expand_all_albums
            ),
            style = ButtonDefaults.textStyleFor(height),
            maxLines = 1
        )
    }
}

@Composable
private fun CollapsibleAlbumSectionHeader(
    section: ArtistAlbumSection,
    isExpanded: Boolean,
    isFirstSection: Boolean,
    isLastSection: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
    onPlayAlbum: () -> Unit
) {
    val expandIconRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 260),
        label = "ArtistAlbumExpandRotation"
    )
    val songCountText = formatSongCount(section.songs.size)
    val subtitle = remember(section.year, section.genre, songCountText) {
        // The genre goes last so a long one is what gets ellipsized, not the song count.
        listOfNotNull(
            section.year?.takeIf { it > 0 }?.toString(),
            songCountText,
            section.genre
        ).joinToString(" • ")
    }

    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
    // Album cards form one segmented group: only the group's outer corners stay round.
    val top = if (isFirstSection) SectionOuterCorner else SectionInnerCorner
    val bottom = when {
        isExpanded -> 0.dp
        isLastSection -> SectionOuterCorner
        else -> SectionInnerCorner
    }
    val shape = remember(top, bottom) {
        AbsoluteSmoothCornerShape(
            cornerRadiusTR = top, smoothnessAsPercentTR = 60,
            cornerRadiusTL = top, smoothnessAsPercentTL = 60,
            cornerRadiusBR = bottom, smoothnessAsPercentBR = 60,
            cornerRadiusBL = bottom, smoothnessAsPercentBL = 60
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(containerColor, shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .clickable(onClick = onToggleExpanded)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmartImage(
                model = section.albumArtUriString,
                contentDescription = section.title,
                targetSize = SmartImageCompactListTargetSize,
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            FilledTonalIconButton(
                onClick = onPlayAlbum,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Icon(vectorResource(Res.drawable.rounded_play_arrow_filled_24), contentDescription = stringResource(Res.string.presentation_batch_d_cd_play_title, section.title))
            }
            Icon(
                imageVector = vectorResource(Res.drawable.rounded_expand_more_24),
                contentDescription = if (isExpanded) {
                    stringResource(Res.string.presentation_batch_d_cd_collapse_title, section.title)
                } else {
                    stringResource(Res.string.presentation_batch_d_cd_expand_title, section.title)
                },
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer {
                    rotationZ = expandIconRotation
                }
            )
        }
    }
}

@Composable
private fun ArtistAlbumSectionSongItem(
    modifier: Modifier = Modifier,
    song: Song,
    songIndex: Int,
    songCount: Int,
    isLastSection: Boolean,
    isCurrentSong: Boolean,
    isPlaying: Boolean,
    onSongClick: () -> Unit,
    onMoreOptionsClick: () -> Unit
) {
    val isLastSong = songIndex == songCount - 1

    val songItemShape = getSegmentedItemShape(
        index = songIndex,
        count = songCount,
        outerRadius = SegmentedItemShapeDefaults.CardOuterCornerRadius
    )

    val containerShape = remember(isLastSong, isLastSection) {
        if (isLastSong) {
            val bottom = if (isLastSection) SectionOuterCorner else SectionInnerCorner
            AbsoluteSmoothCornerShape(
                cornerRadiusTR = 0.dp, smoothnessAsPercentTR = 0,
                cornerRadiusTL = 0.dp, smoothnessAsPercentTL = 0,
                cornerRadiusBR = bottom, smoothnessAsPercentBR = 60,
                cornerRadiusBL = bottom, smoothnessAsPercentBL = 60
            )
        } else {
            RectangleShape
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f), containerShape)
            .padding(horizontal = 8.dp)
    ) {
        Column {
            if (songIndex > 0) {
                Spacer(modifier = Modifier.height(4.dp))
            }

            EnhancedSongListItem(
                modifier = Modifier.fillMaxWidth(),
                song = song,
                isCurrentSong = isCurrentSong,
                isPlaying = isPlaying,
                showAlbumArt = false,
                customShape = songItemShape,
                onMoreOptionsClick = { onMoreOptionsClick() },
                onClick = onSongClick
            )

            if (isLastSong) {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ArtistHeaderBackdrop(
    artistName: String,
    effectiveImageUrl: String?,
    collapseFraction: Float,
    headerHeight: Dp,
    headerImageRequestSize: Size,
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val expandedContentAlpha = 1f - (collapseFraction * 2f).coerceIn(0f, 1f)
    val displayUrl = effectiveImageUrl?.takeIf { it.isNotBlank() }
    val headerOverlayBrush = remember(surfaceColor, expandedContentAlpha) {
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                surfaceColor.copy(alpha = 0.24f * expandedContentAlpha),
                surfaceColor.copy(alpha = 0.84f * expandedContentAlpha),
                surfaceColor
            )
        )
    }
    if (expandedContentAlpha <= 0.01f) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
            .clipToBounds()
    ) {
        if (!displayUrl.isNullOrEmpty()) {
            SmartImage(
                model = displayUrl,
                contentDescription = artistName,
                contentScale = ContentScale.Crop,
                targetSize = headerImageRequestSize,
                allowHardware = true,
                crossfadeDurationMillis = 0,
                alpha = expandedContentAlpha,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            MusicIconPattern(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = expandedContentAlpha }
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(headerOverlayBrush)
        )
    }
}

/** How far the hero rides up over the bottom of the expanded artist header. */
private val ArtistHeroHeaderOverlap = 120.dp

@Composable
private fun SharedArtistTopBarProbe(
    artist: Artist,
    songsCount: Int,
    collapseFraction: Float,
    titleAlpha: Float,
    subtitleAlpha: Float,
    containerAlpha: Float,
    headerHeight: Dp,
    hasCustomImage: Boolean,
    onBackPressed: () -> Unit,
    onChangeImage: () -> Unit,
    onClearCustomImage: () -> Unit
) {
    var showImageMenu by remember { mutableStateOf(false) }
    val surfaceColor = MaterialTheme.colorScheme.surface
    val statusBarColor =
        if (LocalOpenPlayerDarkTheme.current) Color.Black.copy(alpha = 0.6f)
        else Color.White.copy(alpha = 0.4f)
    val solidAlpha = (collapseFraction * 2f).coerceIn(0f, 1f)
    val statusBarBrush = remember(statusBarColor) {
        Brush.verticalGradient(colors = listOf(statusBarColor, Color.Transparent))
    }
    val expandedStatusBarFallback = remember(statusBarColor, surfaceColor) {
        statusBarColor.compositeOver(surfaceColor)
    }
    val fallbackStatusBarColor = remember(expandedStatusBarFallback, surfaceColor, solidAlpha) {
        lerpColor(expandedStatusBarFallback, surfaceColor, solidAlpha)
    }

    OpenPlayerStatusBarStyle(color = fallbackStatusBarColor)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(statusBarBrush)
                .align(Alignment.TopCenter)
        )

        CollapsibleCommonTopBar(
            title = artist.name,
            subtitle = formatSongCount(songsCount),
            collapseFraction = collapseFraction,
            headerHeight = headerHeight,
            onBackClick = onBackPressed,
            // Transparent until something scrolls under the bar; the hero overlaps the
            // header, so a collapse-driven fill would dim it mid-gesture.
            containerColor = surfaceColor.copy(alpha = containerAlpha * solidAlpha),
            collapsedTitleStartPadding = 68.dp,
            expandedTitleStartPadding = 24.dp,
            collapsedTitleEndPadding = 88.dp,
            expandedTitleEndPadding = 88.dp,
            containerHeightRange = 112.dp to 56.dp,
            titleStyle = MaterialTheme.typography.headlineMedium.copy(
                fontFamily = RoundedSans,
                fontWeight = FontWeight.SemiBold,
                textGeometricTransform = TextGeometricTransform(scaleX = 1.08f)
            ),
            titleScaleRange = 1f to 1f,
            titleFontSizeRange = 30.sp to 18.sp,
            maxLines = 1,
            collapsedSubtitleMaxLines = 1,
            expandedSubtitleMaxLines = 1,
            // The hero title docks into this slot itself (DetailHeroFloatingLayer); the bar's
            // own title only shows for titles too long to dock.
            contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = titleAlpha),
            subtitleColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = subtitleAlpha),
            fadeSubtitleOnCollapse = false,
            syncStatusBarWithContainer = false,
            actions = {
                Box(
                    modifier = Modifier.padding(end = 12.dp, top = 4.dp)
                ) {
                    FilledIconButton(
                        onClick = { showImageMenu = true },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.rounded_edit_filled_24),
                            contentDescription = stringResource(Res.string.presentation_batch_d_edit_artist_image_cd)
                        )
                    }

                    ExpressiveDropdownMenu(
                        expanded = showImageMenu,
                        onDismissRequest = { showImageMenu = false },
                        actions = artistImageMenuActions(
                            hasCustomImage = hasCustomImage,
                            onChangeImage = onChangeImage,
                            onClearCustomImage = onClearCustomImage
                        )
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CustomCollapsingTopBar(
    artist: Artist,
    effectiveImageUrl: String?,
    hasCustomImage: Boolean,
    songsCount: Int,
    collapseFraction: Float,
    headerHeight: Dp,
    headerImageRequestSize: Size,
    onBackPressed: () -> Unit,
    onPlayClick: () -> Unit,
    onChangeImage: () -> Unit,
    onClearCustomImage: () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val statusBarColor = if (LocalOpenPlayerDarkTheme.current) Color.Black.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.4f)

    val fabScale = 1f - collapseFraction
    val backgroundAlpha = collapseFraction
    val headerContentAlpha = 1f - (collapseFraction * 2).coerceAtMost(1f)
    val showExpandedArtwork = headerContentAlpha > 0.01f
    val headerOverlayBrush = remember(surfaceColor, headerContentAlpha) {
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                surfaceColor.copy(alpha = 0.30f * headerContentAlpha),
                surfaceColor.copy(alpha = 0.90f * headerContentAlpha),
                surfaceColor.copy(alpha = headerContentAlpha)
            )
        )
    }
    val statusBarBrush = remember(statusBarColor) {
        Brush.verticalGradient(
            colors = listOf(
                statusBarColor,
                Color.Transparent
            )
        )
    }
    val solidAlpha = (collapseFraction * 2f).coerceIn(0f, 1f)
    val expandedStatusBarFallback = remember(statusBarColor, surfaceColor) {
        statusBarColor.compositeOver(surfaceColor)
    }
    val fallbackStatusBarColor = remember(expandedStatusBarFallback, surfaceColor, solidAlpha) {
        lerpColor(expandedStatusBarFallback, surfaceColor, solidAlpha)
    }

    val titleScale = lerp(1f, 0.75f, collapseFraction)
    val titlePaddingStart = lerp(24.dp, 58.dp, collapseFraction)
    val titleMaxLines = if(collapseFraction < 0.5f) 2 else 1
    val titleVerticalBias = lerp(1f, -1f, collapseFraction)
    val animatedTitleAlignment = BiasAlignment(horizontalBias = -1f, verticalBias = titleVerticalBias)
    val titleContainerHeight = lerp(88.dp, 56.dp, collapseFraction)
    val yOffsetCorrection = lerp( (titleContainerHeight / 2) - 64.dp, 0.dp, collapseFraction)


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
            .clipToBounds()
    ) {
        OpenPlayerStatusBarStyle(color = fallbackStatusBarColor)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(headerHeight)
                .background(surfaceColor.copy(alpha = backgroundAlpha))
        ) {
            if (showExpandedArtwork) {
                val displayUrl = effectiveImageUrl?.takeIf { it.isNotBlank() }
                if (!displayUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(displayUrl)
                            .size(headerImageRequestSize)
                            .allowHardware(true)
                            .crossfade(false)
                            .build(),
                        contentDescription = artist.name,
                        contentScale = ContentScale.Crop,
                        alpha = headerContentAlpha,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    MusicIconPattern(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = headerContentAlpha }
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(headerOverlayBrush)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .background(statusBarBrush)
                    .align(Alignment.TopCenter)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                FilledIconButton(
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 4.dp),
                    onClick = onBackPressed,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Icon(imageVector = vectorResource(Res.drawable.rounded_arrow_back_24), contentDescription = stringResource(Res.string.auth_cd_back))
                }

                if (collapseFraction < 0.5f) {
                    var showImageMenu by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 12.dp, top = 4.dp)
                            .graphicsLayer { alpha = 1f - (collapseFraction * 4).coerceAtMost(1f) }
                    ) {
                        SmallFloatingActionButton(
                            onClick = { showImageMenu = true },
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Icon(vectorResource(Res.drawable.rounded_edit_filled_24), contentDescription = stringResource(Res.string.presentation_batch_d_edit_artist_image_cd))
                        }
                        ExpressiveDropdownMenu(
                            expanded = showImageMenu,
                            onDismissRequest = { showImageMenu = false },
                            actions = artistImageMenuActions(
                                hasCustomImage = hasCustomImage,
                                onChangeImage = onChangeImage,
                                onClearCustomImage = onClearCustomImage
                            )
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .align(animatedTitleAlignment)
                        .height(titleContainerHeight)
                        .fillMaxWidth()
                        .offset(y = yOffsetCorrection)
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = titlePaddingStart, end = 120.dp)
                            .graphicsLayer {
                                scaleX = titleScale
                                scaleY = titleScale
                            },
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 26.sp,
                                textGeometricTransform = TextGeometricTransform(scaleX = 1.2f),
                            ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = titleMaxLines,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = formatSongCount(songsCount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                LargeExtendedFloatingActionButton(
                    onClick = onPlayClick,
                    shape = RoundedStarShape(sides = 8, curve = 0.05, rotation = 0f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .graphicsLayer {
                            scaleX = fabScale
                            scaleY = fabScale
                            alpha = fabScale
                        }
                ) {
                    Icon(vectorResource(Res.drawable.rounded_shuffle_24), contentDescription = stringResource(Res.string.cd_shuffle_play_album))
                }
            }
        }
    }
}

@Composable
private fun MusicIconPattern(modifier: Modifier = Modifier) {
    val color1 = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
    val color2 = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)

    Box(modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer)) {
        Icon(
            imageVector = vectorResource(Res.drawable.rounded_music_note_24),
            contentDescription = null, tint = color1,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = 76.dp, y = 72.dp)
                .size(60.dp)
                .graphicsLayer { rotationZ = -12f }
        )
        Icon(
            imageVector = vectorResource(Res.drawable.rounded_graphic_eq_24),
            contentDescription = null, tint = color1,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset(x = 4.dp, y = 78.dp)
                .size(50.dp)
                .graphicsLayer { rotationZ = 18f }
        )
        Icon(
            imageVector = vectorResource(Res.drawable.rounded_album_filled_24),
            contentDescription = null, tint = color2,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset(x = 4.dp, y = (-70).dp)
                .size(70.dp)
                .graphicsLayer { rotationZ = 12f }
        )
        Icon(
            imageVector = vectorResource(Res.drawable.rounded_mic_filled_24),
            contentDescription = null, tint = color1,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = 14.dp, y = (-6).dp)
                .size(60.dp)
                .graphicsLayer { rotationZ = 14f }
        )
        Icon(
            imageVector = vectorResource(Res.drawable.rounded_surround_sound_filled_24),
            contentDescription = null, tint = color2,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(x = (-24).dp, y = 32.dp)
                .size(80.dp)
                .graphicsLayer { rotationZ = 10f }
        )
        Icon(
            imageVector = vectorResource(Res.drawable.rounded_music_note_24),
            contentDescription = null, tint = color1,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-18).dp, y = (-138).dp)
                .size(45.dp)
                .graphicsLayer { rotationZ = -18f }
        )
        Icon(
            imageVector = vectorResource(Res.drawable.rounded_headphones_filled_24),
            contentDescription = null, tint = color2,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 72.dp, y = 34.dp)
                .size(45.dp)
                .graphicsLayer { rotationZ = -8f }
        )
    }
}

@Composable
private fun artistImageMenuActions(
    hasCustomImage: Boolean,
    onChangeImage: () -> Unit,
    onClearCustomImage: () -> Unit
): ImmutableList<DetailHeroMenuAction> {
    val changePhoto = stringResource(Res.string.presentation_batch_d_change_photo)
    val resetToDefault = stringResource(Res.string.presentation_batch_d_reset_to_default)
    val changePhotoIcon = vectorResource(Res.drawable.rounded_add_a_photo_filled_24)
    val resetIcon = vectorResource(Res.drawable.rounded_delete_filled_24)
    return remember(hasCustomImage, changePhoto, resetToDefault, onChangeImage, onClearCustomImage) {
        val changePhotoAction = DetailHeroMenuAction(changePhoto, changePhotoIcon, onChangeImage)
        if (hasCustomImage) {
            persistentListOf(
                changePhotoAction,
                DetailHeroMenuAction(resetToDefault, resetIcon, onClearCustomImage)
            )
        } else {
            persistentListOf(changePhotoAction)
        }
    }
}
