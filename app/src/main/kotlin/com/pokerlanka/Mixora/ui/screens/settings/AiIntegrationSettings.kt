/**
 * Mixora Project (C) 2026
 * Author : Gayan Chinthaka
 * Company: Pokerlanka
 */

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.pokerlanka.mixora.ui.screens.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.pokerlanka.mixora.LocalPlayerAwareWindowInsets
import com.pokerlanka.mixora.R
import com.pokerlanka.mixora.ai.AiModelCategory
import com.pokerlanka.mixora.ai.AiModelOption
import com.pokerlanka.mixora.constants.AiApiKeyKey
import com.pokerlanka.mixora.constants.AiApiValidationStatus
import com.pokerlanka.mixora.constants.AiApiValidationStatusKey
import com.pokerlanka.mixora.constants.AiCustomEndpointKey
import com.pokerlanka.mixora.constants.AiCustomModelKey
import com.pokerlanka.mixora.constants.AiProvider
import com.pokerlanka.mixora.constants.AiProviderKey
import com.pokerlanka.mixora.constants.AiSelectedModelKey
import com.pokerlanka.mixora.ui.component.DefaultDialog
import com.pokerlanka.mixora.ui.component.IconButton
import com.pokerlanka.mixora.ui.component.Material3SettingsGroup
import com.pokerlanka.mixora.ui.component.Material3SettingsItem
import com.pokerlanka.mixora.ui.utils.backToMain
import com.pokerlanka.mixora.utils.rememberEnumPreference
import com.pokerlanka.mixora.utils.rememberPreference
import com.pokerlanka.mixora.viewmodels.AiIntegrationSettingsViewModel
import kotlinx.coroutines.launch

private enum class TestApiVisualState { Idle, Testing, Success, Failed }

