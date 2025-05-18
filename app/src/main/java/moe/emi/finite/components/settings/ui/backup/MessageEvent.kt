package moe.emi.finite.components.settings.ui.backup

data class MessageEvent(
	val message: String,
	var consumed: Boolean = false
) {
	fun consume() { this.consumed = true }
}