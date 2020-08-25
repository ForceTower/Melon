/*
 * This file is part of the UNES Open Source Project.
 * UNES is licensed under the GNU GPLv3.
 *
 * Copyright (c) 2020. João Paulo Sena <joaopaulo761@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
