package io.github.raghavsatyadev.openplayer.data.playlist.rules

/** How a rule playlist combines its conditions. */
enum class SmartMatchMode { ALL, ANY }

private val TEXT_OPERATORS = listOf(
    SmartRuleOperator.CONTAINS, SmartRuleOperator.NOT_CONTAINS, SmartRuleOperator.EQUALS
)

/** What a condition looks at, and the operators that make sense for it. */
enum class SmartRuleField(val operators: List<SmartRuleOperator>) {
    TITLE(TEXT_OPERATORS),
    ARTIST(TEXT_OPERATORS),
    ALBUM(TEXT_OPERATORS),
    GENRE(TEXT_OPERATORS),
    FOLDER_PATH(TEXT_OPERATORS),
    YEAR(listOf(SmartRuleOperator.BETWEEN)),

    /** Range in seconds. */
    DURATION(listOf(SmartRuleOperator.BETWEEN)),
    PLAY_COUNT(listOf(SmartRuleOperator.AT_LEAST, SmartRuleOperator.AT_MOST)),
    LAST_PLAYED(listOf(SmartRuleOperator.WITHIN_DAYS, SmartRuleOperator.NOT_WITHIN_DAYS)),
    DATE_ADDED(listOf(SmartRuleOperator.WITHIN_DAYS, SmartRuleOperator.NOT_WITHIN_DAYS)),
    RATING(listOf(SmartRuleOperator.AT_LEAST, SmartRuleOperator.AT_MOST)),
    MOOD(listOf(SmartRuleOperator.HAS, SmartRuleOperator.HAS_NOT)),
    TAG(listOf(SmartRuleOperator.HAS, SmartRuleOperator.HAS_NOT)),
    FAVORITE(listOf(SmartRuleOperator.IS, SmartRuleOperator.IS_NOT));

    val usesText: Boolean
        get() = when (this) {
            TITLE, ARTIST, ALBUM, GENRE, FOLDER_PATH, MOOD, TAG -> true
            else -> false
        }
    val usesRange: Boolean get() = this == YEAR || this == DURATION
    val usesNumber: Boolean
        get() = this == PLAY_COUNT || this == LAST_PLAYED || this == DATE_ADDED || this == RATING
    val defaultOperator: SmartRuleOperator get() = operators.first()
}

enum class SmartRuleOperator {
    CONTAINS, NOT_CONTAINS, EQUALS,
    BETWEEN,
    AT_LEAST, AT_MOST,
    WITHIN_DAYS, NOT_WITHIN_DAYS,
    HAS, HAS_NOT,
    IS, IS_NOT
}

/**
 * One row of the rule editor. [text] is used by text, mood and tag fields, [min]/[max] by range
 * fields (year, duration in seconds), [number] by count, day and rating fields. [id] only keeps
 * rows apart in the editor.
 */
data class SmartCondition(
    val id: Long = 0L,
    val field: SmartRuleField = SmartRuleField.TITLE,
    val operator: SmartRuleOperator = field.defaultOperator,
    val text: String = "",
    val min: Long = 0L,
    val max: Long = 0L,
    val number: Long = 0L,
) {
    /** False for a row that has nothing to match yet, such as a text field left empty. */
    val isComplete: Boolean
        get() = when {
            this.field.usesText -> text.isNotBlank()
            this.field.usesRange -> max >= min
            else -> true
        }
}

enum class SmartSortField {
    TITLE, ARTIST, ALBUM, YEAR, DATE_ADDED, PLAY_COUNT, LAST_PLAYED, RATING, RANDOM
}

data class SmartSort(val field: SmartSortField, val descending: Boolean = false)

/** A user-defined rule playlist: conditions, how they combine, an optional order and size cap. */
data class SmartRuleDefinition(
    val matchMode: SmartMatchMode = SmartMatchMode.ALL,
    val conditions: List<SmartCondition> = emptyList(),
    val sort: SmartSort? = null,
    val limit: Int? = null,
)
