/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

@file:Suppress("DEPRECATION")
package com.pokerlanka.mixora.ui.theme

import android.graphics.Bitmap
import android.os.Build
import android.util.Base64
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.ktx.toHct
import com.materialkolor.score.Score
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

val DefaultThemeColor = Color(0xFFED5564)

data class ThemeSeedPalette(
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val neutral: Color,
)

@Composable
fun MixoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    seedPalette: ThemeSeedPalette? = null,
    disableAnimations: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val useSystemDynamicColor = (seedPalette == null && themeColor == DefaultThemeColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)

    val appColorScheme =
        remember(seedPalette, themeColor, darkTheme) {
            if (seedPalette != null) {
                exactPaletteColorScheme(
                    palette = seedPalette,
                    isDark = darkTheme,
                )
            } else {
                materialKolorDynamicColorScheme(
                    keyColor = themeColor,
                    isDark = darkTheme,
                    style = paletteStyleFor(themeColor),
                )
            }
        }

    val baseColorScheme =
        if (useSystemDynamicColor) {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        } else {
            appColorScheme
        }

    val colorScheme =
        remember(baseColorScheme, pureBlack, darkTheme) {
            if (darkTheme && pureBlack) {
                baseColorScheme.pureBlack(true)
            } else {
                baseColorScheme
            }
        }

    val animatedColorScheme =
        if (disableAnimations) {
            colorScheme
        } else {
            animateColorScheme(
                targetColorScheme = colorScheme,
                animationSpec = tween(durationMillis = 300),
            )
        }

    MaterialTheme(
        colorScheme = animatedColorScheme,
        typography = AppTypography,
        content = content,
    )
}

