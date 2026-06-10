package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_items")
data class VaultItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,          // The name of the file in the internal storage (e.g. "img_12345.jpg")
    val fileType: String,          // "PHOTO" or "VIDEO"
    val originalName: String,      // Original file name
    val addedTime: Long = System.currentTimeMillis(),
    val size: Long,                // File size in bytes
    val duration: Long = 0L        // Duration in ms if video
)
