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


import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class SignalService extends Service {

    public static final String INTENT_CALIBRATE = "com.twinblade.musicbutler.CALIBRATE";
    public static final String INTENT_GET_CLOSEST = "com.twinblade.musicbutler.GET_CLOSEST";

    private static final String MY_NETWORK = "tufts-guest";
    private static final String NEIGHBOR = "tuftswireless";

    private int mCountPullsMine = 0;
    private int mCountPullsOther = 0;
    private float[] mAvgStrength = {0,0};
    private ArrayList<Speaker> mDatabase = new ArrayList<Speaker>();
    private Speaker currentSpeaker;

    private boolean mIsCalibrating = false;
    private boolean mIsGettingAverage = false;

    private WifiManager mWifiManager;
    private SharedPreferences mSharedPreferences;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //

            if (mIsCalibrating && (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) ||
                    intent.getAction().equals(WifiManager.RSSI_CHANGED_ACTION))) {
                Log.e("MB", "ON RECEIVE");
                List<ScanResult> wifiList = mWifiManager.getScanResults();
                for (ScanResult result : wifiList) {
                    String ssid = result.SSID;
                    if (!ssid.equals(MY_NETWORK) && !ssid.equals(NEIGHBOR)) {
                        continue;
                    }
                    if (mCountPullsOther > 3 || mCountPullsMine > 3) {
                        Log.w(">>>>", "Count greater than 3");
                        break;
                    }
                    int level = result.level;

                    //if (ssid.equals("Verizon-SM-G900V-136A")) {
                    //Log.e("MB", "SSID: " + ssid + "; lvl: " + level);
                    //}
                    if (mCountPullsMine < 3 && ssid.equals(MY_NETWORK)) {
                        currentSpeaker.avgStrength[0] += level / 3.0;
                        mCountPullsMine++;
                       // Log.e("MB", "******mine" + mCountPullsMine);
                        mWifiManager.startScan();
                    }
                    if (mCountPullsOther < 3 && ssid.equals(NEIGHBOR)) {
                        currentSpeaker.avgStrength[1] += level / 3.0;
                        mCountPullsOther++;
                        //Log.e("MB", "******neighbor" + mCountPullsOther);
                        mWifiManager.startScan();
                    }
                    if (mCountPullsMine == 3 && mCountPullsOther == 3) {
                        //
                        //Log.e("++++++++",mAvgStrength[0]+", "+mAvgStrength[1]);
                        mDatabase.add(currentSpeaker);
                        for (int i = 0; i < mDatabase.size(); i++) {
                            Log.e("--" + i, mDatabase.get(i).name + ": " +
                                    mDatabase.get(i).avgStrength[0] + ", " +
                                    mDatabase.get(i).avgStrength[1]);
                        }

                        mIsCalibrating = false;
                        break;
                    }
                    //Log.e("MB", "AVERAGES: " + mAvgStrength[0] + ", " + mAvgStrength[1]);
                }
            } else if (intent.getAction().equals(INTENT_CALIBRATE)) {
                currentSpeaker = new Speaker(Main.ip.getText().toString());
                mCountPullsOther = mCountPullsMine = 0;
                mAvgStrength[0] = mAvgStrength[1] = 0;
                mIsCalibrating = true;
                mWifiManager.startScan();
                Log.e("MB", "CALIBRATING");
            } else if (intent.getAction().equals(INTENT_GET_CLOSEST)) {
                Log.e(">>", "Got intent get closest");
                mIsGettingAverage = true;
                mWifiManager.startScan();

            } else if (mIsGettingAverage && intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                //Log.e("))))>>>>", "Is getting average!");
                List<ScanResult> wifiList = mWifiManager.getScanResults();

                int valsGotten = 0;
                for (ScanResult result : wifiList) {
                    String ssid = result.SSID;
                    if (!ssid.equals(MY_NETWORK) && !ssid.equals(NEIGHBOR)) {
                        continue;
                    }
                    int level = result.level;
                    if (ssid.equals(MY_NETWORK)) {
                        mAvgStrength[0] = level;
                        valsGotten++;
                    }
                    else if (ssid.equals(NEIGHBOR)) {
                        mAvgStrength[1] = level;
                        valsGotten++;
                    }
                    if (valsGotten == 2)
                        break;
                }
                double maxSignal = Math.pow(mAvgStrength[0], 2) +
                        Math.pow(mAvgStrength[1], 2);
                String closestSpeaker = "";
                for (Speaker s : mDatabase) {
                    double sig = Math.pow((s.avgStrength[0] - mAvgStrength[0]), 2) +
                    Math.pow((s.avgStrength[1] - mAvgStrength[1]), 2);
                    if (sig < maxSignal) {
                        maxSignal = sig;
                        closestSpeaker = s.name;
                    }

                }
                mIsGettingAverage = false;
                Log.w(">>>", "Me: " + mAvgStrength[0] + ", " + mAvgStrength[1] + "  Closest: " + closestSpeaker);
                Toast.makeText(context, "Me: " + mAvgStrength[0] + ", " + mAvgStrength[1] + "  Closest: " + closestSpeaker, Toast.LENGTH_SHORT).show();
            }
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

    private void makeApiCall() {
        HttpClient httpClient = new DefaultHttpClient();
        // replace with your url
        HttpPost httpPost = new HttpPost("www.example.com");


        //Post Data
        List<NameValuePair> nameValuePair = new ArrayList<NameValuePair>(2);
        nameValuePair.add(new BasicNameValuePair("username", "test_user"));
        nameValuePair.add(new BasicNameValuePair("password", "123456789"));


        //Encoding POST data
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePair));
        } catch (UnsupportedEncodingException e) {
            // log exception
            e.printStackTrace();
        }

        //making POST request.
        try {
            HttpResponse response = httpClient.execute(httpPost);
            // write response to log
            Log.d("Http Post Response:", response.toString());
        } catch (ClientProtocolException e) {
            // Log exception
            e.printStackTrace();
        } catch (IOException e) {
            // Log exception
            e.printStackTrace();
        }
    }

    private class Speaker {
        public String name;
        public float[] avgStrength = {0, 0};
        public Speaker(String inName) {
            name = inName;
        }

    }

}
