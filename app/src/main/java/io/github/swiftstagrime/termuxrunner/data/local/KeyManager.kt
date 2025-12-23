package io.github.swiftstagrime.termuxrunner.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import com.google.crypto.tink.subtle.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

private val Context.keyDataStore: DataStore<Preferences> by preferencesDataStore(name = "secure_db_key")

@Singleton
class KeyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val KEYSET_NAME = "master_keyset"
        private const val PREF_FILE_NAME = "master_key_preference"
        private const val MASTER_KEY_URI = "android-keystore://master_key"

        private const val DB_NAME = "script_runner_secure.db"

        private val DB_KEY_PREF = stringPreferencesKey("encrypted_room_passphrase")

        init {
            try {
                AeadConfig.register()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private val AEAD_KEY_TEMPLATE = AesGcmKeyManager.aes256GcmTemplate()
    }

    private var cachedKey: ByteArray? = null
    private val mutex = Mutex()

    init {
        AeadConfig.register()
    }

    suspend fun getRoomPassphrase(): ByteArray = mutex.withLock {
        cachedKey?.let { return it }

        try {
            return loadExistingKey()
        } catch (e: Exception) {
            e.printStackTrace()
            resetAll()
            return generateNewKey()
        }
    }

    private suspend fun loadExistingKey(): ByteArray {
        val aead = getMasterKeyOrThrow()

        val preferences = context.keyDataStore.data.first()
        val encryptedKeyBase64 = preferences[DB_KEY_PREF]
            ?: throw GeneralSecurityException("No key found in DataStore, need to generate new")

        val encryptedBytes = Base64.decode(encryptedKeyBase64, Base64.NO_WRAP)

        val decryptedKey = aead.decrypt(encryptedBytes, null)

        cachedKey = decryptedKey
        return decryptedKey
    }

    private suspend fun generateNewKey(): ByteArray {
        val aead = getMasterKeyOrThrow()

        val rawKey = ByteArray(32)
        SecureRandom().nextBytes(rawKey)

        val encryptedBytes = aead.encrypt(rawKey, null)
        val encryptedBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)

        context.keyDataStore.edit { it[DB_KEY_PREF] = encryptedBase64 }

        cachedKey = rawKey
        return rawKey
    }

    private fun getMasterKeyOrThrow(): Aead {
        return AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREF_FILE_NAME)
            .withKeyTemplate(AEAD_KEY_TEMPLATE)
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
            .getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    private suspend fun resetAll() {
        cachedKey = null

        try {
            context.keyDataStore.edit { it.clear() }
        } catch (_: Exception) {
        }

        try {
            context.deleteSharedPreferences(PREF_FILE_NAME)

            val keyStore = KeyStore.getInstance("AndroidKeyStore")
            keyStore.load(null)
            keyStore.deleteEntry("master_key")
        } catch (_: Exception) {
        }
        context.deleteDatabase(DB_NAME)
    }
}