package com.ben.laama.sortpuzzle.android;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import com.ben.laama.sortpuzzle.services.Analytics;
import com.ben.laama.sortpuzzle.services.AnalyticsEvents;

/**
 * Firebase Analytics (+ Crashlytics switch) behind the core's {@link Analytics} interface.
 *
 * <p>Only exists when {@code google-services.json} produced a default {@link FirebaseApp};
 * otherwise {@link #create} returns {@code null} and the game runs on {@code NoOpAnalytics}.
 * Collection starts disabled (manifest) and is switched by {@link #setCollectionEnabled} from the
 * consent outcome. Names and parameters are validated against {@link AnalyticsEvents} so a typo
 * is logged here instead of being dropped silently by the SDK.</p>
 */
public final class FirebaseAnalyticsBridge implements Analytics {

    private static final String TAG = "Analytics";

    private final FirebaseAnalytics firebase;

    private FirebaseAnalyticsBridge(FirebaseAnalytics firebase) {
        this.firebase = firebase;
    }

    /** @return the bridge, or {@code null} when Firebase is not configured in this build */
    public static FirebaseAnalyticsBridge create(Context context) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            Log.i(TAG, "No FirebaseApp (google-services.json missing): analytics off");
            return null;
        }
        return new FirebaseAnalyticsBridge(FirebaseAnalytics.getInstance(context));
    }

    /** Analytics and crash reports together: both follow the same consent decision. */
    public void setCollectionEnabled(boolean enabled) {
        firebase.setAnalyticsCollectionEnabled(enabled);
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled);
        Log.i(TAG, "collection " + (enabled ? "enabled" : "disabled"));
    }

    @Override
    public void event(String name, Object... keyValues) {
        if (!AnalyticsEvents.isValidName(name) || !AnalyticsEvents.isValidParams(keyValues)) {
            Log.w(TAG, "Dropped malformed event " + name);
            return;
        }
        Bundle params = new Bundle();
        if (keyValues != null) {
            for (int i = 0; i < keyValues.length; i += 2) {
                String key = (String) keyValues[i];
                Object value = keyValues[i + 1];
                if (value instanceof Boolean) {
                    params.putLong(key, (Boolean) value ? 1L : 0L);
                } else if (value instanceof Integer || value instanceof Long) {
                    params.putLong(key, ((Number) value).longValue());
                } else if (value instanceof Double) {
                    params.putDouble(key, (Double) value);
                } else {
                    params.putString(key, String.valueOf(value));
                }
            }
        }
        firebase.logEvent(name, params);
    }
}
