package com.example.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.ProfileSurveyEntity

/**
 * Requirement #19: Optional, skippable, locally stored onboarding and new-profile survey.
 * All questions are optional, skippable at every step, and skipping never generates synthetic answers.
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingSurveyScreen(
    profileId: Long,
    initialSurvey: ProfileSurveyEntity? = null,
    isEditMode: Boolean = false,
    onComplete: (ProfileSurveyEntity) -> Unit,
    onSkipAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by rememberSaveable { mutableIntStateOf(1) }
    val totalSteps = 5
    var isSubmitting by rememberSaveable { mutableStateOf(false) }

    // Field states initialized from existing survey if available
    var displayName by rememberSaveable { mutableStateOf(initialSurvey?.displayName ?: "") }
    var selectedGender by rememberSaveable { mutableStateOf(initialSurvey?.gender ?: "") }
    var customGender by rememberSaveable { mutableStateOf("") }
    var sleepingTime by rememberSaveable { mutableStateOf(initialSurvey?.sleepingTime ?: "") }
    var waterGoalText by rememberSaveable { mutableStateOf(initialSurvey?.dailyWaterGoalMl?.toString() ?: "") }
    var waterGoalError by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedDiscoverySource by rememberSaveable { mutableStateOf(initialSurvey?.discoverySource ?: "") }
    var selectedSocialPlatform by rememberSaveable { mutableStateOf(initialSurvey?.discoverySocialPlatform ?: "") }
    var discoveryOtherDetail by rememberSaveable { mutableStateOf(initialSurvey?.discoveryOtherDetail ?: "") }

    val buildSurveyResult = {
        val parsedWater = waterGoalText.toIntOrNull()?.takeIf { it in ProfileSurveyEntity.MIN_WATER_GOAL_ML..ProfileSurveyEntity.MAX_WATER_GOAL_ML }
        val resolvedGender = if (selectedGender == "Self-describe" && customGender.isNotBlank()) {
            customGender.trim().take(30)
        } else {
            selectedGender.ifBlank { null }
        }

        ProfileSurveyEntity(
            profileId = profileId,
            displayName = displayName.trim().take(ProfileSurveyEntity.MAX_NAME_LENGTH).ifBlank { null },
            gender = resolvedGender,
            sleepingTime = sleepingTime.ifBlank { null },
            dailyWaterGoalMl = parsedWater,
            discoverySource = selectedDiscoverySource.ifBlank { null },
            discoverySocialPlatform = if (selectedDiscoverySource == "Social Media") selectedSocialPlatform.ifBlank { null } else null,
            discoveryOtherDetail = discoveryOtherDetail.trim().take(ProfileSurveyEntity.MAX_DETAIL_LENGTH).ifBlank { null },
            isCompleted = true,
            isSkipped = false,
            completedAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }

    val handleSkipAll = {
        if (!isSubmitting) {
            isSubmitting = true
            onSkipAll()
        }
    }

    val handleComplete = {
        if (!isSubmitting) {
            isSubmitting = true
            onComplete(buildSurveyResult())
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
            .testTag("onboarding_survey_screen"),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        IconButton(
                            onClick = { currentStep-- },
                            modifier = Modifier.testTag("survey_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous step"
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    Text(
                        text = if (isEditMode) "Profile Survey" else "Step $currentStep of $totalSteps",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    TextButton(
                        onClick = handleSkipAll,
                        enabled = !isSubmitting,
                        modifier = Modifier.testTag("survey_skip_all_button")
                    ) {
                        Text(
                            text = if (isEditMode) "Close" else "Skip All",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { currentStep.toFloat() / totalSteps.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Skip step button
                        OutlinedButton(
                            onClick = {
                                if (currentStep < totalSteps) {
                                    currentStep++
                                } else {
                                    handleComplete()
                                }
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("survey_skip_step_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Skip Step")
                        }

                        // Next or Complete button
                        Button(
                            onClick = {
                                if (currentStep == 4 && waterGoalText.isNotBlank()) {
                                    val num = waterGoalText.toIntOrNull()
                                    if (num == null || num !in ProfileSurveyEntity.MIN_WATER_GOAL_ML..ProfileSurveyEntity.MAX_WATER_GOAL_ML) {
                                        waterGoalError = "Enter 500 to 6000 ml"
                                        return@Button
                                    } else {
                                        waterGoalError = null
                                    }
                                }

                                if (currentStep < totalSteps) {
                                    currentStep++
                                } else {
                                    handleComplete()
                                }
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("survey_next_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (currentStep == totalSteps) (if (isEditMode) "Save Changes" else "Get Started") else "Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (currentStep == totalSteps) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "100% Private & Local to Device",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() with
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() with
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                label = "survey_step_content"
            ) { step ->
                when (step) {
                    1 -> DisplayNameStep(
                        displayName = displayName,
                        onDisplayNameChange = { displayName = it }
                    )
                    2 -> GenderStep(
                        selectedGender = selectedGender,
                        onGenderSelected = { selectedGender = it },
                        customGender = customGender,
                        onCustomGenderChange = { customGender = it }
                    )
                    3 -> SleepingTimeStep(
                        sleepingTime = sleepingTime,
                        onSleepingTimeSelected = { sleepingTime = it }
                    )
                    4 -> WaterGoalStep(
                        waterGoalText = waterGoalText,
                        onWaterGoalChange = {
                            waterGoalText = it
                            waterGoalError = null
                        },
                        errorMessage = waterGoalError
                    )
                    5 -> DiscoverySourceStep(
                        selectedSource = selectedDiscoverySource,
                        onSourceSelected = { selectedDiscoverySource = it },
                        selectedPlatform = selectedSocialPlatform,
                        onPlatformSelected = { selectedSocialPlatform = it },
                        otherDetail = discoveryOtherDetail,
                        onDetailChange = { discoveryOtherDetail = it }
                    )
                }
            }
        }
    }
}

@Composable
fun DisplayNameStep(
    displayName: String,
    onDisplayNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "What should we call you?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Personalize your workspace experience. This name is kept strictly on your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))
        OutlinedTextField(
            value = displayName,
            onValueChange = { if (it.length <= ProfileSurveyEntity.MAX_NAME_LENGTH) onDisplayNameChange(it) },
            label = { Text("Your Display Name (Optional)") },
            placeholder = { Text("e.g. Alex, Sam, Master") },
            singleLine = true,
            trailingIcon = if (displayName.isNotEmpty()) {
                {
                    IconButton(onClick = { onDisplayNameChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            } else null,
            supportingText = {
                Text("${displayName.length}/${ProfileSurveyEntity.MAX_NAME_LENGTH}")
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("survey_display_name_input")
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GenderStep(
    selectedGender: String,
    onGenderSelected: (String) -> Unit,
    customGender: String,
    onCustomGenderChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "✨", fontSize = 28.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "How do you identify?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Entirely optional. We never infer, assume, or transmit personal demographic data.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ProfileSurveyEntity.GENDER_OPTIONS.forEach { option ->
                val isSelected = selectedGender == option
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onGenderSelected(if (isSelected) "" else option)
                    },
                    label = {
                        Text(option, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("survey_gender_$option")
                )
            }
        }

        AnimatedVisibility(visible = selectedGender == "Self-describe") {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                OutlinedTextField(
                    value = customGender,
                    onValueChange = { if (it.length <= 30) onCustomGenderChange(it) },
                    label = { Text("Self-describe (Optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("survey_custom_gender_input")
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SleepingTimeStep(
    sleepingTime: String,
    onSleepingTimeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = listOf("21:30", "22:00", "22:30", "23:00", "23:30", "00:00")

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bedtime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "When do you go to sleep?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Used to calibrate healthy bedtime reminders and automated quiet hours.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (sleepingTime.isNotBlank()) "Target Bedtime: $sleepingTime" else "No bedtime set (Optional)",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                val isSelected = sleepingTime == preset
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onSleepingTimeSelected(if (isSelected) "" else preset)
                    },
                    label = { Text(preset) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.testTag("survey_sleep_$preset")
                )
            }
        }
    }
}

@Composable
fun WaterGoalStep(
    waterGoalText: String,
    onWaterGoalChange: (String) -> Unit,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocalDrink,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Daily Hydration Target",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Recommended daily intake is between 1,500ml and 3,500ml. If skipped, default 2000ml is preserved.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = waterGoalText,
            onValueChange = { text ->
                val filtered = text.filter { it.isDigit() }
                if (filtered.length <= 4) onWaterGoalChange(filtered)
            },
            label = { Text("Water Intake (ml)") },
            placeholder = { Text("e.g. 2000") },
            singleLine = true,
            isError = errorMessage != null,
            supportingText = if (errorMessage != null) {
                { Text(errorMessage, color = MaterialTheme.colorScheme.error) }
            } else {
                { Text("Allowed range: 500ml to 6000ml") }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("survey_water_goal_input")
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(1500, 2000, 2500, 3000).forEach { preset ->
                FilterChip(
                    selected = waterGoalText == preset.toString(),
                    onClick = { onWaterGoalChange(preset.toString()) },
                    label = { Text("${preset}ml") },
                    modifier = Modifier.weight(1f).testTag("survey_water_preset_$preset")
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DiscoverySourceStep(
    selectedSource: String,
    onSourceSelected: (String) -> Unit,
    selectedPlatform: String,
    onPlatformSelected: (String) -> Unit,
    otherDetail: String,
    onDetailChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "How did you discover LifeOS?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Helps our open-source team understand what resonates with productivity enthusiasts.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProfileSurveyEntity.DISCOVERY_SOURCES.forEach { source ->
                val isSelected = selectedSource == source
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        onSourceSelected(if (isSelected) "" else source)
                    },
                    label = { Text(source) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null,
                    modifier = Modifier.testTag("survey_source_$source")
                )
            }
        }

        // Sub-selection for Social Media
        AnimatedVisibility(visible = selectedSource == "Social Media") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = "Which social platform? (Alphabetical)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfileSurveyEntity.SOCIAL_PLATFORMS.forEach { platform ->
                        val isSelected = selectedPlatform == platform
                        FilterChip(
                            selected = isSelected,
                            onClick = { onPlatformSelected(if (isSelected) "" else platform) },
                            label = { Text(platform) },
                            modifier = Modifier.testTag("survey_social_$platform")
                        )
                    }
                }
            }
        }

        // Detail text field for "Other" or custom note
        AnimatedVisibility(visible = selectedSource == "Other" || selectedPlatform == "Other") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                OutlinedTextField(
                    value = otherDetail,
                    onValueChange = { if (it.length <= ProfileSurveyEntity.MAX_DETAIL_LENGTH) onDetailChange(it) },
                    label = { Text("Tell us more (Optional)") },
                    placeholder = { Text("e.g. Recommended by a colleague, blog post, podcast") },
                    supportingText = { Text("${otherDetail.length}/${ProfileSurveyEntity.MAX_DETAIL_LENGTH}") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("survey_other_detail_input")
                )
            }
        }
    }
}
