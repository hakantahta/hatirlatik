package com.tht.hatirlatik.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.tht.hatirlatik.R;
import com.tht.hatirlatik.preferences.PreferencesManager;

public class AdHelper implements InternetHelper.InternetConnectionListener {
    private static final String TAG = "AdHelper";
    private static AdHelper instance;
    private boolean isInitialized = false;
    private Context context;
    private InternetHelper internetHelper;
    private PreferencesManager preferencesManager;

    private AdHelper() {}

    public static AdHelper getInstance() {
        if (instance == null) {
            instance = new AdHelper();
        }
        return instance;
    }

    public void initialize(Context context) {
        if (!isInitialized) {
            this.context = context.getApplicationContext();
            this.internetHelper = new InternetHelper(this.context);
            this.preferencesManager = new PreferencesManager(this.context);

            // İnternet bağlantısı değişikliklerini dinle
            internetHelper.setConnectionListener(this);

            initializeAds();
        }
    }

    private void initializeAds() {
        if (!internetHelper.hasInternetPermission()) {
            Log.w(TAG, "İnternet izni verilmemiş, reklamlar yüklenemeyecek");
            return;
        }

        if (!internetHelper.isInternetAvailable()) {
            Log.w(TAG, "İnternet bağlantısı yok, reklamlar yüklenemeyecek");
            return;
        }

        if (!preferencesManager.isAdsEnabled()) {
            Log.i(TAG, "Reklamlar kullanıcı tarafından devre dışı bırakılmış");
            return;
        }

        MobileAds.initialize(context, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                isInitialized = true;
                Log.i(TAG, "AdMob başarıyla başlatıldı");
            }
        });
    }

    @Override
    public void onConnectionChanged(boolean isConnected) {
        if (isConnected && !isInitialized) {
            initializeAds();
        }
    }

    public AdRequest getAdRequest() {
        if (!isInitialized) {
            Log.w(TAG, "AdMob henüz başlatılmamış");
            return null;
        }

        if (!internetHelper.hasInternetPermission()) {
            Log.w(TAG, "İnternet izni verilmemiş");
            return null;
        }

        if (!internetHelper.isInternetAvailable()) {
            Log.w(TAG, "İnternet bağlantısı yok");
            Toast.makeText(context, R.string.no_internet_message, Toast.LENGTH_SHORT).show();
            return null;
        }

        if (!preferencesManager.isAdsEnabled()) {
            Log.i(TAG, "Reklamlar devre dışı");
            return null;
        }

        return new AdRequest.Builder().build();
    }

    public boolean canShowAds() {
        return isInitialized && 
               internetHelper.hasInternetPermission() && 
               internetHelper.isInternetAvailable() && 
               preferencesManager.isAdsEnabled();
    }

    public void destroy() {
        if (internetHelper != null) {
            internetHelper.removeConnectionListener();
        }
    }
} 