package moe.emi.finite.service.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
	entities = [RateEntity::class, SubscriptionEntity::class],
	version = 6,
	exportSchema = true,
	autoMigrations = [
		AutoMigration(from = 5, to = 6)
	]
)
abstract class FiniteDB : RoomDatabase() {
	
	abstract fun rateDao(): RateDao
	abstract fun subscriptionDao(): SubscriptionDao
}