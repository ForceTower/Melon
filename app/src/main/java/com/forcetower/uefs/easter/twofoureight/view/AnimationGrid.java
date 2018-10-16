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

package com.forcetower.uefs.easter.twofoureight.view;

import java.util.ArrayList;

class AnimationGrid {
    private final ArrayList<AnimationTile>[][] mAnimationGameGrid;
    private int mActiveAnimations = 0;
    private boolean mOneMoreFrame = false;
    private final ArrayList<AnimationTile> mGlobalAnimation = new ArrayList<>();

    AnimationGrid(int x, int y) {
        //noinspection unchecked
        mAnimationGameGrid = new ArrayList[x][y];
        for (int xx = 0; xx < x; xx++) {
            for (int yy = 0; yy < y; yy++) {
                mAnimationGameGrid[xx][yy] = new ArrayList<>();
            }
        }
    }

    public void startAnimation(int x, int y, int animationType, long length, long delay, int[] extras) {
        AnimationTile animationToAdd = new AnimationTile(x, y, animationType, length, delay, extras);
        if (x == -1 && y == -1) {
            mGlobalAnimation.add(animationToAdd);
        } else {
            mAnimationGameGrid[x][y].add(animationToAdd);
        }
        mActiveAnimations = mActiveAnimations + 1;
    }

    public void tickAll(long timeElapsed) {
        ArrayList<AnimationTile> cancelledAnimations = new ArrayList<>();
        for (AnimationTile animation : mGlobalAnimation) {
            animation.tick(timeElapsed);
            if (animation.animationDone()) {
                cancelledAnimations.add(animation);
                mActiveAnimations = mActiveAnimations - 1;
            }
        }

        for (ArrayList<AnimationTile>[] array : mAnimationGameGrid) {
            for (ArrayList<AnimationTile> list : array) {
                for (AnimationTile animation : list) {
                    animation.tick(timeElapsed);
                    if (animation.animationDone()) {
                        cancelledAnimations.add(animation);
                        mActiveAnimations = mActiveAnimations - 1;
                    }
                }
            }
        }

        for (AnimationTile animation : cancelledAnimations) {
            cancelAnimation(animation);
        }
    }

    public boolean isAnimationActive() {
        if (mActiveAnimations != 0) {
            mOneMoreFrame = true;
            return true;
        } else if (mOneMoreFrame) {
            mOneMoreFrame = false;
            return true;
        } else {
            return false;
        }
    }

    public ArrayList<AnimationTile> getAnimationCell(int x, int y) {
        return mAnimationGameGrid[x][y];
    }

    public void cancelAnimations() {
        for (ArrayList<AnimationTile>[] array : mAnimationGameGrid) {
            for (ArrayList<AnimationTile> list : array) {
                list.clear();
            }
        }
        mGlobalAnimation.clear();
        mActiveAnimations = 0;
    }

    void cancelAnimation(AnimationTile animation) {
        if (animation.getX() == -1 && animation.getY() == -1) {
            mGlobalAnimation.remove(animation);
        } else {
            mAnimationGameGrid[animation.getX()][animation.getY()].remove(animation);
        }
    }

    public ArrayList<AnimationTile> getGlobalAnimationList() {
        return mGlobalAnimation;
    }
}
