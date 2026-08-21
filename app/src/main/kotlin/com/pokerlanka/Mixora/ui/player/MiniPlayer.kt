/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 *
 * Performance optimized MiniPlayer - prevents unnecessary recomposition
 */

package com.pokerlanka.mixora.ui.player

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.Stable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import com.pokerlanka.mixora.LocalDatabase
import com.pokerlanka.mixora.LocalPlayerConnection
import com.pokerlanka.mixora.R
import androidx.compose.ui.platform.LocalView
import com.pokerlanka.mixora.constants.CropAlbumArtKey
import com.pokerlanka.mixora.constants.DarkModeKey
import com.pokerlanka.mixora.constants.MiniPlayerHeight
import com.pokerlanka.mixora.constants.PureBlackMiniPlayerKey
import com.pokerlanka.mixora.constants.SwipeMiniPlayerKey
import com.pokerlanka.mixora.constants.SwipeSensitivityKey
import com.pokerlanka.mixora.constants.SwipeThumbnailKey
import com.pokerlanka.mixora.constants.ThumbnailCornerRadius
import com.pokerlanka.mixora.constants.UseNewMiniPlayerDesignKey
import com.pokerlanka.mixora.db.entities.ArtistEntity
import com.pokerlanka.mixora.models.MediaMetadata
import com.pokerlanka.mixora.playback.CastConnectionHandler
import com.pokerlanka.mixora.playback.PlayerConnection
import com.pokerlanka.mixora.ui.screens.settings.DarkMode
import com.pokerlanka.mixora.ui.utils.resize
import com.pokerlanka.mixora.utils.joinToArtistString
import com.pokerlanka.mixora.utils.rememberEnumPreference
import com.pokerlanka.mixora.utils.rememberPreference
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import com.pokerlanka.mixora.ui.component.Icon as MIcon
import androidx.compose.ui.draw.blur
import com.pokerlanka.mixora.constants.DisableBlurKey
import com.pokerlanka.mixora.constants.MiniPlayerBackgroundStyle
import com.pokerlanka.mixora.constants.MiniPlayerBackgroundStyleKey
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.pokerlanka.mixora.ui.theme.PlayerColorExtractor
import com.pokerlanka.mixora.ui.component.LocalMenuState
import com.pokerlanka.mixora.ui.menu.AddToPlaylistDialog

/**
 * Stable wrapper for progress state - reads values only during draw phase
 * This prevents recomposition when position/duration change
 */
@Stable
class ProgressState(
    private val positionState: MutableLongState,
    private val durationState: MutableLongState,
) {
    val progress: Float
        get() {
            val duration = durationState.longValue
            return if (duration > 0) (positionState.longValue.toFloat() / duration).coerceIn(0f, 1f) else 0f
        }
}

@Composable
fun MiniPlayer(
    positionState: MutableLongState,
    durationState: MutableLongState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    // Create stable progress state - doesn't cause recomposition on position changes
    val progressState = remember { ProgressState(positionState, durationState) }

    NewMiniPlayer(
        progressState = progressState,
        modifier = modifier,
        onClick = onClick,
    )
}

// ============================================================================
// NEW MINI PLAYER DESIGN
// ============================================================================

