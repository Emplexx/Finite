package moe.emi.finite.service.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
	entities = [SubscriptionEntity::class, NotificationEntity::class],
	version = 7,
	exportSchema = true,
	autoMigrations = [
		AutoMigration(from = 5, to = 6),
		AutoMigration(from = 6, to = 7),
	]
)
abstract class FiniteDB : RoomDatabase() {
	abstract fun subscriptionDao(): SubscriptionDao
	abstract fun notificationDao(): NotificationDao
}