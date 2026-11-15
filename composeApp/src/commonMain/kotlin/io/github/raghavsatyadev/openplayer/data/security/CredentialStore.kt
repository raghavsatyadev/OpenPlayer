package io.github.raghavsatyadev.openplayer.data.security

/**
 * A small key-value store for server credentials (URLs, user names, passwords, tokens).
 * Android keeps it in EncryptedSharedPreferences.
 */
interface CredentialStore {
    fun getString(key: String): String?

    fun getLong(key: String, default: Long = 0L): Long

    /** Applies the edits in [block]; [commit] writes synchronously instead of in the background. */
    fun edit(commit: Boolean = false, block: Editor.() -> Unit)

    interface Editor {
        fun putString(key: String, value: String?): Editor

        fun putLong(key: String, value: Long): Editor

        fun clear(): Editor
    }
}

/** Opens the [CredentialStore] with the given file name. */
fun interface CredentialStoreFactory {
    fun open(name: String): CredentialStore
}
