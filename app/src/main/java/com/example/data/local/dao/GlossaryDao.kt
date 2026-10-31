package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.GlossaryItemEntity
import com.example.data.local.entities.GlossaryPackEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GlossaryDao {

    // --- Packs ---

    @Query("SELECT * FROM glossary_packs ORDER BY installedAt DESC")
    fun getAllPacksFlow(): Flow<List<GlossaryPackEntity>>

    @Query("SELECT * FROM glossary_packs ORDER BY installedAt DESC")
    suspend fun getAllPacksList(): List<GlossaryPackEntity>

    @Query("SELECT * FROM glossary_packs WHERE id = :id LIMIT 1")
    suspend fun getPackById(id: String): GlossaryPackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPack(pack: GlossaryPackEntity): Long

    @Query("DELETE FROM glossary_packs WHERE id = :id")
    suspend fun deletePack(id: String)

    // --- Items ---

    @Query("SELECT * FROM glossary_items ORDER BY term ASC")
    fun getAllItemsFlow(): Flow<List<GlossaryItemEntity>>

    @Query("SELECT * FROM glossary_items ORDER BY term ASC")
    suspend fun getAllItemsList(): List<GlossaryItemEntity>

    @Query("SELECT * FROM glossary_items WHERE packId = :packId ORDER BY term ASC")
    fun getItemsForPackFlow(packId: String): Flow<List<GlossaryItemEntity>>

    @Query("SELECT * FROM glossary_items WHERE packId = :packId ORDER BY term ASC")
    suspend fun getItemsForPackList(packId: String): List<GlossaryItemEntity>

    @Query("SELECT * FROM glossary_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: Long): GlossaryItemEntity?

    @Query("""
        SELECT * FROM glossary_items 
        WHERE term LIKE '%' || :query || '%' 
           OR definition LIKE '%' || :query || '%' 
           OR category LIKE '%' || :query || '%'
           OR tagsJson LIKE '%' || :query || '%'
        ORDER BY term ASC
    """)
    fun searchItemsFlow(query: String): Flow<List<GlossaryItemEntity>>

    @Query("SELECT * FROM glossary_items WHERE category = :category ORDER BY term ASC")
    fun getItemsByCategoryFlow(category: String): Flow<List<GlossaryItemEntity>>

    @Query("SELECT * FROM glossary_items WHERE isBookmarked = 1 ORDER BY term ASC")
    fun getBookmarkedItemsFlow(): Flow<List<GlossaryItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: GlossaryItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<GlossaryItemEntity>)

    @Update
    suspend fun updateItem(item: GlossaryItemEntity)

    @Query("UPDATE glossary_items SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun toggleBookmark(id: Long, isBookmarked: Boolean)

    @Query("DELETE FROM glossary_items WHERE id = :id")
    suspend fun deleteItem(id: Long)

    @Query("DELETE FROM glossary_items WHERE packId = :packId")
    suspend fun deleteItemsForPack(packId: String)
}
