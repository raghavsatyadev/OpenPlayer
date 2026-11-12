package io.github.raghavsatyadev.openplayer.utils

import co.touchlab.kermit.Logger

object LogUtils {

    private const val MAX_TAG_LENGTH = 23

    private fun getTag(instance: Any): String {
        val className = (instance as? String) ?: instance.javaClass.simpleName
        return className.take(MAX_TAG_LENGTH)
    }

    private fun buildLogMessage(message: String, args: Array<out Any?>): String {
        val formatted = if (args.isEmpty()) message else String.format(message, *args)
        return "[${Thread.currentThread().name}] - $formatted"
    }

    fun d(tagProvider: Any, message: String, vararg args: Any?) {
        Logger.withTag(getTag(tagProvider)).d { buildLogMessage(message, args) }
    }

    fun i(tagProvider: Any, message: String, vararg args: Any?) {
        Logger.withTag(getTag(tagProvider)).i { buildLogMessage(message, args) }
    }

    fun w(tagProvider: Any, message: String, vararg args: Any?) {
        Logger.withTag(getTag(tagProvider)).w { buildLogMessage(message, args) }
    }

    fun e(tagProvider: Any, throwable: Throwable? = null, message: String, vararg args: Any?) {
        if (throwable != null) {
            Logger.withTag(getTag(tagProvider)).e(throwable) { buildLogMessage(message, args) }
        } else {
            Logger.withTag(getTag(tagProvider)).e { buildLogMessage(message, args) }
        }
    }
}
