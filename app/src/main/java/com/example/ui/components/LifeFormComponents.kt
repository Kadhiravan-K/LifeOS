package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.engine.LocalLifeTheme
import com.example.ui.theme.engine.ThemeCardVariant
import com.example.ui.theme.engine.ThemedCard
import com.example.ui.theme.engine.ThemedDestructiveButton
import com.example.ui.theme.engine.ThemedPrimaryButton
import com.example.ui.theme.engine.ThemedSecondaryButton

/**
 * Unified Card Container for form inputs and sheets across LifeOS.
 * Automatically respects active theme tokens (elevation, gradient, borders, corners).
 */
@Composable
fun LifeInputCard(
    modifier: Modifier = Modifier,
    variant: ThemeCardVariant = ThemeCardVariant.DEFAULT,
    shape: Shape? = null,
    borderWidth: Dp? = null,
    borderColor: Color? = null,
    containerColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    ThemedCard(
        modifier = modifier.fillMaxWidth(),
        variant = variant,
        shape = shape,
        borderWidth = borderWidth,
        borderColor = borderColor,
        containerColor = containerColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

/**
 * Standard Form Section Header with title, optional description, icon, and slot.
 * Ensures consistent visual hierarchy across all creation and editing workflows.
 */
@Composable
fun LifeFormSection(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    icon: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalLifeTheme.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = theme.colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Column {
                    Text(
                        text = title,
                        style = theme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = theme.colors.onSurface
                    )
                    if (!description.isNullOrBlank()) {
                        Text(
                            text = description,
                            style = theme.typography.bodySmall,
                            color = theme.colors.onSurfaceVariant
                        )
                    }
                }
            }
            if (trailingContent != null) {
                trailingContent()
            }
        }
        content()
    }
}

/**
 * Standard Form Field Wrapper for input elements.
 * Displays field label, required asterisk (*), helper text, and animated error message.
 */
@Composable
fun LifeFormField(
    modifier: Modifier = Modifier,
    label: String? = null,
    isRequired: Boolean = false,
    helperText: String? = null,
    errorText: String? = null,
    content: @Composable () -> Unit
) {
    val theme = LocalLifeTheme.current
    val errorColor = MaterialTheme.colorScheme.error
    val isError = !errorText.isNullOrBlank()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (!label.isNullOrBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = label,
                    style = theme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isError) errorColor else theme.colors.onSurfaceVariant
                )
                if (isRequired) {
                    Text(
                        text = "*",
                        style = theme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = errorColor
                    )
                }
            }
        }

        content()

        AnimatedVisibility(
            visible = isError,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 2.dp, top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Error",
                    tint = errorColor,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = errorText ?: "",
                    style = theme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = errorColor
                )
            }
        }

        if (!isError && !helperText.isNullOrBlank()) {
            Text(
                text = helperText,
                style = theme.typography.bodySmall,
                fontSize = 11.sp,
                color = theme.colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp, top = 2.dp)
            )
        }
    }
}

/**
 * Standard bottom action bar for creation / editing forms and dialogs.
 * Provides unified ThemedPrimaryButton, ThemedSecondaryButton, and optional ThemedDestructiveButton.
 */
@Composable
fun LifeFormActions(
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    saveText: String = "Save",
    cancelText: String = "Cancel",
    isSaveEnabled: Boolean = true,
    isSaving: Boolean = false,
    onDelete: (() -> Unit)? = null,
    deleteText: String = "Delete"
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onDelete != null) {
            ThemedDestructiveButton(
                onClick = onDelete,
                text = deleteText,
                modifier = Modifier.testTag("life_form_delete_btn")
            )
            Spacer(modifier = Modifier.weight(1f))
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        ThemedSecondaryButton(
            onClick = onCancel,
            text = cancelText,
            modifier = Modifier.testTag("life_form_cancel_btn")
        )

        Spacer(modifier = Modifier.width(8.dp))

        ThemedPrimaryButton(
            onClick = onSave,
            enabled = isSaveEnabled,
            isLoading = isSaving,
            text = saveText,
            modifier = Modifier.testTag("life_form_save_btn")
        )
    }
}
