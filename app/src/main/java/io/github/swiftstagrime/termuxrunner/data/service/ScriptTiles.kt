package io.github.swiftstagrime.termuxrunner.data.service

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
