package io.github.swiftstagrime.termuxrunner.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.swiftstagrime.termuxrunner.R

@Composable
fun DayOfWeekPicker(
    selectedDays: List<Int>,
    onToggleDay: (Int) -> Unit,
) {
    val days =
        listOf(
            stringResource(R.string.day_sun),
            stringResource(R.string.day_mon),
            stringResource(R.string.day_tue),
            stringResource(R.string.day_wed),
            stringResource(R.string.day_thu),
            stringResource(R.string.day_fri),
            stringResource(R.string.day_sat),
        )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(days) { index, day ->
            val dayNum = index + 1
            val isSelected = selectedDays.contains(dayNum)

            Box(
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLowest
                            },
                        ).border(
                            width = 1.dp,
                            color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape,
                        ).clickable { onToggleDay(dayNum) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color =
                        if (isSelected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
            }
        }
    }
}
