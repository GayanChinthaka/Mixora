/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.pokerlanka.mixora.LocalPlayerConnection
import com.pokerlanka.mixora.R
import com.pokerlanka.mixora.constants.CropAlbumArtKey
import com.pokerlanka.mixora.constants.PlayerBackgroundStyle
import com.pokerlanka.mixora.constants.PlayerBackgroundStyleKey
import com.pokerlanka.mixora.constants.PlayerHorizontalPadding
import com.pokerlanka.mixora.constants.SwipeThumbnailKey
import com.pokerlanka.mixora.constants.ThumbnailCornerRadius
import com.pokerlanka.mixora.ui.component.CastButton
import com.pokerlanka.mixora.utils.makeTimeString
import com.pokerlanka.mixora.utils.rememberEnumPreference
import com.pokerlanka.mixora.utils.rememberPreference
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pre-calculated thumbnail dimensions to avoid repeated calculations during recomposition.
 * All values are computed once and cached.
 */
@Immutable
data class ThumbnailDimensions(
    val itemWidth: Dp,
    val containerSize: Dp,
    val thumbnailSize: Dp,
    val cornerRadius: Dp
)

/**
 * Calculate thumbnail dimensions once based on container size.
 * This function is marked as @Stable to indicate it produces stable results.
 * In landscape mode, uses the smaller dimension (height) to ensure square thumbnail fits.
 */
@Stable
private fun calculateThumbnailDimensions(
    containerWidth: Dp,
    containerHeight: Dp = containerWidth,
    horizontalPadding: Dp = PlayerHorizontalPadding,
    cornerRadius: Dp = ThumbnailCornerRadius,
    isLandscape: Boolean = false
): ThumbnailDimensions {
    // In landscape, use height as the constraining dimension for a square thumbnail
    val effectiveSize = if (isLandscape) {
        minOf(containerWidth, containerHeight) - (horizontalPadding * 2)
    } else {
        containerWidth - (horizontalPadding * 2)
    }
    return ThumbnailDimensions(
        itemWidth = containerWidth,
        containerSize = containerWidth,
        thumbnailSize = effectiveSize,
        cornerRadius = cornerRadius * 2
    )
}

/**
 * Get text color based on player background style.
 * Computed once per background style change.
 */
@Stable
@Composable
private fun getTextColor(playerBackground: PlayerBackgroundStyle): Color {
    return when (playerBackground) {
        PlayerBackgroundStyle.DEFAULT -> MaterialTheme.colorScheme.onBackground
        PlayerBackgroundStyle.BLUR -> Color.White
        PlayerBackgroundStyle.GRADIENT -> Color.White
    }
}

