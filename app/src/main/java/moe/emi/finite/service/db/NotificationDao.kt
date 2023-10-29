package moe.emi.finite.service.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
	
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertAll(vararg notifications: NotificationEntity): List<Long>
	
	@Query("SELECT * FROM notifications")
	fun getAll(): Flow<List<NotificationEntity>>
	
	@Query("SELECT * FROM notifications WHERE id IN (:ids)")
	fun getById(vararg ids: Int): Flow<List<NotificationEntity>>
	
	@Query("SELECT * FROM notifications WHERE subscriptionId IN (:ids)")
	fun getBySubscriptionId(vararg ids: Int): Flow<List<NotificationEntity>>
	
	@Query("DELETE FROM notifications WHERE id = :id")
	suspend fun delete(id: Int): Int
	
	@Query("SELECT * FROM subscriptions JOIN notifications ON subscriptions.id = notifications.subscriptionId")
	suspend fun getSubcriptionsWithReminders(): Map<SubscriptionEntity, List<NotificationEntity>>
	
	@Query(
		"SELECT * FROM notifications " +
				"JOIN subscriptions ON notifications.id IN (:ids) AND subscriptions.id = notifications.subscriptionId")
	suspend fun getTest(vararg ids: Int): Map<NotificationEntity, SubscriptionEntity>
	
	
}