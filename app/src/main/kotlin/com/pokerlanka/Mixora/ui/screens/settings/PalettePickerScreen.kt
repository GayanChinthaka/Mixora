/**
 * Mixora Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.pokerlanka.mixora.ui.screens.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.pokerlanka.mixora.LocalPlayerAwareWindowInsets
import com.pokerlanka.mixora.R
import com.pokerlanka.mixora.constants.CustomThemeColorKey
import com.pokerlanka.mixora.constants.DynamicThemeKey
import com.pokerlanka.mixora.constants.RandomThemeOnStartupKey
import com.pokerlanka.mixora.constants.SelectedThemeColorKey
import com.pokerlanka.mixora.ui.component.PlayerSliderTrack
import com.pokerlanka.mixora.ui.theme.ThemePalette
import com.pokerlanka.mixora.ui.theme.ThemePalettes
import com.pokerlanka.mixora.ui.theme.ThemeSeedPalette
import com.pokerlanka.mixora.ui.theme.ThemeSeedPaletteCodec
import com.pokerlanka.mixora.ui.theme.toHexString
import com.pokerlanka.mixora.ui.theme.toSeedPalette
import com.pokerlanka.mixora.ui.theme.toThemePalette
import com.pokerlanka.mixora.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SeedRole {
    PRIMARY,
    SECONDARY,
    TERTIARY,
    NEUTRAL,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PalettePickerScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val (customThemeColor, onCustomThemeColorChange) =
        rememberPreference(
            CustomThemeColorKey,
            defaultValue = ThemePalettes.Default.id,
        )

    val (_, onSelectedThemeColorChange) =
        rememberPreference(
            SelectedThemeColorKey,
            defaultValue = ThemePalettes.Default.primary.toArgb(),
        )

    val (dynamicThemeEnabled, onDynamicThemeChange) =
        rememberPreference(
            DynamicThemeKey,
            defaultValue = true,
        )

    val (randomThemeOnStartup, onRandomThemeOnStartupChange) =
        rememberPreference(
            RandomThemeOnStartupKey,
            defaultValue = false,
        )

    val selectedPalette =
        remember(customThemeColor) {
            val custom = ThemeSeedPaletteCodec.decodeFromPreference(customThemeColor)?.toThemePalette()
            custom
                ?: ThemePalettes.findById(customThemeColor)
                ?: ThemePalettes.findByPrimaryColor(customThemeColor)
                ?: ThemePalettes.Default
        }

    val selectedSeedPalette = remember(selectedPalette) { selectedPalette.toSeedPalette() }

    var showCustomThemeDialog by rememberSaveable { mutableStateOf(false) }

    fun selectPalette(palette: ThemePalette) {
        onCustomThemeColorChange(palette.id)
        onSelectedThemeColorChange(palette.primary.toArgb())
    }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val text =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            context.contentResolver
                                .openInputStream(uri)
                                ?.bufferedReader()
                                ?.use { it.readText() }
                                .orEmpty()
                        }.getOrNull().orEmpty()
                    }
                val imported = ThemeSeedPaletteCodec.decodeFromJson(text)
                if (imported != null) {
                    val name = ThemeSeedPaletteCodec.extractNameFromJsonOrNull(text)
                    val encoded = ThemeSeedPaletteCodec.encodeForPreference(imported, name)
                    onCustomThemeColorChange(encoded)
                    onSelectedThemeColorChange(imported.primary.toArgb())
                    Toast.makeText(context, context.getString(R.string.theme_import_success), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.theme_import_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }

    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            scope.launch {
                val name = ThemeSeedPaletteCodec.extractNameFromPreference(customThemeColor)
                val payload = ThemeSeedPaletteCodec.encodeAsJson(selectedSeedPalette, name)
                val success =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            context.contentResolver.openOutputStream(uri)?.use { out ->
                                out.write(payload.toByteArray(Charsets.UTF_8))
                                out.flush()
                            } ?: error("Output stream failed")
                        }.isSuccess
                    }
                if (success) {
                    Toast.makeText(context, context.getString(R.string.theme_export_success), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.theme_export_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.color_palette)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val random = ThemePalettes.getRandomPalette()
                            selectPalette(random)
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.shuffle),
                            contentDescription = stringResource(R.string.shuffle_palette),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier =
                    Modifier.windowInsetsPadding(
                        LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                    ),
            ) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(R.string.custom_theme)) },
                    icon = { Icon(painter = painterResource(R.drawable.format_paint), contentDescription = null) },
                    onClick = { showCustomThemeDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Dynamic Mini Player Preview Card
            LiveMiniPlayerPreviewCard(
                palette = selectedSeedPalette,
                paletteName =
                    if (selectedPalette.id.startsWith("custom") || selectedPalette.id.startsWith("random_") || customThemeColor.startsWith("seedPalette:")) {
                        ThemeSeedPaletteCodec.extractNameFromPreference(customThemeColor) ?: stringResource(R.string.palette_custom)
                    } else {
                        stringResource(selectedPalette.nameResId)
                    },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Action options (Random on startup & Export/Import)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.random_theme_on_startup),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = stringResource(R.string.random_theme_on_startup_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Switch(
                            checked = randomThemeOnStartup,
                            onCheckedChange = onRandomThemeOnStartupChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(if (randomThemeOnStartup) R.drawable.check else R.drawable.close),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.restore),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.import_theme), maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = { exportLauncher.launch("Mixora-Theme.json") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.download),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.export_theme), maxLines = 1)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.color_palette),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp),
                )

                Text(
                    text = "${ThemePalettes.allPalettes.size} Presets",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Palettes Grid
            val palettes = ThemePalettes.allPalettes
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(580.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
            ) {
                items(palettes, key = { it.id }) { item ->
                    val isSelected = (selectedPalette.id == item.id)
                    PaletteItemCard(
                        palette = item,
                        isSelected = isSelected,
                        onClick = { selectPalette(item) },
                    )
                }
            }
        }
    }

    if (showCustomThemeDialog) {
        CustomThemeCreatorDialog(
            initialSeed = selectedSeedPalette,
            initialName = ThemeSeedPaletteCodec.extractNameFromPreference(customThemeColor) ?: "",
            onDismiss = { showCustomThemeDialog = false },
            onApply = { newSeed, newName ->
                onDynamicThemeChange(false)
                val encoded = ThemeSeedPaletteCodec.encodeForPreference(newSeed, newName.takeIf { it.isNotBlank() })
                onCustomThemeColorChange(encoded)
                onSelectedThemeColorChange(newSeed.primary.toArgb())
                showCustomThemeDialog = false
                Toast.makeText(context, context.getString(R.string.theme_applied), Toast.LENGTH_SHORT).show()
            },
        )
    }
}

@Composable
private fun LiveMiniPlayerPreviewCard(
    palette: ThemeSeedPalette,
    paletteName: String,
    modifier: Modifier = Modifier,
) {
    var previewDarkMode by rememberSaveable { mutableStateOf(true) }

    val previewColorScheme =
        remember(palette, previewDarkMode) {
            dynamicColorScheme(
                seedColor = palette.primary,
                isDark = previewDarkMode,
                style = PaletteStyle.TonalSpot,
            )
        }

    val animatedBg by animateColorAsState(previewColorScheme.surfaceContainer, tween(300), label = "bg")
    val animatedPrimary by animateColorAsState(previewColorScheme.primary, tween(300), label = "prim")
    val animatedOnPrimary by animateColorAsState(previewColorScheme.onPrimary, tween(300), label = "onPrim")
    val animatedSecondaryContainer by animateColorAsState(previewColorScheme.secondaryContainer, tween(300), label = "secCon")
    val animatedOnSurface by animateColorAsState(previewColorScheme.onSurface, tween(300), label = "onSurf")
    val animatedOnSurfaceVariant by animateColorAsState(previewColorScheme.onSurfaceVariant, tween(300), label = "onSurfVar")

    Card(
        modifier =
            modifier
                .clip(RoundedCornerShape(24.dp))
                .shadow(6.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = animatedBg),
        border = BorderStroke(1.5.dp, animatedPrimary.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
        ) {
            // Header Row: Active Palette Name + Dark/Light toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(animatedPrimary),
                    )
                    Text(
                        text = paletteName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = animatedOnSurface,
                    )
                }

                Surface(
                    onClick = { previewDarkMode = !previewDarkMode },
                    shape = RoundedCornerShape(12.dp),
                    color = animatedSecondaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            painter = painterResource(if (previewDarkMode) R.drawable.dark_mode else R.drawable.auto_awesome),
                            contentDescription = null,
                            tint = animatedOnSurface,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = if (previewDarkMode) "Dark" else "Light",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = animatedOnSurface,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mini Player Representation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Album Art Card
                Box(
                    modifier =
                        Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(palette.primary, palette.secondary, palette.tertiary),
                                ),
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.music_note),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Title & Artist
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.preview_song_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = animatedOnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = stringResource(R.string.preview_artist_name),
                        style = MaterialTheme.typography.bodySmall,
                        color = animatedOnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Favorite Icon Button
                Box(
                    modifier =
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(animatedSecondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.favorite),
                        contentDescription = null,
                        tint = animatedPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Slider
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(animatedOnSurface.copy(alpha = 0.15f)),
            ) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth(0.55f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(animatedPrimary),
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("1:48", style = MaterialTheme.typography.labelSmall, color = animatedOnSurfaceVariant)
                Text("3:24", style = MaterialTheme.typography.labelSmall, color = animatedOnSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Controls Bar & Color Swatches
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Playback Control Buttons
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(animatedSecondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_previous),
                            contentDescription = null,
                            tint = animatedOnSurface,
                            modifier = Modifier.size(16.dp),
                        )
                    }

                    Box(
                        modifier =
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(animatedPrimary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            tint = animatedOnPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }

                    Box(
                        modifier =
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(animatedSecondaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.skip_next),
                            contentDescription = null,
                            tint = animatedOnSurface,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                // Swatch Dots
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SeedDot(palette.primary, "P")
                    SeedDot(palette.secondary, "S")
                    SeedDot(palette.tertiary, "T")
                    SeedDot(palette.neutral, "N")
                }
            }
        }
    }
}

@Composable
private fun SeedDot(
    color: Color,
    label: String,
) {
    Box(
        modifier =
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (color.luminance() > 0.5f) Color.Black else Color.White,
        )
    }
}

@Composable
private fun PaletteItemCard(
    palette: ThemePalette,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(if (isSelected) 1.02f else 1f, spring(), label = "scale")
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent

    Card(
        modifier =
            modifier
                .scale(scale)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
            ),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) borderColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Multi-tone circular color preview
            Box(
                modifier =
                    Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(palette.primary)
                        .border(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        painter = painterResource(R.drawable.check),
                        contentDescription = null,
                        tint = palette.onPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(palette.nameResId),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Mini tone dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(palette.primary))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(palette.secondary))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(palette.tertiary))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(palette.neutral))
            }
        }
    }
}

@Composable
private fun CustomThemeCreatorDialog(
    initialSeed: ThemeSeedPalette,
    initialName: String,
    onDismiss: () -> Unit,
    onApply: (ThemeSeedPalette, String) -> Unit,
) {
    var themeName by rememberSaveable { mutableStateOf(initialName) }
    var activeRole by rememberSaveable { mutableStateOf(SeedRole.PRIMARY) }

    var primary by rememberSaveable { mutableStateOf(initialSeed.primary.toHexString()) }
    var secondary by rememberSaveable { mutableStateOf(initialSeed.secondary.toHexString()) }
    var tertiary by rememberSaveable { mutableStateOf(initialSeed.tertiary.toHexString()) }
    var neutral by rememberSaveable { mutableStateOf(initialSeed.neutral.toHexString()) }

    val parseHex: (String, Color) -> Color = { hex, fallback ->
        runCatching {
            val norm = if (hex.startsWith("#")) hex else "#$hex"
            Color(android.graphics.Color.parseColor(norm))
        }.getOrDefault(fallback)
    }

    val currentPrimary = parseHex(primary, initialSeed.primary)
    val currentSecondary = parseHex(secondary, initialSeed.secondary)
    val currentTertiary = parseHex(tertiary, initialSeed.tertiary)
    val currentNeutral = parseHex(neutral, initialSeed.neutral)

    val currentSeed =
        ThemeSeedPalette(
            primary = currentPrimary,
            secondary = currentSecondary,
            tertiary = currentTertiary,
            neutral = currentNeutral,
        )

    val activeColorHex =
        when (activeRole) {
            SeedRole.PRIMARY -> primary
            SeedRole.SECONDARY -> secondary
            SeedRole.TERTIARY -> tertiary
            SeedRole.NEUTRAL -> neutral
        }

    val onActiveColorChange: (String) -> Unit = { newHex ->
        when (activeRole) {
            SeedRole.PRIMARY -> primary = newHex
            SeedRole.SECONDARY -> secondary = newHex
            SeedRole.TERTIARY -> tertiary = newHex
            SeedRole.NEUTRAL -> neutral = newHex
        }
    }

    val presetColors =
        listOf(
            Color(0xFFED5564), Color(0xFF4A90D9), Color(0xFF1DB954), Color(0xFFE67E22),
            Color(0xFF7851A9), Color(0xFFFF1493), Color(0xFF00FFCC), Color(0xFFF39C12),
            Color(0xFF2C3E50), Color(0xFFE0115F), Color(0xFF00BFFF), Color(0xFF98FF98),
        )

    val focusManager = LocalFocusManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.theme_creator),
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
            ) {
                OutlinedTextField(
                    value = themeName,
                    onValueChange = { themeName = it },
                    label = { Text(stringResource(R.string.theme_name_optional)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Role selector tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SeedRoleTab(
                        label = stringResource(R.string.primary_seed),
                        color = currentPrimary,
                        isSelected = activeRole == SeedRole.PRIMARY,
                        onClick = { activeRole = SeedRole.PRIMARY },
                        modifier = Modifier.weight(1f),
                    )
                    SeedRoleTab(
                        label = stringResource(R.string.secondary_seed),
                        color = currentSecondary,
                        isSelected = activeRole == SeedRole.SECONDARY,
                        onClick = { activeRole = SeedRole.SECONDARY },
                        modifier = Modifier.weight(1f),
                    )
                    SeedRoleTab(
                        label = stringResource(R.string.tertiary_seed),
                        color = currentTertiary,
                        isSelected = activeRole == SeedRole.TERTIARY,
                        onClick = { activeRole = SeedRole.TERTIARY },
                        modifier = Modifier.weight(1f),
                    )
                    SeedRoleTab(
                        label = stringResource(R.string.neutral_seed),
                        color = currentNeutral,
                        isSelected = activeRole == SeedRole.NEUTRAL,
                        onClick = { activeRole = SeedRole.NEUTRAL },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // HEX input
                OutlinedTextField(
                    value = activeColorHex,
                    onValueChange = onActiveColorChange,
                    label = { Text(stringResource(R.string.hex_color)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Box(
                            modifier =
                                Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(parseHex(activeColorHex, Color.Gray))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Preset Swatches for Quick Picking
                Text(
                    text = "Quick Presets",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    presetColors.take(6).forEach { color ->
                        Box(
                            modifier =
                                Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { onActiveColorChange(color.toHexString()) },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    presetColors.drop(6).take(6).forEach { color ->
                        Box(
                            modifier =
                                Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { onActiveColorChange(color.toHexString()) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(currentSeed, themeName) },
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.apply_theme))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun SeedRoleTab(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(color),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
            )
        }
    }
}
