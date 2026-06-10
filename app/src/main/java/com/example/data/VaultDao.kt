package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault_items ORDER BY addedTime DESC")
    fun getAllItems(): Flow<List<VaultItem>>

    @Query("SELECT * FROM vault_items WHERE fileType = :type ORDER BY addedTime DESC")
    fun getItemsByType(type: String): Flow<List<VaultItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: VaultItem): Long

    @Delete
    suspend fun deleteItem(item: VaultItem)

    @Query("DELETE FROM vault_items WHERE id = :id")
    suspend fun deleteItemById(id: Int)
}
