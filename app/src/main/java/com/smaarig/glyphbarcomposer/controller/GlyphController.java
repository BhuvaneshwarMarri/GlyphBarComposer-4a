package com.smaarig.glyphbarcomposer.controller;

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
    private static GlyphController sInstance;
    private GlyphManager mGlyphManager;

    public static synchronized GlyphController getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new GlyphController();
            sInstance.init(context.getApplicationContext());
        }
        return sInstance;
    }

    private GlyphController() {}

    // ── Intensity mapping ─────────────────────────────────────────────────────
    // The Nothing Glyph SDK typically uses an 8-bit range (0–255) for intensity.
    //
    //   State 0 (OFF)  →     0
    //   State 1 (LOW)  →   100   ≈ 40 %
    //   State 2 (MED)  →   180   ≈ 70 %
    //   State 3 (HIGH) →   255   = 100 %
    //
    private static int stateToSdkIntensity(int state) {
        switch (state) {
            case 1:  return 100;
            case 2:  return 180;
            case 3:  return 255;
            default: return 0;
        }
    }

    private final GlyphManager.Callback mCallback = new GlyphManager.Callback() {
        @Override
        public void onServiceConnected(ComponentName componentName) {
            if (mGlyphManager != null) {
                if (Common.is25111()) {
                    mGlyphManager.register(Glyph.DEVICE_25111);
                    Log.d(TAG, "Registered for Phone (4a)");
                } else if (Common.is20111()) mGlyphManager.register(Glyph.DEVICE_20111);
                else if (Common.is22111()) mGlyphManager.register(Glyph.DEVICE_22111);
                else if (Common.is23111()) mGlyphManager.register(Glyph.DEVICE_23111);
                else if (Common.is23113()) mGlyphManager.register(Glyph.DEVICE_23113);
                else if (Common.is24111()) mGlyphManager.register(Glyph.DEVICE_24111);
                else mGlyphManager.register();

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

    /** Turn off all active glyphs. */
    public void turnOffGlyphs() {
        if (mGlyphManager == null) return;
        mGlyphManager.turnOff();
    }

    /**
     * Applies a frame with per-channel intensities (states 0–3) and a duration.
     *
     * @param channelIntensities Map of Glyph channel constants → state (0 = OFF, 1–3 = LOW/MED/HIGH).
     * @param durationMs         Duration in milliseconds.
     */
    public void applyGlyphStateWithIntensities(
            java.util.Map<Integer, Integer> channelIntensities, int durationMs) {
        if (mGlyphManager == null) {
            Log.e(TAG, "applyGlyphStateWithIntensities: GlyphManager is null");
            return;
        }
        try {
            GlyphFrame.Builder builder = mGlyphManager.getGlyphFrameBuilder();
            boolean anyActive = false;

            for (java.util.Map.Entry<Integer, Integer> entry : channelIntensities.entrySet()) {
                int state = entry.getValue();
                int sdkIntensity = stateToSdkIntensity(state);
                if (sdkIntensity > 0) {
                    anyActive = true;
                    // For State 3 (HIGH) or if intensity is max, use the single-argument version
                    // as it's the most compatible across all SDK versions.
                    if (state == 3 || sdkIntensity >= 255) {
                        builder.buildChannel(entry.getKey());
                    } else {
                        builder.buildChannel(entry.getKey(), sdkIntensity);
                    }
                }
            }

            if (!anyActive) {
                mGlyphManager.turnOff();
                return;
            }

            // Ensure the session is open before toggling. 
            // In some cases, we might need to call openSession again if it was closed.
            try {
                mGlyphManager.openSession();
            } catch (Exception e) {
                // Ignore if already open
            }

            builder.buildPeriod(durationMs);
            mGlyphManager.toggle(builder.build());
            Log.i(TAG, "Glyph Frame Applied: " + channelIntensities);
        } catch (Exception e) {
            Log.e(TAG, "Error in applyGlyphStateWithIntensities: " + e.getMessage(), e);
        }
    }

    /**
     * Legacy ON/OFF state (all active channels at full brightness).
     *
     * @param activeChannels List of Glyph channel constants to light up.
     * @param durationMs     Duration in milliseconds.
     */
    public void applyGlyphState(java.util.List<Integer> activeChannels, int durationMs) {
        if (mGlyphManager == null) return;
        try {
            if (activeChannels.isEmpty()) {
                mGlyphManager.turnOff();
                return;
            }
            
            try {
                mGlyphManager.openSession();
            } catch (Exception e) {
                // Ignore if already open
            }

            GlyphFrame.Builder builder = mGlyphManager.getGlyphFrameBuilder();
            for (Integer channel : activeChannels) {
                // Use the most compatible single-argument version for legacy ON
                builder.buildChannel(channel);
            }
            builder.buildPeriod(durationMs);
            mGlyphManager.toggle(builder.build());
        } catch (Exception e) {
            Log.e(TAG, "Error in applyGlyphState: " + e.getMessage());
        }
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