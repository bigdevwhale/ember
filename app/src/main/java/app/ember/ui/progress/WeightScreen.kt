package app.ember.ui.progress

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.ember.R
import app.ember.container
import app.ember.data.WeightEntry
import app.ember.ui.EmberData
import app.ember.ui.components.PrimaryButton
import app.ember.ui.components.SectionTitle
import app.ember.ui.components.TopBar
import app.ember.ui.components.UCard
import app.ember.ui.components.UTextField
import app.ember.ui.components.WeightChart
import app.ember.ui.toKg
import app.ember.ui.theme.U
import app.ember.ui.theme.UType
import app.ember.ui.unitLabel
import app.ember.ui.weightLabel
import app.ember.ui.weightValue
import app.ember.ui.whenLabel
import kotlinx.coroutines.launch

@Composable
fun WeightScreen(data: EmberData, onBack: () -> Unit) {
    val container = LocalContext.current.container
    val s = data.settings
    val units = s.units
    val w = data.weights
    var input by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf(if (s.goalKg > 0) weightValue(s.goalKg, units) else "") }
    var height by remember { mutableStateOf(if (s.heightCm > 0) "${s.heightCm}" else "") }
    fun parse(t: String) = t.replace(',', '.').trim().toDoubleOrNull()
    val value = parse(input)?.takeIf { it in 20.0..700.0 }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.weight), onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).navigationBarsPadding().padding(bottom = 24.dp)) {
            if (w.isNotEmpty()) {
                UCard {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.current).uppercase(), style = UType.Eyebrow)
                            Text(weightLabel(w.last().kg, units), style = UType.Title)
                        }
                        val bmi = if (s.heightCm > 0) w.last().kg / ((s.heightCm / 100.0) * (s.heightCm / 100.0)) else null
                        if (bmi != null) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("BMI", style = UType.Eyebrow)
                                Text("%.1f".format(bmi), style = UType.Number)
                            }
                        }
                    }
                    if (s.goalKg > 0) {
                        val left = w.last().kg - s.goalKg
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (left > 0.05) stringResource(R.string.to_goal, weightLabel(left, units)) else stringResource(R.string.goal_met),
                            style = UType.Muted.copy(color = U.c.sage),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    WeightChart(w.map { it.at to it.kg }, s.goalKg.takeIf { it > 0 }, Modifier.fillMaxWidth().height(160.dp))
                }
            }

            SectionTitle(stringResource(R.string.log_weight))
            Row(verticalAlignment = Alignment.CenterVertically) {
                UTextField(input, { input = it.take(6) }, Modifier.weight(1f), placeholder = w.lastOrNull()?.let { weightValue(it.kg, units) } ?: "70.0", number = true, suffix = unitLabel(units))
                Spacer(Modifier.width(10.dp))
                PrimaryButton(stringResource(R.string.save), {
                    val kg = toKg(value!!, units)
                    container.write { dao().insertWeight(WeightEntry(at = System.currentTimeMillis(), kg = kg)) }
                    input = ""
                }, enabled = value != null)
            }

            SectionTitle(stringResource(R.string.weight_goal))
            Row {
                UTextField(goal, { goal = it.take(6) }, Modifier.weight(1f), placeholder = stringResource(R.string.goal), number = true, suffix = unitLabel(units))
                Spacer(Modifier.width(10.dp))
                UTextField(height, { height = it.filter(Char::isDigit).take(3) }, Modifier.weight(1f), placeholder = stringResource(R.string.height), number = true, suffix = stringResource(R.string.cm))
            }
            Spacer(Modifier.height(8.dp))
            PrimaryButton(stringResource(R.string.save), {
                val g = parse(goal)?.let { toKg(it, units) } ?: 0.0
                val h = height.toIntOrNull()?.takeIf { it in 100..250 } ?: 0
                container.scope.launch { container.settings.update { it.copy(goalKg = g, heightCm = h) } }
            }, Modifier.fillMaxWidth(), color = U.c.sage)

            if (w.isNotEmpty()) {
                SectionTitle(stringResource(R.string.history))
                UCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp)) {
                    w.reversed().forEachIndexed { i, e ->
                        if (i > 0) HorizontalDivider(color = U.c.line)
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(weightLabel(e.kg, units), style = UType.Strong.copy(fontSize = 15.sp), modifier = Modifier.weight(1f))
                            Text(whenLabel(e.at), style = UType.Muted)
                            IconButton(onClick = { container.write { dao().deleteWeight(e) } }) {
                                Icon(Icons.Rounded.Close, stringResource(R.string.delete), tint = U.c.faint)
                            }
                        }
                    }
                }
            }
        }
    }
}
