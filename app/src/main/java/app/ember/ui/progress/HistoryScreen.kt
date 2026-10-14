package app.ember.ui.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.ember.R
import app.ember.ui.EmberData
import app.ember.ui.Go
import app.ember.ui.components.SectionTitle
import app.ember.ui.components.TopBar
import app.ember.ui.locale
import app.ember.ui.theme.U
import app.ember.ui.theme.UType
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Every finished fast, newest first, by month. */
@Composable
fun HistoryScreen(data: EmberData, go: Go) {
    val byMonth = data.stats.done.reversed().groupBy { YearMonth.from(Instant.ofEpochMilli(it.start).atZone(ZoneId.systemDefault())) }
    val fmt = DateTimeFormatter.ofPattern("LLLL yyyy", locale())
    val loc = locale()
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.all_fasts), { go.back() }) {
            IconButton(onClick = { go.fast(0) }) { Icon(Icons.Rounded.Add, stringResource(R.string.add_fast), tint = U.c.accent) }
        }
        LazyColumn(contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 32.dp)) {
            if (byMonth.isEmpty()) item { Text(stringResource(R.string.no_fasts), style = UType.Muted, modifier = Modifier.padding(8.dp)) }
            byMonth.forEach { (m, list) ->
                item(key = "m$m") {
                    SectionTitle(fmt.format(m).replaceFirstChar { it.titlecase(loc) } + " · " + list.size)
                }
                items(list, key = { it.id }) { f -> FastRow(f) { go.fast(f.id) } }
            }
        }
    }
}