@Composable
fun AiIntegrationSettings(
    navController: NavController,
    viewModel: AiIntegrationSettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val availableModels by viewModel.availableModels.collectAsStateWithLifecycle()
    val (provider, setProvider) = rememberEnumPreference(AiProviderKey, AiProvider.NONE)
    val (apiKey, setApiKey) = rememberPreference(AiApiKeyKey, "")
    val (customEndpoint, setCustomEndpoint) = rememberPreference(AiCustomEndpointKey, "")
    val (validationStatus, setValidationStatus) =
        rememberEnumPreference(AiApiValidationStatusKey, AiApiValidationStatus.UNKNOWN)
    val (selectedModel, setSelectedModel) = rememberPreference(AiSelectedModelKey, "")
    val (customModel, setCustomModel) = rememberPreference(AiCustomModelKey, "")
    var showApiKeyDialog by rememberSaveable { mutableStateOf(false) }
    var showProviderDialog by rememberSaveable { mutableStateOf(false) }
    var showEndpointDialog by rememberSaveable { mutableStateOf(false) }
    var showCustomModelDialog by rememberSaveable { mutableStateOf(false) }
    var showModelSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Automatically load models on screen launch if API Key exists and models list is empty
    LaunchedEffect(provider, apiKey, customEndpoint) {
        if (provider != AiProvider.NONE &&
            provider != AiProvider.CUSTOM &&
            apiKey.isNotBlank() &&
            availableModels.isEmpty()
        ) {
            viewModel.fetchModels(provider, apiKey, customEndpoint)
        }
    }

    val hasCustomEndpoint = provider != AiProvider.CUSTOM || customEndpoint.isNotBlank()
    val hasApiConfiguration = provider != AiProvider.NONE && apiKey.isNotBlank() && hasCustomEndpoint
    val hasModelConfiguration =
        when (provider) {
            AiProvider.CUSTOM -> customModel.isNotBlank()
            AiProvider.NONE -> false
            else -> selectedModel.isNotBlank()
        }

    // Model selection box is enabled whenever provider is configured and API key is provided
    val canUseModelPicker =
        provider != AiProvider.NONE &&
            provider != AiProvider.CUSTOM &&
            apiKey.isNotBlank()

    val canTestApi = hasApiConfiguration && hasModelConfiguration && !actionState.isTesting

    if (showApiKeyDialog) {
        ApiKeyDialog(
            value = apiKey,
            onDismiss = { showApiKeyDialog = false },
            onSave = { value ->
                val cleanKey = value.trim()
                setApiKey(cleanKey)
                if (cleanKey.isNotBlank()) {
                    viewModel.validateAndLoadModels(provider, cleanKey, customEndpoint)
                } else {
                    setValidationStatus(AiApiValidationStatus.UNKNOWN)
                    viewModel.clearAvailableModels()
                }
            },
        )
    }

    if (showProviderDialog) {
        ProviderSelectionDialog(
            currentProvider = provider,
            onDismiss = { showProviderDialog = false },
            onSelected = { selectedProvider ->
                if (provider != selectedProvider) {
                    setSelectedModel("")
                    viewModel.clearAvailableModels()
                }
                setProvider(selectedProvider)
                setValidationStatus(AiApiValidationStatus.UNKNOWN)
                showProviderDialog = false
            }
        )
    }

    if (showEndpointDialog) {
        CustomEndpointDialog(
            value = customEndpoint,
            onDismiss = { showEndpointDialog = false },
            onSave = { value ->
                setCustomEndpoint(value.trim())
                setValidationStatus(AiApiValidationStatus.UNKNOWN)
                viewModel.clearError()
            }
        )
    }

    if (showCustomModelDialog) {
        CustomModelDialog(
            value = customModel,
            onDismiss = { showCustomModelDialog = false },
            onSave = { value ->
                setCustomModel(value.trim())
                setValidationStatus(AiApiValidationStatus.UNKNOWN)
                viewModel.clearError()
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ai_integration)) },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        onLongClick = { navController.backToMain() }
                    ) {
                        Icon(painterResource(R.drawable.arrow_back), contentDescription = stringResource(R.string.back_button_desc))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Material3SettingsGroup(
                title = stringResource(R.string.ai_provider_settings),
                items = buildList {
                    // Provider selector
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.auto_awesome),
                            title = { Text(stringResource(R.string.ai_provider)) },
                            description = { Text(provider.label()) },
                            onClick = { showProviderDialog = true }
                        )
                    )

                    // Custom endpoint (if Custom provider selected)
                    if (provider == AiProvider.CUSTOM) {
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.website),
                                title = { Text(stringResource(R.string.ai_custom_endpoint)) },
                                description = {
                                    Text(
                                        if (customEndpoint.isBlank()) "https://..." else customEndpoint,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                onClick = { showEndpointDialog = true }
                            )
                        )
                    }

                    // API Key
                    add(
                        Material3SettingsItem(
                            icon = painterResource(R.drawable.token),
                            title = { Text(stringResource(R.string.ai_api_key)) },
                            description = {
                                Text(
                                    if (apiKey.isBlank()) {
                                        stringResource(R.string.ai_api_key_missing)
                                    } else {
                                        stringResource(R.string.ai_api_key_configured)
                                    }
                                )
                            },
                            enabled = provider != AiProvider.NONE,
                            onClick = { showApiKeyDialog = true }
                        )
                    )

                    // Model selector (for standard providers)
                    if (provider != AiProvider.NONE && provider != AiProvider.CUSTOM) {
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.auto_awesome),
                                title = { Text(stringResource(R.string.ai_model)) },
                                description = {
                                    val desc = when {
                                        actionState.isFetchingModels && availableModels.isEmpty() -> stringResource(R.string.ai_model_loading)
                                        apiKey.isBlank() -> stringResource(R.string.ai_model_api_key_required)
                                        availableModels.isEmpty() -> stringResource(R.string.ai_model_fetch_hint)
                                        selectedModel.isBlank() -> stringResource(R.string.ai_model_not_selected)
                                        else -> availableModels.firstOrNull { it.id == selectedModel }?.displayName ?: selectedModel
                                    }
                                    Text(desc)
                                },
                                trailingContent = {
                                    if (actionState.isFetchingModels) {
                                        CircularWavyProgressIndicator(modifier = Modifier.size(24.dp))
                                    } else if (canUseModelPicker) {
                                        FilledTonalIconButton(
                                            onClick = { viewModel.fetchModels(provider, apiKey, customEndpoint) },
                                            enabled = !actionState.isFetchingModels,
                                        ) {
                                            Icon(
                                                painterResource(R.drawable.sync),
                                                contentDescription = stringResource(R.string.ai_fetch_models),
                                            )
                                        }
                                    }
                                },
                                enabled = canUseModelPicker,
                                onClick = {
                                    if (canUseModelPicker) {
                                        if (availableModels.isEmpty()) {
                                            viewModel.fetchModels(provider, apiKey, customEndpoint)
                                        }
                                        showModelSheet = true
                                    }
                                }
                            )
                        )
                    }

                    // Custom model name (if Custom provider selected)
                    if (provider == AiProvider.CUSTOM) {
                        add(
                            Material3SettingsItem(
                                icon = painterResource(R.drawable.auto_awesome),
                                title = { Text(stringResource(R.string.ai_model)) },
                                description = {
                                    Text(if (customModel.isBlank()) stringResource(R.string.ai_model_not_selected) else customModel)
                                },
                                onClick = { showCustomModelDialog = true }
                            )
                        )
                    }

                    // Test API card
                    val testVisualState = when {
                        actionState.isTesting -> TestApiVisualState.Testing
                        validationStatus == AiApiValidationStatus.SUCCESS -> TestApiVisualState.Success
                        validationStatus == AiApiValidationStatus.FAILED -> TestApiVisualState.Failed
                        else -> TestApiVisualState.Idle
                    }
                    add(
                        Material3SettingsItem(
                            icon = painterResource(
                                when (testVisualState) {
                                    TestApiVisualState.Success -> R.drawable.done
                                    TestApiVisualState.Failed -> R.drawable.error
                                    else -> R.drawable.sync
                                }
                            ),
                            title = { Text(stringResource(R.string.ai_test_api)) },
                            description = {
                                Column {
                                    Text(
                                        text = when (testVisualState) {
                                            TestApiVisualState.Testing -> stringResource(R.string.ai_api_testing)
                                            else -> validationStatus.label()
                                        },
                                        color = when (testVisualState) {
                                            TestApiVisualState.Success -> MaterialTheme.colorScheme.primary
                                            TestApiVisualState.Failed -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    actionState.errorMessage?.let { message ->
                                        Spacer(Modifier.height(6.dp))
                                        AiErrorHintRow(message = message)
                                    }
                                }
                            },
                            trailingContent = {
                                if (actionState.isTesting) {
                                    CircularWavyProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            },
                            enabled = canTestApi,
                            onClick = { viewModel.testApi() }
                        )
                    )
                }
            )

            Spacer(Modifier.height(16.dp))

            // Information note about where AI Integration is used
            Material3SettingsGroup(
                title = stringResource(R.string.ai_usage_note_title),
                items = listOf(
                    Material3SettingsItem(
                        icon = painterResource(R.drawable.info),
                        title = { Text(stringResource(R.string.ai_usage_note_title)) },
                        description = {
                            Text(
                                text = stringResource(R.string.ai_usage_note_desc),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    )
                )
            )

            // Extra bottom spacing so the awareness note is never covered by the mini player
            Spacer(Modifier.height(48.dp))

            // Dynamic model picker sheet if standard provider model picker tapped
            if (showModelSheet && canUseModelPicker) {
                ModelPickerPreferenceSheet(
                    selectedModel = selectedModel,
                    availableModels = availableModels,
                    isFetchingModels = actionState.isFetchingModels,
                    onDismiss = { showModelSheet = false },
                    onModelSelected = {
                        setSelectedModel(it)
                        setValidationStatus(AiApiValidationStatus.UNKNOWN)
                        viewModel.clearError()
                    }
                )
            }
        }
    }
}

