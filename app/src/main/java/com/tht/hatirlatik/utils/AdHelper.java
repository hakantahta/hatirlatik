package com.tht.hatirlatik.utils;

import android.content.Context;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

public class AdHelper {
    private static AdHelper instance;
    private boolean isInitialized = false;

    private AdHelper() {}

    public static AdHelper getInstance() {
        if (instance == null) {
            instance = new AdHelper();
        }
        return instance;
    }

    public void initialize(Context context) {
        if (!isInitialized) {
            MobileAds.initialize(context, new OnInitializationCompleteListener() {
                @Override
                public void onInitializationComplete(InitializationStatus initializationStatus) {
                    isInitialized = true;
                }
            });
        }
    }

    public AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }
} 