package moe.emi.finite.core.db

import arrow.core.raise.Raise
import arrow.core.raise.RaiseDSL

sealed interface DBError {
	data object WriteFailed : DBError
	data object IllegalState : DBError
}

context( Raise<DBError>)
@RaiseDSL
fun List<Long>.validateWrite(expect: Int) = size.validateWrite(expect).let { this }

context( Raise<DBError>)
@RaiseDSL
fun Int.validateWrite(expect: Int) {
	val actual = this@Int
	if (actual < expect) raise(DBError.WriteFailed)
	if (actual > expect) raise(DBError.IllegalState)
}