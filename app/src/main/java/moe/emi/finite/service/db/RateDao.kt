package moe.emi.finite.service.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RateDao {

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertAll(vararg rates: RateEntity)
	
	@Query("SELECT * FROM rates")
	suspend fun getAll(): List<RateEntity>
	
	@Query("SELECT * FROM rates")
	fun getAllObservable(): Flow<List<RateEntity>>
}