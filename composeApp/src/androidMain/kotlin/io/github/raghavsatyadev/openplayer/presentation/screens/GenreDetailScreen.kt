package io.github.raghavsatyadev.openplayer.presentation.screens

import io.github.raghavsatyadev.openplayer.presentation.navigation.navigateSafely
import io.github.raghavsatyadev.openplayer.presentation.navigation.navigateSafelyReplacing
import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.zIndex
import org.koin.compose.viewmodel.koinViewModel
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.github.raghavsatyadev.openplayer.data.model.Artist
import io.github.raghavsatyadev.openplayer.data.model.Song
import io.github.raghavsatyadev.openplayer.data.offline.CloudOfflineRepository
import io.github.raghavsatyadev.openplayer.data.preferences.AlbumArtPaletteStyle
import io.github.raghavsatyadev.openplayer.presentation.components.AutoScrollingTextOnDemand
import io.github.raghavsatyadev.openplayer.presentation.components.EditMultipleSongsSheet
import io.github.raghavsatyadev.openplayer.presentation.components.ExpressiveTopBarContent
import io.github.raghavsatyadev.openplayer.presentation.components.ExpressiveScrollBar
import io.github.raghavsatyadev.openplayer.presentation.components.GenreSortBottomSheet
import io.github.raghavsatyadev.openplayer.presentation.components.MiniPlayerHeight
import io.github.raghavsatyadev.openplayer.presentation.components.MultiSelectionBottomSheet
import io.github.raghavsatyadev.openplayer.presentation.components.PlayerSheetAwareBackHandler
import io.github.raghavsatyadev.openplayer.presentation.components.SmartImageCompactListTargetSize
import io.github.raghavsatyadev.openplayer.presentation.components.SmartImage
import io.github.raghavsatyadev.openplayer.presentation.components.SongInfoBottomSheet
import io.github.raghavsatyadev.openplayer.presentation.components.extractFastScrollGlyph
import io.github.raghavsatyadev.openplayer.presentation.components.subcomps.EnhancedSongListItem
import io.github.raghavsatyadev.openplayer.presentation.components.subcomps.SelectionActionRow
import io.github.raghavsatyadev.openplayer.presentation.components.subcomps.SelectionCountPill
import io.github.raghavsatyadev.openplayer.presentation.screens.QuickFillDialog
import io.github.raghavsatyadev.openplayer.presentation.selection.dragToSelect
import io.github.raghavsatyadev.openplayer.presentation.selection.rememberDragToSelectState
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.CloudDownloadsViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.GenreDetailListItem
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.GenreDetailViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.SortOption
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.SectionData
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.AlbumData
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlayerViewModel
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.StablePlayerState
import io.github.raghavsatyadev.openplayer.presentation.viewmodel.UNKNOWN_GENRE_ID
import io.github.raghavsatyadev.openplayer.ui.theme.LocalOpenPlayerDarkTheme
import io.github.raghavsatyadev.openplayer.ui.theme.LocalShowScrollbar
import io.github.raghavsatyadev.openplayer.utils.GenreParser
import io.github.raghavsatyadev.openplayer.utils.formatDuration
import io.github.raghavsatyadev.openplayer.utils.formatSongCount
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import kotlin.math.roundToInt
import kotlinx.collections.immutable.toImmutableList
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GenreDetailScreen(
    navController: NavHostController,
    genreId: String,
    decodedGenreId: String = java.net.URLDecoder.decode(genreId, "UTF-8"),
    playerViewModel: PlayerViewModel,
    viewModel: GenreDetailViewModel = koinViewModel(),
    playlistViewModel: io.github.raghavsatyadev.openplayer.presentation.viewmodel.PlaylistViewModel = koinViewModel(),
    cloudDownloadsViewModel: CloudDownloadsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stablePlayerState by playerViewModel.stablePlayerState.collectAsStateWithLifecycle()
    val favoriteSongIds by playerViewModel.favoriteSongIds.collectAsStateWithLifecycle()
    val playlistUiState by playlistViewModel.uiState.collectAsStateWithLifecycle()
    val libraryGenres by playerViewModel.genres.collectAsStateWithLifecycle()

    val haptic = LocalHapticFeedback.current

    // Song selection is shared with Library and Search through the player view model.
    val multiSelectionState = playerViewModel.multiSelectionStateHolder
    val selectedSongs by multiSelectionState.selectedSongs.collectAsStateWithLifecycle()
    val isSelectionMode by multiSelectionState.isSelectionMode.collectAsStateWithLifecycle()
    val selectedSongIds by multiSelectionState.selectedSongIds.collectAsStateWithLifecycle()
    var showMultiSelectionSheet by remember { mutableStateOf(false) }
    var showBatchEditSheet by remember { mutableStateOf(false) }

    // Start and leave with an empty selection: the holder is shared, so a selection made here
    // must not show up in Search or Library, nor one made there show up here. Leaving through the
    // top-bar back button pops the screen without going through the back handler below.
    DisposableEffect(multiSelectionState) {
        multiSelectionState.clearSelection()
        onDispose { multiSelectionState.clearSelection() }
    }

    // Back clears the selection before leaving the genre; an expanded player sheet collapses first.
    PlayerSheetAwareBackHandler(
        coordinator = playerViewModel.playerSheetBackCoordinator,
        enabled = isSelectionMode
    ) {
        multiSelectionState.clearSelection()
    }

    // Saving a batch edit or deleting clears the selection in the view model without dismissing
    // the sheets. Drop them with the selection so the next long press only selects a song. A
    // denied write permission keeps the selection, and with it the batch-edit dialog.
    LaunchedEffect(isSelectionMode) {
        if (!isSelectionMode) {
            showMultiSelectionSheet = false
            showBatchEditSheet = false
        }
    }

    val onSongLongPress: (Song) -> Unit = remember(multiSelectionState, haptic) {
        { song ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            multiSelectionState.toggleSelection(song)
        }
    }
    val dragSelectState = rememberDragToSelectState<Song>()
    // Read the holder directly: the collected selectedSongIds lag a frame behind the long press.
    val currentSelectedSongIds: () -> Set<String> = remember(multiSelectionState) {
        { multiSelectionState.selectedSongIds.value }
    }
    val onSongDragSelectionChange: (List<Song>, List<Song>) -> Unit = remember(multiSelectionState) {
        { select, deselect -> multiSelectionState.applyDragSelection(select, deselect) }
    }
    // Height of the floating selection actions, measured from the bottom of the screen.
    var selectionActionsOverlayHeight by remember { mutableStateOf(0.dp) }

    var isTransitionFinished by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        isTransitionFinished = true
    }

    val density = LocalDensity.current
    val darkMode = LocalOpenPlayerDarkTheme.current

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val systemNavBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val minTopBarHeight = 58.dp + statusBarHeight
    val maxTopBarHeight = 200.dp
    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }

    val topBarHeight = remember { Animatable(maxTopBarHeightPx) }
    // The status-bar inset can change without recreating the activity (rotation, window
    // resize; MainActivity handles orientation|screenSize itself). Clamp the bar's height
    // into the new range so the collapsed bar never sits under the status bar.
    LaunchedEffect(minTopBarHeightPx, maxTopBarHeightPx) {
        val clamped = topBarHeight.value.coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
        if (clamped != topBarHeight.value) topBarHeight.snapTo(clamped)
    }
    val collapseFraction by remember(minTopBarHeightPx, maxTopBarHeightPx) {
        derivedStateOf {
            1f - ((topBarHeight.value - minTopBarHeightPx) / (maxTopBarHeightPx - minTopBarHeightPx)).coerceIn(0f, 1f)
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

    val nestedScrollConnection = remember(minTopBarHeightPx, maxTopBarHeightPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val isScrollingDown = delta < 0

                if (!isScrollingDown && (lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0)) {
                    return Offset.Zero
                }

                val previousHeight = topBarHeight.value
                val newHeight = (previousHeight + delta).coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
                val consumed = newHeight - previousHeight

                if (consumed.roundToInt() != 0) {
                    coroutineScope.launch { topBarHeight.snapTo(newHeight) }
                }

                val canConsumeScroll = !(isScrollingDown && newHeight == minTopBarHeightPx)
                return if (canConsumeScroll) Offset(0f, consumed) else Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val currentHeight = topBarHeight.value
                if (currentHeight > minTopBarHeightPx && currentHeight < maxTopBarHeightPx) {
                    val targetHeight = if (available.y > 500f) {
                        maxTopBarHeightPx
                    } else if (available.y < -500f) {
                        minTopBarHeightPx
                    } else {
                        if (currentHeight > (minTopBarHeightPx + maxTopBarHeightPx) / 2) maxTopBarHeightPx else minTopBarHeightPx
                    }
                    
                    coroutineScope.launch {
                        topBarHeight.animateTo(
                            targetValue = targetHeight,
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow) 
                        )
                    }
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    val defaultContainer = MaterialTheme.colorScheme.surfaceVariant
    val defaultOnContainer = MaterialTheme.colorScheme.onSurfaceVariant
    val themeGenre = uiState.genre
    val themeColor = remember(themeGenre, decodedGenreId, darkMode, defaultContainer, defaultOnContainer) {
        if (themeGenre != null) {
            io.github.raghavsatyadev.openplayer.ui.theme.GenreThemeUtils.getGenreThemeColor(
                genre = themeGenre,
                isDark = darkMode,
                fallbackGenreId = decodedGenreId
            )
        } else {
            io.github.raghavsatyadev.openplayer.ui.theme.GenreThemeColor(
                defaultContainer,
                defaultOnContainer
            )
        }
    }
    
    val startColor = themeColor.container
    val contentColor = themeColor.onContainer
    
    val initialDisplayName = remember(decodedGenreId) {
        // Song info passes the song's whole genre value; the view model opens its first genre, or
        // the Unknown bucket for a placeholder such as "<unknown>".
        (GenreParser.split(decodedGenreId).firstOrNull() ?: UNKNOWN_GENRE_ID)
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }
    val genreDisplayName = themeGenre?.name ?: uiState.genre?.name ?: initialDisplayName
    val genreShuffleLabel = stringResource(Res.string.presentation_batch_b_genre_shuffle_label, genreDisplayName)
    val genreFastScrollLabelProvider = remember(uiState.flattenedItems, uiState.sortOption) {
        { index: Int ->
            genreFastScrollLabel(
                items = uiState.flattenedItems,
                index = index,
                sortOption = uiState.sortOption
            )
        }
    }
    
    val toastAddedToQueue = stringResource(Res.string.toast_added_to_queue)
    val toastPlayingNext = stringResource(Res.string.toast_playing_next)

    var showSortSheet by remember { mutableStateOf(false) }
    var showSongOptionsSheet by remember { mutableStateOf<Song?>(null) }
    var showPlaylistBottomSheet by remember { mutableStateOf(false) }
    var playlistSheetSongs by remember { mutableStateOf<ImmutableList<Song>>(persistentListOf()) }
    var showQuickFillDialog by remember { mutableStateOf(false) }

    val isUnknownGenre = remember(decodedGenreId) {
        GenreParser.split(decodedGenreId).isEmpty()
    }
    
    val customGenres by playerViewModel.customGenres.collectAsStateWithLifecycle()
    val customGenreIcons by playerViewModel.customGenreIcons.collectAsStateWithLifecycle()
    val genrePaletteStyle by playerViewModel.albumArtPaletteStyle.collectAsStateWithLifecycle(
        initialValue = AlbumArtPaletteStyle.default
    )
    val isMiniPlayerVisible = stablePlayerState.currentSong != null
    val fabBottomPadding by animateDpAsState(
        targetValue = if (isMiniPlayerVisible) MiniPlayerHeight + systemNavBarInset + 16.dp else systemNavBarInset + 16.dp,
        label = "fabPadding"
    )

    val baseColorScheme = MaterialTheme.colorScheme

    val genreColorScheme = remember(themeGenre, decodedGenreId, darkMode, genrePaletteStyle) {
        io.github.raghavsatyadev.openplayer.ui.theme.GenreThemeUtils.getGenreDetailColorScheme(
            genre = themeGenre,
            fallbackGenreId = decodedGenreId,
            isDark = darkMode,
            paletteStyle = genrePaletteStyle
        )
    }

    MaterialTheme(colorScheme = genreColorScheme) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val currentTopBarHeightDp = with(density) { topBarHeight.value.toDp() }
            val displayItems = if (isTransitionFinished || uiState.flattenedItems.size < 20) {
                uiState.flattenedItems
            } else {
                uiState.flattenedItems.take(20)
            }
            // Headers, spacers and dividers share the list with the songs but are not selectable.
            val dragSelectSongAt: (Int) -> Song? = remember(displayItems) {
                { index -> (displayItems.getOrNull(index) as? GenreDetailListItem.SongItem)?.song }
            }

            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(
                    top = minTopBarHeight + 8.dp,
                    start = 8.dp,
                    end = if (showScrollBar) 24.dp else 8.dp,
                    bottom = fabBottomPadding + 148.dp
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        val extraHeight = (topBarHeight.value - minTopBarHeightPx).roundToInt()
                        IntOffset(0, extraHeight)
                    }
                    .dragToSelect(
                        state = dragSelectState,
                        listState = lazyListState,
                        itemAt = dragSelectSongAt,
                        itemKey = Song::id,
                        selectedKeys = currentSelectedSongIds,
                        onSelectionChange = onSongDragSelectionChange,
                        // The collapsed top bar always covers this much of the list's top.
                        topInset = minTopBarHeight,
                        bottomInset = when {
                            isSelectionMode -> selectionActionsOverlayHeight
                            isMiniPlayerVisible -> systemNavBarInset + MiniPlayerHeight
                            else -> systemNavBarInset
                        }
                    )
            ) {
                items(
                    items = displayItems,
                    key = { it.key },
                    contentType = { it::class }
                ) { item ->
                    when (item) {
                        is GenreDetailListItem.ArtistHeader -> {
                            GenreArtistHeader(item.artistName, item.artistImageUrl)
                        }
                        is GenreDetailListItem.AlbumHeader -> {
                            GenreAlbumHeader(
                                album = item.album,
                                useArtistStyle = item.useArtistStyle,
                                onSongClick = { song ->
                                    playerViewModel.showAndPlaySong(song, uiState.sortedSongs, genreDisplayName)
                                }
                            )
                        }
                        is GenreDetailListItem.SongItem -> {
                            GenreSongItemWrapper(
                                item = item,
                                stablePlayerState = stablePlayerState,
                                onSongClick = { song ->
                                    playerViewModel.showAndPlaySong(song, uiState.sortedSongs, genreDisplayName)
                                },
                                onMoreOptionsClick = { song -> showSongOptionsSheet = song },
                                isSelectionMode = isSelectionMode,
                                isSelected = item.song.id in selectedSongIds,
                                selectionIndex = multiSelectionState.getSelectionIndex(item.song.id),
                                onLongPress = {
                                    onSongLongPress(item.song)
                                    dragSelectState.onItemLongPressed(item.song)
                                }
                            )
                        }
                        is GenreDetailListItem.Spacer -> {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(item.heightDp.dp)
                                    .run {
                                        if (item.useSurfaceBackground) background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f))
                                        else this
                                    }
                            )
                        }
                        is GenreDetailListItem.Divider -> {
                             Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f))
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                HorizontalDivider(modifier = Modifier.alpha(0.3f))
                            }
                        }
                    }
                }
            }

            if (showScrollBar) {
                ExpressiveScrollBar(
                    listState = lazyListState,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(
                            top = minTopBarHeight + 12.dp,
                            bottom = fabBottomPadding + 112.dp
                        ),
                    dragLabelProvider = genreFastScrollLabelProvider
                )
            }

            GenreCollapsibleTopBar(
                title = genreDisplayName,
                collapseFraction = collapseFraction,
                headerHeight = currentTopBarHeightDp,
                onBackPressed = { navController.popBackStack() },
                startColor = startColor,
                contentColor = contentColor,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                collapsedContentColor = MaterialTheme.colorScheme.onSurface
            )

            SelectionCountPill(
                selectedCount = selectedSongs.size,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(6f)
                    .padding(top = minTopBarHeight + 24.dp)
            )

            if (isSelectionMode) {
                // SelectionActionRow has no background of its own, so give it a surface while it
                // floats over the list.
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .onSizeChanged { size ->
                            selectionActionsOverlayHeight = with(density) { size.height.toDp() }
                        }
                        .padding(start = 16.dp, end = 16.dp, bottom = fabBottomPadding + 16.dp)
                        .zIndex(10f)
                ) {
                    SelectionActionRow(
                        selectedCount = selectedSongs.size,
                        onSelectAll = { multiSelectionState.selectAll(uiState.sortedSongs) },
                        onDeselect = { multiSelectionState.clearSelection() },
                        onOptionsClick = { showMultiSelectionSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                }
            } else {
                // Only reachable outside selection mode, so the sort sheet can't open mid-selection.
                Box(
                     modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = fabBottomPadding + 26.dp, end = 16.dp)
                        .zIndex(10f)
                ) {
                     MediumFloatingActionButton(
                        onClick = { showSortSheet = true },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        shape = AbsoluteSmoothCornerShape(24.dp, 60)
                    ) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.rounded_more_vert_24),
                            contentDescription = stringResource(Res.string.cd_options),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            if (showSortSheet) {
                GenreSortBottomSheet(
                    onDismiss = { showSortSheet = false },
                    currentSort = uiState.sortOption,
                    onSortSelected = {
                        viewModel.updateSortOption(it)
                        showSortSheet = false
                    },
                    onShuffle = {
                        if (uiState.songs.isNotEmpty()) {
                            playerViewModel.showAndPlaySong(uiState.sortedSongs.random(), uiState.sortedSongs, genreShuffleLabel)
                            showSortSheet = false
                        }
                    },
                    headerContent = if (isUnknownGenre) {
                        {
                            Button(
                                onClick = {
                                    showSortSheet = false
                                    showQuickFillDialog = true
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                ),
                                shape = racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape(16.dp, 60)
                            ) {
                                Icon(vectorResource(Res.drawable.rounded_auto_fix_high_filled_24), contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(Res.string.genre_quick_fill),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else null
                )
            }

            MaterialTheme(colorScheme = baseColorScheme) {
                QuickFillDialog(
                    visible = showQuickFillDialog,
                    songs = remember(uiState.songs) { uiState.songs.toImmutableList() },
                    customGenres = customGenres,
                    customGenreIcons = customGenreIcons,
                    onDismiss = { showQuickFillDialog = false },
                    onApply = { songs, genre ->
                        playerViewModel.batchEditGenre(songs, genre)
                        showQuickFillDialog = false
                    },
                    onAddCustomGenre = { genre, iconRes ->
                        playerViewModel.addCustomGenre(genre, iconRes)
                    }
                )
            }
        
            showSongOptionsSheet?.let { song ->
                val isFavorite = favoriteSongIds.contains(song.id)

                MaterialTheme(
                    colorScheme = genreColorScheme,
                    typography = MaterialTheme.typography,
                    shapes = MaterialTheme.shapes
                ) {
                    SongInfoBottomSheet(
                        song = song,
                        isFavorite = isFavorite,
                        onToggleFavorite = {
                            playerViewModel.toggleFavoriteSpecificSong(song)
                        },
                        onDismiss = { showSongOptionsSheet = null },
                        onPlaySong = {
                            playerViewModel.showAndPlaySong(song, uiState.sortedSongs, genreDisplayName)
                            showSongOptionsSheet = null
                        },
                        onAddToQueue = {
                            playerViewModel.addSongToQueue(song)
                            showSongOptionsSheet = null
                            playerViewModel.sendToast(toastAddedToQueue)
                        },
                        onAddNextToQueue = {
                            playerViewModel.addSongNextToQueue(song)
                            showSongOptionsSheet = null
                            playerViewModel.sendToast(toastPlayingNext)
                        },
                        onAddToPlayList = {
                            playlistSheetSongs = persistentListOf(song)
                            showPlaylistBottomSheet = true
                        },
                        onDeleteFromDevice = playerViewModel::deleteFromDevice,
                        onNavigateToAlbum = {
                            navController.navigateSafelyReplacing(
                                route = io.github.raghavsatyadev.openplayer.presentation.navigation.Screen.AlbumDetail.createRoute(song.albumId),
                                patternToPop = io.github.raghavsatyadev.openplayer.presentation.navigation.Screen.AlbumDetail.route
                            )
                            showSongOptionsSheet = null
                        },
                        onNavigateToArtist = {
                            navController.navigateSafelyReplacing(
                                route = io.github.raghavsatyadev.openplayer.presentation.navigation.Screen.ArtistDetail.createRoute(song.artistId),
                                patternToPop = io.github.raghavsatyadev.openplayer.presentation.navigation.Screen.ArtistDetail.route
                            )
                            showSongOptionsSheet = null
                        },
                        onNavigateToArtistById = { artistId ->
                            navController.navigateSafelyReplacing(
                                route = io.github.raghavsatyadev.openplayer.presentation.navigation.Screen.ArtistDetail.createRoute(artistId),
                                patternToPop = io.github.raghavsatyadev.openplayer.presentation.navigation.Screen.ArtistDetail.route
                            )
                            showSongOptionsSheet = null
                        },
                        onNavigateToGenre = {
                            song.genre?.let {
                                navController.navigateSafelyReplacing(
                                    route = io.github.raghavsatyadev.openplayer.presentation.navigation.Screen.GenreDetail.createRoute(java.net.URLEncoder.encode(it, "UTF-8")),
                                    patternToPop = io.github.raghavsatyadev.openplayer.presentation.navigation.Screen.GenreDetail.route
                                )
                            }
                            showSongOptionsSheet = null
                        },
                        onEditSong = { newTitle, newArtist, newAlbum, newAlbumArtist, newComposer, newGenre, newLyrics, newTrackNumber, newDiscNumber, replayGainTrackGainDb, replayGainAlbumGainDb, coverArtUpdate, customMetadataChanges ->
                            playerViewModel.editSongMetadata(
                                song,
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
                        removeFromListTrigger = {}
                    )
                }
            }

            // Shared by the single-song options sheet and the multi-selection sheet.
            if (showPlaylistBottomSheet && playlistSheetSongs.isNotEmpty()) {
                io.github.raghavsatyadev.openplayer.presentation.components.PlaylistBottomSheet(
                    playlistUiState = playlistUiState,
                    songs = playlistSheetSongs,
                    onDismiss = {
                        showPlaylistBottomSheet = false
                        playlistSheetSongs = persistentListOf()
                    },
                    bottomBarHeight = 0.dp,
                    playerViewModel = playerViewModel
                )
            }

            if (showMultiSelectionSheet && selectedSongs.isNotEmpty()) {
                MultiSelectionBottomSheet(
                    selectedSongs = selectedSongs,
                    favoriteSongIds = favoriteSongIds,
                    onDismiss = { showMultiSelectionSheet = false },
                    onPlayAll = {
                        playerViewModel.playSelectedSongs(selectedSongs)
                        showMultiSelectionSheet = false
                    },
                    onAddToQueue = {
                        playerViewModel.addSelectedToQueue(selectedSongs)
                        showMultiSelectionSheet = false
                    },
                    onPlayNext = {
                        playerViewModel.addSelectedAsNext(selectedSongs)
                        showMultiSelectionSheet = false
                    },
                    onAddToPlaylist = {
                        playlistSheetSongs = selectedSongs
                        multiSelectionState.clearSelection()
                        showMultiSelectionSheet = false
                        showPlaylistBottomSheet = true
                    },
                    onToggleLikeAll = { shouldLike ->
                        if (shouldLike) {
                            playerViewModel.likeSelectedSongs(selectedSongs)
                        } else {
                            playerViewModel.unlikeSelectedSongs(selectedSongs)
                        }
                        showMultiSelectionSheet = false
                    },
                    onShareAll = {
                        playerViewModel.shareSelectedAsZip(selectedSongs)
                        showMultiSelectionSheet = false
                    },
                    onDownloadAll = {
                        val cloudSongCount = CloudOfflineRepository.downloadCandidates(selectedSongs).size
                        cloudDownloadsViewModel.downloadSelected(selectedSongs)
                        multiSelectionState.clearSelection()
                        coroutineScope.launch {
                            playerViewModel.sendToast(
                                getPluralString(
                                    Res.plurals.cloud_download_selected_started,
                                    cloudSongCount,
                                    cloudSongCount
                                )
                            )
                        }
                        showMultiSelectionSheet = false
                    },
                    onDeleteAll = { deleteActivity, onComplete ->
                        playerViewModel.deleteSelectedFromDevice(deleteActivity, selectedSongs) {
                            showMultiSelectionSheet = false
                            onComplete(true)
                        }
                    },
                    onBatchEdit = {
                        showMultiSelectionSheet = false
                        showBatchEditSheet = true
                    }
                )
            }

            if (showBatchEditSheet && selectedSongs.isNotEmpty()) {
                EditMultipleSongsSheet(
                    visible = showBatchEditSheet,
                    songs = selectedSongs,
                    onDismiss = { showBatchEditSheet = false },
                    onSave = { songs, title, artist, album, albumArtist, composer, genre, lyrics, trackNumber, discNumber, replayGainTrackGainDb, replayGainAlbumGainDb, coverArtUpdate ->
                        playerViewModel.saveBatchMetadata(
                            songs = songs,
                            title = title,
                            artist = artist,
                            album = album,
                            albumArtist = albumArtist,
                            composer = composer,
                            genre = genre,
                            lyrics = lyrics,
                            trackNumber = trackNumber,
                            discNumber = discNumber,
                            replayGainTrackGainDb = replayGainTrackGainDb,
                            replayGainAlbumGainDb = replayGainAlbumGainDb,
                            coverArtUpdate = coverArtUpdate
                        )
                    }
                )
            }

            if (uiState.isLoadingSongs) {
                ContainedLoadingIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

private fun genreFastScrollLabel(
    items: List<GenreDetailListItem>,
    index: Int,
    sortOption: SortOption
): String? {
    if (items.isEmpty()) return null

    val clampedIndex = index.coerceIn(0, items.lastIndex)
    for (candidateIndex in clampedIndex downTo 0) {
        val label = items[candidateIndex].fastScrollLabel(sortOption)
        if (!label.isNullOrBlank()) {
            return label
        }
    }

    return null
}

private fun GenreDetailListItem.fastScrollLabel(sortOption: SortOption): String? =
    when (sortOption) {
        SortOption.ARTIST -> when (this) {
            is GenreDetailListItem.ArtistHeader -> extractFastScrollGlyph(artistName)
            is GenreDetailListItem.AlbumHeader -> extractFastScrollGlyph(album.songs.firstOrNull()?.artist)
            is GenreDetailListItem.SongItem -> extractFastScrollGlyph(song.artist)
            is GenreDetailListItem.Spacer,
            is GenreDetailListItem.Divider -> null
        }

        SortOption.ALBUM -> when (this) {
            is GenreDetailListItem.ArtistHeader -> null
            is GenreDetailListItem.AlbumHeader -> extractFastScrollGlyph(album.name)
            is GenreDetailListItem.SongItem -> extractFastScrollGlyph(song.album)
            is GenreDetailListItem.Spacer,
            is GenreDetailListItem.Divider -> null
        }

        SortOption.TITLE -> when (this) {
            is GenreDetailListItem.ArtistHeader -> null
            is GenreDetailListItem.AlbumHeader -> null
            is GenreDetailListItem.SongItem -> extractFastScrollGlyph(song.title)
            is GenreDetailListItem.Spacer,
            is GenreDetailListItem.Divider -> null
        }
    }

@Composable
fun GenreCollapsibleTopBar(
    title: String,
    collapseFraction: Float,
    headerHeight: Dp,
    onBackPressed: () -> Unit,
    startColor: Color,
    containerColor: Color,
    contentColor: Color,
    collapsedContentColor: Color
) {
    val solidAlpha = (collapseFraction * 2f).coerceIn(0f, 1f)
    val animatedContentColor = androidx.compose.ui.graphics.lerp(
        start = contentColor,
        stop = collapsedContentColor,
        fraction = solidAlpha
    )

    val gradientAlpha = 0.8f * (1f - solidAlpha)
    
    val verticalGradient = remember(startColor, gradientAlpha) {
        Brush.verticalGradient(
            colors = listOf(
                startColor.copy(alpha = gradientAlpha),
                startColor.copy(alpha = 0f)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
            .zIndex(5f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(containerColor.copy(alpha = solidAlpha)) 
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(verticalGradient)
        )

        Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
             FilledIconButton(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 4.dp)
                    .zIndex(10f),
                onClick = onBackPressed,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = animatedContentColor.copy(alpha = 0.1f),
                    contentColor = animatedContentColor
                )
            ) {
                Icon(vectorResource(Res.drawable.rounded_arrow_back_24), contentDescription = stringResource(Res.string.auth_cd_back), tint = animatedContentColor)
            }

            ExpressiveTopBarContent(
                title = title,
                collapseFraction = collapseFraction,
                modifier = Modifier.fillMaxSize(),
                collapsedTitleStartPadding = 68.dp,
                expandedTitleStartPadding = 20.dp,
                maxLines = 1,
                contentColor = animatedContentColor
            )
        }
    }
}


@Composable
fun GenreArtistHeader(
    artistName: String,
    artistImageUrl: String?
) {
    val headerShape = remember {
        AbsoluteSmoothCornerShape(
            cornerRadiusTR = 24.dp, smoothnessAsPercentTR = 60,
            cornerRadiusTL = 24.dp, smoothnessAsPercentTL = 60,
            cornerRadiusBR = 0.dp, smoothnessAsPercentBR = 0,
            cornerRadiusBL = 0.dp, smoothnessAsPercentBL = 0
        )
    }

    val context = LocalContext.current
    val imageRequest = remember(artistImageUrl) {
        if (!artistImageUrl.isNullOrEmpty()) {
            ImageRequest.Builder(context)
                .data(artistImageUrl)
                .crossfade(true)
                .build()
        } else null
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f),
        shape = headerShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageRequest != null) {
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = artistName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = vectorResource(Res.drawable.rounded_person_filled_24),
                            contentDescription = stringResource(Res.string.cd_generic_artist),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxSize()
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))
                Text(
                    text = artistName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun GenreAlbumHeader(
    album: AlbumData,
    useArtistStyle: Boolean,
    onSongClick: (Song) -> Unit
) {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f)
    val shape = remember(useArtistStyle) {
        if (useArtistStyle) {
            RectangleShape
        } else {
             AbsoluteSmoothCornerShape(
                cornerRadiusTR = 24.dp, smoothnessAsPercentTR = 60,
                cornerRadiusTL = 24.dp, smoothnessAsPercentTL = 60,
                cornerRadiusBR = 0.dp, smoothnessAsPercentBR = 0,
                cornerRadiusBL = 0.dp, smoothnessAsPercentBL = 0
            )
        }
    }
    
    Box(
         modifier = Modifier
            .fillMaxWidth()
            .background(containerColor, shape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SmartImage(
                model = album.artUri,
                contentDescription = null,
                targetSize = SmartImageCompactListTargetSize,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                val shouldScroll = album.name.length > 20
                if (shouldScroll) {
                    AutoScrollingTextOnDemand(
                        text = album.name,
                        style = MaterialTheme.typography.titleMedium,
                        gradientEdgeColor = MaterialTheme.colorScheme.surface,
                        expansionFractionProvider = { 1f },
                    )
                } else {
                    Text(
                        text = album.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = formatSongCount(album.songs.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = {
                    if(album.songs.isNotEmpty()) onSongClick(album.songs.first())
                },
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(vectorResource(Res.drawable.rounded_play_arrow_filled_24), contentDescription = stringResource(Res.string.cd_play_album))
            }
        }
    }
}

@Composable
fun GenreSongItemWrapper(
    item: io.github.raghavsatyadev.openplayer.presentation.viewmodel.GenreDetailListItem.SongItem,
    stablePlayerState: StablePlayerState,
    onSongClick: (Song) -> Unit,
    onMoreOptionsClick: (Song) -> Unit,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    selectionIndex: Int? = null,
    onLongPress: () -> Unit = {}
) {
    val song = item.song
    val isFirstInAlbum = item.isFirstInAlbum
    val isLastInAlbum = item.isLastInAlbum
    val isLastAlbumInSection = item.isLastAlbumInSection
    val useArtistStyle = item.useArtistStyle

    val songItemShape = remember(isFirstInAlbum, isLastInAlbum) {
        when {
            isFirstInAlbum && isLastInAlbum -> RoundedCornerShape(16.dp)
            isFirstInAlbum -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
            isLastInAlbum -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
            else -> RoundedCornerShape(4.dp)
        }
    }
    
    val containerShape = remember(isLastInAlbum, isLastAlbumInSection) {
        if (isLastInAlbum && isLastAlbumInSection) {
            AbsoluteSmoothCornerShape(
                cornerRadiusTR = 0.dp, smoothnessAsPercentTR = 0,
                cornerRadiusTL = 0.dp, smoothnessAsPercentTL = 0,
                cornerRadiusBR = 24.dp, smoothnessAsPercentBR = 60,
                cornerRadiusBL = 24.dp, smoothnessAsPercentBL = 60
            ) 
        } else {
           RectangleShape
        }
    }
   
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f), containerShape)
            .padding(horizontal = 8.dp) 
            .padding(bottom = if (isLastInAlbum && !isLastAlbumInSection && useArtistStyle) 8.dp else 0.dp)
    ) {
        Column {
            if (!isFirstInAlbum) Spacer(Modifier.height(2.dp))
            
            val isCurrent = stablePlayerState.currentSong?.id == song.id
            val isPlaying = stablePlayerState.isPlaying

            EnhancedSongListItem(
                 song = song,
                 isPlaying = isPlaying,
                 isCurrentSong = isCurrent,
                 showAlbumArt = false,
                 customShape = songItemShape,
                 onClick = { onSongClick(song) },
                 onMoreOptionsClick = onMoreOptionsClick,
                 isSelected = isSelected,
                 selectionIndex = selectionIndex,
                 isSelectionMode = isSelectionMode,
                 onLongPress = onLongPress
             )
             
             if (isLastInAlbum) Spacer(Modifier.height(8.dp))
        }
    }
}
