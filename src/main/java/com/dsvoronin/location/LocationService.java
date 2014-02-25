package com.dsvoronin.location;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import de.greenrobot.event.EventBus;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * ------------------
 * How to use:
 * ------------------
 * 1. add service to your app's AndroidManifest.xml:
 * <service
 * android:name=".LocationService"
 * android:exported="false" />
 * ------------------
 *
 * @author dsvoronin
 */
public class LocationService extends Service implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener {

    private static final String TAG = LocationService.class.getName();

    private static final long DEFAULT_UPDATE_INTERVAL = TimeUnit.SECONDS.toMillis(30);

    private Context mContext;
    private LocationClient mLocationClient;
    private LocationRequest mLocationRequest;

    private boolean mServicesAvailable = false;
    private boolean mInProgress;

    // settings
    private long mUpdateInterval;

    public static Intent create(Context context) {
        return new Intent(context, LocationService.class);
    }

    /**
     * @return true if at least one of Network|GPS location services is enabled
     */
    public static boolean checkServicesEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * Alert Dialog with
     */
    public static void showDefaultEnableLocationServicesDialog(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.loc_enable_dialog_title);
        builder.setMessage(R.string.loc_enable_dialog_message);
        builder.setPositiveButton(R.string.loc_enable_dialog_ok_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                // Show location settings when the user acknowledges the alert dialog
                context.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        });
        builder.setNegativeButton(R.string.loc_enable_dialog_no_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        Dialog alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();
    }

    /**
     * simple check for google play services available
     */
    private boolean servicesConnected() {
        return GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS;
    }

    /**
     * Create a new location client, using the enclosing class to
     * handle callbacks.
     */
    private void setUpLocationClientIfNeeded() {
        if (mLocationClient == null)
            mLocationClient = new LocationClient(this, this, this);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();

        initSettings();

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(mUpdateInterval);
        mLocationRequest.setFastestInterval(mUpdateInterval);

        mInProgress = false;
        mServicesAvailable = servicesConnected();

        mLocationClient = new LocationClient(this, this, this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (!mServicesAvailable || mLocationClient.isConnected() || mInProgress)
            return START_STICKY;

        setUpLocationClientIfNeeded();
        if (!mLocationClient.isConnected() || !mLocationClient.isConnecting() && !mInProgress) {
            mInProgress = true;
            mLocationClient.connect();
        }

        return START_STICKY;
    }

    /**
     * adjustable params sets here
     * add this params to app's res/, to change default values
     * loc_update_interval in ms
     * ------------------
     * ex: settings.xml
     * <integer name="loc_update_interval">30000</integer>
     */
    private void initSettings() {
        Resources resources = mContext.getResources();
        if (resources != null) {
            try {
                mUpdateInterval = resources.getInteger(R.integer.loc_update_interval);
            } catch (Resources.NotFoundException e) {
                Log.d(TAG, "Default Location update time will be used");
                mUpdateInterval = DEFAULT_UPDATE_INTERVAL;
            }
        }
    }

    @Override
    public void onDestroy() {
        mInProgress = false;
        if (mServicesAvailable && mLocationClient != null) {
            mLocationClient.removeLocationUpdates(this);
            // Destroy the current location client
            mLocationClient = null;
        }
        Log.d(TAG, "Location Service destroy");
        super.onDestroy();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "PlayServices connected");
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    @Override
    public void onDisconnected() {
        mInProgress = false;
        mLocationClient = null;

        Log.d(TAG, "PlayServices disconnected");
    }

    /**
     * Google Play services can resolve some errors it detects.
     * If the error has a resolution, try sending an Intent to
     * start a Google Play services activity that can resolve
     * error.
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mInProgress = false;
        Log.d(TAG, "PlayServices connection failed");

        if (connectionResult.hasResolution()) {
        } else {
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "New Location:" + location);
        EventBus.getDefault().post(new OnLocationChangedEvent(location));
    }

    /**
     * This service is not bindable
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