@Composable
private fun animateColorScheme(
    targetColorScheme: ColorScheme,
    animationSpec: FiniteAnimationSpec<Color>,
): ColorScheme =
    ColorScheme(
        primary = animateColorAsState(targetColorScheme.primary, animationSpec, label = "primary").value,
        onPrimary = animateColorAsState(targetColorScheme.onPrimary, animationSpec, label = "onPrimary").value,
        primaryContainer = animateColorAsState(targetColorScheme.primaryContainer, animationSpec, label = "primaryContainer").value,
        onPrimaryContainer = animateColorAsState(targetColorScheme.onPrimaryContainer, animationSpec, label = "onPrimaryContainer").value,
        inversePrimary = animateColorAsState(targetColorScheme.inversePrimary, animationSpec, label = "inversePrimary").value,
        secondary = animateColorAsState(targetColorScheme.secondary, animationSpec, label = "secondary").value,
        onSecondary = animateColorAsState(targetColorScheme.onSecondary, animationSpec, label = "onSecondary").value,
        secondaryContainer = animateColorAsState(targetColorScheme.secondaryContainer, animationSpec, label = "secondaryContainer").value,
        onSecondaryContainer = animateColorAsState(targetColorScheme.onSecondaryContainer, animationSpec, label = "onSecondaryContainer").value,
        tertiary = animateColorAsState(targetColorScheme.tertiary, animationSpec, label = "tertiary").value,
        onTertiary = animateColorAsState(targetColorScheme.onTertiary, animationSpec, label = "onTertiary").value,
        tertiaryContainer = animateColorAsState(targetColorScheme.tertiaryContainer, animationSpec, label = "tertiaryContainer").value,
        onTertiaryContainer = animateColorAsState(targetColorScheme.onTertiaryContainer, animationSpec, label = "onTertiaryContainer").value,
        background = animateColorAsState(targetColorScheme.background, animationSpec, label = "background").value,
        onBackground = animateColorAsState(targetColorScheme.onBackground, animationSpec, label = "onBackground").value,
        surface = animateColorAsState(targetColorScheme.surface, animationSpec, label = "surface").value,
        onSurface = animateColorAsState(targetColorScheme.onSurface, animationSpec, label = "onSurface").value,
        surfaceVariant = animateColorAsState(targetColorScheme.surfaceVariant, animationSpec, label = "surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(targetColorScheme.onSurfaceVariant, animationSpec, label = "onSurfaceVariant").value,
        surfaceTint = animateColorAsState(targetColorScheme.surfaceTint, animationSpec, label = "surfaceTint").value,
        inverseSurface = animateColorAsState(targetColorScheme.inverseSurface, animationSpec, label = "inverseSurface").value,
        inverseOnSurface = animateColorAsState(targetColorScheme.inverseOnSurface, animationSpec, label = "inverseOnSurface").value,
        error = animateColorAsState(targetColorScheme.error, animationSpec, label = "error").value,
        onError = animateColorAsState(targetColorScheme.onError, animationSpec, label = "onError").value,
        errorContainer = animateColorAsState(targetColorScheme.errorContainer, animationSpec, label = "errorContainer").value,
        onErrorContainer = animateColorAsState(targetColorScheme.onErrorContainer, animationSpec, label = "onErrorContainer").value,
        outline = animateColorAsState(targetColorScheme.outline, animationSpec, label = "outline").value,
        outlineVariant = animateColorAsState(targetColorScheme.outlineVariant, animationSpec, label = "outlineVariant").value,
        scrim = animateColorAsState(targetColorScheme.scrim, animationSpec, label = "scrim").value,
        surfaceBright = animateColorAsState(targetColorScheme.surfaceBright, animationSpec, label = "surfaceBright").value,
        surfaceDim = animateColorAsState(targetColorScheme.surfaceDim, animationSpec, label = "surfaceDim").value,
        surfaceContainer = animateColorAsState(targetColorScheme.surfaceContainer, animationSpec, label = "surfaceContainer").value,
        surfaceContainerLow = animateColorAsState(targetColorScheme.surfaceContainerLow, animationSpec, label = "surfaceContainerLow").value,
        surfaceContainerLowest = animateColorAsState(targetColorScheme.surfaceContainerLowest, animationSpec, label = "surfaceContainerLowest").value,
        surfaceContainerHigh = animateColorAsState(targetColorScheme.surfaceContainerHigh, animationSpec, label = "surfaceContainerHigh").value,
        surfaceContainerHighest = animateColorAsState(targetColorScheme.surfaceContainerHighest, animationSpec, label = "surfaceContainerHighest").value,
    )

private fun exactPaletteColorScheme(
    palette: ThemeSeedPalette,
    isDark: Boolean,
): ColorScheme =
    mergedSeedColorScheme(
        primarySeed = palette.primary,
        secondarySeed = palette.secondary,
        tertiarySeed = palette.tertiary,
        neutralSeed = palette.neutral,
        isDark = isDark,
    )

private fun materialKolorDynamicColorScheme(
    keyColor: Color,
    isDark: Boolean,
    contrastLevel: Double = 0.0,
    style: PaletteStyle,
): ColorScheme =
    mergedSeedColorScheme(
        primarySeed = keyColor,
        secondarySeed = keyColor,
        tertiarySeed = keyColor,
        neutralSeed = keyColor,
        isDark = isDark,
        contrastLevel = contrastLevel,
        style = style,
    )

private fun mergedSeedColorScheme(
    primarySeed: Color,
    secondarySeed: Color,
    tertiarySeed: Color,
    neutralSeed: Color,
    isDark: Boolean,
    contrastLevel: Double = 0.0,
    style: PaletteStyle = paletteStyleFor(primarySeed),
): ColorScheme {
    val primaryScheme = materialKolorScheme(primarySeed, isDark, contrastLevel, style)
    val secondaryScheme = materialKolorScheme(secondarySeed, isDark, contrastLevel, paletteStyleFor(secondarySeed))
    val tertiaryScheme = materialKolorScheme(tertiarySeed, isDark, contrastLevel, paletteStyleFor(tertiarySeed))
    val neutralScheme = materialKolorScheme(neutralSeed, isDark, contrastLevel, paletteStyleFor(neutralSeed))

    return ColorScheme(
        primary = primaryScheme.primary,
        onPrimary = primaryScheme.onPrimary,
        primaryContainer = primaryScheme.primaryContainer,
        onPrimaryContainer = primaryScheme.onPrimaryContainer,
        inversePrimary = primaryScheme.inversePrimary,
        secondary = secondaryScheme.primary,
        onSecondary = secondaryScheme.onPrimary,
        secondaryContainer = secondaryScheme.primaryContainer,
        onSecondaryContainer = secondaryScheme.onPrimaryContainer,
        tertiary = tertiaryScheme.primary,
        onTertiary = tertiaryScheme.onPrimary,
        tertiaryContainer = tertiaryScheme.primaryContainer,
        onTertiaryContainer = tertiaryScheme.onPrimaryContainer,
        background = neutralScheme.background,
        onBackground = neutralScheme.onBackground,
        surface = neutralScheme.surface,
        onSurface = neutralScheme.onSurface,
        surfaceVariant = neutralScheme.surfaceVariant,
        onSurfaceVariant = neutralScheme.onSurfaceVariant,
        inverseSurface = neutralScheme.inverseSurface,
        inverseOnSurface = neutralScheme.inverseOnSurface,
        surfaceBright = neutralScheme.surfaceBright,
        surfaceDim = neutralScheme.surfaceDim,
        surfaceContainer = neutralScheme.surfaceContainer,
        surfaceContainerLow = neutralScheme.surfaceContainerLow,
        surfaceContainerLowest = neutralScheme.surfaceContainerLowest,
        surfaceContainerHigh = neutralScheme.surfaceContainerHigh,
        surfaceContainerHighest = neutralScheme.surfaceContainerHighest,
        outline = neutralScheme.outline,
        outlineVariant = neutralScheme.outlineVariant,
        error = primaryScheme.error,
        onError = primaryScheme.onError,
        errorContainer = primaryScheme.errorContainer,
        onErrorContainer = primaryScheme.onErrorContainer,
        scrim = neutralScheme.scrim,
        surfaceTint = primaryScheme.surfaceTint,
    )
}

private fun materialKolorScheme(
    seedColor: Color,
    isDark: Boolean,
    contrastLevel: Double,
    style: PaletteStyle,
): ColorScheme =
    dynamicColorScheme(
        seedColor = seedColor,
        isDark = isDark,
        contrastLevel = contrastLevel,
        style = style,
    )

private fun paletteStyleFor(seedColor: Color): PaletteStyle {
    val chroma = seedColor.toHct().chroma
    return when {
        chroma < 4.0 -> PaletteStyle.Monochrome
        chroma < 12.0 -> PaletteStyle.Neutral
        else -> PaletteStyle.TonalSpot
    }
}

fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation = Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, 0xff4285f4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}

