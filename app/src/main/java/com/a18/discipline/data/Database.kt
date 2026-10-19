package com.a18.discipline.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// JSON retains the shared backup shape; date/kind/ID remain indexed for native queries.
@Entity(tableName = "entries", primaryKeys = ["kind", "id"], indices = [Index(value = ["kind", "date"])])
data class Entry(val kind: String, val id: String, val date: String, val payload: String, val encrypted: Boolean = false)

@Dao interface EntryDao {
    @Query("SELECT * FROM entries ORDER BY date, id") fun observe(): Flow<List<Entry>>
    @Query("SELECT * FROM entries") suspend fun all(): List<Entry>
    @Query("SELECT * FROM entries WHERE kind=:kind AND id=:id") suspend fun find(kind: String, id: String): Entry?
    @Upsert suspend fun put(entry: Entry)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insert(entries: List<Entry>)
    @Query("DELETE FROM entries WHERE kind=:kind AND id=:id") suspend fun delete(kind: String, id: String)
    @Query("DELETE FROM entries WHERE kind='privateLogs' OR (kind='drafts' AND encrypted=1 AND id LIKE 'private:%')") suspend fun deletePrivate()
}

@Database(entities = [Entry::class], version = 1, exportSchema = true)
abstract class DisciplineDatabase: RoomDatabase() { abstract fun entries(): EntryDao }
