package io.github.raghavsatyadev.openplayer.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import co.touchlab.kermit.Logger

/**
 * Opens credential stores as EncryptedSharedPreferences. If the encrypted file can't be read it is
 * deleted and recreated; if encryption is unavailable altogether it falls back to plain
 * "<name>_plain" preferences.
 */
class EncryptedCredentialStoreFactory(private val context: Context) : CredentialStoreFactory {
    override fun open(name: String): CredentialStore = SharedPreferencesCredentialStore(createPrefs(name))

    private fun createEncryptedPrefs(name: String): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            name,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun createPrefs(name: String): SharedPreferences = try {
        createEncryptedPrefs(name)
    } catch (e: Exception) {
        Logger.e(e) { "$name: EncryptedSharedPreferences unreadable, deleting and recreating" }
        context.deleteSharedPreferences(name)
        try {
            createEncryptedPrefs(name)
        } catch (e2: Exception) {
            Logger.e(e2) { "$name: Encrypted prefs unavailable, falling back to plain" }
            context.getSharedPreferences("${name}_plain", Context.MODE_PRIVATE)
        }
    }
}

private class SharedPreferencesCredentialStore(private val prefs: SharedPreferences) : CredentialStore {
    override fun getString(key: String): String? = prefs.getString(key, null)

    override fun getLong(key: String, default: Long): Long = prefs.getLong(key, default)

    override fun edit(commit: Boolean, block: CredentialStore.Editor.() -> Unit) {
        val editor = prefs.edit()
        EditorAdapter(editor).block()
        if (commit) editor.commit() else editor.apply()
    }

    private class EditorAdapter(private val editor: SharedPreferences.Editor) : CredentialStore.Editor {
        override fun putString(key: String, value: String?) = apply { editor.putString(key, value) }

        override fun putLong(key: String, value: Long) = apply { editor.putLong(key, value) }

        override fun clear() = apply { editor.clear() }
    }
}
