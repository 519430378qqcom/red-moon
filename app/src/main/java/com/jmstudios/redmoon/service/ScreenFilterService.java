package com.jmstudios.redmoon.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.WindowManager;

import com.jmstudios.redmoon.helper.FilterCommandFactory;
import com.jmstudios.redmoon.helper.FilterCommandParser;
import com.jmstudios.redmoon.manager.ScreenManager;
import com.jmstudios.redmoon.manager.WindowViewManager;
import com.jmstudios.redmoon.model.SettingsModel;
import com.jmstudios.redmoon.presenter.ScreenFilterPresenter;
import com.jmstudios.redmoon.receiver.OrientationChangeReceiver;
import com.jmstudios.redmoon.view.ScreenFilterView;

public class ScreenFilterService extends Service implements ServiceLifeCycleController {
    public static final int VALID_COMMAND_START = 0;
    public static final int COMMAND_ON = 0;
    public static final int COMMAND_OFF = 1;
    public static final int COMMAND_PAUSE = 2;
    public static final int VALID_COMMAND_END = 2;

    public static final String BUNDLE_KEY_COMMAND = "jmstudios.bundle.key.COMMAND";

    private static final String TAG = "ScreenFilterService";
    private static final boolean DEBUG = true;

    private ScreenFilterPresenter mPresenter;
    private SettingsModel mSettingsModel;
    private OrientationChangeReceiver mOrientationReceiver;

    @Override
    public void onCreate() {
        super.onCreate();

        if (DEBUG) Log.i(TAG, "onCreate");

        // Initialize helpers and managers
        Context context = this;
        WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        ScreenFilterView view = new ScreenFilterView(context);
        WindowViewManager wvm = new WindowViewManager(windowManager);
        ScreenManager sm = new ScreenManager(this, windowManager);
        NotificationCompat.Builder nb = new NotificationCompat.Builder(this);
        FilterCommandFactory fcf = new FilterCommandFactory(this);
        FilterCommandParser fcp = new FilterCommandParser();

        // Wire MVP classes
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mSettingsModel = new SettingsModel(context.getResources(), sharedPreferences);

        mPresenter = new ScreenFilterPresenter(view, mSettingsModel, this, context, wvm, sm, nb, fcf, fcp);

        // Make Presenter listen to settings changes and orientation changes
        mSettingsModel.openSettingsChangeListener();
        mSettingsModel.addOnSettingsChangedListener(mPresenter);

        registerOrientationReceiver(mPresenter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.i(TAG, String.format("onStartCommand(%s, %d, %d", intent, flags, startId));

        mPresenter.onScreenFilterCommand(intent);

        // Do not attempt to restart if the hosting process is killed by Android
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Prevent binding.
        return null;
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.i(TAG, "onDestroy");

        mSettingsModel.closeSettingsChangeListener();
        unregisterOrientationReceiver();

        super.onDestroy();
    }

    @Override
    public void stop() {
        if (DEBUG) Log.i(TAG, "Received stop request");

        stopSelf();
    }

    private void registerOrientationReceiver(OrientationChangeReceiver.OnOrientationChangeListener listener) {
        if (mOrientationReceiver != null) {
            return;
        }

        IntentFilter orientationIntentFilter = new IntentFilter();
        orientationIntentFilter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);

        mOrientationReceiver = new OrientationChangeReceiver(this, listener);
        registerReceiver(mOrientationReceiver, orientationIntentFilter);
    }

    private void unregisterOrientationReceiver() {
        unregisterReceiver(mOrientationReceiver);
        mOrientationReceiver = null;
    }
}