@Composable
private fun NewMiniPlayer(
    progressState: ProgressState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val playerConnection = LocalPlayerConnection.current ?: return
    val view = LocalView.current

    // Theme settings - these rarely change
    val miniPlayerBackground by rememberEnumPreference(
        MiniPlayerBackgroundStyleKey,
        defaultValue = MiniPlayerBackgroundStyle.DEFAULT,
    )
    val context = LocalContext.current
    var gradientColors by remember { mutableStateOf<List<Color>>(emptyList()) }
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val darkTheme by rememberEnumPreference(DarkModeKey, defaultValue = DarkMode.AUTO)
    val useDarkTheme =
        remember(darkTheme, isSystemInDarkTheme) {
            if (darkTheme == DarkMode.AUTO) isSystemInDarkTheme else darkTheme == DarkMode.ON
        }

    // Player states - only collect what's needed at this level
    val playbackState by playerConnection.playbackState.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()
    val isPlaying by playerConnection.isPlaying.collectAsState()

    // Cast state - safely access castConnectionHandler to prevent crashes during service lifecycle changes
    val castHandler =
        remember(playerConnection) {
            try {
                playerConnection.service.castConnectionHandler
            } catch (e: Exception) {
                null
            }
        }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }

    // Swipe settings
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeMiniPlayer by rememberPreference(SwipeMiniPlayerKey, true)
    val (disableBlur) = rememberPreference(DisableBlurKey, false)

    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()

    val windowInfo = LocalWindowInfo.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val isTabletLandscape =
        remember(windowInfo.containerSize.width, configuration.orientation) {
            (windowInfo.containerSize.width / density.density) >= 600f && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }

    // Swipe animation state
    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val animationSpec =
        remember {
            spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
        }

    val autoSwipeThreshold =
        remember(swipeSensitivity) {
            (600 / (1f + kotlin.math.exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
        }

    LaunchedEffect(mediaMetadata?.id, miniPlayerBackground) {
        gradientColors = emptyList()
        if (miniPlayerBackground == MiniPlayerBackgroundStyle.GRADIENT) {
            val url = mediaMetadata?.thumbnailUrl
            if (url != null) {
                withContext(Dispatchers.IO) {
                    val request = ImageRequest.Builder(context)
                        .data(url)
                        .size(100, 100)
                        .allowHardware(false)
                        .build()
                    val result = runCatching { context.imageLoader.execute(request) }.getOrNull()
                    val bitmap = result?.image?.toBitmap()
                    if (bitmap != null) {
                        val palette = withContext(Dispatchers.Default) {
                            Palette.from(bitmap)
                                .maximumColorCount(8)
                                .resizeBitmapArea(100 * 100)
                                .generate()
                        }
                        val extracted = PlayerColorExtractor.extractGradientColors(
                            palette = palette,
                            fallbackColor = 0xFF000000.toInt(),
                        )
                        withContext(Dispatchers.Main) {
                            gradientColors = extracted
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            gradientColors = emptyList()
                        }
                    }
                }
            }
        } else {
            gradientColors = emptyList()
        }
    }

    // Memoize colors
    val backgroundColor = when (miniPlayerBackground) {
        MiniPlayerBackgroundStyle.DEFAULT    -> MaterialTheme.colorScheme.surfaceContainer
        MiniPlayerBackgroundStyle.TRANSPARENT -> Color.Black.copy(alpha = 0.25f)
        MiniPlayerBackgroundStyle.BLUR       -> MaterialTheme.colorScheme.surfaceContainer
        MiniPlayerBackgroundStyle.GRADIENT   -> MaterialTheme.colorScheme.surfaceContainer
        MiniPlayerBackgroundStyle.PURE_BLACK -> Color.Black
    }
    val forceLightColors = !useDarkTheme && (miniPlayerBackground == MiniPlayerBackgroundStyle.PURE_BLACK ||
            miniPlayerBackground == MiniPlayerBackgroundStyle.BLUR ||
            miniPlayerBackground == MiniPlayerBackgroundStyle.GRADIENT)

    val primaryColor = if (forceLightColors) Color.White else MaterialTheme.colorScheme.primary
    val outlineColor = if (forceLightColors) Color.White else MaterialTheme.colorScheme.outline
    val onSurfaceColor = if (forceLightColors) Color.White else MaterialTheme.colorScheme.onSurface
    val errorColor = if (forceLightColors) Color(0xFFFF6B6B) else MaterialTheme.colorScheme.error

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(MiniPlayerHeight)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Swipe indicator arrows (<< or >>)
        if (offsetXAnimatable.value.absoluteValue > 40f) {
            Box(
                modifier =
                    Modifier
                        .align(if (offsetXAnimatable.value > 0) Alignment.CenterStart else Alignment.CenterEnd)
                        .padding(horizontal = 16.dp),
            ) {
                Icon(
                    painter =
                        painterResource(
                            if (offsetXAnimatable.value > 0) R.drawable.skip_previous else R.drawable.skip_next,
                        ),
                    contentDescription = null,
                    tint =
                        primaryColor.copy(
                            alpha = (offsetXAnimatable.value.absoluteValue / autoSwipeThreshold).coerceIn(0f, 1f),
                        ),
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        val interactionSource = remember { MutableInteractionSource() }
        Box(
            modifier =
                Modifier
                    .then(if (isTabletLandscape) Modifier.width(500.dp).align(Alignment.Center) else Modifier.fillMaxWidth())
                    .height(64.dp)
                    .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                    .clip(RoundedCornerShape(32.dp))
                    .background(color = backgroundColor)
                    .border(1.dp, outlineColor.copy(alpha = 0.3f), RoundedCornerShape(32.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onClick,
                    ).let { baseModifier ->
                        if (swipeMiniPlayer) {
                            baseModifier.pointerInput(Unit) {
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        dragStartTime = System.currentTimeMillis()
                                        totalDragDistance = 0f
                                    },
                                    onDragCancel = {
                                        coroutineScope.launch {
                                            offsetXAnimatable.animateTo(0f, animationSpec)
                                        }
                                    },
                                    onHorizontalDrag = { _, dragAmount ->
                                        val adjustedDragAmount =
                                            if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                                        val canSkipPrev = playerConnection.player.previousMediaItemIndex != -1
                                        val canSkipNxt = playerConnection.player.nextMediaItemIndex != -1
                                        val tryingToSwipeRight = adjustedDragAmount > 0
                                        val tryingToSwipeLeft = adjustedDragAmount < 0
                                        val allowLeft = tryingToSwipeLeft && canSkipNxt
                                        val allowRight = tryingToSwipeRight && canSkipPrev

                                        val canReturnToCenter =
                                            (tryingToSwipeRight && !canSkipPrev && offsetXAnimatable.value < 0) ||
                                                (tryingToSwipeLeft && !canSkipNxt && offsetXAnimatable.value > 0)

                                        if (allowLeft || allowRight || canReturnToCenter) {
                                            totalDragDistance += kotlin.math.abs(adjustedDragAmount)
                                            coroutineScope.launch {
                                                offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDragAmount)
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        val dragDuration = System.currentTimeMillis() - dragStartTime
                                        val velocity = if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                                        val currentOffset = offsetXAnimatable.value
                                        val minDistanceThreshold = 50f
                                        val velocityThreshold = (swipeSensitivity * -8.25f) + 8.5f

                                        val shouldChangeSong =
                                            (kotlin.math.abs(currentOffset) > minDistanceThreshold && velocity > velocityThreshold) ||
                                                (kotlin.math.abs(currentOffset) > autoSwipeThreshold)

                                        if (shouldChangeSong) {
                                            val canSkipPrev = playerConnection.player.previousMediaItemIndex != -1
                                            val canSkipNxt = playerConnection.player.nextMediaItemIndex != -1
                                            if (currentOffset > 0 && canSkipPrev) {
                                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                                                playerConnection.seekToPrevious()
                                            } else if (currentOffset <= 0 && canSkipNxt) {
                                                view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
                                                playerConnection.seekToNext()
                                            }
                                        }
                                        coroutineScope.launch {
                                            offsetXAnimatable.animateTo(0f, animationSpec)
                                        }
                                    },
                                )
                            }
                        } else {
                            baseModifier
                        }
                    },
        ) {
            when (miniPlayerBackground) {
                MiniPlayerBackgroundStyle.BLUR -> {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        mediaMetadata?.thumbnailUrl?.let { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .let { if (disableBlur) it else it.blur(60.dp) },
                            )
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.45f)),
                            )
                        }
                    }
                }
                MiniPlayerBackgroundStyle.GRADIENT -> {
                    val colors = if (gradientColors.isNotEmpty()) gradientColors
                    else listOf(
                        MaterialTheme.colorScheme.surfaceContainer,
                        MaterialTheme.colorScheme.surfaceContainer,
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(colors)
                            )
                            .background(Color.Black.copy(alpha = 0.15f)),
                    )
                }
                else -> {}
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                // Album artwork with circular progress ring
                NewMiniPlayerArtwork(
                    progressState = progressState,
                    playbackState = playbackState,
                    isCasting = isCasting,
                    castHandler = castHandler,
                    playerConnection = playerConnection,
                    mediaMetadata = mediaMetadata,
                    primaryColor = primaryColor,
                    outlineColor = outlineColor,
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Song info (title & artist)
                NewMiniPlayerSongInfo(
                    mediaMetadata = mediaMetadata,
                    onSurfaceColor = onSurfaceColor,
                    errorColor = errorColor,
                    modifier = Modifier.weight(1f),
                )

                // Cast indicator
                if (isCasting) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        painter = painterResource(R.drawable.cast_connected),
                        contentDescription = "Casting",
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Transport controls: Previous, Play/Pause, Next
                MiniPlayerTransportControls(
                    isPlaying = isPlaying,
                    playbackState = playbackState,
                    canSkipPrevious = canSkipPrevious,
                    canSkipNext = canSkipNext,
                    isCasting = isCasting,
                    castHandler = castHandler,
                    playerConnection = playerConnection,
                    primaryColor = primaryColor,
                    onSurfaceColor = onSurfaceColor,
                )
            }
        }
    }
}

