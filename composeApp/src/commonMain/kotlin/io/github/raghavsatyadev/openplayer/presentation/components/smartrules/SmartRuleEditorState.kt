package io.github.raghavsatyadev.openplayer.presentation.components.smartrules

import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartCondition
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartMatchMode
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartRuleDefinition
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartRuleField
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartRuleOperator
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartSort
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartSortField

/** What the rule editor shows and edits. Immutable: every change returns a new state. */
data class SmartRuleEditorState(
    val name: String = "",
    val matchMode: SmartMatchMode = SmartMatchMode.ALL,
    val conditions: List<SmartCondition> = emptyList(),
    val sort: SmartSort? = null,
    /** Kept as typed so a half-typed number isn't rewritten under the cursor. */
    val limitText: String = "",
    private val nextId: Long = conditions.maxOfOrNull { it.id + 1 } ?: 1L,
) {
    val limit: Int? get() = limitText.toIntOrNull()?.takeIf { it > 0 }

    val canSave: Boolean
        get() = name.isNotBlank() && conditions.isNotEmpty() && conditions.all { it.isComplete }

    /** The saved form; rows that aren't complete yet are left out. */
    fun toDefinition(): SmartRuleDefinition = SmartRuleDefinition(
        matchMode = matchMode,
        conditions = conditions.filter { it.isComplete },
        sort = sort,
        limit = limit,
    )

    fun addCondition(): SmartRuleEditorState =
        copy(conditions = conditions + defaultCondition(nextId), nextId = nextId + 1)

    fun removeCondition(id: Long): SmartRuleEditorState =
        copy(conditions = conditions.filterNot { it.id == id })

    fun updateCondition(id: Long, change: (SmartCondition) -> SmartCondition): SmartRuleEditorState =
        copy(conditions = conditions.map { if (it.id == id) change(it) else it })

    /** Switching field resets the operator and value, which don't carry over between fields. */
    fun changeField(id: Long, field: SmartRuleField): SmartRuleEditorState =
        updateCondition(id) { old -> if (old.field == field) old else defaultCondition(old.id, field) }

    fun setSortField(field: SmartSortField?): SmartRuleEditorState = copy(
        sort = field?.let { SmartSort(it, descending = sort?.descending ?: defaultDescending(it)) }
    )

    fun toggleSortDirection(): SmartRuleEditorState =
        copy(sort = sort?.let { it.copy(descending = !it.descending) })

    companion object {
        fun from(name: String, definition: SmartRuleDefinition): SmartRuleEditorState {
            val rows = definition.conditions.mapIndexed { index, c -> c.copy(id = index + 1L) }
            return SmartRuleEditorState(
                name = name,
                matchMode = definition.matchMode,
                conditions = rows,
                sort = definition.sort,
                limitText = definition.limit?.toString().orEmpty(),
            )
        }

        fun defaultCondition(id: Long, field: SmartRuleField = SmartRuleField.TITLE): SmartCondition =
            when (field) {
                SmartRuleField.YEAR -> SmartCondition(id, field, SmartRuleOperator.BETWEEN, min = 2000, max = 2025)
                SmartRuleField.DURATION -> SmartCondition(id, field, SmartRuleOperator.BETWEEN, min = 0, max = 600)
                SmartRuleField.PLAY_COUNT -> SmartCondition(id, field, number = 1)
                SmartRuleField.LAST_PLAYED, SmartRuleField.DATE_ADDED -> SmartCondition(id, field, number = 30)
                SmartRuleField.RATING -> SmartCondition(id, field, number = 4)
                else -> SmartCondition(id, field)
            }

        /** Counts and dates read best biggest-first, text and years smallest-first. */
        fun defaultDescending(field: SmartSortField): Boolean = when (field) {
            SmartSortField.DATE_ADDED, SmartSortField.PLAY_COUNT, SmartSortField.LAST_PLAYED,
            SmartSortField.RATING -> true
            else -> false
        }
    }
}
