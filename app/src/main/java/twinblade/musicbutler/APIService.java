package twinblade.musicbutler;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.util.List;


public class APIService extends Service {
    private WifiManager mWifiManager;
    private SharedPreferences mSharedPreferences;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //
        }
    };

    @Override
    public void onCreate() {
        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        filter.addAction(INTENT_CALIBRATE);
        filter.addAction(INTENT_GET_CLOSEST);
        registerReceiver(mReceiver, filter);



        mWifiManager.startScan();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
