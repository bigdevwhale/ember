package app.ember.ui.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NoFood
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ember.R
import app.ember.content.Article
import app.ember.domain.Stages
import app.ember.ui.EmberData
import app.ember.ui.Go
import app.ember.ui.articleIcon
import app.ember.ui.components.IconDisc
import app.ember.ui.components.SectionTitle
import app.ember.ui.components.UCard
import app.ember.ui.content
import app.ember.ui.stageColor
import app.ember.ui.theme.U
import app.ember.ui.theme.UType

@Composable
fun LearnTab(data: EmberData, go: Go) {
    val text = content()
    val current = data.active?.let { Stages.at(it.hours(data.now)) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(bottom = 24.dp)) {
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.tab_learn), style = UType.Title, modifier = Modifier.padding(horizontal = 18.dp))
        Text(stringResource(R.string.learn_sub), style = UType.Sub, modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp))

        SectionTitle(stringResource(R.string.stages), Modifier.padding(horizontal = 18.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stages.all.forEach { st ->
                val here = current?.id == st.id
                val color = stageColor(st.index)
                Column(
                    Modifier
                        .width(150.dp)
                        .height(150.dp)
                        .clip(U.RMd)
                        .background(color.copy(alpha = if (U.c.dark) 0.18f else 0.11f))
                        .then(if (here) Modifier.border(2.dp, color, U.RMd) else Modifier)
                        .clickable { go.stages(st.id) }
                        .padding(14.dp),
                ) {
                    Text(stringResource(R.string.hours_plus, st.hour), style = UType.Strong.copy(color = color, fontSize = 15.sp))
                    Spacer(Modifier.height(6.dp))
                    Text(text.stage(st.id).title, style = UType.Strong)
                    Spacer(Modifier.weight(1f))
                    Text(
                        if (here) stringResource(R.string.you_are_here) else text.stage(st.id).short,
                        style = UType.Muted.copy(fontSize = 12.sp, color = if (here) color else U.c.muted), maxLines = 3, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Column(Modifier.padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(16.dp))
            UCard(onClick = { go.hunger() }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconDisc(Icons.Rounded.NoFood, 44.dp, U.c.rose, U.c.roseSoft)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.hunger_sos), style = UType.Strong)
                        Text(stringResource(R.string.hunger_sos_text), style = UType.Muted)
                    }
                }
            }
            SectionTitle(stringResource(R.string.articles))
            text.articles.forEach { a -> ArticleCard(a) { go.article(a.id) } }
        }
    }
}

@Composable
fun ArticleCard(a: Article, onClick: () -> Unit) {
    UCard(Modifier.padding(bottom = 10.dp), onClick = onClick) {
        Row {
            IconDisc(articleIcon(a.id), 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(a.title, style = UType.Strong)
                Spacer(Modifier.height(2.dp))
                Text(a.lead, style = UType.Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.min_read, a.minutes), style = UType.Small)
            }
        }
    }
}

/** A coloured rail used beside stage cards. */
@Composable
fun StageRail(index: Int, last: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(stageColor(index)))
        if (!last) Box(Modifier.width(2.dp).weight(1f).background(stageColor(index).copy(alpha = 0.3f)))
    }
}
