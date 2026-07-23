package io.github.swiftstagrime.termuxrunner.data.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class WebhookConfig
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        companion object {
            private const val PREFS_NAME = "webhook_config"
            private const val KEY_PORT = "port"
            private const val KEY_TOKEN = "token"
            private const val KEY_LAN_ACCESS = "lan_access"
        }

        private val prefs by lazy { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

        var port: Int
            get() = prefs.getInt(KEY_PORT, 0)
            set(value) = prefs.edit().putInt(KEY_PORT, value).apply()

        var token: String?
            get() = prefs.getString(KEY_TOKEN, null)
            set(value) = prefs.edit().putString(KEY_TOKEN, value).apply()

        var lanAccess: Boolean
            get() = prefs.getBoolean(KEY_LAN_ACCESS, false)
            set(value) = prefs.edit().putBoolean(KEY_LAN_ACCESS, value).apply()

        fun generateToken(): String {
            val chars = "abcdefghijklmnopqrstuvwxyz0123456789"
            val token =
                buildString(32) {
                    for (i in 0 until 32) {
                        append(chars[Random.nextInt(0, chars.length)])
                    }
                }
            this.token = token
            return token
        }

        fun getPortOrGenerate(): Int {
            if (port > 0) return port
            val newPort = (8000..9000).random()
            this.port = newPort
            return newPort
        }
    }
