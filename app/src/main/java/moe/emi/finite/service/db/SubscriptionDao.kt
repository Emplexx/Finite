package moe.emi.finite.service.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
	
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertAll(vararg subscriptions: SubscriptionEntity)
	
	@Query("SELECT * FROM subscriptions")
	fun getAllObservable(): Flow<List<SubscriptionEntity>>
	
	@Query("DELETE FROM subscriptions WHERE id = :id")
	suspend fun delete(id: Int): Int
	
	@Query("SELECT * FROM subscriptions WHERE id = :id")
	fun getObservable(id: Int): Flow<SubscriptionEntity?>
	
}