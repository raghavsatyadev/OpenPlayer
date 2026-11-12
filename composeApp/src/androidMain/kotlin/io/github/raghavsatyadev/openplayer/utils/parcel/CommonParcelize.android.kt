package io.github.raghavsatyadev.openplayer.utils.parcel

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.BINARY)
actual annotation class CommonParcelize

actual typealias CommonParcelable = android.os.Parcelable
