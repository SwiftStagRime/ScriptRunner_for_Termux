package io.github.swiftstagrime.termuxrunner.data.service

import android.content.ComponentName
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScriptTileService1 : BaseScriptTileService() {
    override val tileIndex = 1
}

@AndroidEntryPoint
class ScriptTileService2 : BaseScriptTileService() {
    override val tileIndex = 2
}

@AndroidEntryPoint
class ScriptTileService3 : BaseScriptTileService() {
    override val tileIndex = 3
}

@AndroidEntryPoint
class ScriptTileService4 : BaseScriptTileService() {
    override val tileIndex = 4
}

@AndroidEntryPoint
class ScriptTileService5 : BaseScriptTileService() {
    override val tileIndex = 5
}

fun tileIndexForComponent(component: ComponentName?): Int? =
    when (component?.className) {
        ScriptTileService1::class.java.name -> 1
        ScriptTileService2::class.java.name -> 2
        ScriptTileService3::class.java.name -> 3
        ScriptTileService4::class.java.name -> 4
        ScriptTileService5::class.java.name -> 5
        else -> null
    }
