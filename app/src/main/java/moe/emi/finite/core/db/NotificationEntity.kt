package moe.emi.finite.core.db

import androidx.annotation.IntRange
import androidx.room.Entity
import androidx.room.PrimaryKey
import moe.emi.finite.core.model.Timespan

@Entity(tableName = "notifications")
data class NotificationEntity(
	@PrimaryKey(autoGenerate = true)
	val id: Int = 0,
	val subscriptionId: Int = 0,
	
	@androidx.room.ColumnInfo(name = "count")
	val unitsInAdvance: Int? = null,
	@androidx.room.ColumnInfo(name = "timespan")
	val timespanInAdvance: Timespan? = null,
	
	@IntRange(0, 23)
	val hours: Int,
	
	@IntRange(0, 59)
	val minutes: Int,
)