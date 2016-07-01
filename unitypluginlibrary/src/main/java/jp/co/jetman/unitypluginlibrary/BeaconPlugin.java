package jp.co.jetman.unitypluginlibrary;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.widget.Toast;

import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;

import java.util.Collection;

public class BeaconPlugin extends UnityPlayerActivity implements BeaconConsumer {

    private static final String TAG = BeaconPlugin.class.getSimpleName();

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final String IBEACON_FORMAT = "m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24";
    private static final String UUID = "00000000-9ABA-1001-B000-001C4D25538C";

    private static BeaconPlugin instance;
    private static String callbackLogTo = "";
    private static String beaconInfoString = "";

    private static Beacon[] Beacons;

    protected static void DebugLog(String message) {
        if (callbackLogTo != null) {
            UnityPlayer.UnitySendMessage(callbackLogTo, "DebugLog", "[" + TAG + "] " + message);
        }
    }

    public static void Initialize(String _callbackLogTo) {
        callbackLogTo = _callbackLogTo;
        DebugLog("initialized.");
    }

    public static String GetOneBeaconLatestInfo() {
        return beaconInfoString;
    }

    // ---------

    private BeaconManager manager;
    private Identifier identifier = null;//Identifier.parse(UUID);

    @Override
    protected void onCreate(Bundle saveInstanceState) {
        super.onCreate(saveInstanceState);
        DebugLog("onCreate()");

        init();
    }

    @Override
    protected void onPause() {
        super.onPause();
        DebugLog("onPause()");

        if (manager != null) {
            DebugLog("BeaconManager.unbind()");
            manager.unbind(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        DebugLog("onResume()");

        if (manager != null) {
            DebugLog("BeaconManager.bind()");
            manager.bind(this);
        }
    }

    private void init() {
        BeaconPlugin.DebugLog("init()");

        final Activity activity = UnityPlayer.currentActivity;

        // CHECK Permission for Android 6.0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
            }
        }

        manager = BeaconManager.getInstanceForApplication(activity.getApplicationContext());
        manager.getBeaconParsers().add(new BeaconParser().setBeaconLayout(IBEACON_FORMAT));

        //Toast.makeText(activity, "Hello world!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBeaconServiceConnect() {
        //Log.d(TAG, "onBeaconServiceConnect");
        BeaconPlugin.DebugLog("onBeaconServiceConnect()");

        manager.setMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                //Log.d(TAG, "didEnterRegion");
                BeaconPlugin.DebugLog("didEnterRegion()");

                try {
                    manager.startRangingBeaconsInRegion(new Region("unique-id-001", identifier, null, null));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void didExitRegion(Region region) {
                //Log.d(TAG, "didExitRegion");
                BeaconPlugin.DebugLog("didExitRegion()");

                try {
                    manager.stopRangingBeaconsInRegion(new Region("unique-id-111", identifier, null, null));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void didDetermineStateForRegion(int i, Region region) {
                //Log.d(TAG, "didDetermineStateForRegion");
                BeaconPlugin.DebugLog("didDetermineStateForRegion()");
            }
        });

        manager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {

                if (beacons != null) {
                    int i = 0;
                    for (Beacon beacon : beacons) {
                        final String outStr = "UUID:" + beacon.getId1() + ", major:" + beacon.getId2() + ", minor:" + beacon.getId3() + ", Distance:" + beacon.getDistance() + ",RSSI" + beacon.getRssi() + ", TxPower" + beacon.getTxPower();
                        beaconInfoString = outStr;
                        Beacons[i] = beacon;
                        //Log.d(TAG, outStr);
                        BeaconPlugin.DebugLog(outStr);

                        i++;

                        runOnUiThread(new Runnable() {
                            public void run() {

                            }
                        });
                    }
                    OrderBeacons();
                }
            }
        });

        try {
            manager.startMonitoringBeaconsInRegion(new Region("unique-id-111", identifier, null, null));
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    void OrderBeacons(){
        int i,j,index;
        Beacon current;

        for(i=0;i<Beacons.length;i++){
            current = Beacons[i];
            index = i;
            for(j=i+1;j<Beacons.length;j++){
                if(current.getRssi() >= Beacons[j].getRssi())
                    index = j;
            }
            Beacons[i] = Beacons[index];
            Beacons[index] = current;
        }
    }

    private String[] sendBeaconInfo(){
        int i=0;
        String[] BeaconInfo = new String[Beacons.length];
        for(Beacon current : Beacons) {
            //UUID+Major+Minor+Distance+RSSI+TxPower
            BeaconInfo[i] = current.getId1() + "+" + current.getId2() + "+" + current.getId3() + "+" + current.getDistance() + "+" + current.getRssi() + "+" + current.getTxPower();
        }

        return BeaconInfo;
    }



}
