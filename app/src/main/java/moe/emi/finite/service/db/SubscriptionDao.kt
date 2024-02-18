package moe.emi.finite.service.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
	
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertAll(vararg subscriptions: SubscriptionEntity): List<Long>
	
	@Query("SELECT * FROM subscriptions")
	fun getAllObservable(): Flow<List<SubscriptionEntity>>
	
	@Query("SELECT * FROM subscriptions WHERE id IN (:ids)")
	fun getByIdObservable(vararg ids: Int): Flow<List<SubscriptionEntity>>
	
	@Query("DELETE FROM subscriptions WHERE id = :id")
	suspend fun delete(id: Int): Int
	
	@Query("SELECT * FROM subscriptions WHERE id = :id")
	fun getObservable(id: Int): Flow<SubscriptionEntity?>
	
	@Query("DELETE FROM subscriptions")
	fun clearAll()
	
	@RawQuery
	suspend fun checkpoint(query: SimpleSQLiteQuery): Int
	
}