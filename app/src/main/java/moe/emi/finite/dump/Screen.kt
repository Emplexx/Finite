package moe.emi.finite.dump

sealed interface Screen {
	object Ok : Screen
	object Loading : Screen
	data class Err(val e: Exception) : Screen
}