@Composable
fun Thumbnail(
    sliderPositionProvider: () -> Long?,
    modifier: Modifier = Modifier,
    isPlayerExpanded: () -> Boolean = { true },
    isLandscape: Boolean = false,
    onSleepTimerClick: () -> Unit = {},
    sleepTimerEnabled: Boolean = false,
    sleepTimerTimeLeft: Long = 0L,
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val context = LocalContext.current
    val layoutDirection = LocalLayoutDirection.current

    // Collect states from PlayerConnection
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val error by playerConnection.error.collectAsState()
    val queueTitle by playerConnection.queueTitle.collectAsStateWithLifecycle()
    val queueWindows by playerConnection.queueWindows.collectAsStateWithLifecycle()
    val currentWindowIndex by playerConnection.currentWindowIndex.collectAsStateWithLifecycle()

    // Preferences
    val swipeThumbnail by rememberPreference(SwipeThumbnailKey, true)
    val cropArtwork by rememberPreference(CropAlbumArtKey, false)
    val playerBackground by rememberEnumPreference(
        key = PlayerBackgroundStyleKey,
        defaultValue = PlayerBackgroundStyle.DEFAULT
    )

    // Pre-calculate text color based on background style
    val textBackgroundColor = getTextColor(playerBackground)

    // Pager state
    val pageCount = if (queueWindows.isNotEmpty()) queueWindows.size else 1
    val initialPage = remember {
        currentWindowIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
    }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { if (queueWindows.isNotEmpty()) queueWindows.size else 1 }
    )

    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    var userSwiped by remember { mutableStateOf(false) }

    LaunchedEffect(isDragged) {
        if (isDragged) {
            userSwiped = true
        }
    }

    // Synchronize pager when currentWindowIndex changes externally
    LaunchedEffect(currentWindowIndex) {
        if (currentWindowIndex in 0 until pagerState.pageCount && !userSwiped) {
            if (pagerState.currentPage != currentWindowIndex) {
                pagerState.animateScrollToPage(currentWindowIndex)
            }
        }
    }

    // Synchronize playback position only when user swipes to a settled page
    LaunchedEffect(pagerState.isScrollInProgress) {
        if (!pagerState.isScrollInProgress && userSwiped) {
            userSwiped = false
            val targetPage = pagerState.settledPage
            val currentIdx = playerConnection.currentWindowIndex.value
            if (targetPage in queueWindows.indices && targetPage != currentIdx) {
                val window = queueWindows[targetPage]
                val isCasting = playerConnection.service.castConnectionHandler?.isCasting?.value == true
                val castHandler = playerConnection.service.castConnectionHandler
                if (isCasting) {
                    val mediaId = window.mediaItem.mediaId
                    val navigated = castHandler?.navigateToMediaIfInQueue(mediaId) ?: false
                    if (!navigated) {
                        playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex)
                    }
                } else {
                    playerConnection.player.seekToDefaultPosition(window.firstPeriodIndex)
                    playerConnection.player.playWhenReady = true
                }
            }
        }
    }

    // Seek effect state
    var showSeekEffect by remember { mutableStateOf(false) }
    var seekDirection by remember { mutableStateOf("") }
    var seekEffectTrigger by remember { mutableLongStateOf(0L) }

    Box(
        modifier = modifier
            .graphicsLayer {
                // Use hardware layer for entire Thumbnail to ensure smooth 120Hz animations
                compositingStrategy = CompositingStrategy.Offscreen
            }
    ) {
        // Error view
        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .padding(32.dp)
                .align(Alignment.Center),
        ) {
            error?.let { playbackError ->
                PlaybackError(
                    error = playbackError,
                    retry = playerConnection.player::prepare,
                )
            }
        }

        // Main thumbnail view
        AnimatedVisibility(
            visible = error == null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxSize()
                .then(if (!isLandscape) Modifier.statusBarsPadding() else Modifier),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (isLandscape) Arrangement.Center else Arrangement.Top
            ) {
                // Now Playing header - hide in landscape mode
                if (!isLandscape) {
                    ThumbnailHeader(
                        queueTitle = queueTitle,
                        albumTitle = mediaMetadata?.album?.title,
                        textColor = textBackgroundColor,
                        onSleepTimerClick = onSleepTimerClick,
                        sleepTimerEnabled = sleepTimerEnabled,
                        sleepTimerTimeLeft = sleepTimerTimeLeft,
                    )
                }

                // Thumbnail content
                BoxWithConstraints(
                    contentAlignment = Alignment.Center,
                    modifier = if (isLandscape) {
                        Modifier.weight(1f, false)
                    } else {
                        Modifier.fillMaxSize()
                    }
                ) {
                    val dimensions = remember(maxWidth, maxHeight, isLandscape) {
                        calculateThumbnailDimensions(
                            containerWidth = maxWidth,
                            containerHeight = maxHeight,
                            isLandscape = isLandscape
                        )
                    }

                    val onSeekCallback = remember {
                        { direction: String, showEffect: Boolean ->
                            seekDirection = direction
                            showSeekEffect = showEffect
                            seekEffectTrigger = System.currentTimeMillis()
                        }
                    }

                    val isScrollEnabled by remember(swipeThumbnail) {
                        derivedStateOf { swipeThumbnail && isPlayerExpanded() }
                    }

                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = isScrollEnabled,
                        beyondViewportPageCount = 1,
                        key = { page ->
                            if (page in queueWindows.indices) {
                                val window = queueWindows[page]
                                "${window.firstPeriodIndex}_${window.mediaItem.mediaId}_$page"
                            } else {
                                mediaMetadata?.id ?: page.toString()
                            }
                        },
                        modifier = if (isLandscape) {
                            Modifier.size(dimensions.thumbnailSize + (PlayerHorizontalPadding * 2))
                        } else {
                            Modifier.fillMaxSize()
                        }
                    ) { page ->
                        val window = queueWindows.getOrNull(page)
                        val mediaItem = window?.mediaItem ?: playerConnection.player.currentMediaItem
                        if (mediaItem != null) {
                            ThumbnailItem(
                                item = mediaItem,
                                dimensions = dimensions,
                                textBackgroundColor = textBackgroundColor,
                                layoutDirection = layoutDirection,
                                onSeek = onSeekCallback,
                                playerConnection = playerConnection,
                                context = context,
                                isLandscape = isLandscape,
                                cropArtwork = cropArtwork,
                                currentMediaId = mediaMetadata?.id,
                                currentMediaThumbnail = mediaMetadata?.thumbnailUrl
                            )
                        } else {
                            HiddenThumbnailPlaceholder(
                                textBackgroundColor = textBackgroundColor
                            )
                        }
                    }
                }
            }
        }

        // Seek effect
        LaunchedEffect(seekEffectTrigger) {
            if (seekEffectTrigger > 0L && showSeekEffect) {
                delay(1000)
                showSeekEffect = false
            }
        }

        AnimatedVisibility(
            visible = showSeekEffect,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            SeekEffectOverlay(seekDirection = seekDirection)
        }
    }
}

