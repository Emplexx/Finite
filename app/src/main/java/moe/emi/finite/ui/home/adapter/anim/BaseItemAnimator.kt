package moe.emi.finite.ui.home.adapter.anim

import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.Interpolator
import androidx.core.view.ViewCompat
import androidx.core.view.ViewPropertyAnimatorListener
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator

/**
 * This implementation of [RecyclerView.ItemAnimator] provides basic
 * animations on remove, add, and move events that happen to the items in
 * a RecyclerView. RecyclerView uses a CollapsingItemAnimator by default.
 *
 * @see RecyclerView.setItemAnimator
 */
abstract class BaseItemAnimator<AN>() : SimpleItemAnimator() {
	private val mPendingRemovals = ArrayList<RecyclerView.ViewHolder>()
	private val mPendingAdditions = ArrayList<RecyclerView.ViewHolder>()
	private val mPendingMoves = ArrayList<MoveInfo>()
	private val mPendingChanges = ArrayList<ChangeInfo>()
	private val mAdditionsList = ArrayList<ArrayList<RecyclerView.ViewHolder>>()
	private val mMovesList = ArrayList<ArrayList<MoveInfo>>()
	private val mChangesList = ArrayList<ArrayList<ChangeInfo>>()
	private val mAddAnimations = ArrayList<RecyclerView.ViewHolder>()
	private val mMoveAnimations = ArrayList<RecyclerView.ViewHolder>()
	private val mRemoveAnimations = ArrayList<RecyclerView.ViewHolder>()
	private val mChangeAnimations = ArrayList<RecyclerView.ViewHolder>()
	
	/**
	 * @return the interpolator used for the animations
	 */
	var interpolator: Interpolator? = null
	
	
	
	class MoveInfo(
		var holder: RecyclerView.ViewHolder,
		var fromX: Int,
		var fromY: Int,
		var toX: Int,
		var toY: Int
	)
	
	class ChangeInfo(
		var oldHolder: RecyclerView.ViewHolder?,
		var newHolder: RecyclerView.ViewHolder?
	) {
		var fromX = 0
		var fromY = 0
		var toX = 0
		var toY = 0
		
		constructor(
			oldHolder: RecyclerView.ViewHolder, newHolder: RecyclerView.ViewHolder,
			fromX: Int, fromY: Int, toX: Int, toY: Int
		) : this(oldHolder, newHolder) {
			this.fromX = fromX
			this.fromY = fromY
			this.toX = toX
			this.toY = toY
		}
		
		override fun toString(): String {
			return "ChangeInfo{" +
					"oldHolder=" + oldHolder +
					", newHolder=" + newHolder +
					", fromX=" + fromX +
					", fromY=" + fromY +
					", toX=" + toX +
					", toY=" + toY +
					'}'
		}
	}
	
