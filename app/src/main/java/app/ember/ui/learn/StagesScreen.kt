package app.ember.ui.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ember.R
import app.ember.domain.Stages
import app.ember.ui.EmberData
import app.ember.ui.components.TopBar
import app.ember.ui.content
import app.ember.ui.stageColor
import app.ember.ui.theme.U
import app.ember.ui.theme.UType

/** The whole journey of a fast, hour by hour; opens at [focus] and marks where the current fast is. */
@Composable
fun StagesScreen(focus: String, data: EmberData, onBack: () -> Unit) {
    val text = content()
    val current = data.active?.let { Stages.at(it.hours(data.now)) }
    val state = rememberLazyListState()
    LaunchedEffect(focus) {
        val i = Stages.all.indexOfFirst { it.id == focus }
        if (i > 0) state.scrollToItem(i + 1)
    }
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.stages), onBack)
        LazyColumn(state = state, contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 32.dp)) {
            item {
                Text(stringResource(R.string.stages_intro), style = UType.Sub, modifier = Modifier.padding(start = 4.dp, bottom = 16.dp))
            }
            itemsIndexed(Stages.all) { i, st ->
                val t = text.stage(st.id)
                val color = stageColor(st.index)
                Row(Modifier.height(IntrinsicSize.Min)) {
                    StageRail(st.index, i == Stages.all.lastIndex, Modifier.padding(top = 6.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.padding(bottom = 22.dp)) {
                        Row {
                            Text(stringResource(R.string.hours_plus, st.hour), style = UType.Strong.copy(color = color, fontSize = 14.sp))
                            if (current?.id == st.id) {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    stringResource(R.string.you_are_here), style = UType.Small.copy(color = U.c.onAccent),
                                    modifier = Modifier.clip(U.RSm).background(color).padding(horizontal = 8.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(t.title, style = UType.Title.copy(fontSize = 21.sp))
                        Spacer(Modifier.height(6.dp))
                        Text(t.body, style = UType.Body)
                        Spacer(Modifier.height(10.dp))
                        Column(Modifier.clip(U.RMd).background(color.copy(alpha = if (U.c.dark) 0.16f else 0.09f)).padding(14.dp)) {
                            Text(stringResource(R.string.what_helps).uppercase(), style = UType.Eyebrow.copy(color = color))
                            t.tips.forEach { tip ->
                                Row(Modifier.padding(top = 6.dp)) {
                                    Text("•", style = UType.Body.copy(color = color))
                                    Spacer(Modifier.width(8.dp))
                                    Text(tip, style = UType.Body.copy(fontSize = 14.sp, lineHeight = 20.sp))
                                }
                            }
                        }
                    }
                }
            }
            item {
                Text(stringResource(R.string.disclaimer), style = UType.Small, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}
