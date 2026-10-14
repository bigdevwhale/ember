package app.ember.ui.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ember.R
import app.ember.ui.Go
import app.ember.ui.components.IconDisc
import app.ember.ui.components.SectionTitle
import app.ember.ui.components.TopBar
import app.ember.ui.articleIcon
import app.ember.ui.content
import app.ember.ui.theme.U
import app.ember.ui.theme.UType

@Composable
fun ArticleScreen(id: String, go: Go) {
    val text = content()
    val a = text.article(id) ?: return
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar("", { go.back() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 22.dp).navigationBarsPadding().padding(bottom = 32.dp)) {
            IconDisc(articleIcon(a.id), 52.dp)
            Spacer(Modifier.height(14.dp))
            Text(a.title, style = UType.Display.copy(fontSize = 28.sp, lineHeight = 34.sp))
            Spacer(Modifier.height(8.dp))
            Text(a.lead, style = UType.Sub.copy(fontSize = 16.sp, lineHeight = 23.sp))
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.min_read, a.minutes), style = UType.Small)
            Spacer(Modifier.height(10.dp))
            a.blocks.forEach { b ->
                when {
                    b.startsWith("## ") -> Text(b.removePrefix("## "), style = UType.Strong.copy(fontSize = 18.sp), modifier = Modifier.padding(top = 18.dp, bottom = 2.dp))
                    b.startsWith("• ") -> Row(Modifier.padding(top = 6.dp, start = 4.dp)) {
                        Text("•", style = UType.Body.copy(color = U.c.accent))
                        Spacer(Modifier.width(10.dp))
                        Text(b.removePrefix("• "), style = UType.Body)
                    }
                    b.startsWith("> ") -> Text(
                        b.removePrefix("> "), style = UType.Quote.copy(fontSize = 17.sp, lineHeight = 25.sp),
                        modifier = Modifier.padding(top = 14.dp).fillMaxWidth().clip(U.RMd).background(U.c.accentSoft).padding(16.dp),
                    )
                    else -> Text(b, style = UType.Body.copy(fontSize = 16.sp, lineHeight = 25.sp), modifier = Modifier.padding(top = 10.dp))
                }
            }
            val i = text.articles.indexOf(a)
            val more = (1..2).map { text.articles[(i + it) % text.articles.size] }
            SectionTitle(stringResource(R.string.read_next))
            more.forEach { m -> ArticleCard(m) { go.back(); go.article(m.id) } }
        }
    }
}
