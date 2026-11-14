package io.github.raghavsatyadev.openplayer.data.preferences

import androidx.compose.runtime.Composable
import io.github.raghavsatyadev.openplayer.resources.*
import org.jetbrains.compose.resources.StringResource

enum class AppLanguage(val tag: String, val labelRes: StringResource) {
    SYSTEM("", Res.string.setcat_language_system),
    ENGLISH("en", Res.string.setcat_language_english),
    ARABIC("ar", Res.string.setcat_language_arabic),
    GERMAN("de", Res.string.setcat_language_german),
    SPANISH("es", Res.string.setcat_language_spanish),
    FRENCH("fr", Res.string.setcat_language_french),
    INDONESIAN("in", Res.string.setcat_language_indonesian),
    ITALIAN("it", Res.string.setcat_language_italian),
    KOREAN("ko", Res.string.setcat_language_korean),
    NORWEGIAN_BOKMAL("nb", Res.string.setcat_language_norwegian_bokmal),
    RUSSIAN("ru", Res.string.setcat_language_russian),
    TURKISH("tr", Res.string.setcat_language_turkish),
    ZH_RCN("zh-CN", Res.string.language_zh_rCN),
    VIETNAMESE("vi", Res.string.setcat_language_vietnamese);

    companion object {
        val supportedLanguageTags: Set<String> = entries.map { it.tag }.toSet()

        @Composable
        fun getLanguageOptions(): Map<String, String> {
            val systemOption = SYSTEM.tag to stringResource(SYSTEM.labelRes)
            val otherOptions = entries
                .filter { it != SYSTEM }
                .map { it.tag to stringResource(it.labelRes) }
                .sortedBy { it.second.lowercase() }

            val result = LinkedHashMap<String, String>()
            result[systemOption.first] = systemOption.second
            for (option in otherOptions) {
                result[option.first] = option.second
            }
            return result
        }

        fun normalize(languageTag: String?): String {
            val normalized = languageTag?.trim() ?: return SYSTEM.tag
            return entries.find { it.tag.equals(normalized, ignoreCase = true) }?.tag ?: SYSTEM.tag
        }
    }
}
