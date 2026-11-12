package io.github.raghavsatyadev.openplayer.presentation.components.smartrules

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.raghavsatyadev.openplayer.R
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartRuleField
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartRuleOperator
import io.github.raghavsatyadev.openplayer.data.playlist.rules.SmartSortField

@Composable
internal fun smartRuleFieldLabel(field: SmartRuleField): String = stringResource(
    when (field) {
        SmartRuleField.TITLE -> R.string.smart_rules_field_title
        SmartRuleField.ARTIST -> R.string.smart_rules_field_artist
        SmartRuleField.ALBUM -> R.string.smart_rules_field_album
        SmartRuleField.GENRE -> R.string.smart_rules_field_genre
        SmartRuleField.FOLDER_PATH -> R.string.smart_rules_field_folder_path
        SmartRuleField.YEAR -> R.string.smart_rules_field_year
        SmartRuleField.DURATION -> R.string.smart_rules_field_duration
        SmartRuleField.PLAY_COUNT -> R.string.smart_rules_field_play_count
        SmartRuleField.LAST_PLAYED -> R.string.smart_rules_field_last_played
        SmartRuleField.DATE_ADDED -> R.string.smart_rules_field_date_added
        SmartRuleField.RATING -> R.string.smart_rules_field_rating
        SmartRuleField.MOOD -> R.string.smart_rules_field_mood
        SmartRuleField.TAG -> R.string.smart_rules_field_tag
        SmartRuleField.FAVORITE -> R.string.smart_rules_field_favorite
    }
)

@Composable
internal fun smartRuleOperatorLabel(operator: SmartRuleOperator): String = stringResource(
    when (operator) {
        SmartRuleOperator.CONTAINS -> R.string.smart_rules_op_contains
        SmartRuleOperator.NOT_CONTAINS -> R.string.smart_rules_op_not_contains
        SmartRuleOperator.EQUALS -> R.string.smart_rules_op_equals
        SmartRuleOperator.BETWEEN -> R.string.smart_rules_op_between
        SmartRuleOperator.AT_LEAST -> R.string.smart_rules_op_at_least
        SmartRuleOperator.AT_MOST -> R.string.smart_rules_op_at_most
        SmartRuleOperator.WITHIN_DAYS -> R.string.smart_rules_op_within_days
        SmartRuleOperator.NOT_WITHIN_DAYS -> R.string.smart_rules_op_not_within_days
        SmartRuleOperator.HAS -> R.string.smart_rules_op_has
        SmartRuleOperator.HAS_NOT -> R.string.smart_rules_op_has_not
        SmartRuleOperator.IS -> R.string.smart_rules_op_is
        SmartRuleOperator.IS_NOT -> R.string.smart_rules_op_is_not
    }
)

@Composable
internal fun smartSortFieldLabel(field: SmartSortField): String = stringResource(
    when (field) {
        SmartSortField.TITLE -> R.string.smart_rules_sort_title
        SmartSortField.ARTIST -> R.string.smart_rules_sort_artist
        SmartSortField.ALBUM -> R.string.smart_rules_sort_album
        SmartSortField.YEAR -> R.string.smart_rules_sort_year
        SmartSortField.DATE_ADDED -> R.string.smart_rules_sort_date_added
        SmartSortField.PLAY_COUNT -> R.string.smart_rules_sort_play_count
        SmartSortField.LAST_PLAYED -> R.string.smart_rules_sort_last_played
        SmartSortField.RATING -> R.string.smart_rules_sort_rating
        SmartSortField.RANDOM -> R.string.smart_rules_sort_random
    }
)
