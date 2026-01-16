package io.github.swiftstagrime.termuxrunner.di

import androidx.sqlite.db.SupportSQLiteOpenHelper
import io.github.swiftstagrime.termuxrunner.data.local.KeyManager
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import javax.inject.Inject

class KeyManagerFactory
    @Inject
    constructor(
        private val keyManager: KeyManager,
    ) : SupportSQLiteOpenHelper.Factory {
        override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
            val passphrase = runBlocking { keyManager.getRoomPassphrase() }

            val actualFactory = SupportOpenHelperFactory(passphrase)
            return actualFactory.create(configuration)
        }
    }