	override fun runPendingAnimations() {
		val removalsPending = !mPendingRemovals.isEmpty()
		val movesPending = !mPendingMoves.isEmpty()
		val changesPending = !mPendingChanges.isEmpty()
		val additionsPending = !mPendingAdditions.isEmpty()
		if (!removalsPending && !movesPending && !additionsPending && !changesPending) {
			// nothing to animate
			return
		}
		// First, remove stuff
		for (holder: RecyclerView.ViewHolder in mPendingRemovals) {
			animateRemoveImpl(holder)
		}
		mPendingRemovals.clear()
		// Next, move stuff
		if (movesPending) {
			val moves = ArrayList<MoveInfo>()
			moves.addAll(mPendingMoves)
			mMovesList.add(moves)
			mPendingMoves.clear()
			val mover: Runnable = Runnable {
				for (moveInfo: MoveInfo in moves) {
					animateMoveImpl(
						moveInfo.holder, moveInfo.fromX, moveInfo.fromY,
						moveInfo.toX, moveInfo.toY,
						additionsPending
					)
				}
				moves.clear()
				mMovesList.remove(moves)
			}
			if (removalsPending) {
				val view = moves[0].holder.itemView
				ViewCompat.postOnAnimationDelayed(view, mover, 0)
			} else {
				mover.run()
			}
		}
		// Next, change stuff, to run in parallel with move animations
		if (changesPending) {
			val changes = ArrayList<ChangeInfo>()
			changes.addAll(mPendingChanges)
			mChangesList.add(changes)
			mPendingChanges.clear()
			val changer: Runnable = Runnable {
				for (change: ChangeInfo in changes) {
					animateChangeImpl(change)
				}
				changes.clear()
				mChangesList.remove(changes)
			}
			if (removalsPending) {
				val holder = changes[0].oldHolder
				val moveDuration = if (movesPending) moveDuration else 0
				ViewCompat.postOnAnimationDelayed(
					holder!!.itemView, changer, getRemoveDelay(
						removeDuration, moveDuration, changeDuration
					)
				)
			} else {
				changer.run()
			}
		}
		// Next, add stuff
		if (additionsPending) {
			val additions = ArrayList<RecyclerView.ViewHolder>()
			additions.addAll(mPendingAdditions)
			mAdditionsList.add(additions)
			mPendingAdditions.clear()
			val adder: Runnable = Runnable {
				for (holder: RecyclerView.ViewHolder in additions) {
					animateAddImpl(holder, movesPending)
				}
				additions.clear()
				mAdditionsList.remove(additions)
			}
			if (removalsPending || movesPending || changesPending) {
				val removeDuration = if (removalsPending) removeDuration else 0
				val moveDuration = if (movesPending) moveDuration else 0
				val changeDuration = if (changesPending) changeDuration else 0
				val view = additions[0].itemView
				ViewCompat.postOnAnimationDelayed(
					view,
					adder,
					getAddDelay(removeDuration, moveDuration, changeDuration)
				)
			} else {
				adder.run()
			}
		}
	}
	
	/**
	 * used to calculated the delay until the remove animation should start
	 *
	 * @param remove the remove duration
	 * @param move   the move duration
	 * @param change the change duration
	 * @return the calculated delay for the remove items animation
	 */
	open fun getRemoveDelay(remove: Long, move: Long, change: Long): Long {
		return remove + Math.max(move, change)
	}
	
	/**
	 * used to calculated the delay until the add animation should start
	 *
	 * @param remove the remove duration
	 * @param move   the move duration
	 * @param change the change duration
	 * @return the calculated delay for the add items animation
	 */
	open fun getAddDelay(remove: Long, move: Long, change: Long): Long {
		return remove + Math.max(move, change)
	}
	
