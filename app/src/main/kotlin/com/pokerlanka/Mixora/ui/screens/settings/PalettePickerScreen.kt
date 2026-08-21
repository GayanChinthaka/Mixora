/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
import com.pokerlanka.mixora.constants.DarkModeKey
import com.pokerlanka.mixora.constants.DynamicThemeKey
import com.pokerlanka.mixora.constants.SelectedThemeColorKey
import com.pokerlanka.mixora.ui.theme.ThemePalette
import com.pokerlanka.mixora.ui.theme.ThemePalettes
import com.pokerlanka.mixora.ui.theme.ThemeSeedPalette
import com.pokerlanka.mixora.ui.theme.toSeedPalette
import com.pokerlanka.mixora.utils.rememberEnumPreference
import com.pokerlanka.mixora.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PalettePickerScreen(
    navController: NavController,
) {
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

    val (darkMode) =
        rememberEnumPreference(
            DarkModeKey,
            defaultValue = DarkMode.AUTO,
        )
    val isSystemDark = isSystemInDarkTheme()
    val isAppDarkTheme =
        remember(darkMode, isSystemDark) {
            if (darkMode == DarkMode.AUTO) isSystemDark else darkMode == DarkMode.ON
        }

    val selectedPalette =
        remember(customThemeColor) {
            ThemePalettes.findById(customThemeColor)
                ?: ThemePalettes.findByPrimaryColor(customThemeColor)
                ?: ThemePalettes.Default
        }

    val selectedSeedPalette = remember(selectedPalette) { selectedPalette.toSeedPalette() }

    // Dynamic color values when Dynamic Theme is enabled
    val currentThemePrimary = MaterialTheme.colorScheme.primary
    val currentThemeSecondary = MaterialTheme.colorScheme.secondary
    val currentThemeTertiary = MaterialTheme.colorScheme.tertiary
    val currentThemeNeutral = MaterialTheme.colorScheme.surfaceTint

    val effectiveSeedPalette =
        remember(dynamicThemeEnabled, selectedSeedPalette, currentThemePrimary, currentThemeSecondary, currentThemeTertiary, currentThemeNeutral) {
            if (dynamicThemeEnabled) {
                ThemeSeedPalette(
                    primary = currentThemePrimary,
                    secondary = currentThemeSecondary,
                    tertiary = currentThemeTertiary,
                    neutral = currentThemeNeutral,
                )
            } else {
                selectedSeedPalette
            }
        }

    val effectivePaletteName =
        if (dynamicThemeEnabled) {
            stringResource(R.string.palette_dynamic)
        } else {
            stringResource(selectedPalette.nameResId)
        }

    fun selectPalette(palette: ThemePalette) {
        if (dynamicThemeEnabled) return
        onCustomThemeColorChange(palette.id)
        onSelectedThemeColorChange(palette.primary.toArgb())
    }

    val playerAwareInsets = LocalPlayerAwareWindowInsets.current

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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { innerPadding ->
        val palettes = ThemePalettes.allPalettes

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
            contentPadding =
                PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = playerAwareInsets.asPaddingValues().calculateBottomPadding() + 24.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header Item 1: Live Mini Player Preview Card
            item(span = { GridItemSpan(maxLineSpan) }) {
                LiveMiniPlayerPreviewCard(
                    palette = effectiveSeedPalette,
                    paletteName = effectivePaletteName,
                    isAppDarkTheme = isAppDarkTheme,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Header Item 2: Dynamic Theme Toggle Card
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onDynamicThemeChange(!dynamicThemeEnabled) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    border = if (dynamicThemeEnabled) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
                ) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.palette),
                                    contentDescription = null,
                                    tint = if (dynamicThemeEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.enable_dynamic_theme),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = stringResource(R.string.dynamic_theme_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        Switch(
                            checked = dynamicThemeEnabled,
                            onCheckedChange = onDynamicThemeChange,
                            thumbContent = {
                                Icon(
                                    painter = painterResource(if (dynamicThemeEnabled) R.drawable.check else R.drawable.close),
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            },
                        )
                    }
                }
            }

            // Header Item 3: Title Row ("Color Palette", Presets Count)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.color_palette),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (dynamicThemeEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp),
                    )

                    Text(
                        text = "${palettes.size} Presets",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Grid Items: 60+ Curated Palettes
            items(palettes, key = { it.id }) { item ->
                val isSelected = (!dynamicThemeEnabled && selectedPalette.id == item.id)
                PaletteItemCard(
                    palette = item,
                    isSelected = isSelected,
                    enabled = !dynamicThemeEnabled,
                    modifier = Modifier.alpha(if (dynamicThemeEnabled) 0.45f else 1f),
                    onClick = { selectPalette(item) },
                )
            }
        }
    }
}

@Composable
private fun LiveMiniPlayerPreviewCard(
    palette: ThemeSeedPalette,
    paletteName: String,
    isAppDarkTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    var previewDarkMode by rememberSaveable { mutableStateOf(isAppDarkTheme) }

    LaunchedEffect(isAppDarkTheme) {
        previewDarkMode = isAppDarkTheme
    }

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
    enabled: Boolean,
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
                .clickable(enabled = enabled, onClick = onClick),
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
