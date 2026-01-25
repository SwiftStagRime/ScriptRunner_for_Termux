package io.github.swiftstagrime.termuxrunner.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Category(
    val id: Int = 0,
    val name: String,
    val orderIndex: Int = 0,
) : Parcelable
