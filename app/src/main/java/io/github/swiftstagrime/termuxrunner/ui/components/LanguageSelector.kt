package io.github.swiftstagrime.termuxrunner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.util.LocaleManager
import io.github.swiftstagrime.termuxrunner.ui.theme.ScriptRunnerForTermuxTheme

@Composable
fun LanguageDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    val currentLanguage = LocaleManager.getCurrentLanguage()

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        LocaleManager.supportedLanguages.forEach { language ->
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = language.flagEmoji,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = language.name, style = MaterialTheme.typography.bodyMedium)
                    }
                },
                onClick = {
                    onDismiss()
                    onLanguageSelected(language.code)
                },
                trailingIcon = {
                    if (currentLanguage.code == language.code) {
                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    }
}

@Composable
fun LanguageSelectorButton(modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val currentLanguage = LocaleManager.getCurrentLanguage()

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "${currentLanguage.flagEmoji} ${currentLanguage.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(20.dp))
        }

        LanguageDropdownMenu(
            expanded = expanded,
            onDismiss = { expanded = false },
            onLanguageSelected = { LocaleManager.setLocale(it) }
        )
    }
}

@Composable
fun LanguageSelectorIcon() {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Default.Language,
                contentDescription = stringResource(R.string.language_label)
            )
        }

        LanguageDropdownMenu(
            expanded = expanded,
            onDismiss = { expanded = false },
            onLanguageSelected = { LocaleManager.setLocale(it) }
        )
    }
}

@Preview
@Composable
private fun PreviewSelector() {
    ScriptRunnerForTermuxTheme {
        LanguageDropdownMenu(
            true,
            {},
            {}
        )
    }
}