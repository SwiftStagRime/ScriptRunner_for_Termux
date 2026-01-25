package io.github.swiftstagrime.termuxrunner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.swiftstagrime.termuxrunner.R
import io.github.swiftstagrime.termuxrunner.domain.model.Category
import io.github.swiftstagrime.termuxrunner.ui.preview.sampleCategories
import io.github.swiftstagrime.termuxrunner.ui.theme.ScriptRunnerForTermuxTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySpinner(
    categories: List<Category>,
    selectedCategoryId: Int?,
    onCategorySelected: (Int?) -> Unit,
    onAddNewClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCategory = categories.find { it.id == selectedCategoryId }

    Column {
        Text(
            text = "Category",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
        ) {
            val fillMaxWidth =
                Modifier
                    .fillMaxWidth()
            OutlinedTextField(
                value = selectedCategory?.name ?: stringResource(R.string.uncategorized),
                onValueChange = {},
                readOnly = true,
                modifier =
                    fillMaxWidth.menuAnchor(
                        ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                        true,
                    ),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(Icons.Default.Category, null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.primary,
                        unfocusedTextColor =
                            if (selectedCategoryId != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                    ),
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(16.dp),
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.uncategorized)) },
                    onClick = {
                        onCategorySelected(null)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )

                categories.forEach { category ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = category.name,
                                color =
                                    if (category.id == selectedCategoryId) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                            )
                        },
                        onClick = {
                            onCategorySelected(category.id)
                            expanded = false
                        },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(R.string.add_new_category),
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.secondary)
                    },
                    onClick = {
                        expanded = false
                        onAddNewClick()
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Uncategorized Selected")
@Composable
private fun PreviewCategorySpinnerUncategorized() {
    ScriptRunnerForTermuxTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CategorySpinner(
                categories = sampleCategories,
                selectedCategoryId = null,
                onCategorySelected = {},
                onAddNewClick = {},
            )
        }
    }
}

@Preview(showBackground = true, name = "Category Selected", uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCategorySpinnerSelected() {
    ScriptRunnerForTermuxTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            CategorySpinner(
                categories = sampleCategories,
                selectedCategoryId = 2,
                onCategorySelected = {},
                onAddNewClick = {},
            )
        }
    }
}
