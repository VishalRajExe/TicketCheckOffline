package com.ticketcheck.offline.data.dao

import androidx.room.*
import com.ticketcheck.offline.data.entities.EventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Query("SELECT * FROM events ORDER BY id DESC LIMIT 1")
    fun observeCurrentEvent(): Flow<EventEntity?>

    @Query("SELECT * FROM events ORDER BY id DESC LIMIT 1")
    suspend fun getCurrentEvent(): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity): Long

    @Update
    suspend fun update(event: EventEntity)

    @Query("DELETE FROM events")
    suspend fun deleteAll()
}
