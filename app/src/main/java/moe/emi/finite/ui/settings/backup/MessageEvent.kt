package moe.emi.finite.ui.settings.backup

data class MessageEvent(
	val message: String,
	var consumed: Boolean = false
) {
	fun consume() { this.consumed = true }
}