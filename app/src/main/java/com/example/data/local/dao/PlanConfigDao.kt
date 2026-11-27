package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PlanConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanConfigDao {
    @Query("SELECT * FROM plan_config WHERE id = 1")
    fun getConfig(): Flow<PlanConfigEntity?>

    @Query("SELECT * FROM plan_config WHERE id = 1")
    suspend fun getConfigOnce(): PlanConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConfig(config: PlanConfigEntity)
}