/**
 * Artwork with circular progress indicator
 */
@Composable
private fun NewMiniPlayerArtwork(
    progressState: ProgressState,
    playbackState: Int,
    isCasting: Boolean,
    castHandler: CastConnectionHandler?,
    playerConnection: PlayerConnection,
    mediaMetadata: MediaMetadata?,
    primaryColor: Color,
    outlineColor: Color,
) {
    val isPlaying by playerConnection.isPlaying.collectAsState()
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying

    val trackColor = outlineColor.copy(alpha = 0.2f)
    val strokeWidth = 2.5.dp

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            Modifier
                .size(46.dp)
                .drawWithContent {
                    drawContent()
                    val progress = progressState.progress
                    val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                    val startAngle = -90f
                    val sweepAngle = 360f * progress
                    val diameter = size.minDimension
                    val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)

                    // Draw track
                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = stroke,
                    )
                    // Draw progress
                    drawArc(
                        color = primaryColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(diameter, diameter),
                        style = stroke,
                    )
                },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .border(1.dp, outlineColor.copy(alpha = 0.3f), CircleShape),
        ) {
            mediaMetadata?.let { metadata ->
                val thumbnailUrl =
                    remember(metadata.thumbnailUrl) {
                        metadata.thumbnailUrl?.resize(120, 120)
                    }
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            }

            val isBuffering = playbackState == Player.STATE_BUFFERING || (playbackState == Player.STATE_IDLE && effectiveIsPlaying)
            if (isBuffering) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape),
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/**
 * Transport controls: Previous, Play/Pause, Next buttons
 */
