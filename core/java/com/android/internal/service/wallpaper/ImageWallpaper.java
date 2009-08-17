/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.internal.service.wallpaper;

import android.app.WallpaperManager;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;
import android.content.Context;
import android.content.IntentFilter;
import android.content.Intent;
import android.content.BroadcastReceiver;

/**
 * Default built-in wallpaper that simply shows a static image.
 */
public class ImageWallpaper extends WallpaperService {
    WallpaperManager mWallpaperManager;
    ImageWallpaper.DrawableEngine mEngine;
    private WallpaperObserver mReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        mWallpaperManager = (WallpaperManager) getSystemService(WALLPAPER_SERVICE);
        IntentFilter filter = new IntentFilter(Intent.ACTION_WALLPAPER_CHANGED);
        mReceiver = new WallpaperObserver();
        registerReceiver(mReceiver, filter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    public Engine onCreateEngine() {
        mEngine = new DrawableEngine();
        return mEngine;
    }

    class WallpaperObserver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            mEngine.updateWallpaper();
            mEngine.drawFrame();
        }
    }

    class DrawableEngine extends Engine {
        private final Object mLock = new Object();
        private final Rect mBounds = new Rect();
        Drawable mBackground;
        float mXOffset;
        float mYOffset;

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            updateWallpaper();
            surfaceHolder.setSizeFromLayout();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            drawFrame();
        }
        
        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
                int xPixels, int yPixels) {
            mXOffset = xOffset;
            mYOffset = xOffset;
            drawFrame();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            drawFrame();
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            super.onSurfaceCreated(holder);
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
        }
        
        void drawFrame() {
            SurfaceHolder sh = getSurfaceHolder();
            Canvas c = sh.lockCanvas();
            if (c != null) {
                final Rect frame = sh.getSurfaceFrame();
                synchronized (mLock) {
                    final Drawable background = mBackground;
                    final int dw = frame.width();
                    final int dh = frame.height();
                    final int bw = mBackground.getIntrinsicWidth();
                    final int bh = mBackground.getIntrinsicHeight();
                    final int availw = bw-dw;
                    final int availh = bh-dh;
                    int xPixels = availw > 0
                            ? -(int)(availw*mXOffset+.5f) : -(int)(availw/2);
                    int yPixels = availh > 0
                            ? -(int)(availh*mYOffset+.5f) : -(int)(availh/2);
                    c.translate(xPixels, yPixels);
                    c.drawColor(0xff000000);
                    background.draw(c);
                }
                sh.unlockCanvasAndPost(c);
            }
        }

        void updateWallpaper() {
            synchronized (mLock) {
                mBackground = mWallpaperManager.getDrawable();
                mBounds.left = mBounds.top = 0;
                mBounds.right = mBackground.getIntrinsicWidth();
                mBounds.bottom = mBackground.getIntrinsicHeight();
                mBackground.setBounds(mBounds);
            }
        }
    }
}
