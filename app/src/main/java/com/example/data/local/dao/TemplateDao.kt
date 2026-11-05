package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.TemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM message_templates ORDER BY is_default DESC, id ASC")
    fun getAllTemplatesFlow(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM message_templates WHERE is_default = 1 LIMIT 1")
    suspend fun getDefaultTemplate(): TemplateEntity?

    @Query("SELECT * FROM message_templates WHERE id = :id")
    suspend fun getTemplateById(id: Long): TemplateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: TemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(templates: List<TemplateEntity>): List<Long>

    @Update
    suspend fun update(template: TemplateEntity)

    @Query("UPDATE message_templates SET is_default = 0")
    suspend fun clearDefault()

    @Query("UPDATE message_templates SET is_default = 1 WHERE id = :id")
    suspend fun setDefault(id: Long)

    @Query("DELETE FROM message_templates WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM message_templates")
    suspend fun getAllTemplatesList(): List<TemplateEntity>
}