@Composable
private fun ProviderSelectionDialog(
    currentProvider: AiProvider,
    onDismiss: () -> Unit,
    onSelected: (AiProvider) -> Unit
) {
    val providers = listOf(
        AiProvider.GEMINI,
        AiProvider.CHATGPT,
        AiProvider.OPENROUTER,
        AiProvider.CUSTOM,
        AiProvider.NONE,
    )
    DefaultDialog(
        onDismiss = onDismiss,
        icon = { Icon(painterResource(R.drawable.auto_awesome), contentDescription = null) },
        title = { Text(stringResource(R.string.ai_provider)) },
        buttons = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    ) {
        Column(Modifier.fillMaxWidth()) {
            providers.forEach { providerOption ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .selectable(
                            selected = providerOption == currentProvider,
                            role = Role.RadioButton,
                            onClick = { onSelected(providerOption) }
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = providerOption.label(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (providerOption == currentProvider) FontWeight.Bold else FontWeight.Normal,
                        color = if (providerOption == currentProvider) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun ApiKeyDialog(
    value: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var field by remember { mutableStateOf(TextFieldValue(value)) }
    val clipboardManager = LocalClipboardManager.current
    val hasClipboardText = remember {
        try {
            val text = clipboardManager.getText()?.text
            !text.isNullOrBlank()
        } catch (e: Exception) {
            false
        }
    }

    DefaultDialog(
        onDismiss = onDismiss,
        icon = { Icon(painterResource(R.drawable.token), contentDescription = null) },
        title = { Text(stringResource(R.string.ai_api_key)) },
        buttons = {
            ApiKeyDialogButtons(
                canSave = field.text.isNotBlank(),
                onDismiss = onDismiss,
                onSave = {
                    onSave(field.text)
                    onDismiss()
                },
            )
        },
    ) {
        OutlinedTextField(
            value = field,
            onValueChange = { field = it },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            label = { Text(stringResource(R.string.ai_api_key)) },
            trailingIcon = if (hasClipboardText) {
                {
                    androidx.compose.material3.IconButton(
                        onClick = {
                            try {
                                val text = clipboardManager.getText()?.text
                                if (!text.isNullOrBlank()) {
                                    field = TextFieldValue(text)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.content_copy),
                            contentDescription = stringResource(R.string.ai_paste_key),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else null
        )
    }
}

@Composable
private fun CustomEndpointDialog(
    value: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var field by remember { mutableStateOf(TextFieldValue(value)) }

    DefaultDialog(
        onDismiss = onDismiss,
        icon = { Icon(painterResource(R.drawable.website), contentDescription = null) },
        title = { Text(stringResource(R.string.ai_custom_endpoint)) },
        buttons = {
            ApiKeyDialogButtons(
                canSave = field.text.isBlank() || field.text.startsWith("http://") || field.text.startsWith("https://"),
                onDismiss = onDismiss,
                onSave = {
                    onSave(field.text)
                    onDismiss()
                },
            )
        },
    ) {
        OutlinedTextField(
            value = field,
            onValueChange = { field = it },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            label = { Text(stringResource(R.string.ai_custom_endpoint)) },
            placeholder = { Text("https://api.openai.com/v1/chat/completions") }
        )
    }
}

@Composable
private fun CustomModelDialog(
    value: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var field by remember { mutableStateOf(TextFieldValue(value)) }

    DefaultDialog(
        onDismiss = onDismiss,
        icon = { Icon(painterResource(R.drawable.auto_awesome), contentDescription = null) },
        title = { Text(stringResource(R.string.ai_model)) },
        buttons = {
            ApiKeyDialogButtons(
                canSave = field.text.isNotBlank(),
                onDismiss = onDismiss,
                onSave = {
                    onSave(field.text)
                    onDismiss()
                },
            )
        },
    ) {
        OutlinedTextField(
            value = field,
            onValueChange = { field = it },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            label = { Text(stringResource(R.string.ai_model)) },
            placeholder = { Text("gpt-4o") }
        )
    }
}

@Composable
private fun RowScope.ApiKeyDialogButtons(
    canSave: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    TextButton(onClick = onDismiss, shapes = ButtonDefaults.shapes()) {
        Text(stringResource(android.R.string.cancel))
    }
    TextButton(
        enabled = canSave,
        onClick = onSave,
        shapes = ButtonDefaults.shapes(),
    ) {
        Text(stringResource(R.string.save))
    }
}

@Composable
private fun AiProvider.label(): String =
    when (this) {
        AiProvider.CHATGPT -> "OpenAI"
        AiProvider.GEMINI -> "Gemini"
        AiProvider.OPENROUTER -> stringResource(R.string.ai_provider_openrouter)
        AiProvider.CUSTOM -> stringResource(R.string.custom)
        AiProvider.NONE -> stringResource(R.string.ai_provider_none)
    }

@Composable
private fun aiModelCategoryLabels(): Map<AiModelCategory, String> {
    val text = stringResource(R.string.ai_model_category_text)
    val computerUse = stringResource(R.string.ai_model_category_computer_use)
    val agent = stringResource(R.string.ai_model_category_agent)
    val image = stringResource(R.string.ai_model_category_image)
    val video = stringResource(R.string.ai_model_category_video)
    val music = stringResource(R.string.ai_model_category_music)
    val speech = stringResource(R.string.ai_model_category_speech)
    val live = stringResource(R.string.ai_model_category_live)
    val groundedQa = stringResource(R.string.ai_model_category_grounded_qa)
    val embedding = stringResource(R.string.ai_model_category_embedding)
    val other = stringResource(R.string.ai_model_category_other)
    return remember(text, computerUse, agent, image, video, music, speech, live, groundedQa, embedding, other) {
        mapOf(
            AiModelCategory.TEXT to text,
            AiModelCategory.COMPUTER_USE to computerUse,
            AiModelCategory.AGENT to agent,
            AiModelCategory.IMAGE to image,
            AiModelCategory.VIDEO to video,
            AiModelCategory.MUSIC to music,
            AiModelCategory.SPEECH to speech,
            AiModelCategory.LIVE to live,
            AiModelCategory.GROUNDED_QA to groundedQa,
            AiModelCategory.EMBEDDING to embedding,
            AiModelCategory.OTHER to other,
        )
    }
}

@Composable
private fun AiApiValidationStatus.label(): String =
    when (this) {
        AiApiValidationStatus.UNKNOWN -> stringResource(R.string.ai_api_status_unknown)
        AiApiValidationStatus.SUCCESS -> stringResource(R.string.ai_api_status_success)
        AiApiValidationStatus.FAILED -> stringResource(R.string.ai_api_status_failed)
    }

@Composable
private fun AiErrorHintRow(message: String) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            painter = painterResource(R.drawable.error),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ModelPickerPreferenceSheet(
    selectedModel: String,
    availableModels: List<AiModelOption>,
    isFetchingModels: Boolean,
    onDismiss: () -> Unit,
    onModelSelected: (String) -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    val categoryLabels = aiModelCategoryLabels()
    val unsupportedNote = stringResource(R.string.ai_model_unsupported)
    val filteredModels by remember(availableModels, searchQuery, categoryLabels) {
        derivedStateOf {
            val query = searchQuery.trim()
            if (query.isBlank()) {
                availableModels
            } else {
                availableModels.filter { model ->
                    model.displayName.contains(query, ignoreCase = true) ||
                        model.id.contains(query, ignoreCase = true) ||
                        categoryLabels[model.category]?.contains(query, ignoreCase = true) == true
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Text(
            text = stringResource(R.string.ai_model),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier =
                Modifier
                    .padding(horizontal = 26.dp)
                    .padding(top = 18.dp, bottom = 22.dp),
        )
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = {},
                    expanded = false,
                    onExpandedChange = {},
                    placeholder = { Text(stringResource(R.string.ai_model_search)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.search),
                            contentDescription = null,
                        )
                    },
                    trailingIcon =
                        if (searchQuery.isNotBlank()) {
                            {
                                androidx.compose.material3.IconButton(
                                    onClick = { searchQuery = "" },
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.close),
                                        contentDescription = stringResource(R.string.clear),
                                    )
                                }
                            }
                        } else {
                            null
                        },
                )
            },
            expanded = false,
            onExpandedChange = {},
            windowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 26.dp)
                    .padding(bottom = 18.dp),
        ) {}
        if (isFetchingModels && availableModels.isEmpty()) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularWavyProgressIndicator(modifier = Modifier.size(36.dp))
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 26.dp, end = 26.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp),
            ) {
                if (filteredModels.isEmpty()) {
                    item(key = "empty", contentType = "empty") {
                        Text(
                            text = stringResource(R.string.ai_model_no_results),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 28.dp),
                        )
                    }
                }
                items(
                    items = filteredModels,
                    key = { it.id },
                    contentType = { "model" },
                ) { model ->
                    val id = model.id
                    val selected = id == selectedModel
                    val usable = model.category.isCallableForText
                    val categoryLabel = categoryLabels[model.category].orEmpty()
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.extraLarge)
                                .background(
                                    when {
                                        selected -> MaterialTheme.colorScheme.primary
                                        usable -> MaterialTheme.colorScheme.surfaceContainerHigh
                                        else -> MaterialTheme.colorScheme.surfaceContainer
                                    },
                                ).selectable(
                                    selected = selected,
                                    enabled = usable,
                                    role = Role.RadioButton,
                                    onClick = {
                                        onModelSelected(id)
                                        coroutineScope
                                            .launch {
                                                sheetState.hide()
                                            }.invokeOnCompletion {
                                                onDismiss()
                                            }
                                    },
                                ).padding(horizontal = 24.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = model.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color =
                                    when {
                                        selected -> MaterialTheme.colorScheme.onPrimary
                                        usable -> MaterialTheme.colorScheme.onSurface
                                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                    },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = if (usable) categoryLabel else "$categoryLabel · $unsupportedNote",
                                style = MaterialTheme.typography.bodyMedium,
                                color =
                                    when {
                                        selected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f)
                                        usable -> MaterialTheme.colorScheme.onSurfaceVariant
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                                    },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
