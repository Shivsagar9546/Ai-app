package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.DEFAULT_SYSTEM_PROMPT
import com.example.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    viewModel: ChatViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isAdminUnlocked by viewModel.isAdminUnlocked.collectAsState()
    val adminSettings by viewModel.adminSettings.collectAsState()
    val testResult by viewModel.testApiResult.collectAsState()

    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    // Admin Config States
    var customModelInput by remember(adminSettings.geminiModel) {
        mutableStateOf(
            if (adminSettings.geminiModel != "auto" &&
                adminSettings.geminiModel != "gemini-3.5-flash" &&
                adminSettings.geminiModel != "gemini-flash-latest" &&
                adminSettings.geminiModel != "gemini-3.1-flash-lite-preview" &&
                adminSettings.geminiModel != "gemini-3.1-pro-preview"
            ) {
                adminSettings.geminiModel
            } else {
                ""
            }
        )
    }
    var geminiKeyInput by remember(adminSettings.geminiApiKey) { mutableStateOf(adminSettings.geminiApiKey) }
    var openAiKeyInput by remember(adminSettings.openAiApiKey) { mutableStateOf(adminSettings.openAiApiKey) }
    var poeKeyInput by remember(adminSettings.poeApiKey) { mutableStateOf(adminSettings.poeApiKey) }
    var openRouterKeyInput by remember(adminSettings.openRouterApiKey) { mutableStateOf(adminSettings.openRouterApiKey) }
    var systemPromptInput by remember(adminSettings.systemPrompt) { mutableStateOf(adminSettings.systemPrompt) }
    var showGeminiKey by remember { mutableStateOf(false) }
    var showOpenAiKey by remember { mutableStateOf(false) }
    var showPoeKey by remember { mutableStateOf(false) }
    var showPoeModelsDropdown by remember { mutableStateOf(false) }
    var showOpenRouterKey by remember { mutableStateOf(false) }
    var showOpenRouterModelsDropdown by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }

    val advancedHelper = remember { com.example.data.preferences.AdvancedSettingsHelper(context) }
    var geminiKey2Input by remember { mutableStateOf(advancedHelper.geminiApiKey2) }
    var geminiKey3Input by remember { mutableStateOf(advancedHelper.geminiApiKey3) }
    var geminiKey4Input by remember { mutableStateOf(advancedHelper.geminiApiKey4) }
    var geminiKey5Input by remember { mutableStateOf(advancedHelper.geminiApiKey5) }
    var showKeysVault by remember { mutableStateOf(false) }
    var dailyQuotaInput by remember { mutableStateOf(advancedHelper.dailyQuotaLimit.toFloat()) }
    var isMaintMode by remember { mutableStateOf(advancedHelper.isMaintenanceMode) }
    var isAutoSpeakMode by remember { mutableStateOf(advancedHelper.isAutoSpeakEnabled) }
    var adminSecQuestion by remember { mutableStateOf(advancedHelper.adminSecurityQuestion) }
    var adminSecAnswer by remember { mutableStateOf(advancedHelper.adminSecurityAnswer) }
    var feedbacksList by remember { mutableStateOf(advancedHelper.getFeedbacks()) }
    var showForgotPinDialog by remember { mutableStateOf(false) }
    var recoveryAnswerInput by remember { mutableStateOf("") }
    var recoveryError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isAdminUnlocked) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = "Admin & Backend Panel",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (isAdminUnlocked) {
                        TextButton(
                            onClick = { viewModel.lockAdmin() },
                            modifier = Modifier.testTag("admin_lock_button")
                        ) {
                            Text("Lock", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding()
    ) { paddingValues ->
        if (!isAdminUnlocked) {
            // PIN Verification Screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Admin Access",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Enter 4-digit admin PIN (Default: 1234)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = {
                        if (it.length <= 8) {
                            enteredPin = it
                            pinError = false
                        }
                    },
                    label = { Text("Admin PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    isError = pinError,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .testTag("admin_pin_input"),
                    shape = RoundedCornerShape(14.dp)
                )

                if (pinError) {
                    Text(
                        text = "Invalid PIN. Default is 1234.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val valid = viewModel.verifyAdminPin(enteredPin)
                        if (!valid) {
                            pinError = true
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(48.dp)
                        .testTag("admin_pin_unlock_button")
                ) {
                    Text("Unlock Dashboard", fontWeight = FontWeight.Bold)
                }

                if (advancedHelper.adminSecurityQuestion.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { showForgotPinDialog = true }) {
                        Text("Forgot PIN? Reset via Security Question", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        } else {
            // Unlocked Admin Dashboard
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Usage Metrics & Analytics
                Text(
                    text = "Usage & Request Statistics",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("Total Requests", "${adminSettings.totalRequests}", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    StatCard("Today", "${adminSettings.todayRequests}", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                    StatCard("Screen Scans", "${adminSettings.screenScanRequests}", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("Gemini Calls", "${adminSettings.geminiRequests}", Color(0xFF4F46E5), Modifier.weight(1f))
                    StatCard("OpenAI Calls", "${adminSettings.openAiRequests}", Color(0xFF10B981), Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("Poe Calls", "${adminSettings.poeRequests}", Color(0xFFEAB308), Modifier.weight(1f))
                    StatCard("OpenRouter", "${adminSettings.openRouterRequests}", Color(0xFF06B6D4), Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("Errors Raised", "${adminSettings.errorCount}", MaterialTheme.colorScheme.error, Modifier.weight(1f))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { viewModel.resetStats() }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Statistics")
                    }
                }

                // Section 2: AI Provider Settings
                Text(
                    text = "AI Routing & Provider Settings",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(
                            text = "Default AI Provider",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )

                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilterChip(
                                selected = adminSettings.defaultProvider == "gemini",
                                onClick = { viewModel.updateAdminSettings(defaultProvider = "gemini") },
                                label = { Text("Google Gemini (Default)") },
                                leadingIcon = {
                                    if (adminSettings.defaultProvider == "gemini") {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            )

                            FilterChip(
                                selected = adminSettings.defaultProvider == "openai",
                                onClick = { viewModel.updateAdminSettings(defaultProvider = "openai") },
                                label = { Text("OpenAI") },
                                leadingIcon = {
                                    if (adminSettings.defaultProvider == "openai") {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            )

                            FilterChip(
                                selected = adminSettings.defaultProvider == "poe",
                                onClick = { viewModel.updateAdminSettings(defaultProvider = "poe") },
                                label = { Text("Poe API") },
                                leadingIcon = {
                                    if (adminSettings.defaultProvider == "poe") {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            )

                            FilterChip(
                                selected = adminSettings.defaultProvider == "openrouter",
                                onClick = { viewModel.updateAdminSettings(defaultProvider = "openrouter") },
                                label = { Text("OpenRouter (Free/Premium)") },
                                leadingIcon = {
                                    if (adminSettings.defaultProvider == "openrouter") {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Auto-Fallback on Failure",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Automatically switches to alternative provider if primary hits rate limit or error",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = adminSettings.isFallbackEnabled,
                                onCheckedChange = { viewModel.updateAdminSettings(isFallbackEnabled = it) }
                            )
                        }

                        androidx.compose.material3.HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Google Search Grounding (Web Search)",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Enables Gemini to search the web in real-time to answer with current info and web links",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = adminSettings.isWebSearchEnabled,
                                onCheckedChange = { viewModel.updateAdminSettings(isWebSearchEnabled = it) },
                                modifier = Modifier.testTag("web_search_switch")
                            )
                        }
                    }
                }

                // Section 3: Gemini Configuration
                Text(
                    text = "Google Gemini Configuration",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Model Selector
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = "Model Presets:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = adminSettings.geminiModel == "auto",
                                    onClick = { viewModel.updateAdminSettings(geminiModel = "auto") },
                                    label = { Text("Auto-Detect (Smart 🧠)") }
                                )
                                FilterChip(
                                    selected = adminSettings.geminiModel == "gemini-3.5-flash",
                                    onClick = { viewModel.updateAdminSettings(geminiModel = "gemini-3.5-flash") },
                                    label = { Text("gemini-3.5-flash (Fast ⚡)") }
                                )
                                FilterChip(
                                    selected = adminSettings.geminiModel == "gemini-flash-latest",
                                    onClick = { viewModel.updateAdminSettings(geminiModel = "gemini-flash-latest") },
                                    label = { Text("gemini-flash-latest") }
                                )
                                FilterChip(
                                    selected = adminSettings.geminiModel == "gemini-3.1-pro-preview",
                                    onClick = { viewModel.updateAdminSettings(geminiModel = "gemini-3.1-pro-preview") },
                                    label = { Text("gemini-3.1-pro (Complex)") }
                                )
                                FilterChip(
                                    selected = adminSettings.geminiModel == "gemini-3.1-flash-lite-preview",
                                    onClick = { viewModel.updateAdminSettings(geminiModel = "gemini-3.1-flash-lite-preview") },
                                    label = { Text("gemini-3.1-lite") }
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Custom / Future Model Input Field
                            OutlinedTextField(
                                value = customModelInput,
                                onValueChange = {
                                    customModelInput = it
                                    if (it.isNotBlank()) {
                                        viewModel.updateAdminSettings(geminiModel = it.trim())
                                    }
                                },
                                label = { Text("Or Type Any Custom/Future Model Name") },
                                placeholder = { Text("e.g. gemini-3.7-pro, gemini-3.5-flash") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }

                        // API Key Field with Masking
                        OutlinedTextField(
                            value = geminiKeyInput,
                            onValueChange = { geminiKeyInput = it },
                            label = { Text("Gemini API Key Override") },
                            placeholder = { Text("Uses BuildConfig.GEMINI_API_KEY if empty") },
                            visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                                    Icon(
                                        imageVector = if (showGeminiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle key visibility"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("gemini_api_key_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.updateAdminSettings(geminiApiKey = geminiKeyInput.trim())
                                    Toast.makeText(context, "Gemini key updated", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save Key")
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.testGeminiConnection(geminiKeyInput.trim(), adminSettings.geminiModel)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Test Connection")
                            }
                        }
                    }
                }

                // Section 4: OpenAI Configuration
                Text(
                    text = "OpenAI Configuration",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Model:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            FilterChip(
                                selected = adminSettings.openAiModel == "gpt-4o-mini",
                                onClick = { viewModel.updateAdminSettings(openAiModel = "gpt-4o-mini") },
                                label = { Text("gpt-4o-mini") }
                            )
                            FilterChip(
                                selected = adminSettings.openAiModel == "gpt-4o",
                                onClick = { viewModel.updateAdminSettings(openAiModel = "gpt-4o") },
                                label = { Text("gpt-4o") }
                            )
                        }

                        OutlinedTextField(
                            value = openAiKeyInput,
                            onValueChange = { openAiKeyInput = it },
                            label = { Text("OpenAI API Key (sk-...)") },
                            placeholder = { Text("Enter your secret OpenAI API key") },
                            visualTransformation = if (showOpenAiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showOpenAiKey = !showOpenAiKey }) {
                                    Icon(
                                        imageVector = if (showOpenAiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle key visibility"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("openai_api_key_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.updateAdminSettings(openAiApiKey = openAiKeyInput.trim())
                                    Toast.makeText(context, "OpenAI key updated", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save Key")
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.testOpenAiConnection(openAiKeyInput.trim(), adminSettings.openAiModel)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Test Connection")
                            }
                        }
                    }
                }

                // Section Poe: Poe Configuration
                Text(
                    text = "Poe Configuration",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Model:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { showPoeModelsDropdown = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (adminSettings.poeModel.isNotBlank()) adminSettings.poeModel else "Select Poe Model ▼",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                DropdownMenu(
                                    expanded = showPoeModelsDropdown,
                                    onDismissRequest = { showPoeModelsDropdown = false }
                                ) {
                                    val modelsList by viewModel.poeModelsState.collectAsState()
                                    if (modelsList.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No models loaded. Click 'Refresh Models'.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                            onClick = { showPoeModelsDropdown = false }
                                        )
                                    } else {
                                        modelsList.forEach { modelName ->
                                            DropdownMenuItem(
                                                text = { Text(modelName) },
                                                onClick = {
                                                    viewModel.updateAdminSettings(poeModel = modelName)
                                                    showPoeModelsDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = poeKeyInput,
                            onValueChange = { poeKeyInput = it },
                            label = { Text("Poe API Key (pb-...)") },
                            placeholder = { Text("Enter your Poe API key") },
                            visualTransformation = if (showPoeKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showPoeKey = !showPoeKey }) {
                                    Icon(
                                        imageVector = if (showPoeKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle key visibility"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("poe_api_key_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.updateAdminSettings(poeApiKey = poeKeyInput.trim())
                                    Toast.makeText(context, "Poe API key saved", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save Key")
                            }

                            OutlinedButton(
                                onClick = {
                                    poeKeyInput = ""
                                    viewModel.updateAdminSettings(poeApiKey = "", poeModel = "")
                                    Toast.makeText(context, "Poe API key removed", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Remove Key")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.testPoeConnection(poeKeyInput.trim(), adminSettings.poeModel)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Test Connection")
                            }

                            val isRefreshing by viewModel.isRefreshingPoeModels.collectAsState()
                            OutlinedButton(
                                onClick = {
                                    viewModel.refreshPoeModels(poeKeyInput.trim())
                                },
                                enabled = !isRefreshing,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isRefreshing) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Refreshing...")
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Refresh Models")
                                }
                            }
                        }
                    }
                }

                // Section OpenRouter: OpenRouter Configuration
                Text(
                    text = "OpenRouter Configuration",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Model:", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { showOpenRouterModelsDropdown = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (adminSettings.openRouterModel.isNotBlank()) adminSettings.openRouterModel else "Select OpenRouter Model ▼",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                DropdownMenu(
                                    expanded = showOpenRouterModelsDropdown,
                                    onDismissRequest = { showOpenRouterModelsDropdown = false }
                                ) {
                                    val modelsList by viewModel.openRouterModelsState.collectAsState()
                                    if (modelsList.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No models loaded. Click 'Refresh Models'.", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                            onClick = { showOpenRouterModelsDropdown = false }
                                        )
                                    } else {
                                        modelsList.forEach { modelName ->
                                            DropdownMenuItem(
                                                text = { Text(modelName) },
                                                onClick = {
                                                    viewModel.updateAdminSettings(openRouterModel = modelName)
                                                    showOpenRouterModelsDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = openRouterKeyInput,
                            onValueChange = { openRouterKeyInput = it },
                            label = { Text("OpenRouter API Key (sk-or-...)") },
                            placeholder = { Text("Enter your OpenRouter API key") },
                            visualTransformation = if (showOpenRouterKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showOpenRouterKey = !showOpenRouterKey }) {
                                    Icon(
                                        imageVector = if (showOpenRouterKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle key visibility"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("openrouter_api_key_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.updateAdminSettings(openRouterApiKey = openRouterKeyInput.trim())
                                    Toast.makeText(context, "OpenRouter API key saved", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save Key")
                            }

                            OutlinedButton(
                                onClick = {
                                    openRouterKeyInput = ""
                                    viewModel.updateAdminSettings(openRouterApiKey = "", openRouterModel = "google/gemini-flash-1.5:free")
                                    Toast.makeText(context, "OpenRouter API key removed", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Remove Key")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.testOpenRouterConnection(openRouterKeyInput.trim(), adminSettings.openRouterModel)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Test Connection")
                            }

                            val isRefreshingOR by viewModel.isRefreshingOpenRouterModels.collectAsState()
                            OutlinedButton(
                                onClick = {
                                    viewModel.refreshOpenRouterModels(openRouterKeyInput.trim())
                                },
                                enabled = !isRefreshingOR,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isRefreshingOR) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Refreshing...")
                                } else {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Refresh Models")
                                }
                            }
                        }
                    }
                }

                // Test API Result Banner
                if (testResult != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (testResult!!.second) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (testResult!!.second) Icons.Default.CheckCircle else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (testResult!!.second) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = testResult!!.first,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Section 5: System Prompt Editor
                Text(
                    text = "Global Assistant System Prompt",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = systemPromptInput,
                            onValueChange = { systemPromptInput = it },
                            label = { Text("System Instructions") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            maxLines = 8,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = {
                                    systemPromptInput = DEFAULT_SYSTEM_PROMPT
                                    viewModel.updateAdminSettings(systemPrompt = DEFAULT_SYSTEM_PROMPT)
                                }
                            ) {
                                Text("Reset Default")
                            }

                            Button(
                                onClick = {
                                    viewModel.updateAdminSettings(systemPrompt = systemPromptInput.trim())
                                    Toast.makeText(context, "System prompt updated", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("Save System Prompt")
                            }
                        }
                    }
                }

                // Section 6: Security & PIN Management
                Text(
                    text = "Admin Security",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                // -------------------------------------------------------------
                // ADVANCED CONTROLS & AUTOMATION (Features 12, 13 & 14)
                // -------------------------------------------------------------
                Text(
                    text = "Advanced Controls & Automation",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        
                        // Feature 12: API Latency Speed display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.0f)) {
                                Text(
                                    text = "Last API Speed",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Latency measured on last successful AI response",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            val latency = advancedHelper.lastApiLatencyMs
                            val latencyText = if (latency > 0) "${latency}ms" else "No requests yet"
                            val latencyColor = if (latency in 1..800) Color(0xFF10B981) else if (latency > 800) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = latencyColor.copy(alpha = 0.15f),
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(
                                    text = latencyText,
                                    color = latencyColor,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Feature 3: Remote Maintenance Mode Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Maintenance Mode",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Locks out user functions when undergoing background updates",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isMaintMode,
                                onCheckedChange = {
                                    isMaintMode = it
                                    advancedHelper.isMaintenanceMode = it
                                    Toast.makeText(context, "Maintenance Mode updated", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        // Feature 2: Global Usage Limiter (Quota)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Daily Request Quota Limit",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "${dailyQuotaInput.toInt()} Scans",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                )
                            }
                            Text(
                                text = "Enforces request capping to prevent personal key overruns or abuse",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = dailyQuotaInput,
                                onValueChange = {
                                    dailyQuotaInput = it
                                    advancedHelper.dailyQuotaLimit = it.toInt()
                                },
                                valueRange = 10f..500f,
                                steps = 49
                            )
                        }

                        // Feature 13: Multiple Keys Backup Vault (Rotation)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Keys Backup Vault (Rotation)",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Rotating fallback keys to prevent API limits or failures",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = { showKeysVault = !showKeysVault }) {
                                    Text(if (showKeysVault) "Hide" else "Show Keys")
                                }
                            }

                            if (showKeysVault) {
                                OutlinedTextField(
                                    value = geminiKey2Input,
                                    onValueChange = { geminiKey2Input = it; advancedHelper.geminiApiKey2 = it.trim() },
                                    label = { Text("Gemini Key 2") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = geminiKey3Input,
                                    onValueChange = { geminiKey3Input = it; advancedHelper.geminiApiKey3 = it.trim() },
                                    label = { Text("Gemini Key 3") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = geminiKey4Input,
                                    onValueChange = { geminiKey4Input = it; advancedHelper.geminiApiKey4 = it.trim() },
                                    label = { Text("Gemini Key 4") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = geminiKey5Input,
                                    onValueChange = { geminiKey5Input = it; advancedHelper.geminiApiKey5 = it.trim() },
                                    label = { Text("Gemini Key 5") },
                                    singleLine = true,
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = "💡 Keys are stored locally and will be automatically tried in sequence if primary key is rate-limited or exhausted.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Feature 14: Multi-Admin Backup security questions (Account recovery)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "PIN Security Question Recovery",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            OutlinedTextField(
                                value = adminSecQuestion,
                                onValueChange = { adminSecQuestion = it; advancedHelper.adminSecurityQuestion = it },
                                label = { Text("Recovery Security Question") },
                                placeholder = { Text("e.g., What was my first coding language?") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = adminSecAnswer,
                                onValueChange = { adminSecAnswer = it; advancedHelper.adminSecurityAnswer = it.trim() },
                                label = { Text("Security Answer") },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Feature 14: Logs & User Feedbacks Database Explorer
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "System Logs & Feedbacks Explorer",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                if (feedbacksList.isNotEmpty()) {
                                    TextButton(onClick = {
                                        advancedHelper.clearFeedbacks()
                                        feedbacksList = emptyList()
                                        Toast.makeText(context, "Logs cleared", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Text("Clear Logs")
                                    }
                                }
                            }
                            if (feedbacksList.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "No log files or user feedbacks collected yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .verticalScroll(rememberScrollState())
                                    ) {
                                        feedbacksList.forEach { log ->
                                            Text(
                                                text = log,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(bottom = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { showChangePinDialog = true },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Password, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Change Admin PIN (Current: ${adminSettings.adminPin})")
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Change PIN Dialog
    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            title = { Text("Change Admin PIN") },
            text = {
                OutlinedTextField(
                    value = newPinInput,
                    onValueChange = { if (it.length <= 8) newPinInput = it },
                    label = { Text("New PIN") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinInput.isNotBlank()) {
                            viewModel.updateAdminSettings(adminPin = newPinInput.trim())
                            Toast.makeText(context, "Admin PIN changed successfully", Toast.LENGTH_SHORT).show()
                        }
                        showChangePinDialog = false
                        newPinInput = ""
                    }
                ) {
                    Text("Change PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showForgotPinDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPinDialog = false },
            title = { Text("Admin PIN Recovery") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Security Question:",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = advancedHelper.adminSecurityQuestion,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = recoveryAnswerInput,
                        onValueChange = { recoveryAnswerInput = it; recoveryError = false },
                        label = { Text("Your Answer") },
                        singleLine = true,
                        isError = recoveryError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (recoveryError) {
                        Text(
                            text = "Incorrect answer. Please try again.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (recoveryAnswerInput.trim().equals(advancedHelper.adminSecurityAnswer, ignoreCase = true)) {
                            viewModel.updateAdminSettings(adminPin = "1234")
                            Toast.makeText(context, "PIN has been reset to 1234. Use 1234 to login.", Toast.LENGTH_LONG).show()
                            showForgotPinDialog = false
                            recoveryAnswerInput = ""
                            recoveryError = false
                        } else {
                            recoveryError = true
                        }
                    }
                ) {
                    Text("Verify & Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
        }
    }
}
