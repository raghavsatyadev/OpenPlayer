package io.github.raghavsatyadev.openplayer.utils.parcel

/**
 * Parcelize for shared models. On Android this is a real annotation registered with the
 * parcelize compiler plugin (additionalAnnotation in composeApp/build.gradle.kts), so the
 * generated Parcel code is the same as with @Parcelize. Other targets ignore it.
 */
@OptIn(ExperimentalMultiplatform::class)
@OptionalExpectation
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
expect annotation class CommonParcelize()

/** android.os.Parcelable on Android, an empty marker elsewhere. */
expect interface CommonParcelable
