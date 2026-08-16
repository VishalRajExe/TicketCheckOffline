package com.ticketcheck.offline.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val eventDate: String,
    val venue: String,
    val ticketPrefix: String,
    val createdAt: Long = System.currentTimeMillis()
)
