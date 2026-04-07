package io.github.swiftstagrime.termuxrunner.ui.features.tiles

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.swiftstagrime.termuxrunner.data.service.ScriptTileService1
import io.github.swiftstagrime.termuxrunner.data.service.ScriptTileService2
import io.github.swiftstagrime.termuxrunner.data.service.ScriptTileService3
import io.github.swiftstagrime.termuxrunner.data.service.ScriptTileService4
import io.github.swiftstagrime.termuxrunner.data.service.ScriptTileService5
import io.github.swiftstagrime.termuxrunner.di.DefaultDispatcher
import io.github.swiftstagrime.termuxrunner.di.IoDispatcher
import io.github.swiftstagrime.termuxrunner.domain.model.Category
import io.github.swiftstagrime.termuxrunner.domain.model.Script
import io.github.swiftstagrime.termuxrunner.domain.repository.CategoryRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.ScriptRepository
import io.github.swiftstagrime.termuxrunner.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TileSettingsViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    private val scriptRepo: ScriptRepository,
    private val categoryRepository: CategoryRepository,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    val allScripts: StateFlow<List<Script>> = scriptRepo
        .getAllScripts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = categoryRepository
        .getAllCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val tileIdMappings: Flow<Map<Int, Int?>> = combine(
        (1..5).map { idx -> prefs.getScriptIdForTile(idx).map { idx to it } }
    ) { array -> array.toMap() }

    val tileMappings: StateFlow<Map<Int, Script?>> = combine(
        tileIdMappings,
        allScripts
    ) { idMap, scripts ->
        idMap.mapValues { (_, id) ->
            if (id != null) scripts.find { it.id == id } else null
        }
    }
        .flowOn(defaultDispatcher)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    fun assignScript(tileIndex: Int, scriptId: Int) {
        viewModelScope.launch(ioDispatcher) {
            prefs.setScriptIdForTile(tileIndex, scriptId)
            requestTileUpdate(tileIndex)
        }
    }

    fun clearTile(tileIndex: Int) {
        viewModelScope.launch(ioDispatcher) {
            prefs.setScriptIdForTile(tileIndex, null)
            requestTileUpdate(tileIndex)
        }
    }

    private fun requestTileUpdate(index: Int) {
        val clazz = when (index) {
            1 -> ScriptTileService1::class.java
            2 -> ScriptTileService2::class.java
            3 -> ScriptTileService3::class.java
            4 -> ScriptTileService4::class.java
            5 -> ScriptTileService5::class.java
            else -> null
        }

        clazz?.let {
            TileService.requestListeningState(
                context,
                ComponentName(context, it),
            )
        }
    }
}