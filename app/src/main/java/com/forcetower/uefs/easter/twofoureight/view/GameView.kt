/*
 * Copyright (c) 2019.
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

package com.forcetower.uefs.easter.twofoureight.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.forcetower.uefs.R
import timber.log.Timber

/**
 * Created by João Paulo on 02/06/2018.
 */
class GameView : View {

    private val mPaint = Paint()

    // Layout variables
    private var mCellSize = 0
    private var mTextSize = 0f
    private var mCellTextSize = 0f
    private var mGridWidth = 0
    private var mStartingX: Int = 0
    private var mStartingY: Int = 0
    private var mEndingX: Int = 0
    private var mEndingY: Int = 0

    // Assets
    private var mBackgroundRectangle: Drawable? = null
    private lateinit var mCellRectangle: Array<Drawable?>
    private lateinit var mBitmapCell: Array<BitmapDrawable?>
    private var mLightUpRectangle: Drawable? = null
    private var mFadeRectangle: Drawable? = null

    private var mLastFPSTime = System.nanoTime()
    private var mCurrentTime = System.nanoTime()

    private var mGameOverTextSize: Float = 0.toFloat()

    private var mRefreshLastTime = true
    private var mNumberOfSquaresX: Int = 0
    private var mNumberOfSquaresY: Int = 0
    private var mGameState: Game.State? = null
    private var mAnimationGrid = AnimationGrid(4, 4)
    private var mBackground: Bitmap? = null
    private var mGameGrid: GameGrid? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        try {
            setSquareCount(Game.DEFAULT_HEIGHT_X, Game.DEFAULT_WIDTH_Y)
            mCellRectangle = arrayOfNulls<Drawable?>(Game.DEFAULT_TILE_TYPES)
            mBitmapCell = arrayOfNulls(Game.DEFAULT_TILE_TYPES)

            updateGrid(GameGrid(4, 4))
            // Getting assets
            mBackgroundRectangle = context.getDrawable(R.drawable.background_rectangle)
            mCellRectangle[0] = context.getDrawable(R.drawable.cell_rectangle)
            mCellRectangle[1] = context.getDrawable(R.drawable.cell_rectangle_2)
            mCellRectangle[2] = context.getDrawable(R.drawable.cell_rectangle_4)
            mCellRectangle[3] = context.getDrawable(R.drawable.cell_rectangle_8)
            mCellRectangle[4] = context.getDrawable(R.drawable.cell_rectangle_16)
            mCellRectangle[5] = context.getDrawable(R.drawable.cell_rectangle_32)
            mCellRectangle[6] = context.getDrawable(R.drawable.cell_rectangle_64)
            mCellRectangle[7] = context.getDrawable(R.drawable.cell_rectangle_128)
            mCellRectangle[8] = context.getDrawable(R.drawable.cell_rectangle_256)
            mCellRectangle[9] = context.getDrawable(R.drawable.cell_rectangle_512)
            mCellRectangle[10] = context.getDrawable(R.drawable.cell_rectangle_1024)
            mCellRectangle[11] = context.getDrawable(R.drawable.cell_rectangle_2048)
            mCellRectangle[12] = context.getDrawable(R.drawable.cell_rectangle_4096)
            mCellRectangle[13] = context.getDrawable(R.drawable.cell_rectangle_8192)
            mCellRectangle[14] = context.getDrawable(R.drawable.cell_rectangle_16384)
            mCellRectangle[15] = context.getDrawable(R.drawable.cell_rectangle_32768)
            mCellRectangle[16] = context.getDrawable(R.drawable.cell_rectangle_65536)
            mCellRectangle[17] = context.getDrawable(R.drawable.cell_rectangle_131072)
            mCellRectangle[18] = context.getDrawable(R.drawable.cell_rectangle_262144)
            mCellRectangle[19] = context.getDrawable(R.drawable.cell_rectangle_524288)
            for (xx in 20 until mCellRectangle.size) {
                mCellRectangle[xx] = context.getDrawable(R.drawable.cell_rectangle_524288)
            }

            mLightUpRectangle = context.getDrawable(R.drawable.light_up_rectangle)
            mFadeRectangle = context.getDrawable(R.drawable.fade_rectangle)
            mPaint.isAntiAlias = true
        } catch (e: Exception) {
            Timber.e("Failed loading assets")
        }
    }

    fun updateGrid(grid: GameGrid) {
        mGameGrid = grid
    }

    fun setGameState(state: Game.State) {
        mGameState = state
    }

    private fun setSquareCount(x: Int, y: Int) {
        mNumberOfSquaresX = x
        mNumberOfSquaresY = y
        mAnimationGrid = AnimationGrid(mNumberOfSquaresX, mNumberOfSquaresY)
    }

    override fun onSizeChanged(width: Int, height: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(width, height, oldw, oldh)
        getLayout(width, height)
        createBackgroundBitmap(width, height)
        createBitmapCells()
    }

    public override fun onDraw(canvas: Canvas) {
        // Reset the transparency of the screen
        canvas.drawBitmap(mBackground!!, 0f, 0f, mPaint)
        drawTiles(canvas)

        // Refresh the screen if there is still an animation running
        if (mAnimationGrid.isAnimationActive) {
            // invalidate(mStartingX, mStartingY, mEndingX, mEndingY)
            invalidate()
            tick()
            // Refresh one last time on game end.
        } else if (!(mGameState != Game.State.WON && mGameState != Game.State.LOST) && mRefreshLastTime) {
            invalidate()
            mRefreshLastTime = false
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = View.MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = View.MeasureSpec.getSize(heightMeasureSpec)

        val size: Int
        if (widthMode == View.MeasureSpec.EXACTLY && widthSize > 0) {
            size = widthSize
        } else if (heightMode == View.MeasureSpec.EXACTLY && heightSize > 0) {
            size = heightSize
        } else {
            size = if (widthSize < heightSize) widthSize else heightSize
        }
        val finalMeasureSpec = View.MeasureSpec.makeMeasureSpec(size, View.MeasureSpec.EXACTLY)
        super.onMeasure(finalMeasureSpec, finalMeasureSpec)
    }

    private fun getLayout(width: Int, height: Int) {
        mCellSize = Math.min(width / (mNumberOfSquaresX + 1), height / (mNumberOfSquaresY + 1))
        mGridWidth = mCellSize / 5
        val boardMiddleX = width / 2
        val boardMiddleY = height / 2

        mPaint.textAlign = Paint.Align.CENTER
        mPaint.textSize = mCellSize.toFloat()
        mTextSize = mCellSize * mCellSize / Math.max(mCellSize.toFloat(), mPaint.measureText("0000"))
        mCellTextSize = mTextSize

        mGameOverTextSize = mTextSize * 2

        // Grid Dimensions
        val halfNumSquaresX = mNumberOfSquaresX / 2.0
        val halfNumSquaresY = mNumberOfSquaresY / 2.0

        mStartingX =
            (boardMiddleX.toDouble() - (mCellSize + mGridWidth) * halfNumSquaresX - (mGridWidth / 2).toDouble()).toInt()
        mEndingX =
            (boardMiddleX.toDouble() + (mCellSize + mGridWidth) * halfNumSquaresX + (mGridWidth / 2).toDouble()).toInt()
        mStartingY =
            (boardMiddleY.toDouble() - (mCellSize + mGridWidth) * halfNumSquaresY - (mGridWidth / 2).toDouble()).toInt()
        mEndingY =
            (boardMiddleY.toDouble() + (mCellSize + mGridWidth) * halfNumSquaresY + (mGridWidth / 2).toDouble()).toInt()
        reSyncTime()
    }

    private fun drawDrawable(
        canvas: Canvas,
        draw: Drawable,
        startingX: Int,
        startingY: Int,
        endingX: Int,
        endingY: Int
    ) {
        draw.setBounds(startingX, startingY, endingX, endingY)
        draw.draw(canvas)
    }

    private fun createBackgroundBitmap(width: Int, height: Int) {
        mBackground = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mBackground!!)
        drawTileBackground(canvas)
        drawEmptyTiles(canvas)
    }

    private fun drawTileBackground(canvas: Canvas) {
        drawDrawable(canvas, mBackgroundRectangle!!, mStartingX, mStartingY, mEndingX, mEndingY)
    }

    private fun drawEmptyTiles(canvas: Canvas) {
        // Outputting the game mGameGrid
        for (xx in 0 until mNumberOfSquaresX) {
            for (yy in 0 until mNumberOfSquaresY) {
                val sX = mStartingX + mGridWidth + (mCellSize + mGridWidth) * xx
                val eX = sX + mCellSize
                val sY = mStartingY + mGridWidth + (mCellSize + mGridWidth) * yy
                val eY = sY + mCellSize
                drawDrawable(canvas, mCellRectangle[0]!!, sX, sY, eX, eY)
            }
        }
    }

    private fun drawTiles(canvas: Canvas) {
        mPaint.textSize = mTextSize
        mPaint.textAlign = Paint.Align.CENTER
        // Outputting the individual cells
        for (xx in 0 until mNumberOfSquaresX) {
            for (yy in 0 until mNumberOfSquaresY) {
                val sX = mStartingX + mGridWidth + (mCellSize + mGridWidth) * xx
                val eX = sX + mCellSize
                val sY = mStartingY + mGridWidth + (mCellSize + mGridWidth) * yy
                val eY = sY + mCellSize

                val currentTile = mGameGrid!!.getCellContent(xx, yy)
                if (currentTile != null) {
                    // Get and represent the value of the tile
                    val value = currentTile.value
                    val index = log2(value)

                    // Check for any active animations
                    val aArray = mAnimationGrid.getAnimationCell(xx, yy)
                    var animated = false
                    for (i in aArray.indices.reversed()) {
                        val aCell = aArray[i]
                        // If this animation is not active, skip it
                        if (aCell.animationType == SPAWN_ANIMATION) {
                            animated = true
                        }
                        if (!aCell.isActive) {
                            continue
                        }

                        if (aCell.animationType == SPAWN_ANIMATION) { // Spawning animation
                            val percentDone = aCell.percentageDone
                            val textScaleSize = percentDone.toFloat()
                            mPaint.textSize = mTextSize * textScaleSize

                            val cellScaleSize = mCellSize / 2 * (1 - textScaleSize)
                            mBitmapCell[index]!!.setBounds(
                                (sX + cellScaleSize).toInt(),
                                (sY + cellScaleSize).toInt(),
                                (eX - cellScaleSize).toInt(),
                                (eY - cellScaleSize).toInt()
                            )
                            mBitmapCell[index]!!.draw(canvas)
                        } else if (aCell.animationType == MERGE_ANIMATION) { // Merging Animation
                            val percentDone = aCell.percentageDone
                            val textScaleSize = (1.0 + INITIAL_VELOCITY * percentDone +
                                MERGING_ACCELERATION.toDouble() * percentDone * percentDone / 2).toFloat()
                            mPaint.textSize = mTextSize * textScaleSize

                            val cellScaleSize = mCellSize / 2 * (1 - textScaleSize)
                            mBitmapCell[index]!!.setBounds(
                                (sX + cellScaleSize).toInt(),
                                (sY + cellScaleSize).toInt(),
                                (eX - cellScaleSize).toInt(),
                                (eY - cellScaleSize).toInt()
                            )
                            mBitmapCell[index]!!.draw(canvas)
                        } else if (aCell.animationType == MOVE_ANIMATION) { // Moving animation
                            val percentDone = aCell.percentageDone
                            var tempIndex = index
                            if (aArray.size >= 2) {
                                tempIndex -= 1
                            }
                            val previousX = aCell.extras!![0]
                            val previousY = aCell.extras[1]
                            val currentX = currentTile.x
                            val currentY = currentTile.y
                            val dX =
                                ((currentX - previousX).toDouble() * (mCellSize + mGridWidth).toDouble() * (percentDone - 1) * 1.0).toInt()
                            val dY =
                                ((currentY - previousY).toDouble() * (mCellSize + mGridWidth).toDouble() * (percentDone - 1) * 1.0).toInt()
                            mBitmapCell[tempIndex]!!.setBounds(sX + dX, sY + dY, eX + dX, eY + dY)
                            mBitmapCell[tempIndex]!!.draw(canvas)
                        }
                        animated = true
                    }

                    // No active animations? Just draw the cell
                    if (!animated) {
                        mBitmapCell[index]!!.setBounds(sX, sY, eX, eY)
                        mBitmapCell[index]!!.draw(canvas)
                    }
                }
            }
        }
    }

    private fun createBitmapCells() {
        mPaint.textAlign = Paint.Align.CENTER
        for (xx in mBitmapCell.indices) {
            val value = Math.pow(2.0, xx.toDouble()).toInt()
            mPaint.textSize = mCellTextSize
            val tempTextSize = mCellTextSize * mCellSize.toFloat() * 0.9f / Math.max(
                mCellSize * 0.9f,
                mPaint.measureText(value.toString())
            )
            mPaint.textSize = tempTextSize
            val bitmap = Bitmap.createBitmap(mCellSize, mCellSize, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawDrawable(canvas, mCellRectangle[xx]!!, 0, 0, mCellSize, mCellSize)
            drawTileText(canvas, value, 0, 0)
            mBitmapCell[xx] = BitmapDrawable(resources, bitmap)
        }
    }

    private fun drawTileText(canvas: Canvas, value: Int, sX: Int, sY: Int) {
        val textShiftY = centerText()
        if (value == 2) {
            mPaint.color = ContextCompat.getColor(context, R.color.text_shadow)
            mPaint.setShadowLayer(2.0f, 0f, 0f, ContextCompat.getColor(context, R.color.text_white))
        } else {
            mPaint.color = ContextCompat.getColor(context, R.color.text_white)
            mPaint.setShadowLayer(2.0f, 0f, 0f, ContextCompat.getColor(context, R.color.text_shadow))
        }
        canvas.drawText("" + value, (sX + mCellSize / 2).toFloat(), (sY + mCellSize / 2 - textShiftY).toFloat(), mPaint)
    }

    private fun tick() {
        mCurrentTime = System.nanoTime()
        mAnimationGrid.tickAll(mCurrentTime - mLastFPSTime)
        mLastFPSTime = mCurrentTime
    }

    override fun performClick(): Boolean {
        Timber.d("Performed a click in the game")
        return super.performClick()
    }

    fun reSyncTime() {
        mLastFPSTime = System.nanoTime()
    }

    private fun centerText(): Int {
        return ((mPaint.descent() + mPaint.ascent()) / 2).toInt()
    }

    fun spawnTile(tile: Tile) {
        mAnimationGrid.startAnimation(
            tile.x, tile.y, SPAWN_ANIMATION,
            SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null
        ) // Direction: -1 = EXPANDING
    }

    fun cancelAnimations() {
        mAnimationGrid.cancelAnimations()
    }

    fun moveTile(x: Int, y: Int, extras: IntArray) {
        mAnimationGrid.startAnimation(x, y, MOVE_ANIMATION, MOVE_ANIMATION_TIME, 0, extras)
    }

    fun mergeTile(x: Int, y: Int) {
        mAnimationGrid.startAnimation(
            x, y, MERGE_ANIMATION,
            SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null
        )
    }

    fun endGame() {
        mAnimationGrid.startAnimation(
            -1,
            -1,
            FADE_GLOBAL_ANIMATION,
            NOTIFICATION_ANIMATION_TIME,
            NOTIFICATION_DELAY_TIME,
            null
        )
    }

    fun setRefreshLastTime(refreshLastTime: Boolean) {
        mRefreshLastTime = refreshLastTime
    }

    companion object {
        private const val BASE_ANIMATION_TIME = 100000000
        private const val MERGING_ACCELERATION = (-0.5).toFloat()
        private const val INITIAL_VELOCITY = (1 - MERGING_ACCELERATION) / 4
        private const val SPAWN_ANIMATION = -1
        private const val MOVE_ANIMATION = 0
        private const val MERGE_ANIMATION = 1
        private const val FADE_GLOBAL_ANIMATION = 0
        private const val MOVE_ANIMATION_TIME = BASE_ANIMATION_TIME.toLong()
        private const val SPAWN_ANIMATION_TIME = BASE_ANIMATION_TIME.toLong()
        private const val NOTIFICATION_ANIMATION_TIME = (BASE_ANIMATION_TIME * 5).toLong()
        private const val NOTIFICATION_DELAY_TIME = MOVE_ANIMATION_TIME + SPAWN_ANIMATION_TIME

        private fun log2(n: Int): Int {
            if (n <= 0) throw IllegalArgumentException()
            return 31 - Integer.numberOfLeadingZeros(n)
        }
    }
}