/**
 * Header component showing "Now Playing" and queue/album title.
 */
@Composable
private fun ThumbnailHeader(
    queueTitle: String?,
    albumTitle: String?,
    textColor: Color,
    modifier: Modifier = Modifier,
    onSleepTimerClick: () -> Unit = {},
    sleepTimerEnabled: Boolean = false,
    sleepTimerTimeLeft: Long = 0L,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 48.dp)
        ) {
            Text(
                text = stringResource(R.string.now_playing),
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )
            val playingFrom = queueTitle ?: albumTitle
            if (!playingFrom.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = playingFrom,
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor.copy(alpha = 0.8f),
                    maxLines = 1,
                    modifier = Modifier.basicMarquee()
                )
            }
        }

        IconButton(
            onClick = onSleepTimerClick,
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            if (sleepTimerEnabled) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.bedtime),
                        contentDescription = stringResource(R.string.sleep_timer),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = makeTimeString(sleepTimerTimeLeft),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor,
                        fontSize = 9.sp
                    )
                }
            } else {
                Icon(
                    painter = painterResource(R.drawable.bedtime),
                    contentDescription = stringResource(R.string.sleep_timer),
                    tint = textColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Individual thumbnail item in the carousel.
 */
@Composable
private fun ThumbnailItem(
    item: MediaItem,
    dimensions: ThumbnailDimensions,
    textBackgroundColor: Color,
    layoutDirection: LayoutDirection,
    onSeek: (String, Boolean) -> Unit,
    playerConnection: com.pokerlanka.mixora.playback.PlayerConnection,
    context: android.content.Context,
    isLandscape: Boolean = false,
    cropArtwork: Boolean = false,
    currentMediaId: String? = null,
    currentMediaThumbnail: String? = null,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()

    // Flash animation alpha for the 3 columns
    val leftFlashAlpha = remember { Animatable(0f) }
    val centerFlashAlpha = remember { Animatable(0f) }
    val rightFlashAlpha = remember { Animatable(0f) }

    // Tap counting and continuous seek accumulation state
    var leftTapCount by remember { mutableIntStateOf(0) }
    var leftAccumulatedSec by remember { mutableIntStateOf(0) }
    var leftResetJob by remember { mutableStateOf<Job?>(null) }

    var rightTapCount by remember { mutableIntStateOf(0) }
    var rightAccumulatedSec by remember { mutableIntStateOf(0) }
    var rightResetJob by remember { mutableStateOf<Job?>(null) }


    val overlayColor = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = modifier
            .then(
                if (isLandscape) {
                    Modifier.size(dimensions.thumbnailSize + (PlayerHorizontalPadding * 2))
                } else {
                    Modifier
                        .width(dimensions.itemWidth)
                        .fillMaxSize()
                }
            )
            .padding(horizontal = PlayerHorizontalPadding)
            .graphicsLayer {
                // Render entire thumbnail item on separate hardware layer for smooth animations
                compositingStrategy = CompositingStrategy.Offscreen
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(dimensions.thumbnailSize)
                .clip(RoundedCornerShape(dimensions.cornerRadius))
        ) {
            val artworkUriToUse = if (item.mediaId == currentMediaId && !currentMediaThumbnail.isNullOrBlank()) {
                currentMediaThumbnail
            } else {
                item.mediaMetadata.artworkUri?.toString()
            }

            ThumbnailImage(
                artworkUri = artworkUriToUse,
                cropArtwork = cropArtwork
            )


            // 3-Column Touch Layer with Flash Overlays
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Column 1: Backward Seek (Left 1/3)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(overlayColor.copy(alpha = leftFlashAlpha.value))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        coroutineScope.launch {
                                            leftFlashAlpha.snapTo(0.22f)
                                            leftFlashAlpha.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
                                        }

                                        leftResetJob?.cancel()
                                        leftTapCount++

                                        if (leftTapCount == 1) {
                                            leftResetJob = coroutineScope.launch {
                                                delay(400)
                                                leftTapCount = 0
                                                leftAccumulatedSec = 0
                                            }
                                        } else {
                                            leftAccumulatedSec += 5
                                            val currentPosition = playerConnection.player.currentPosition
                                            playerConnection.player.seekTo((currentPosition - 5000L).coerceAtLeast(0))
                                            onSeek(context.getString(R.string.seek_backward_accumulated, leftAccumulatedSec), true)

                                            leftResetJob = coroutineScope.launch {
                                                delay(650)
                                                leftTapCount = 0
                                                leftAccumulatedSec = 0
                                            }
                                        }
                                    }
                                )
                            }
                    )

                    // Column 2: Play / Pause (Center 1/3)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(overlayColor.copy(alpha = centerFlashAlpha.value))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        coroutineScope.launch {
                                            centerFlashAlpha.snapTo(0.22f)
                                            centerFlashAlpha.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
                                        }

                                        val isCasting = playerConnection.service.castConnectionHandler?.isCasting?.value == true
                                        val castIsPlaying = playerConnection.service.castConnectionHandler?.castIsPlaying?.value == true
                                        val castHandler = playerConnection.service.castConnectionHandler
                                        val playbackState = playerConnection.playbackState.value

                                        if (isCasting) {
                                            if (castIsPlaying) {
                                                castHandler?.pause()
                                            } else {
                                                castHandler?.play()
                                            }
                                        } else if (playbackState == Player.STATE_ENDED) {
                                            playerConnection.player.seekTo(0, 0)
                                            playerConnection.player.playWhenReady = true
                                        } else {
                                            playerConnection.togglePlayPause()
                                        }
                                    }
                                )
                            }
                    )

                    // Column 3: Forward Seek (Right 1/3)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(overlayColor.copy(alpha = rightFlashAlpha.value))
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        coroutineScope.launch {
                                            rightFlashAlpha.snapTo(0.22f)
                                            rightFlashAlpha.animateTo(0f, tween(300, easing = FastOutSlowInEasing))
                                        }

                                        rightResetJob?.cancel()
                                        rightTapCount++

                                        if (rightTapCount == 1) {
                                            rightResetJob = coroutineScope.launch {
                                                delay(400)
                                                rightTapCount = 0
                                                rightAccumulatedSec = 0
                                            }
                                        } else {
                                            rightAccumulatedSec += 5
                                            val currentPosition = playerConnection.player.currentPosition
                                            val duration = playerConnection.player.duration
                                            playerConnection.player.seekTo((currentPosition + 5000L).coerceAtMost(duration))
                                            onSeek(context.getString(R.string.seek_forward_accumulated, rightAccumulatedSec), true)

                                            rightResetJob = coroutineScope.launch {
                                                delay(650)
                                                rightTapCount = 0
                                                rightAccumulatedSec = 0
                                            }
                                        }
                                    }
                                )
                            }
                    )
                }
            }

            // Cast button at top-right corner of thumbnail
            CastButton(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                tintColor = textBackgroundColor
            )
        }
    }
}

/**
 * Placeholder shown when thumbnail is hidden.
 */
@Composable
private fun HiddenThumbnailPlaceholder(
    textBackgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.small_icon),
            contentDescription = null,
            modifier = Modifier.size(120.dp)
        )
    }
}

/**
 * Actual thumbnail image with caching and hardware layer rendering.
 */
@Composable
private fun ThumbnailImage(
    artworkUri: String?,
    cropArtwork: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                // Use offscreen compositing for hardware acceleration during animations
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(artworkUri)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .networkCachePolicy(CachePolicy.ENABLED)
                .build(),
            contentDescription = null,
            contentScale = if (cropArtwork) ContentScale.Crop else ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Seek effect overlay showing seek direction.
 */
@Composable
private fun SeekEffectOverlay(
    seekDirection: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = seekDirection,
        color = Color.White,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .padding(8.dp)
    )
}