	override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
		resetAnimation(holder)
		mPendingRemovals.add(holder)
		return true
	}
	
	private fun animateRemoveImpl(holder: RecyclerView.ViewHolder) {
		val animation = removeAnimation(holder)
		mRemoveAnimations.add(holder)
		
		setupAnimationListener(
			animation,
			onStart = { dispatchRemoveStarting(holder) },
			onEnd = {
				cleanupAnimationListener(animation)
				removeAnimationCleanup(holder)
				dispatchRemoveFinished(holder)
				mRemoveAnimations.remove(holder)
				dispatchFinishedWhenDone()
			}
		)
		
		startAnimation(animation)
		
//		animation.setListener(object : VpaListenerAdapter() {
//			override fun onAnimationStart(view: View) {
//				dispatchRemoveStarting(holder)
//			}
//
//			override fun onAnimationEnd(view: View) {
//				animation.setListener(null)
//				removeAnimationCleanup(holder)
//				dispatchRemoveFinished(holder)
//				mRemoveAnimations.remove(holder)
//				dispatchFinishedWhenDone()
//			}
//		}).start()
	}
	
	abstract fun removeAnimation(holder: RecyclerView.ViewHolder): AN
	abstract fun removeAnimationCleanup(holder: RecyclerView.ViewHolder)
	override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
		resetAnimation(holder)
		addAnimationPrepare(holder)
		mPendingAdditions.add(holder)
		return true
	}
	
	
	
	private fun animateAddImpl(
		holder: RecyclerView.ViewHolder,
		movesPending: Boolean,
	) {
		val animation = addAnimation(holder, movesPending)
		mAddAnimations.add(holder)
		
		setupAnimationListener(
			animation,
			onStart = {
				dispatchAddStarting(holder)
			},
			onCancel = {
				addAnimationCleanup(holder)
			},
			onEnd = {
				cleanupAnimationListener(animation)
				dispatchAddFinished(holder)
				mAddAnimations.remove(holder)
				dispatchFinishedWhenDone()
				addAnimationCleanup(holder)
			}
		)
		startAnimation(animation)
		
//		animation.setListener(object : VpaListenerAdapter() {
//			override fun onAnimationStart(view: View) {
//				dispatchAddStarting(holder)
//			}
//
//			override fun onAnimationCancel(view: View) {
//				addAnimationCleanup(holder)
//			}
//
//			override fun onAnimationEnd(view: View) {
//				animation.setListener(null)
//				dispatchAddFinished(holder)
//				mAddAnimations.remove(holder)
//				dispatchFinishedWhenDone()
//				addAnimationCleanup(holder)
//			}
//		}).start()
	}
	
	/**
	 * the animation to prepare the view before the add animation is run
	 *
	 * @param holder
	 */
	abstract fun addAnimationPrepare(holder: RecyclerView.ViewHolder)
	
	/**
	 * the animation for adding a view
	 *
	 * @param holder
	 * @return
	 */
	abstract fun addAnimation(holder: RecyclerView.ViewHolder, movesPending: Boolean): AN
	
	/**
	 * the cleanup method if the animation needs to be stopped. and tro prepare for the next view
	 *
	 * @param holder
	 */
	abstract fun addAnimationCleanup(holder: RecyclerView.ViewHolder)
	override fun animateMove(
		holder: RecyclerView.ViewHolder, fromX: Int, fromY: Int,
		toX: Int, toY: Int
	): Boolean {
		var fromX = fromX
		var fromY = fromY
		val view = holder.itemView
		fromX = (fromX + ViewCompat.getTranslationX(holder.itemView)).toInt()
		fromY = (fromY + ViewCompat.getTranslationY(holder.itemView)).toInt()
		resetAnimation(holder)
		val deltaX = toX - fromX
		val deltaY = toY - fromY
		if (deltaX == 0 && deltaY == 0) {
			dispatchMoveFinished(holder)
			return false
		}
		if (deltaX != 0) {
			ViewCompat.setTranslationX(view, -deltaX.toFloat())
		}
		if (deltaY != 0) {
			ViewCompat.setTranslationY(view, -deltaY.toFloat())
		}
		mPendingMoves.add(MoveInfo(holder, fromX, fromY, toX, toY))
		return true
	}
	
	abstract fun animateMove(
		holder: RecyclerView.ViewHolder,
		x: Boolean,
		y: Boolean,
		additionsPending: Boolean
	): AN
	
	private fun animateMoveImpl(
		holder: RecyclerView.ViewHolder,
		fromX: Int,
		fromY: Int,
		toX: Int,
		toY: Int,
		additionsPending: Boolean,
	) {
		val view = holder.itemView
		val deltaX = toX - fromX
		val deltaY = toY - fromY
		
		val animation = animateMove(holder, deltaX != 0, deltaY != 0, additionsPending)
		mMoveAnimations.add(holder)
		setupAnimationListener(
			animation,
			onStart = { dispatchMoveStarting(holder) },
			onEnd = {
				cleanupAnimationListener(animation)
				dispatchMoveFinished(holder)
				mMoveAnimations.remove(holder)
				dispatchFinishedWhenDone()
			}
		)
		startAnimation(animation)
		
//		var springY: SpringAnimation? = null
//
//		if (deltaX != 0) {
//			ViewCompat.animate(view).translationX(0f)
//		}
//		if (deltaY != 0) {
////			ViewCompat.animate(view).translationY(0f)
//			springY = SpringAnimation(view, DynamicAnimation.TRANSLATION_Y, 0f)
//		}
		
//		val animation = ViewCompat.animate(view)

//		animation.setDuration(moveDuration).setListener(object : VpaListenerAdapter() {
//			override fun onAnimationStart(view: View) {
//				dispatchMoveStarting(holder)
//			}
//
//			override fun onAnimationCancel(view: View) {
//				if (deltaX != 0) {
//					ViewCompat.setTranslationX(view, 0f)
//				}
//				if (deltaY != 0) {
//					ViewCompat.setTranslationY(view, 0f)
//				}
//			}
//
//			override fun onAnimationEnd(view: View) {
//				animation.setListener(null)
//				dispatchMoveFinished(holder)
//				mMoveAnimations.remove(holder)
//				dispatchFinishedWhenDone()
//			}
//		}).start()
	
		
	}
	
	override fun animateChange(
		oldHolder: RecyclerView.ViewHolder, newHolder: RecyclerView.ViewHolder,
		fromX: Int, fromY: Int, toX: Int, toY: Int
	): Boolean {
		if (oldHolder === newHolder) {
			// Don't know how to run change animations when the same view holder is re-used.
			// run a move animation to handle position changes.
			return animateMove(oldHolder, fromX, fromY, toX, toY)
		}
		changeAnimation(
			oldHolder, newHolder,
			fromX, fromY, toX, toY
		)
		mPendingChanges.add(
			ChangeInfo(
				oldHolder,
				newHolder,
				fromX,
				fromY,
				toX,
				toY
			)
		)
		return true
	}
	
	private fun animateChangeImpl(changeInfo: ChangeInfo) {
		val holder = changeInfo.oldHolder
		val view = holder?.itemView
		val newHolder = changeInfo.newHolder
		val newView = newHolder?.itemView
		if (view != null) {
			val oldViewAnim = changeOldAnimation(holder, changeInfo)
			mChangeAnimations.add(changeInfo.oldHolder!!)
			
			setupAnimationListener(
				oldViewAnim,
				onStart = {
					dispatchChangeStarting(changeInfo.oldHolder, true)
				},
				onEnd = {
					cleanupAnimationListener(oldViewAnim)
					changeAnimationCleanup(holder)
					ViewCompat.setTranslationX(view, 0f)
					ViewCompat.setTranslationY(view, 0f)
					dispatchChangeFinished(changeInfo.oldHolder, true)
					mChangeAnimations.remove(changeInfo.oldHolder)
					dispatchFinishedWhenDone()
				}
			)
			
			startAnimation(oldViewAnim)
			
//			oldViewAnim.setListener(object : VpaListenerAdapter() {
//				override fun onAnimationStart(view: View) {
//					dispatchChangeStarting(changeInfo.oldHolder, true)
//				}
//
//				override fun onAnimationEnd(view: View) {
//					oldViewAnim.setListener(null)
//					changeAnimationCleanup(holder)
//					ViewCompat.setTranslationX(view, 0f)
//					ViewCompat.setTranslationY(view, 0f)
//					dispatchChangeFinished(changeInfo.oldHolder, true)
//					mChangeAnimations.remove(changeInfo.oldHolder)
//					dispatchFinishedWhenDone()
//				}
//			}).start()
		}
		if (newView != null) {
			val newViewAnimation = changeNewAnimation(newHolder)
			mChangeAnimations.add(changeInfo.newHolder!!)
			
			setupAnimationListener(
				newViewAnimation,
				onStart = {
					dispatchChangeStarting(changeInfo.newHolder, false)
				},
				onEnd = {
					cleanupAnimationListener(newViewAnimation)
					changeAnimationCleanup(newHolder)
					ViewCompat.setTranslationX(newView, 0f)
					ViewCompat.setTranslationY(newView, 0f)
					dispatchChangeFinished(changeInfo.newHolder, false)
					mChangeAnimations.remove(changeInfo.newHolder)
					dispatchFinishedWhenDone()
				}
			)
			startAnimation(newViewAnimation)
			
//			newViewAnimation.setListener(object : VpaListenerAdapter() {
//				override fun onAnimationStart(view: View) {
//					dispatchChangeStarting(changeInfo.newHolder, false)
//				}
//
//				override fun onAnimationEnd(view: View) {
//					newViewAnimation.setListener(null)
//					changeAnimationCleanup(newHolder)
//					ViewCompat.setTranslationX(newView, 0f)
//					ViewCompat.setTranslationY(newView, 0f)
//					dispatchChangeFinished(changeInfo.newHolder, false)
//					mChangeAnimations.remove(changeInfo.newHolder)
//					dispatchFinishedWhenDone()
//				}
//			}).start()
		}
	}
	
	/**
	 * the whole change animation if we have to cross animate two views
	 *
	 * @param oldHolder
	 * @param newHolder
	 * @param fromX
	 * @param fromY
	 * @param toX
	 * @param toY
	 */
	open fun changeAnimation(
		oldHolder: RecyclerView.ViewHolder,
		newHolder: RecyclerView.ViewHolder?,
		fromX: Int,
		fromY: Int,
		toX: Int,
		toY: Int
	) {
		val prevTranslationX = ViewCompat.getTranslationX(oldHolder.itemView)
		val prevTranslationY = ViewCompat.getTranslationY(oldHolder.itemView)
		val prevValue = ViewCompat.getAlpha(oldHolder.itemView)
		resetAnimation(oldHolder)
		val deltaX = (toX - fromX - prevTranslationX).toInt()
		val deltaY = (toY - fromY - prevTranslationY).toInt()
		// recover prev translation state after ending animation
		ViewCompat.setTranslationX(oldHolder.itemView, prevTranslationX)
		ViewCompat.setTranslationY(oldHolder.itemView, prevTranslationY)
		ViewCompat.setAlpha(oldHolder.itemView, prevValue)
		if (newHolder != null) {
			// carry over translation values
			resetAnimation(newHolder)
			ViewCompat.setTranslationX(newHolder.itemView, -deltaX.toFloat())
			ViewCompat.setTranslationY(newHolder.itemView, -deltaY.toFloat())
			ViewCompat.setAlpha(newHolder.itemView, 0f)
		}
	}
	
	/**
	 * the animation for removing the old view
	 *
	 * @param holder
	 * @return
	 */
	abstract fun changeOldAnimation(
		holder: RecyclerView.ViewHolder,
		changeInfo: ChangeInfo?
	): AN
	
	/**
	 * the animation for changing the new view
	 *
	 * @param holder
	 * @return
	 */
	abstract fun changeNewAnimation(holder: RecyclerView.ViewHolder): AN
	
	/**
	 * the cleanup method if the animation needs to be stopped. and tro prepare for the next view
	 *
	 * @param holder
	 */
	abstract fun changeAnimationCleanup(holder: RecyclerView.ViewHolder?)
	private fun endChangeAnimation(
		infoList: MutableList<ChangeInfo>,
		item: RecyclerView.ViewHolder
	) {
		for (i in infoList.indices.reversed()) {
			val changeInfo = infoList[i]
			if (endChangeAnimationIfNecessary(changeInfo, item)) {
				if (changeInfo.oldHolder == null && changeInfo.newHolder == null) {
					infoList.remove(changeInfo)
				}
			}
		}
	}
	
	private fun endChangeAnimationIfNecessary(changeInfo: ChangeInfo) {
		if (changeInfo.oldHolder != null) {
			endChangeAnimationIfNecessary(changeInfo, changeInfo.oldHolder!!)
		}
		if (changeInfo.newHolder != null) {
			endChangeAnimationIfNecessary(changeInfo, changeInfo.newHolder!!)
		}
	}
	
	private fun endChangeAnimationIfNecessary(
		changeInfo: ChangeInfo,
		item: RecyclerView.ViewHolder
	): Boolean {
		var oldItem = false
		if (changeInfo.newHolder === item) {
			changeInfo.newHolder = null
		} else if (changeInfo.oldHolder === item) {
			changeInfo.oldHolder = null
			oldItem = true
		} else {
			return false
		}
		changeAnimationCleanup(item)
		ViewCompat.setTranslationX(item.itemView, 0f)
		ViewCompat.setTranslationY(item.itemView, 0f)
		dispatchChangeFinished(item, oldItem)
		return true
	}
	
	override fun endAnimation(item: RecyclerView.ViewHolder) {
		val view = item.itemView
		// this will trigger end callback which should set properties to their target values.
		ViewCompat.animate(view).cancel()
		// TODO if some other animations are chained to end, how do we cancel them as well?
		for (i in mPendingMoves.indices.reversed()) {
			val moveInfo = mPendingMoves[i]
			if (moveInfo.holder === item) {
				ViewCompat.setTranslationY(view, 0f)
				ViewCompat.setTranslationX(view, 0f)
				dispatchMoveFinished(item)
				mPendingMoves.removeAt(i)
			}
		}
		endChangeAnimation(mPendingChanges, item)
		if (mPendingRemovals.remove(item)) {
			removeAnimationCleanup(item)
			dispatchRemoveFinished(item)
		}
		if (mPendingAdditions.remove(item)) {
			addAnimationCleanup(item)
			dispatchAddFinished(item)
		}
		for (i in mChangesList.indices.reversed()) {
			val changes = mChangesList[i]
			endChangeAnimation(changes, item)
			if (changes.isEmpty()) {
				mChangesList.removeAt(i)
			}
		}
		for (i in mMovesList.indices.reversed()) {
			val moves = mMovesList[i]
			for (j in moves.indices.reversed()) {
				val moveInfo = moves[j]
				if (moveInfo.holder === item) {
					ViewCompat.setTranslationY(view, 0f)
					ViewCompat.setTranslationX(view, 0f)
					dispatchMoveFinished(item)
					moves.removeAt(j)
					if (moves.isEmpty()) {
						mMovesList.removeAt(i)
					}
					break
				}
			}
		}
		for (i in mAdditionsList.indices.reversed()) {
			val additions = mAdditionsList[i]
			if (additions.remove(item)) {
				addAnimationCleanup(item)
				dispatchAddFinished(item)
				if (additions.isEmpty()) {
					mAdditionsList.removeAt(i)
				}
			}
		}
		
		// animations should be ended by the cancel above.
		if (mRemoveAnimations.remove(item) && DEBUG) {
			throw IllegalStateException(
				"after animation is cancelled, item should not be in "
						+ "mRemoveAnimations list"
			)
		}
		if (mAddAnimations.remove(item) && DEBUG) {
			throw IllegalStateException(
				"after animation is cancelled, item should not be in "
						+ "mAddAnimations list"
			)
		}
		if (mChangeAnimations.remove(item) && DEBUG) {
			throw IllegalStateException(
				("after animation is cancelled, item should not be in "
						+ "mChangeAnimations list")
			)
		}
		if (mMoveAnimations.remove(item) && DEBUG) {
			throw IllegalStateException(
				("after animation is cancelled, item should not be in "
						+ "mMoveAnimations list")
			)
		}
		dispatchFinishedWhenDone()
	}
	
	fun resetAnimation(holder: RecyclerView.ViewHolder) {
		if (sDefaultInterpolator == null) {
			sDefaultInterpolator = (ValueAnimator()).interpolator
		}
		holder.itemView.animate().setInterpolator(sDefaultInterpolator)
		endAnimation(holder)
	}
	
	override fun isRunning(): Boolean {
		return ((!mPendingAdditions.isEmpty() ||
				!mPendingChanges.isEmpty() ||
				!mPendingMoves.isEmpty() ||
				!mPendingRemovals.isEmpty() ||
				!mMoveAnimations.isEmpty() ||
				!mRemoveAnimations.isEmpty() ||
				!mAddAnimations.isEmpty() ||
				!mChangeAnimations.isEmpty() ||
				!mMovesList.isEmpty() ||
				!mAdditionsList.isEmpty() ||
				!mChangesList.isEmpty()))
	}
	
	/**
	 * Check the state of currently pending and running animations. If there are none
	 * pending/running, call [.dispatchAnimationsFinished] to notify any
	 * listeners.
	 */
	private fun dispatchFinishedWhenDone() {
		if (!isRunning) {
			dispatchAnimationsFinished()
		}
	}
	
	override fun endAnimations() {
		var count = mPendingMoves.size
		for (i in count - 1 downTo 0) {
			val item = mPendingMoves[i]
			val view = item.holder.itemView
			ViewCompat.setTranslationY(view, 0f)
			ViewCompat.setTranslationX(view, 0f)
			dispatchMoveFinished(item.holder)
			mPendingMoves.removeAt(i)
		}
		count = mPendingRemovals.size
		for (i in count - 1 downTo 0) {
			val item = mPendingRemovals[i]
			dispatchRemoveFinished(item)
			mPendingRemovals.removeAt(i)
		}
		count = mPendingAdditions.size
		for (i in count - 1 downTo 0) {
			val item = mPendingAdditions[i]
			val view = item.itemView
			addAnimationCleanup(item)
			dispatchAddFinished(item)
			mPendingAdditions.removeAt(i)
		}
		count = mPendingChanges.size
		for (i in count - 1 downTo 0) {
			endChangeAnimationIfNecessary(mPendingChanges[i])
		}
		mPendingChanges.clear()
		if (!isRunning) {
			return
		}
		var listCount = mMovesList.size
		for (i in listCount - 1 downTo 0) {
			val moves = mMovesList[i]
			count = moves.size
			for (j in count - 1 downTo 0) {
				val moveInfo = moves[j]
				val item = moveInfo.holder
				val view = item.itemView
				ViewCompat.setTranslationY(view, 0f)
				ViewCompat.setTranslationX(view, 0f)
				dispatchMoveFinished(moveInfo.holder)
				moves.removeAt(j)
				if (moves.isEmpty()) {
					mMovesList.remove(moves)
				}
			}
		}
		listCount = mAdditionsList.size
		for (i in listCount - 1 downTo 0) {
			val additions = mAdditionsList[i]
			count = additions.size
			for (j in count - 1 downTo 0) {
				val item = additions[j]
				val view = item.itemView
				addAnimationCleanup(item)
				dispatchAddFinished(item)
				additions.removeAt(j)
				if (additions.isEmpty()) {
					mAdditionsList.remove(additions)
				}
			}
		}
		listCount = mChangesList.size
		for (i in listCount - 1 downTo 0) {
			val changes = mChangesList[i]
			count = changes.size
			for (j in count - 1 downTo 0) {
				endChangeAnimationIfNecessary(changes[j])
				if (changes.isEmpty()) {
					mChangesList.remove(changes)
				}
			}
		}
		cancelAll(mRemoveAnimations)
		cancelAll(mMoveAnimations)
		cancelAll(mAddAnimations)
		cancelAll(mChangeAnimations)
		dispatchAnimationsFinished()
	}
	
	fun cancelAll(viewHolders: List<RecyclerView.ViewHolder>) {
		for (i in viewHolders.indices.reversed()) {
			ViewCompat.animate(viewHolders[i].itemView).cancel()
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	override fun canReuseUpdatedViewHolder(
		viewHolder: RecyclerView.ViewHolder,
		payloads: List<Any>
	): Boolean {
		return !payloads.isEmpty() || super.canReuseUpdatedViewHolder(viewHolder, payloads)
	}
	
	private open class VpaListenerAdapter() : ViewPropertyAnimatorListener {
		override fun onAnimationStart(view: View) {}
		override fun onAnimationEnd(view: View) {}
		override fun onAnimationCancel(view: View) {}
	}
	
	abstract fun setupAnimationListener(
		animation: AN,
		onStart: () -> Unit = {},
		onEnd: () -> Unit = {},
		onCancel: () -> Unit = {},
	)
	
	abstract fun cleanupAnimationListener(animation: AN)
	
	abstract fun startAnimation(
		animation: AN
	)
	
	companion object {
		private val DEBUG = false
		private var sDefaultInterpolator: TimeInterpolator? = null
	}
}