@Serializable
data class ThemeExportV1(
    val version: Int = 1,
    val name: String? = null,
    val primary: String,
    val secondary: String,
    val tertiary: String,
    val neutral: String,
)

object ThemeSeedPaletteCodec {
    private const val PreferencePrefix = "seedPalette:"
    private val json =
        Json {
            prettyPrint = true
            ignoreUnknownKeys = true
            encodeDefaults = true
            isLenient = true
        }

    fun encodeForPreference(
        palette: ThemeSeedPalette,
        name: String? = null,
    ): String {
        val payload =
            json.encodeToString(
                ThemeExportV1(
                    name = name,
                    primary = palette.primary.toHexArgbString(),
                    secondary = palette.secondary.toHexArgbString(),
                    tertiary = palette.tertiary.toHexArgbString(),
                    neutral = palette.neutral.toHexArgbString(),
                ),
            )
        val b64 = Base64.encodeToString(payload.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
        return PreferencePrefix + b64
    }

    fun decodeFromPreference(value: String): ThemeSeedPalette? {
        if (!value.startsWith(PreferencePrefix)) return null
        val b64 = value.removePrefix(PreferencePrefix)
        val decoded =
            runCatching {
                val bytes = Base64.decode(b64, Base64.URL_SAFE or Base64.NO_WRAP)
                bytes.toString(Charsets.UTF_8)
            }.getOrNull()
                ?: return null
        return decodeFromJson(decoded)
    }

    fun encodeAsJson(
        palette: ThemeSeedPalette,
        name: String? = null,
    ): String =
        json.encodeToString(
            ThemeExportV1(
                name = name,
                primary = palette.primary.toHexArgbString(),
                secondary = palette.secondary.toHexArgbString(),
                tertiary = palette.tertiary.toHexArgbString(),
                neutral = palette.neutral.toHexArgbString(),
            ),
        )

    fun decodeFromJson(text: String): ThemeSeedPalette? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        return runCatching {
            val element = json.parseToJsonElement(trimmed)
            val obj = element.jsonObject

            val version = obj["version"]?.jsonPrimitive?.content?.toIntOrNull() ?: 1
            if (version != 1) return@runCatching null

            fun getColor(key: String): Color? =
                obj[key]
                    ?.jsonPrimitive
                    ?.content
                    ?.toColorOrNull()

            val primary = getColor("primary") ?: return@runCatching null
            val secondary = getColor("secondary") ?: primary
            val tertiary = getColor("tertiary") ?: primary
            val neutral = getColor("neutral") ?: primary

            ThemeSeedPalette(
                primary = primary,
                secondary = secondary,
                tertiary = tertiary,
                neutral = neutral,
            )
        }.getOrNull()
            ?: decodeFromLegacyObject(trimmed)
    }

    fun extractNameFromJsonOrNull(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        return runCatching {
            val element: JsonElement = json.parseToJsonElement(trimmed)
            element.jsonObject["name"]
                ?.jsonPrimitive
                ?.content
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    fun extractNameFromPreference(value: String): String? {
        if (!value.startsWith(PreferencePrefix)) return null
        val b64 = value.removePrefix(PreferencePrefix)
        val decoded =
            runCatching {
                val bytes = Base64.decode(b64, Base64.URL_SAFE or Base64.NO_WRAP)
                bytes.toString(Charsets.UTF_8)
            }.getOrNull()
                ?: return null
        return extractNameFromJsonOrNull(decoded)
    }

    private fun decodeFromLegacyObject(text: String): ThemeSeedPalette? {
        val trimmed = text.trim()
        if (!trimmed.startsWith("{")) return null
        return runCatching {
            val element = json.parseToJsonElement(trimmed)
            val obj = element.jsonObject

            fun getHex(key: String): String? =
                obj[key]
                    ?.jsonPrimitive
                    ?.content
                    ?.takeIf { it.isNotBlank() }

            val primary = getHex("primary")?.toColorOrNull() ?: return@runCatching null
            val secondary = getHex("secondary")?.toColorOrNull() ?: primary
            val tertiary = getHex("tertiary")?.toColorOrNull() ?: primary
            val neutral = getHex("neutral")?.toColorOrNull() ?: primary

            ThemeSeedPalette(primary, secondary, tertiary, neutral)
        }.getOrNull()
    }

    private fun Color.toHexArgbString(): String = String.format("#%08X", this.toArgb())

    private fun String.toColorOrNull(): Color? {
        val normalized = trim()
        if (normalized.isEmpty()) return null
        return runCatching {
            val withHash = if (normalized.startsWith("#")) normalized else "#$normalized"
            Color(android.graphics.Color.parseColor(withHash))
        }.getOrNull()
    }
}
