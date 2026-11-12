package io.github.raghavsatyadev.openplayer.resources

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource

/*
 * Compose Resources only substitutes %N$s and %N$d, so strings with %%, %1$.1f, %2$02d or plain %d
 * would render wrong. These wrappers keep Android's behaviour: no args returns the raw text, args go
 * through String.format with the default locale.
 */

@Composable
fun stringResource(resource: StringResource): String =
    org.jetbrains.compose.resources.stringResource(resource)

@Composable
fun stringResource(resource: StringResource, vararg formatArgs: Any): String =
    formatResourceString(org.jetbrains.compose.resources.stringResource(resource), formatArgs)

@Composable
fun pluralStringResource(resource: PluralStringResource, quantity: Int): String =
    org.jetbrains.compose.resources.pluralStringResource(resource, quantity)

@Composable
fun pluralStringResource(resource: PluralStringResource, quantity: Int, vararg formatArgs: Any): String =
    formatResourceString(org.jetbrains.compose.resources.pluralStringResource(resource, quantity), formatArgs)

suspend fun getString(resource: StringResource, vararg formatArgs: Any): String =
    formatResourceString(org.jetbrains.compose.resources.getString(resource), formatArgs)

suspend fun getPluralString(resource: PluralStringResource, quantity: Int, vararg formatArgs: Any): String =
    formatResourceString(org.jetbrains.compose.resources.getPluralString(resource, quantity), formatArgs)

/** Android-style String.format; returns [pattern] untouched when there are no args. */
internal expect fun formatResourceString(pattern: String, args: Array<out Any>): String
