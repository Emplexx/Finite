package moe.emi.finite.service.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rates")
data class RateEntity(
	@PrimaryKey val code: String,
	val rate: Double,
)