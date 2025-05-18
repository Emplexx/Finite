package moe.emi.finite.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import moe.emi.finite.core.model.Reminder

@Dao
interface NotificationDao {
	
	@Insert(onConflict = OnConflictStrategy.REPLACE)
	suspend fun insertAll(vararg notifications: NotificationEntity): List<Long>
	
	@Query("SELECT * FROM notifications")
	fun getAll(): Flow<List<NotificationEntity>>
	
	@Query("SELECT * FROM notifications WHERE id IN (:ids)")
	fun getById(vararg ids: Int): Flow<List<NotificationEntity>>
	
	@Query("SELECT * FROM notifications WHERE subscriptionId IN (:ids)")
	fun getEntitiesBySubscriptionId(vararg ids: Int): Flow<List<NotificationEntity>>
	
	@Query("DELETE FROM notifications WHERE id = :id")
	suspend fun deleteReminderEntity(id: Int): Int
	
	@Query("DELETE FROM notifications WHERE subscriptionId = :subscriptionId")
	suspend fun deleteBySubscriptionId(subscriptionId: Int): Int
	
	
	@Query("SELECT * FROM subscriptions JOIN notifications ON subscriptions.id = notifications.subscriptionId")
	suspend fun getSubcriptionsWithReminders(): Map<SubscriptionEntity, List<NotificationEntity>>
	
	
	@Query("SELECT * FROM subscriptions JOIN notifications ON subscriptions.id = notifications.subscriptionId WHERE subscriptions.active = TRUE")
	suspend fun getActiveSubscriptionsWithReminders(): Map<SubscriptionEntity, List<NotificationEntity>>
	
	@Query(
		"SELECT * FROM notifications " +
				"JOIN subscriptions ON notifications.id IN (:ids) AND subscriptions.id = notifications.subscriptionId"
	)
	suspend fun getRemindersWithSubs(vararg ids: Int): Map<NotificationEntity, SubscriptionEntity>
	
	
	@Query("DELETE FROM notifications")
	fun clearAll()
	
}

fun NotificationDao.getBySubscriptionId(vararg ids: Int) =
	getEntitiesBySubscriptionId(*ids).map { list ->
		list.map { it.toReminder() }
	}

suspend fun NotificationDao.insertAll(vararg models: Reminder) =
	withContext(Dispatchers.IO) { insertAll(*models.map { it.toEntity() }.toTypedArray()) }

suspend fun NotificationDao.deleteReminder(id: Int) =
	withContext(Dispatchers.IO) { deleteReminderEntity(id) }