@Composable
private fun MiniPlayerTransportControls(
    isPlaying: Boolean,
    playbackState: Int,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    isCasting: Boolean,
    castHandler: CastConnectionHandler?,
    playerConnection: PlayerConnection,
    primaryColor: Color,
    onSurfaceColor: Color,
) {
    val view = LocalView.current
    val castIsPlaying by castHandler?.castIsPlaying?.collectAsState() ?: remember { mutableStateOf(false) }
    val effectiveIsPlaying = if (isCasting) castIsPlaying else isPlaying

    val onPrevious = remember(playerConnection, view) {
        {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
            playerConnection.seekToPrevious()
        }
    }
    val onNext = remember(playerConnection, view) {
        {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
            playerConnection.seekToNext()
        }
    }
    val onPlayPause: () -> Unit = remember(playbackState, isCasting, castIsPlaying, playerConnection, castHandler, view) {
        {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONTEXT_CLICK)
            if (isCasting) {
                if (castIsPlaying) castHandler?.pause() else castHandler?.play()
            } else if (playbackState == Player.STATE_ENDED) {
                playerConnection.player.seekTo(0, 0)
                playerConnection.player.playWhenReady = true
            } else {
                playerConnection.togglePlayPause()
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Previous Button
        IconButton(
            onClick = onPrevious,
            enabled = canSkipPrevious,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.skip_previous),
                contentDescription = stringResource(R.string.previous),
                tint = if (canSkipPrevious) onSurfaceColor else onSurfaceColor.copy(alpha = 0.35f),
                modifier = Modifier.size(20.dp),
            )
        }

        // Play / Pause Button
        Box(
            contentAlignment = Alignment.Center,
            modifier =
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(primaryColor)
                    .clickable(onClick = onPlayPause),
        ) {
            val isBuffering = playbackState == Player.STATE_BUFFERING || (playbackState == Player.STATE_IDLE && effectiveIsPlaying)
            if (isBuffering) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Icon(
                    painter =
                        painterResource(
                            when {
                                playbackState == Player.STATE_ENDED -> R.drawable.replay
                                effectiveIsPlaying -> R.drawable.pause
                                else -> R.drawable.play
                            },
                        ),
                    contentDescription = stringResource(
                        if (playbackState == Player.STATE_ENDED || !effectiveIsPlaying) R.string.play else R.string.pause
                    ),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // Next Button
        IconButton(
            onClick = onNext,
            enabled = canSkipNext,
            modifier = Modifier.size(36.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.skip_next),
                contentDescription = stringResource(R.string.next),
                tint = if (canSkipNext) onSurfaceColor else onSurfaceColor.copy(alpha = 0.35f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Song info display - title and artist
 */
@Composable
private fun NewMiniPlayerSongInfo(
    mediaMetadata: MediaMetadata?,
    onSurfaceColor: Color,
    errorColor: Color,
    modifier: Modifier = Modifier,
) {
    val error by LocalPlayerConnection.current?.error?.collectAsState() ?: remember { mutableStateOf(null) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        mediaMetadata?.let { metadata ->
            Text(
                text = metadata.title,
                color = onSurfaceColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (metadata.explicit) MIcon.Explicit()
                if (metadata.artists.any { it.name.isNotBlank() }) {
                    Text(
                        text = metadata.artists.joinToArtistString(" ${stringResource(R.string.and)} ") { it.name },
                        color = onSurfaceColor.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.basicMarquee(iterations = 1, initialDelayMillis = 3000, velocity = 30.dp),
                    )
                }
            }

            AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    text = stringResource(R.string.error_playing),
                    color = errorColor,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
