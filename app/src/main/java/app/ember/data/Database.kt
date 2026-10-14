package app.ember.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** One fast. While [end] is null it is the fast in progress; there is at most one. */
@Entity(tableName = "fasts")
data class Fast(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val start: Long,
    val end: Long? = null,
    /** The goal set when the fast began. */
    val goalMinutes: Int,
    /** Plan id from [app.ember.domain.Plans], or "custom". */
    val plan: String,
    /** 1 (rough) .. 5 (great), asked when the fast ends. */
    val mood: Int? = null,
    val note: String = "",
) {
    fun minutes(now: Long = System.currentTimeMillis()): Long = ((end ?: now) - start).coerceAtLeast(0) / 60_000
    fun hours(now: Long = System.currentTimeMillis()): Double = ((end ?: now) - start).coerceAtLeast(0) / 3_600_000.0
    val goalReached: Boolean get() = end != null && minutes(end) >= goalMinutes
}

@Entity(tableName = "weights")
data class WeightEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long,
    /** Always stored in kilograms; shown in the user's units. */
    val kg: Double,
)

/** Glasses of water drunk on one day ([day] is the epoch day). */
@Entity(tableName = "water")
data class WaterDay(
    @PrimaryKey val day: Long,
    val glasses: Int,
)

/** How the body feels during a fast. Scores are 1..5. */
@Entity(tableName = "checkins")
data class CheckIn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long,
    val hunger: Int,
    val energy: Int,
    val mood: Int,
    val note: String = "",
)

class Snapshot(
    val fasts: List<Fast>,
    val weights: List<WeightEntry>,
    val water: List<WaterDay>,
    val checkIns: List<CheckIn>,
)

@Dao
abstract class EmberDao {
    // Fasts
    @Query("SELECT * FROM fasts ORDER BY start")
    abstract fun observeFasts(): Flow<List<Fast>>

    @Query("SELECT * FROM fasts ORDER BY start")
    abstract suspend fun fasts(): List<Fast>

    @Query("SELECT * FROM fasts WHERE `end` IS NULL ORDER BY start DESC LIMIT 1")
    abstract suspend fun active(): Fast?

    @Query("SELECT * FROM fasts WHERE id = :id")
    abstract suspend fun fast(id: Long): Fast?

    @Insert
    abstract suspend fun insertFast(f: Fast): Long

    @Update
    abstract suspend fun updateFast(f: Fast)

    @Delete
    abstract suspend fun deleteFast(f: Fast)

    // Weight
    @Query("SELECT * FROM weights ORDER BY at")
    abstract fun observeWeights(): Flow<List<WeightEntry>>

    @Query("SELECT * FROM weights ORDER BY at")
    abstract suspend fun weights(): List<WeightEntry>

    @Insert
    abstract suspend fun insertWeight(w: WeightEntry): Long

    @Delete
    abstract suspend fun deleteWeight(w: WeightEntry)

    // Water
    @Query("SELECT * FROM water ORDER BY day")
    abstract fun observeWater(): Flow<List<WaterDay>>

    @Query("SELECT * FROM water ORDER BY day")
    abstract suspend fun water(): List<WaterDay>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun setWater(w: WaterDay)

    // Check-ins
    @Query("SELECT * FROM checkins ORDER BY at")
    abstract fun observeCheckIns(): Flow<List<CheckIn>>

    @Query("SELECT * FROM checkins ORDER BY at")
    abstract suspend fun checkIns(): List<CheckIn>

    @Insert
    abstract suspend fun insertCheckIn(c: CheckIn): Long

    @Delete
    abstract suspend fun deleteCheckIn(c: CheckIn)

    // Backup
    @Transaction
    open suspend fun snapshot() = Snapshot(fasts(), weights(), water(), checkIns())

    @Query("DELETE FROM fasts") protected abstract suspend fun clearFasts()
    @Query("DELETE FROM weights") protected abstract suspend fun clearWeights()
    @Query("DELETE FROM water") protected abstract suspend fun clearWater()
    @Query("DELETE FROM checkins") protected abstract suspend fun clearCheckIns()

    @Insert protected abstract suspend fun insertFasts(l: List<Fast>)
    @Insert protected abstract suspend fun insertWeights(l: List<WeightEntry>)
    @Insert protected abstract suspend fun insertWaters(l: List<WaterDay>)
    @Insert protected abstract suspend fun insertCheckIns(l: List<CheckIn>)

    @Transaction
    open suspend fun replaceAll(s: Snapshot) {
        clearFasts(); clearWeights(); clearWater(); clearCheckIns()
        insertFasts(s.fasts); insertWeights(s.weights); insertWaters(s.water); insertCheckIns(s.checkIns)
    }
}

@Database(
    entities = [Fast::class, WeightEntry::class, WaterDay::class, CheckIn::class],
    version = 1,
    exportSchema = true,
)
abstract class EmberDatabase : RoomDatabase() {
    abstract fun dao(): EmberDao

    companion object {
        fun create(context: Context): EmberDatabase =
            Room.databaseBuilder(context.applicationContext, EmberDatabase::class.java, "ember.db").build()
    }
}
