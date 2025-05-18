package moe.emi.finite.dump

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

sealed interface TypeWrapper<out A> {
	object IMPL : TypeWrapper<Nothing>
}

@OptIn(ExperimentalContracts::class)
@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")
inline fun <A, B, R> with(a: A, b: B, block: context(A, B) (TypeWrapper<B>) -> R): R {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	return block(a, b, TypeWrapper.IMPL)
}

@OptIn(ExperimentalContracts::class)
@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")
inline fun <A, B, C, R> with(a: A, b: B, c: C, block: context(A, B, C) (TypeWrapper<C>) -> R): R {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	return block(a, b, c, TypeWrapper.IMPL)
}

@OptIn(ExperimentalContracts::class)
@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")
inline fun <A, B, C, D, R> with(a: A, b: B, c: C, d: D, block: context(A, B, C, D) (TypeWrapper<D>) -> R): R {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	return block(a, b, c, d, TypeWrapper.IMPL)
}

@OptIn(ExperimentalContracts::class)
@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")
inline fun <A, B, C, D, E, R> with(a: A, b: B, c: C, d: D, e: E, block: context(A, B, C, D, E) (TypeWrapper<E>) -> R): R {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	return block(a, b, c, d, e, TypeWrapper.IMPL)
}

@OptIn(ExperimentalContracts::class)
@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")
inline fun <A, B, C, D, E, F, R> with(a: A, b: B, c: C, d: D, e: E, f: F, block: context(A, B, C, D, E, F) (TypeWrapper<F>) -> R): R {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	return block(a, b, c, d, e, f, TypeWrapper.IMPL)
}

@OptIn(ExperimentalContracts::class)
@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")
inline fun <A, B, C, D, E, F, G, R> with(a: A, b: B, c: C, d: D, e: E, f: F, g: G, block: context(A, B, C, D, E, F, G) (TypeWrapper<G>) -> R): R {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	return block(a, b, c, d, e, f, g, TypeWrapper.IMPL)
}

@OptIn(ExperimentalContracts::class)
@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")
inline fun <A, B, C, D, E, F, G, H, R> with(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, block: context(A, B, C, D, E, F, G, H) (TypeWrapper<H>) -> R): R {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	return block(a, b, c, d, e, f, g, h, TypeWrapper.IMPL)
}

@OptIn(ExperimentalContracts::class)
@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")
inline fun <A, B, C, D, E, F, G, H, I, R> with(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, block: context(A, B, C, D, E, F, G, H, I) (TypeWrapper<I>) -> R): R {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	return block(a, b, c, d, e, f, g, h, i, TypeWrapper.IMPL)
}

@OptIn(ExperimentalContracts::class)
@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")
inline fun <A, B, C, D, E, F, G, H, I, J, R> with(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, block: context(A, B, C, D, E, F, G, H, I, J) (TypeWrapper<J>) -> R): R {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	return block(a, b, c, d, e, f, g, h, i, j, TypeWrapper.IMPL)
}

@OptIn(ExperimentalContracts::class)
@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")
inline fun <A, B, C, D, E, F, G, H, I, J, K, R> with(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, block: context(A, B, C, D, E, F, G, H, I, J, K) (TypeWrapper<K>) -> R): R {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	return block(a, b, c, d, e, f, g, h, i, j, k, TypeWrapper.IMPL)
}

@OptIn(ExperimentalContracts::class)
@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")
inline fun <A, B, C, D, E, F, G, H, I, J, K, L, R> with(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, block: context(A, B, C, D, E, F, G, H, I, J, K, L) (TypeWrapper<L>) -> R): R {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	return block(a, b, c, d, e, f, g, h, i, j, k, l, TypeWrapper.IMPL)
}

@OptIn(ExperimentalContracts::class)
@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")
inline fun <A, B, C, D, E, F, G, H, I, J, K, L, M, R> with(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, block: context(A, B, C, D, E, F, G, H, I, J, K, L, M) (TypeWrapper<M>) -> R): R {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	return block(a, b, c, d, e, f, g, h, i, j, k, l, m, TypeWrapper.IMPL)
}

@OptIn(ExperimentalContracts::class)
@Suppress("SUBTYPING_BETWEEN_CONTEXT_RECEIVERS")
inline fun <A, B, C, D, E, F, G, H, I, J, K, L, M, N, R> with(a: A, b: B, c: C, d: D, e: E, f: F, g: G, h: H, i: I, j: J, k: K, l: L, m: M, n: N, block: context(A, B, C, D, E, F, G, H, I, J, K, L, M, N) (TypeWrapper<N>) -> R): R {
	contract {
		callsInPlace(block, InvocationKind.EXACTLY_ONCE)
	}
	return block(a, b, c, d, e, f, g, h, i, j, k, l, m, n, TypeWrapper.IMPL)
}