package io.github.swiftstagrime.termuxrunner.ui.preview

import io.github.swiftstagrime.termuxrunner.domain.model.Script

val sampleScripts = listOf(
    Script(
        id = 1,
        name = "Update System",
        code = "pkg update && pkg upgrade -y",
        interpreter = "bash",
        executionParams = "",
        runInBackground = false
    ),
    Script(
        id = 2,
        name = "Python Server",
        code = "print('Starting server...')\nimport http.server",
        interpreter = "python",
        executionParams = "-m http.server 8080",
        runInBackground = true
    )
)
val configSampleScript = Script(
    id = 1,
    name = "Python API Server",
    code = "print('hello')",
    interpreter = "python",
    executionParams = "-m http.server",
    runInBackground = false,
    keepSessionOpen = true,
    envVars = mapOf(
        "PORT" to "8080",
        "API_KEY" to "secret_123",
        "DB_HOST" to "localhost"
    ),
    iconPath = null
)