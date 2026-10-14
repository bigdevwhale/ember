package app.ember.content

import java.util.Locale

/** What the body is doing in one stretch of a fast. Ids and start hours live in [app.ember.domain.Stages]. */
data class StageText(
    val title: String,
    /** One line for the timer screen. */
    val short: String,
    /** Two or three short paragraphs separated by blank lines. */
    val body: String,
    /** What helps during this stage. */
    val tips: List<String>,
)

/**
 * A short read. Each block is a paragraph; "## " starts a heading, "• " a bullet,
 * "> " a highlighted note.
 */
data class Article(
    val id: String,
    val title: String,
    val lead: String,
    val minutes: Int,
    val blocks: List<String>,
)

/** "Right now" line shown from [hour] of a fast until the next note. */
data class HourNote(val hour: Int, val text: String)

data class HungerTip(val title: String, val text: String)

class Content(
    /** Keyed by stage id: digest, settle, switch, burn, ketosis, autophagy, growth, renewal. */
    val stages: Map<String, StageText>,
    val articles: List<Article>,
    /** One is shown per day. */
    val tips: List<String>,
    /** Sorted by hour, first at hour 0. */
    val hours: List<HourNote>,
    val hunger: List<HungerTip>,
    /** Said when a fast reaches its goal. */
    val cheers: List<String>,
) {
    fun stage(id: String): StageText = stages.getValue(id)

    fun article(id: String): Article? = articles.firstOrNull { it.id == id }

    fun note(hours: Double): String = this.hours.lastOrNull { it.hour <= hours }?.text ?: this.hours.first().text

    fun tip(day: Long): String = tips[Math.floorMod(day, tips.size.toLong()).toInt()]

    fun cheer(seed: Long): String = cheers[Math.floorMod(seed, cheers.size.toLong()).toInt()]

    companion object {
        fun of(locale: Locale): Content = if (locale.language == "ru") ContentRu else ContentEn
    }
}
