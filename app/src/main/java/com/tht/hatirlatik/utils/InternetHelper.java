package com.tht.hatirlatik.utils;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

public class InternetHelper {
    private static final String TAG = "InternetHelper";
    private final Context context;
    private final ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private InternetConnectionListener connectionListener;

    public interface InternetConnectionListener {
        void onConnectionChanged(boolean isConnected);
    }

    public InternetHelper(Context context) {
        this.context = context;
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public void setConnectionListener(InternetConnectionListener listener) {
        this.connectionListener = listener;
        registerNetworkCallback();
    }

    public void removeConnectionListener() {
        if (networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception e) {
                Log.e(TAG, "Network callback zaten kayıtlı değil: " + e.getMessage());
            }
        }
    }

    private void registerNetworkCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            NetworkRequest networkRequest = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    .build();

            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(@NonNull Network network) {
                    super.onAvailable(network);
                    if (connectionListener != null) {
                        connectionListener.onConnectionChanged(true);
                    }
                }

                @Override
                public void onLost(@NonNull Network network) {
                    super.onLost(network);
                    if (connectionListener != null) {
                        connectionListener.onConnectionChanged(false);
                    }
                }
            };

            connectivityManager.registerNetworkCallback(networkRequest, networkCallback);
        }
    }

    public boolean hasInternetPermission() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.INTERNET)
                == PackageManager.PERMISSION_GRANTED;
    }

    public boolean isInternetAvailable() {
        if (!hasInternetPermission()) {
            Log.w(TAG, "İnternet izni verilmemiş");
            return false;
        }

        if (connectivityManager == null) {
            Log.e(TAG, "ConnectivityManager null");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
            if (capabilities != null) {
                if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    Log.d(TAG, "WiFi bağlantısı mevcut");
                    return true;
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                    Log.d(TAG, "Mobil veri bağlantısı mevcut");
                    return true;
                } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                    Log.d(TAG, "Ethernet bağlantısı mevcut");
                    return true;
                }
            }
        }

        Log.w(TAG, "İnternet bağlantısı yok");
        return false;
    }

    public void openInternetSettings() {
        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
} 