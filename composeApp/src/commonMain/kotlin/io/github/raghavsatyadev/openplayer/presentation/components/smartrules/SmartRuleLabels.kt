package io.github.raghavsatyadev.openplayer.presentation.components.smartrules

import androidx.compose.runtime.Composable
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartRuleField
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartRuleOperator
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartSortField

@Composable
internal fun smartRuleFieldLabel(field: SmartRuleField): String = stringResource(
    when (field) {
        SmartRuleField.TITLE -> Res.string.smart_rules_field_title
        SmartRuleField.ARTIST -> Res.string.smart_rules_field_artist
        SmartRuleField.ALBUM -> Res.string.smart_rules_field_album
        SmartRuleField.GENRE -> Res.string.smart_rules_field_genre
        SmartRuleField.FOLDER_PATH -> Res.string.smart_rules_field_folder_path
        SmartRuleField.YEAR -> Res.string.smart_rules_field_year
        SmartRuleField.DURATION -> Res.string.smart_rules_field_duration
        SmartRuleField.PLAY_COUNT -> Res.string.smart_rules_field_play_count
        SmartRuleField.LAST_PLAYED -> Res.string.smart_rules_field_last_played
        SmartRuleField.DATE_ADDED -> Res.string.smart_rules_field_date_added
        SmartRuleField.RATING -> Res.string.smart_rules_field_rating
        SmartRuleField.MOOD -> Res.string.smart_rules_field_mood
        SmartRuleField.TAG -> Res.string.smart_rules_field_tag
        SmartRuleField.FAVORITE -> Res.string.smart_rules_field_favorite
    }
)

@Composable
internal fun smartRuleOperatorLabel(operator: SmartRuleOperator): String = stringResource(
    when (operator) {
        SmartRuleOperator.CONTAINS -> Res.string.smart_rules_op_contains
        SmartRuleOperator.NOT_CONTAINS -> Res.string.smart_rules_op_not_contains
        SmartRuleOperator.EQUALS -> Res.string.smart_rules_op_equals
        SmartRuleOperator.BETWEEN -> Res.string.smart_rules_op_between
        SmartRuleOperator.AT_LEAST -> Res.string.smart_rules_op_at_least
        SmartRuleOperator.AT_MOST -> Res.string.smart_rules_op_at_most
        SmartRuleOperator.WITHIN_DAYS -> Res.string.smart_rules_op_within_days
        SmartRuleOperator.NOT_WITHIN_DAYS -> Res.string.smart_rules_op_not_within_days
        SmartRuleOperator.HAS -> Res.string.smart_rules_op_has
        SmartRuleOperator.HAS_NOT -> Res.string.smart_rules_op_has_not
        SmartRuleOperator.IS -> Res.string.smart_rules_op_is
        SmartRuleOperator.IS_NOT -> Res.string.smart_rules_op_is_not
    }
)

@Composable
internal fun smartSortFieldLabel(field: SmartSortField): String = stringResource(
    when (field) {
        SmartSortField.TITLE -> Res.string.smart_rules_sort_title
        SmartSortField.ARTIST -> Res.string.smart_rules_sort_artist
        SmartSortField.ALBUM -> Res.string.smart_rules_sort_album
        SmartSortField.YEAR -> Res.string.smart_rules_sort_year
        SmartSortField.DATE_ADDED -> Res.string.smart_rules_sort_date_added
        SmartSortField.PLAY_COUNT -> Res.string.smart_rules_sort_play_count
        SmartSortField.LAST_PLAYED -> Res.string.smart_rules_sort_last_played
        SmartSortField.RATING -> Res.string.smart_rules_sort_rating
        SmartSortField.RANDOM -> Res.string.smart_rules_sort_random
    }
)
