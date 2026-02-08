package io.github.swiftstagrime.termuxrunner.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun StyledTextField(
    value: String?,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    isError: Boolean = false,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    supportingText: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    var isFocused by remember { mutableStateOf(false) }
    val isFloating = isFocused || value?.isNotEmpty() == true

    val labelBgColor by animateColorAsState(
        targetValue =
            when {
                isError && isFloating -> MaterialTheme.colorScheme.error
                isFloating -> MaterialTheme.colorScheme.primary
                else -> Color.Transparent
            },
        label = "labelBg",
    )

    val labelTextColor by animateColorAsState(
        targetValue =
            when {
                isError && isFloating -> MaterialTheme.colorScheme.onError
                isFloating -> MaterialTheme.colorScheme.onPrimary
                isError -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        label = "labelTx",
    )

    OutlinedTextField(
        value = value ?: "",
        onValueChange = onValueChange,
        modifier =
            modifier
                .onFocusChanged { isFocused = it.isFocused },
        isError = isError,
        readOnly = readOnly,
        supportingText = supportingText,
        label = {
            Text(
                text = label,
                color = labelTextColor,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isFloating) FontWeight.Bold else FontWeight.Normal,
                modifier =
                    Modifier
                        .background(
                            color = labelBgColor,
                            shape = RoundedCornerShape(4.dp),
                        ).padding(
                            horizontal = if (isFloating) 6.dp else 0.dp,
                            vertical = if (isFloating) 2.dp else 0.dp,
                        ),
            )
        },
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        placeholder = placeholder,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        enabled = enabled,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(12.dp),
        colors =
            OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                errorBorderColor = MaterialTheme.colorScheme.error,
                cursorColor = MaterialTheme.colorScheme.primary,
                errorCursorColor = MaterialTheme.colorScheme.error,
                errorSupportingTextColor = MaterialTheme.colorScheme.error,
            ),
    )
}
