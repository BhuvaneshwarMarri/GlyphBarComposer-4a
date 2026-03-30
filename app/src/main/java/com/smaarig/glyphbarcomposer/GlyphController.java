package com.smaarig.glyphbarcomposer;

import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import com.nothing.ketchum.Common;
import com.nothing.ketchum.Glyph;
import com.nothing.ketchum.GlyphException;
import com.nothing.ketchum.GlyphFrame;
import com.nothing.ketchum.GlyphManager;

public class GlyphController {
    private static final String TAG = "GlyphController";
    private GlyphManager mGlyphManager;
    private final GlyphManager.Callback mCallback = new GlyphManager.Callback() {
        @Override
        public void onServiceConnected(ComponentName componentName) {
            if (mGlyphManager != null) {
                // Phone (4a) specific registration: Glyph.DEVICE_25111
                if (Common.is25111()) {
                    mGlyphManager.register(Glyph.DEVICE_25111);
                    Log.d(TAG, "Registered for Phone (4a)");
                } else if (Common.is20111()) mGlyphManager.register(Glyph.DEVICE_20111);
                else if (Common.is22111()) mGlyphManager.register(Glyph.DEVICE_22111);
                else if (Common.is23111()) mGlyphManager.register(Glyph.DEVICE_23111);
                else if (Common.is23113()) mGlyphManager.register(Glyph.DEVICE_23113);
                else if (Common.is24111()) mGlyphManager.register(Glyph.DEVICE_24111);
                else {
                    mGlyphManager.register();
                }

                try {
                    mGlyphManager.openSession();
                    Log.d(TAG, "Glyph Session Opened");
                } catch (GlyphException e) {
                    Log.e(TAG, "Failed to open session: " + e.getMessage());
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            if (mGlyphManager != null) {
                try {
                    mGlyphManager.closeSession();
                } catch (GlyphException e) {
                    Log.e(TAG, "Failed to close session on disconnect: " + e.getMessage());
                }
            }
        }
    };

    public void init(Context context) {
        mGlyphManager = GlyphManager.getInstance(context);
        mGlyphManager.init(mCallback);
    }

    // Example 1: Light up the top LEDs of Phone (4a) (A1-A3)
    public void lightUpPhone4aTop() {
        if (mGlyphManager == null) return;
        try {
            // Build a frame specifically for Phone (4a) using its LED constants
            GlyphFrame frame = mGlyphManager.getGlyphFrameBuilder()
                    .buildChannel(Glyph.Code_25111.A_1)
                    .buildChannel(Glyph.Code_25111.A_2)
                    .buildChannel(Glyph.Code_25111.A_3)
                    .buildPeriod(2000)
                    .buildCycles(3)
                    .build();

            mGlyphManager.toggle(frame);
        } catch (Exception e) {
            Log.e(TAG, "Error in lightUpPhone4aTop: " + e.getMessage());
        }
    }

    // Example 2: Animate a breathing effect on all A LEDs of Phone (4a)
    public void animatePhone4aAll() {
        if (mGlyphManager == null) return;
        try {
            GlyphFrame frame = mGlyphManager.getGlyphFrameBuilder()
                    .buildChannel(Glyph.Code_25111.A_1)
                    .buildChannel(Glyph.Code_25111.A_2)
                    .buildChannel(Glyph.Code_25111.A_3)
                    .buildChannel(Glyph.Code_25111.A_4)
                    .buildChannel(Glyph.Code_25111.A_5)
                    .buildChannel(Glyph.Code_25111.A_6)
                    .buildInterval(10)
                    .buildCycles(2)
                    .buildPeriod(3000)
                    .build();

            mGlyphManager.animate(frame);
        } catch (Exception e) {
            Log.e(TAG, "Error in animatePhone4aAll: " + e.getMessage());
        }
    }

    // Example 3: Turn off all active glyphs
    public void turnOffGlyphs() {
        if (mGlyphManager == null) return;
        mGlyphManager.turnOff();
    }

    /**
     * Applies a state with multiple active Glyph channels for a specific duration.
     * @param activeChannels List of Glyph channel constants to be turned on.
     * @param durationMs Duration in milliseconds.
     */
    public void applyGlyphState(java.util.List<Integer> activeChannels, int durationMs) {
        if (mGlyphManager == null) return;
        try {
            if (activeChannels.isEmpty()) {
                mGlyphManager.turnOff();
                return;
            }

            GlyphFrame.Builder builder = mGlyphManager.getGlyphFrameBuilder();
            for (Integer channel : activeChannels) {
                builder.buildChannel(channel);
            }
            builder.buildPeriod(durationMs);
            
            GlyphFrame frame = builder.build();
            mGlyphManager.toggle(frame);
        } catch (Exception e) {
            Log.e(TAG, "Error in applyGlyphState with duration: " + e.getMessage());
        }
    }

    /**
     * Plays a single frame for a specific duration.
     */
    public void playSingleFrame(java.util.List<Integer> activeChannels, int durationMs) {
        applyGlyphState(activeChannels, durationMs);
    }

    public void deinit() {
        if (mGlyphManager != null) {
            try {
                mGlyphManager.closeSession();
                Log.d(TAG, "Glyph Session Closed");
            } catch (GlyphException e) {
                Log.e(TAG, "Failed to close session in deinit: " + e.getMessage());
            }
            mGlyphManager.unInit();
        }
    }
}
