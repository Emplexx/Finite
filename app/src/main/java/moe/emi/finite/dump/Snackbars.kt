package moe.emi.finite.dump

import android.view.View
import com.google.android.material.snackbar.Snackbar

interface HasSnackbarAnchor {
	val snackbarAnchor: View
}

object Length {
	const val Indefinite = -2
	const val Short = -1
	const val Long = 0
}


// Lowest level snackbar function

private fun Pair<View, View?>.snackbar(
	message: CharSequence,
	duration: Int = Length.Short,
	options: Snackbar.() -> Unit = {}
) = Snackbar.make(this.first, message, duration)
	.apply { this@snackbar.second?.let(::setAnchorView) }
	.apply(options)
	.also { it.show() }


// Snackbar functions with options lambda

fun View.snackbar(
	message: CharSequence,
	duration: Int = Length.Short,
	options: Snackbar.() -> Unit = {}
) = (this to null).snackbar(message, duration, options)

context(HasSnackbarAnchor)
fun View.snackbar(
	message: CharSequence,
	duration: Int = Length.Short,
	options: Snackbar.() -> Unit = {}
) = (this@View to this@HasSnackbarAnchor.snackbarAnchor)
	.snackbar(message, duration, options)


// Highest level Snackbar functions with action label and onAction lambda

fun View.snackbar(
	message: CharSequence,
	action: CharSequence,
	duration: Int = Length.Short,
	onAction: (View) -> Unit = {},
) = this.snackbar(message, duration) { setAction(action, onAction) }

context(HasSnackbarAnchor)
fun View.snackbar(
	message: CharSequence,
	action: CharSequence,
	duration: Int = Length.Short,
	onAction: (View) -> Unit = {},
) = (this@View to this@HasSnackbarAnchor.snackbarAnchor)
	.snackbar(message, duration) { setAction(action, onAction) }



