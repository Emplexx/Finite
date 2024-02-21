package moe.emi.finite.ui.details

data class Event(
	val key: String,
) {
	var consumed: Boolean = false
	fun consume() { consumed = true }
	
	companion object {
		const val Deleted = "Delete"
		const val Error = "Error"
	}
}
