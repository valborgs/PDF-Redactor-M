package org.comon.pdfredactorm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.comon.pdfredactorm.data.local.entity.RedactionEntity

@Dao
interface RedactionDao {
    @Query("SELECT * FROM redactions WHERE projectId = :projectId")
    suspend fun getRedactionsForProject(projectId: String): List<RedactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRedactions(redactions: List<RedactionEntity>)

    @Query("DELETE FROM redactions WHERE projectId = :projectId")
    suspend fun deleteRedactionsForProject(projectId: String)
}
