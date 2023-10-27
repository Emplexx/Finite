package moe.emi.finite.dump

import androidx.datastore.preferences.protobuf.Api
import com.slack.eithernet.ApiResult
import moe.emi.finite.service.api.ApiError
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

sealed class Response<out T> {
	
	object Loading : Response<Nothing>()
	
	data class Success<out T>(
		val data: T
	) : Response<T>()
	
	data class Failure(
		val e: Exception
	) : Response<Nothing>() {
	
	}
	
	val dataOrNull: T?
		get() {
			return when (this) {
				is Success -> this.data
				is Failure -> null
				is Loading -> null
			}
		}
	
	infix fun dataOr(other: @UnsafeVariance T): T {
		return when (this) {
			is Success -> this.data
			is Failure -> other
			is Loading -> other
		}
	}
	
	val isSuccess: Boolean
		get() = this is Success
	
	fun assertSuccess(): Success<T> {
		return this as Success<T>
	}
	
	
	inline fun ifSuccess(block: (T) -> Unit): Response<T> {
		if (this is Success) block((this).data)
		return this
	}
	
	fun ifFailure(block: (Exception) -> Unit): Response<T> {
		if (this is Failure) block((this).e)
		return this
	}
	
	
	fun <R> map(transform: (T) -> R): Response<R> {
		return when (this) {
			is Success -> Success(transform(this.data))
			is Failure -> Failure(this.e)
			is Loading -> Loading
		}
	}
	
	fun dropValue(): Response<Nothing?> = this.map { null }
	
	fun <R> flatMap(transform: (T) -> Response<R>): Response<R> {
		return when (this) {
			is Success -> transform(this.data)
			is Failure -> Failure(this.e)
			is Loading -> Loading
		}
	}
	
	companion object {
		
		class ApiException(result: ApiResult<Any, Any>) : Exception()
		
		fun <T : Any, E : Any> ApiResult<T, E>.toResponse(): Response<T> {
			
			return when (this) {
				is ApiResult.Success -> Response.Success(this.value)
				// TODO better handling of Exceptions (who cares)
				else -> Response.Failure(ApiException(this))
			}
		}
	}
	
	fun <R> mapCatching(transform: (T) -> R): Response<R> {
		return when (this) {
			is Success -> try {
				Success(transform(this.data))
			} catch (e: Exception) {
				Failure(e)
			}
			is Failure -> Failure(this.e)
			is Loading -> Loading
		}
	}
}