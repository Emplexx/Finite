package moe.emi.finite.ui.details

data class Message(
	val key: String,
) {
	var consumed: Boolean = false
	
	fun consume() { consumed = true }
}
