package io.github.raghavsatyadev.openplayer.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.github.raghavsatyadev.openplayer.resources.*
import io.github.raghavsatyadev.openplayer.resources.stringResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun HomeOptionsBottomSheet(
    onNavigateToMashup: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(bottom = 32.dp)) {
        ListItem(
            content = { Text(stringResource(Res.string.home_option_dj_mashup)) },
            leadingContent = {
                Icon(
                    painter = painterResource(Res.drawable.rounded_instant_mix_24),
                    contentDescription = stringResource(Res.string.home_option_dj_mashup)
                )
            },
            modifier = Modifier
                .padding(20.dp)
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onNavigateToMashup)
        )
    }
}