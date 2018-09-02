/*
 * Copyright (c) 2018.
 * João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This file is part of the UNES Open Source Project.
 *
 * UNES is licensed under the MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.forcetower.unes.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.TypedArray
import android.database.DataSetObserver
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.Interpolator

import com.forcetower.unes.R
import com.forcetower.unes.core.util.AnimUtils

import java.util.Arrays

import androidx.viewpager.widget.ViewPager

class InkPageIndicator @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : View(context, attrs, defStyle), ViewPager.OnPageChangeListener, View.OnAttachStateChangeListener {

    // configurable attributes
    private val dotDiameter: Int
    private val gap: Int
    private val animDuration: Long
    private var unselectedColour: Int = 0
    private var selectedColour: Int = 0

    // derived from attributes
    private val dotRadius: Float
    private val halfDotRadius: Float
    private val animHalfDuration: Long
    private var dotTopY: Float = 0.toFloat()
    private var dotCenterY: Float = 0.toFloat()
    private var dotBottomY: Float = 0.toFloat()

    // ViewPager
    private var viewPager: ViewPager? = null

    // state
    private var pageCount: Int = 0
    private var currentPage: Int = 0
    private var previousPage: Int = 0
    private var selectedDotX: Float = 0.toFloat()
    private var selectedDotInPosition: Boolean = false
    private var dotCenterX: FloatArray? = null
    private var joiningFractions: FloatArray? = null
    private var retreatingJoinX1: Float = 0.toFloat()
    private var retreatingJoinX2: Float = 0.toFloat()
    private var dotRevealFractions: FloatArray? = null
    private var pageChanging: Boolean = false
    private var isAttachedToWindowOver: Boolean = false

    // drawing
    private val unselectedPaint: Paint
    private val selectedPaint: Paint
    private val combinedUnselectedPath: Path
    private val unselectedDotPath: Path
    private val unselectedDotLeftPath: Path
    private val unselectedDotRightPath: Path
    private val rectF: RectF

    private val joiningAnimationSet: AnimatorSet? = null
    private var retreatAnimation: PendingRetreatAnimator? = null
    private var revealAnimations: Array<PendingRevealAnimator?>? = null
    private val interpolator: Interpolator?

    // working values for beziers
    private var endX1: Float = 0.toFloat()
    private var endY1: Float = 0.toFloat()
    private var endX2: Float = 0.toFloat()
    private var endY2: Float = 0.toFloat()
    private var controlX1: Float = 0.toFloat()
    private var controlY1: Float = 0.toFloat()
    private var controlX2: Float = 0.toFloat()
    private var controlY2: Float = 0.toFloat()

    private val desiredHeight: Int
        get() = paddingTop + dotDiameter + paddingBottom

    private val requiredWidth: Int
        get() = pageCount * dotDiameter + (pageCount - 1) * gap

    private val desiredWidth: Int
        get() = paddingLeft + requiredWidth + paddingRight

    private val retreatingJoinPath: Path
        get() {
            unselectedDotPath.rewind()
            rectF.set(retreatingJoinX1, dotTopY, retreatingJoinX2, dotBottomY)
            unselectedDotPath.addRoundRect(rectF, dotRadius, dotRadius, Path.Direction.CW)
            return unselectedDotPath
        }

    init {

        val density = context.resources.displayMetrics.density.toInt()

        // Load attributes
        val a = getContext().obtainStyledAttributes(
                attrs, R.styleable.InkPageIndicator, defStyle, 0)

        dotDiameter = a.getDimensionPixelSize(R.styleable.InkPageIndicator_dotDiameter,
                DEFAULT_DOT_SIZE * density)
        dotRadius = (dotDiameter / 2).toFloat()
        halfDotRadius = dotRadius / 2
        gap = a.getDimensionPixelSize(R.styleable.InkPageIndicator_dotGap,
                DEFAULT_GAP * density)
        animDuration = a.getInteger(R.styleable.InkPageIndicator_animationDuration,
                DEFAULT_ANIM_DURATION).toLong()
        animHalfDuration = animDuration / 2
        unselectedColour = a.getColor(R.styleable.InkPageIndicator_pageIndicatorColor,
                DEFAULT_UNSELECTED_COLOUR)
        selectedColour = a.getColor(R.styleable.InkPageIndicator_currentPageIndicatorColor,
                DEFAULT_SELECTED_COLOUR)

        a.recycle()

        unselectedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        unselectedPaint.color = unselectedColour
        selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        selectedPaint.color = selectedColour
        interpolator = AnimUtils.getFastOutSlowInInterpolator(context)

        // create paths & rect now – reuse & rewind later
        combinedUnselectedPath = Path()
        unselectedDotPath = Path()
        unselectedDotLeftPath = Path()
        unselectedDotRightPath = Path()
        rectF = RectF()

        addOnAttachStateChangeListener(this)
    }

    fun setViewPager(viewPager: ViewPager) {
        this.viewPager = viewPager
        viewPager.addOnPageChangeListener(this)
        setPageCount(viewPager.adapter!!.count)
        viewPager.adapter!!.registerDataSetObserver(object : DataSetObserver() {
            override fun onChanged() {
                setPageCount(this@InkPageIndicator.viewPager!!.adapter!!.count)
            }
        })
        setCurrentPageImmediate()
    }

    fun setUnselectedColour(unselectedColour: Int) {
        this.unselectedColour = unselectedColour
        unselectedPaint.color = unselectedColour
        invalidate()
    }

    fun setSelectedColour(selectedColour: Int) {
        this.selectedColour = selectedColour
        selectedPaint.color = selectedColour
        invalidate()
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        if (isAttachedToWindow) {
            var fraction = positionOffset
            val currentPosition = if (pageChanging) previousPage else currentPage
            var leftDotPosition = position
            // when swiping from #2 to #1 ViewPager reports position as 1 and a descending offset
            // need to convert this into our left-dot-based 'coordinate space'
            if (currentPosition != position) {
                fraction = 1f - positionOffset

                // if user scrolls completely to next page then the position param updates to that
                // new page but we're not ready to switch our 'current' page yet so adjust for that
                if (fraction == 1f) {
                    leftDotPosition = Math.min(currentPosition, position)
                }
            }
            setJoiningFraction(leftDotPosition, fraction)
        }
    }

    override fun onPageSelected(position: Int) {
        if (isAttachedToWindow) {
            // this is the main event we're interested in!
            setSelectedPage(position)
        } else {
            // when not attached, don't animate the move, just store immediately
            setCurrentPageImmediate()
        }
    }

    override fun onPageScrollStateChanged(state: Int) {
        // nothing to do
    }

    private fun setPageCount(pages: Int) {
        pageCount = pages
        resetState()
        requestLayout()
    }

    private fun calculateDotPositions(width: Int, height: Int) {
        val left = paddingLeft
        val top = paddingTop
        val right = width - paddingRight
        val bottom = height - paddingBottom

        val requiredWidth = requiredWidth
        val startLeft = left.toFloat() + ((right - left - requiredWidth) / 2).toFloat() + dotRadius

        dotCenterX = FloatArray(pageCount)
        for (i in 0 until pageCount) {
            dotCenterX!![i] = startLeft + i * (dotDiameter + gap)
        }
        // todo just top aligning for now… should make this smarter
        dotTopY = top.toFloat()
        dotCenterY = top + dotRadius
        dotBottomY = (top + dotDiameter).toFloat()

        setCurrentPageImmediate()
    }

    private fun setCurrentPageImmediate() {
        if (viewPager != null) {
            currentPage = viewPager!!.currentItem
        } else {
            currentPage = 0
        }
        if (dotCenterX != null) {
            if (currentPage < dotCenterX!!.size)
                selectedDotX = dotCenterX!![currentPage]
        }
    }

    private fun resetState() {
        joiningFractions = FloatArray(pageCount - 1)
        Arrays.fill(joiningFractions!!, 0f)
        dotRevealFractions = FloatArray(pageCount)
        Arrays.fill(dotRevealFractions!!, 0f)
        retreatingJoinX1 = INVALID_FRACTION
        retreatingJoinX2 = INVALID_FRACTION
        selectedDotInPosition = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        val desiredHeight = desiredHeight
        val height: Int
        height = when (View.MeasureSpec.getMode(heightMeasureSpec)) {
            View.MeasureSpec.EXACTLY -> View.MeasureSpec.getSize(heightMeasureSpec)
            View.MeasureSpec.AT_MOST -> Math.min(desiredHeight, View.MeasureSpec.getSize(heightMeasureSpec))
            View.MeasureSpec.UNSPECIFIED -> desiredHeight
            else -> desiredHeight
        }

        val desiredWidth = desiredWidth
        val width: Int
        width = when (View.MeasureSpec.getMode(widthMeasureSpec)) {
            View.MeasureSpec.EXACTLY -> View.MeasureSpec.getSize(widthMeasureSpec)
            View.MeasureSpec.AT_MOST -> Math.min(desiredWidth, View.MeasureSpec.getSize(widthMeasureSpec))
            View.MeasureSpec.UNSPECIFIED -> desiredWidth
            else -> desiredWidth
        }
        setMeasuredDimension(width, height)
        calculateDotPositions(width, height)
    }

    override fun onViewAttachedToWindow(view: View) {
        isAttachedToWindowOver = true
    }

    override fun onViewDetachedFromWindow(view: View) {
        isAttachedToWindowOver = false
    }

    override fun onDraw(canvas: Canvas) {
        if (viewPager == null || pageCount == 0) return
        drawUnselected(canvas)
        drawSelected(canvas)
    }

    private fun drawUnselected(canvas: Canvas) {

        combinedUnselectedPath.rewind()

        // draw any settled, revealing or joining dots
        for (page in 0 until pageCount) {
            val nextXIndex = if (page == pageCount - 1) page else page + 1
            combinedUnselectedPath.op(getUnselectedPath(page,
                    dotCenterX!![page],
                    dotCenterX!![nextXIndex],
                    if (page == pageCount - 1) INVALID_FRACTION else joiningFractions!![page],
                    dotRevealFractions!![page]), Path.Op.UNION)
        }
        // draw any retreating joins
        if (retreatingJoinX1 != INVALID_FRACTION) {
            combinedUnselectedPath.op(retreatingJoinPath, Path.Op.UNION)
        }
        canvas.drawPath(combinedUnselectedPath, unselectedPaint)
    }

    /**
     *
     * Unselected dots can be in 6 states:
     *
     * #1 At rest
     * #2 Joining neighbour, still separate
     * #3 Joining neighbour, combined curved
     * #4 Joining neighbour, combined straight
     * #5 Join retreating
     * #6 Dot re-showing / revealing
     *
     * It can also be in a combination of these states e.g. joining one neighbour while
     * retreating from another.  We therefore create a Path so that we can examine each
     * dot pair separately and later take the union for these cases.
     *
     * This function returns a path for the given dot **and any action to it's right** e.g. joining
     * or retreating from it's neighbour
     *
     * @param page
     * @return
     */
    private fun getUnselectedPath(page: Int,
                                  centerX: Float,
                                  nextCenterX: Float,
                                  joiningFraction: Float,
                                  dotRevealFraction: Float): Path {

        unselectedDotPath.rewind()

        if ((joiningFraction == 0f || joiningFraction == INVALID_FRACTION)
                && dotRevealFraction == 0f
                && !(page == currentPage && selectedDotInPosition == true)) {

            // case #1 – At rest
            unselectedDotPath.addCircle(dotCenterX!![page], dotCenterY, dotRadius, Path.Direction.CW)
        }

        if (joiningFraction > 0f && joiningFraction <= 0.5f
                && retreatingJoinX1 == INVALID_FRACTION) {

            // case #2 – Joining neighbour, still separate

            // start with the left dot
            unselectedDotLeftPath.rewind()

            // start at the bottom center
            unselectedDotLeftPath.moveTo(centerX, dotBottomY)

            // semi circle to the top center
            rectF.set(centerX - dotRadius, dotTopY, centerX + dotRadius, dotBottomY)
            unselectedDotLeftPath.arcTo(rectF, 90f, 180f, true)

            // cubic to the right middle
            endX1 = centerX + dotRadius + joiningFraction * gap
            endY1 = dotCenterY
            controlX1 = centerX + halfDotRadius
            controlY1 = dotTopY
            controlX2 = endX1
            controlY2 = endY1 - halfDotRadius
            unselectedDotLeftPath.cubicTo(controlX1, controlY1,
                    controlX2, controlY2,
                    endX1, endY1)

            // cubic back to the bottom center
            endX2 = centerX
            endY2 = dotBottomY
            controlX1 = endX1
            controlY1 = endY1 + halfDotRadius
            controlX2 = centerX + halfDotRadius
            controlY2 = dotBottomY
            unselectedDotLeftPath.cubicTo(controlX1, controlY1,
                    controlX2, controlY2,
                    endX2, endY2)

            unselectedDotPath.op(unselectedDotLeftPath, Path.Op.UNION)

            // now do the next dot to the right
            unselectedDotRightPath.rewind()

            // start at the bottom center
            unselectedDotRightPath.moveTo(nextCenterX, dotBottomY)

            // semi circle to the top center
            rectF.set(nextCenterX - dotRadius, dotTopY, nextCenterX + dotRadius, dotBottomY)
            unselectedDotRightPath.arcTo(rectF, 90f, -180f, true)

            // cubic to the left middle
            endX1 = nextCenterX - dotRadius - joiningFraction * gap
            endY1 = dotCenterY
            controlX1 = nextCenterX - halfDotRadius
            controlY1 = dotTopY
            controlX2 = endX1
            controlY2 = endY1 - halfDotRadius
            unselectedDotRightPath.cubicTo(controlX1, controlY1,
                    controlX2, controlY2,
                    endX1, endY1)

            // cubic back to the bottom center
            endX2 = nextCenterX
            endY2 = dotBottomY
            controlX1 = endX1
            controlY1 = endY1 + halfDotRadius
            controlX2 = endX2 - halfDotRadius
            controlY2 = dotBottomY
            unselectedDotRightPath.cubicTo(controlX1, controlY1,
                    controlX2, controlY2,
                    endX2, endY2)
            unselectedDotPath.op(unselectedDotRightPath, Path.Op.UNION)
        }

        if (joiningFraction > 0.5f && joiningFraction < 1f
                && retreatingJoinX1 == INVALID_FRACTION) {

            // case #3 – Joining neighbour, combined curved

            // adjust the fraction so that it goes from 0.3 -> 1 to produce a more realistic 'join'
            val adjustedFraction = (joiningFraction - 0.2f) * 1.25f

            // start in the bottom left
            unselectedDotPath.moveTo(centerX, dotBottomY)

            // semi-circle to the top left
            rectF.set(centerX - dotRadius, dotTopY, centerX + dotRadius, dotBottomY)
            unselectedDotPath.arcTo(rectF, 90f, 180f, true)

            // bezier to the middle top of the join
            endX1 = centerX + dotRadius + (gap / 2).toFloat()
            endY1 = dotCenterY - adjustedFraction * dotRadius
            controlX1 = endX1 - adjustedFraction * dotRadius
            controlY1 = dotTopY
            controlX2 = endX1 - (1 - adjustedFraction) * dotRadius
            controlY2 = endY1
            unselectedDotPath.cubicTo(controlX1, controlY1,
                    controlX2, controlY2,
                    endX1, endY1)

            // bezier to the top right of the join
            endX2 = nextCenterX
            endY2 = dotTopY
            controlX1 = endX1 + (1 - adjustedFraction) * dotRadius
            controlY1 = endY1
            controlX2 = endX1 + adjustedFraction * dotRadius
            controlY2 = dotTopY
            unselectedDotPath.cubicTo(controlX1, controlY1,
                    controlX2, controlY2,
                    endX2, endY2)

            // semi-circle to the bottom right
            rectF.set(nextCenterX - dotRadius, dotTopY, nextCenterX + dotRadius, dotBottomY)
            unselectedDotPath.arcTo(rectF, 270f, 180f, true)

            // bezier to the middle bottom of the join
            // endX1 stays the same
            endY1 = dotCenterY + adjustedFraction * dotRadius
            controlX1 = endX1 + adjustedFraction * dotRadius
            controlY1 = dotBottomY
            controlX2 = endX1 + (1 - adjustedFraction) * dotRadius
            controlY2 = endY1
            unselectedDotPath.cubicTo(controlX1, controlY1,
                    controlX2, controlY2,
                    endX1, endY1)

            // bezier back to the start point in the bottom left
            endX2 = centerX
            endY2 = dotBottomY
            controlX1 = endX1 - (1 - adjustedFraction) * dotRadius
            controlY1 = endY1
            controlX2 = endX1 - adjustedFraction * dotRadius
            controlY2 = endY2
            unselectedDotPath.cubicTo(controlX1, controlY1,
                    controlX2, controlY2,
                    endX2, endY2)
        }
        if (joiningFraction == 1f && retreatingJoinX1 == INVALID_FRACTION) {

            // case #4 Joining neighbour, combined straight technically we could use case 3 for this
            // situation as well but assume that this is an optimization rather than faffing around
            // with beziers just to draw a rounded rect
            rectF.set(centerX - dotRadius, dotTopY, nextCenterX + dotRadius, dotBottomY)
            unselectedDotPath.addRoundRect(rectF, dotRadius, dotRadius, Path.Direction.CW)
        }

        // case #5 is handled by #getRetreatingJoinPath()
        // this is done separately so that we can have a single retreating path spanning
        // multiple dots and therefore animate it's movement smoothly

        if (dotRevealFraction > MINIMAL_REVEAL) {

            // case #6 – previously hidden dot revealing
            unselectedDotPath.addCircle(centerX, dotCenterY, dotRevealFraction * dotRadius,
                    Path.Direction.CW)
        }

        return unselectedDotPath
    }

    private fun drawSelected(canvas: Canvas) {
        canvas.drawCircle(selectedDotX, dotCenterY, dotRadius, selectedPaint)
    }

    private fun setSelectedPage(now: Int) {
        if (now == currentPage) return

        pageChanging = true
        previousPage = currentPage
        currentPage = now
        val steps = Math.abs(now - previousPage)

        if (steps > 1) {
            if (now > previousPage) {
                for (i in 0 until steps) {
                    setJoiningFraction(previousPage + i, 1f)
                }
            } else {
                for (i in -1 downTo -steps + 1) {
                    setJoiningFraction(previousPage + i, 1f)
                }
            }
        }

        // create the anim to move the selected dot – this animator will kick off
        // retreat animations when it has moved 75% of the way.
        // The retreat animation in turn will kick of reveal anims when the
        // retreat has passed any dots to be revealed
        val moveAnimation = createMoveSelectedAnimator(dotCenterX!![now], previousPage, now, steps)
        moveAnimation.start()
    }

    private fun createMoveSelectedAnimator(
            moveTo: Float, was: Int, now: Int, steps: Int): ValueAnimator {

        // create the actual move animator
        val moveSelected = ValueAnimator.ofFloat(selectedDotX, moveTo)

        // also set up a pending retreat anim – this starts when the move is 75% complete
        retreatAnimation = PendingRetreatAnimator(was, now, steps,
                if (now > was)
                    RightwardStartPredicate(moveTo - (moveTo - selectedDotX) * 0.25f)
                else
                    LeftwardStartPredicate(moveTo + (selectedDotX - moveTo) * 0.25f))
        retreatAnimation!!.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                resetState()
                pageChanging = false
            }
        })
        moveSelected.addUpdateListener { valueAnimator ->
            // todo avoid autoboxing
            selectedDotX = valueAnimator.animatedValue as Float
            retreatAnimation!!.startIfNecessary(selectedDotX)
            postInvalidateOnAnimation()
        }
        moveSelected.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                // set a flag so that we continue to draw the unselected dot in the target position
                // until the selected dot has finished moving into place
                selectedDotInPosition = false
            }

            override fun onAnimationEnd(animation: Animator) {
                // set a flag when anim finishes so that we don't draw both selected & unselected
                // page dots
                selectedDotInPosition = true
            }
        })
        // slightly delay the start to give the joins a chance to run
        // unless dot isn't in position yet – then don't delay!
        moveSelected.startDelay = if (selectedDotInPosition) animDuration / 4L else 0L
        moveSelected.duration = animDuration * 3L / 4L
        moveSelected.interpolator = interpolator
        return moveSelected
    }

    private fun setJoiningFraction(leftDot: Int, fraction: Float) {
        if (leftDot < joiningFractions!!.size) {

            if (leftDot == 1) {
                Log.d("PageIndicator", "dot 1 fraction:\t$fraction")
            }

            joiningFractions!![leftDot] = fraction
            postInvalidateOnAnimation()
        }
    }

    private fun clearJoiningFractions() {
        Arrays.fill(joiningFractions!!, 0f)
        postInvalidateOnAnimation()
    }

    private fun setDotRevealFraction(dot: Int, fraction: Float) {
        dotRevealFractions!![dot] = fraction
        postInvalidateOnAnimation()
    }

    private fun cancelJoiningAnimations() {
        if (joiningAnimationSet != null && joiningAnimationSet.isRunning) {
            joiningAnimationSet.cancel()
        }
    }

    /**
     * A [ValueAnimator] that starts once a given predicate returns true.
     */
    abstract inner class PendingStartAnimator(protected var predicate: StartPredicate) : ValueAnimator() {

        private var hasStarted: Boolean = false

        init {
            hasStarted = false
        }

        fun startIfNecessary(currentValue: Float) {
            if (!hasStarted && predicate.shouldStart(currentValue)) {
                start()
                hasStarted = true
            }
        }
    }

    /**
     * An Animator that shows and then shrinks a retreating join between the previous and newly
     * selected pages.  This also sets up some pending dot reveals – to be started when the retreat
     * has passed the dot to be revealed.
     */
    inner class PendingRetreatAnimator(was: Int, now: Int, steps: Int, predicate: StartPredicate) : PendingStartAnimator(predicate) {

        init {
            duration = animHalfDuration
            interpolator = interpolator

            // work out the start/end values of the retreating join from the direction we're
            // travelling in.  Also look at the current selected dot position, i.e. we're moving on
            // before a prior anim has finished.
            val initialX1 = if (now > was)
                Math.min(dotCenterX!![was], selectedDotX) - dotRadius
            else
                dotCenterX!![now] - dotRadius
            val finalX1 = if (now > was)
                dotCenterX!![now] - dotRadius
            else
                dotCenterX!![now] - dotRadius
            val initialX2 = if (now > was)
                dotCenterX!![now] + dotRadius
            else
                Math.max(dotCenterX!![was], selectedDotX) + dotRadius
            val finalX2 = if (now > was)
                dotCenterX!![now] + dotRadius
            else
                dotCenterX!![now] + dotRadius

            revealAnimations = arrayOfNulls(steps)
            // hold on to the indexes of the dots that will be hidden by the retreat so that
            // we can initialize their revealFraction's i.e. make sure they're hidden while the
            // reveal animation runs
            val dotsToHide = IntArray(steps)
            if (initialX1 != finalX1) { // rightward retreat
                setFloatValues(initialX1, finalX1)
                // create the reveal animations that will run when the retreat passes them
                for (i in 0 until steps) {
                    revealAnimations!![i] = PendingRevealAnimator(was + i,
                            RightwardStartPredicate(dotCenterX!![was + i]))
                    dotsToHide[i] = was + i
                }
                addUpdateListener { valueAnimator ->
                    // todo avoid autoboxing
                    retreatingJoinX1 = valueAnimator.animatedValue as Float
                    postInvalidateOnAnimation()
                    // start any reveal animations if we've passed them
                    for (pendingReveal in revealAnimations!!) {
                        pendingReveal!!.startIfNecessary(retreatingJoinX1)
                    }
                }
            } else { // (initialX2 != finalX2) leftward retreat
                setFloatValues(initialX2, finalX2)
                // create the reveal animations that will run when the retreat passes them
                for (i in 0 until steps) {
                    revealAnimations!![i] = PendingRevealAnimator(was - i,
                            LeftwardStartPredicate(dotCenterX!![was - i]))
                    dotsToHide[i] = was - i
                }
                addUpdateListener { valueAnimator ->
                    // todo avoid autoboxing
                    retreatingJoinX2 = valueAnimator.animatedValue as Float
                    postInvalidateOnAnimation()
                    // start any reveal animations if we've passed them
                    for (pendingReveal in revealAnimations!!) {
                        pendingReveal!!.startIfNecessary(retreatingJoinX2)
                    }
                }
            }

            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    cancelJoiningAnimations()
                    clearJoiningFractions()
                    // we need to set this so that the dots are hidden until the reveal anim runs
                    for (dot in dotsToHide) {
                        setDotRevealFraction(dot, MINIMAL_REVEAL)
                    }
                    retreatingJoinX1 = initialX1
                    retreatingJoinX2 = initialX2
                    postInvalidateOnAnimation()
                }

                override fun onAnimationEnd(animation: Animator) {
                    retreatingJoinX1 = INVALID_FRACTION
                    retreatingJoinX2 = INVALID_FRACTION
                    postInvalidateOnAnimation()
                }
            })
        }
    }

    /**
     * An Animator that animates a given dot's revealFraction i.e. scales it up
     */
    inner class PendingRevealAnimator(private val dot: Int, predicate: StartPredicate) : PendingStartAnimator(predicate) {

        init {
            setFloatValues(MINIMAL_REVEAL, 1f)
            duration = animHalfDuration
            interpolator = interpolator
            addUpdateListener { valueAnimator ->
                // todo avoid autoboxing
                setDotRevealFraction(this@PendingRevealAnimator.dot,
                        valueAnimator.animatedValue as Float)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    setDotRevealFraction(this@PendingRevealAnimator.dot, 0f)
                    postInvalidateOnAnimation()
                }
            })
        }
    }

    /**
     * A predicate used to start an animation when a test passes
     */
    abstract inner class StartPredicate(protected var thresholdValue: Float) {

        internal abstract fun shouldStart(currentValue: Float): Boolean

    }

    /**
     * A predicate used to start an animation when a given value is greater than a threshold
     */
    inner class RightwardStartPredicate(thresholdValue: Float) : StartPredicate(thresholdValue) {

        override fun shouldStart(currentValue: Float): Boolean {
            return currentValue > thresholdValue
        }
    }

    /**
     * A predicate used to start an animation then a given value is less than a threshold
     */
    inner class LeftwardStartPredicate(thresholdValue: Float) : StartPredicate(thresholdValue) {

        override fun shouldStart(currentValue: Float): Boolean {
            return currentValue < thresholdValue
        }
    }

    companion object {

        // defaults
        private const val DEFAULT_DOT_SIZE = 8                      // dp
        private const val DEFAULT_GAP = 12                          // dp
        private const val DEFAULT_ANIM_DURATION = 400               // ms
        private const val DEFAULT_UNSELECTED_COLOUR = -0x7f000001    // 50% white
        private const val DEFAULT_SELECTED_COLOUR = -0x1      // 100% white

        // constants
        private const val INVALID_FRACTION = -1f
        private const val MINIMAL_REVEAL = 0.00001f
    }
}
