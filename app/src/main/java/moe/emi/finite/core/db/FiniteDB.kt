package moe.emi.finite.core.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
	entities = [SubscriptionEntity::class, NotificationEntity::class],
	version = 1,
	exportSchema = true,
)
abstract class FiniteDB : RoomDatabase() {
	abstract fun subscriptionDao(): SubscriptionDao
	abstract fun notificationDao(): NotificationDao
}