/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

package com.pokerlanka.mixora.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.pokerlanka.mixora.R
import com.pokerlanka.mixora.constants.DarkModeKey
import com.pokerlanka.mixora.constants.DisableAnimationsKey
import com.pokerlanka.mixora.constants.DisableBlurKey
import com.pokerlanka.mixora.constants.PureBlackKey
import com.pokerlanka.mixora.ui.component.Material3SettingsGroup
import com.pokerlanka.mixora.ui.component.Material3SettingsItem
import com.pokerlanka.mixora.utils.rememberEnumPreference
import com.pokerlanka.mixora.utils.rememberPreference

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeScreen(
    navController: NavController,
) {
    val (darkMode, onDarkModeChange) =
        rememberEnumPreference(
            DarkModeKey,
            defaultValue = DarkMode.AUTO,
        )

    val isSystemDark = isSystemInDarkTheme()
    val isDarkThemeActive =
        remember(darkMode, isSystemDark) {
            if (darkMode == DarkMode.AUTO) isSystemDark else darkMode == DarkMode.ON
        }

    val (pureBlack, onPureBlackChange) =
        rememberPreference(
            PureBlackKey,
            defaultValue = false,
        )

    val (disableBlur, onDisableBlurChange) =
        rememberPreference(
            DisableBlurKey,
            defaultValue = false,
        )

    val (disableAnimations, onDisableAnimationsChange) =
        rememberPreference(
            DisableAnimationsKey,
            defaultValue = false,
        )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.theme)) },
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
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.theme_mode),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 12.dp),
            )

            // Theme Mode Selector Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ThemeModeCard(
                    title = stringResource(R.string.theme_system_default),
                    icon = painterResource(R.drawable.time_auto),
                    isSelected = darkMode == DarkMode.AUTO,
                    onClick = { onDarkModeChange(DarkMode.AUTO) },
                    modifier = Modifier.weight(1f),
                )

                ThemeModeCard(
                    title = stringResource(R.string.theme_dark),
                    icon = painterResource(R.drawable.dark_mode),
                    isSelected = darkMode == DarkMode.ON,
                    onClick = { onDarkModeChange(DarkMode.ON) },
                    modifier = Modifier.weight(1f),
                )

                ThemeModeCard(
                    title = stringResource(R.string.theme_light),
                    icon = painterResource(R.drawable.auto_awesome),
                    isSelected = darkMode == DarkMode.OFF,
                    onClick = { onDarkModeChange(DarkMode.OFF) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pure Black option if Dark Mode active
            if (isDarkThemeActive) {
                Material3SettingsGroup(
                    title = stringResource(R.string.dark_mode),
                    items =
                        listOf(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.contrast),
                                title = { Text(stringResource(R.string.pure_black)) },
                                description = { Text(stringResource(R.string.pure_black_desc)) },
                                trailingContent = {
                                    Switch(
                                        checked = pureBlack,
                                        onCheckedChange = onPureBlackChange,
                                        thumbContent = {
                                            Icon(
                                                painter = painterResource(if (pureBlack) R.drawable.check else R.drawable.close),
                                                contentDescription = null,
                                                modifier = Modifier.size(SwitchDefaults.IconSize),
                                            )
                                        },
                                    )
                                },
                                onClick = { onPureBlackChange(!pureBlack) },
                            ),
                        ),
                )

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Blur & Animation Group
            Material3SettingsGroup(
                title = stringResource(R.string.blur_and_animation),
                items =
                    listOf(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.blur_off),
                            title = { Text(stringResource(R.string.disable_blur)) },
                            description = { Text(stringResource(R.string.disable_blur_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = disableBlur,
                                    onCheckedChange = onDisableBlurChange,
                                    thumbContent = {
                                        Icon(
                                            painter = painterResource(if (disableBlur) R.drawable.check else R.drawable.close),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = { onDisableBlurChange(!disableBlur) },
                        ),
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.animation),
                            title = { Text(stringResource(R.string.disable_animations)) },
                            description = { Text(stringResource(R.string.disable_animations_desc)) },
                            trailingContent = {
                                Switch(
                                    checked = disableAnimations,
                                    onCheckedChange = onDisableAnimationsChange,
                                    thumbContent = {
                                        Icon(
                                            painter = painterResource(if (disableAnimations) R.drawable.check else R.drawable.close),
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize),
                                        )
                                    },
                                )
                            },
                            onClick = { onDisableAnimationsChange(!disableAnimations) },
                        ),
                    ),
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ThemeModeCard(
    title: String,
    icon: Painter,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        }

    val contentColor =
        if (isSelected) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        }

    val border =
        if (isSelected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }

    Card(
        modifier =
            modifier
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = border,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                painter = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else contentColor,
                modifier = Modifier.size(28.dp),
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
            )
        }
    }
}
