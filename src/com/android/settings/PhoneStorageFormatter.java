package com.android.settings;

import android.app.ProgressDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.storage.IMountService;
import android.os.storage.StorageEventListener;
import android.os.storage.StorageManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.internal.R;

import com.android.internal.os.storage.ExternalStorageFormatter;

/**
 * Takes care of unmounting and formatting external storage.
 */
public class PhoneStorageFormatter extends ExternalStorageFormatter 
//implements DialogInterface.OnCancelListener //del for not allowing to cancel format
{
    static final String TAG = "PhoneStorageFormatter";

    private static final boolean DBG = false;

    public static final String FORMAT_ONLY = "com.android.internal.os.storage.FORMAT_ONLY";
    public static final String FORMAT_AND_FACTORY_RESET = "com.android.internal.os.storage.FORMAT_AND_FACTORY_RESET";
    public static final String FORMAT_ALL_AND_FACTORY_RESET = "com.android.internal.os.storage.FORMAT_ALL_AND_FACTORY_RESET";

    public static final String EXTRA_ALWAYS_RESET = "always_reset";

    public static final ComponentName COMPONENT_NAME = new ComponentName(
           "android", PhoneStorageFormatter.class.getName());

    // Access using getMountService()
    private IMountService mMountService = null;

    private StorageManager mStorageManager = null;

    private PowerManager.WakeLock mWakeLock;

    private ProgressDialog mProgressDialog = null;

    private boolean mFactoryReset = false;
    private boolean mAlwaysReset = false;
    private boolean mFormatAll = false;

    StorageEventListener mStorageListener = new StorageEventListener() {
        @Override
        public void onStorageStateChanged(String path, String oldState, String newState) {
            if (DBG)
                Log.i(TAG, "Received storage state changed notification that "
                             + path + " changed state from " + oldState + " to "
                             + newState);
            updateProgressState();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        if (mStorageManager == null) {
            mStorageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
            mStorageManager.registerListener(mStorageListener);
        }

        mWakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE))
                        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                        "ExternalStorageFormatter");
        mWakeLock.acquire();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (FORMAT_AND_FACTORY_RESET.equals(intent.getAction())) {
            mFactoryReset = true;
        }

        if (FORMAT_ALL_AND_FACTORY_RESET.equals(intent.getAction())) {
            mFactoryReset = true;
            mFormatAll = true;
        }

        if (intent.getBooleanExtra(EXTRA_ALWAYS_RESET, false)) {
            mAlwaysReset = true;
        }

        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.getWindow().setType(
                            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            /*-----------Start: Deleted for not allowing to cancel format --------------
            if (!mAlwaysReset) {
                mProgressDialog.setOnCancelListener(this);
            }
            */
            updateProgressState();
            mProgressDialog.show();
        }

        return Service.START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        if (mStorageManager != null) {
            mStorageManager.unregisterListener(mStorageListener);
        }
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
        mWakeLock.release();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    /* del for not allowing to cancel format
    @Override
    public void onCancel(DialogInterface dialog) {
        IMountService mountService = getMountService();
        String extStoragePath = Environment.getPhoneStorageDirectory()
                                                .toString();
        try {
            mountService.mountVolume(extStoragePath);
        } catch (RemoteException e) {
            Log.w(TAG, "Failed talking with mount service", e);
        }
        stopSelf();
    }
    */
    void fail(int msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        if (mAlwaysReset) {
            sendBroadcast(new Intent("android.intent.action.MASTER_CLEAR"));
        }
        stopSelf();
    }

    void updateProgressState() {
        String status = Environment.getInternalStorageState();

        String mstatus1 = Environment.getExternalStorageState();
        if (mFormatAll) {
            if (Environment.MEDIA_MOUNTED.equals(status)
            || Environment.MEDIA_MOUNTED_READ_ONLY.equals(status)
            || Environment.MEDIA_MOUNTED.equals(mstatus1)
            || Environment.MEDIA_MOUNTED_READ_ONLY.equals(mstatus1)) {
                updateProgressDialog(R.string.progress_unmounting);
                IMountService mountService = getMountService();
                String extStoragePath = Environment.getInternalStorageDirectory()
                                                            .toString();
                String extStoragePathnotPhone = Environment
                                                        .getExternalStorageDirectory().toString();
                try {
                    mountService.unmountVolume(extStoragePath, true, true);
                    mountService.unmountVolume(extStoragePathnotPhone, true, true);
                } catch (RemoteException e) {
                    Log.w(TAG, "Failed talking with mount service", e);
                    mFormatAll = false;
                }
            } else if ((Environment.MEDIA_NOFS.equals(status)
                        || Environment.MEDIA_UNMOUNTED.equals(status) 
                        || Environment.MEDIA_UNMOUNTABLE.equals(status))
                        && (Environment.MEDIA_NOFS.equals(mstatus1)
                          || Environment.MEDIA_UNMOUNTED.equals(mstatus1) 
                          || Environment.MEDIA_UNMOUNTABLE.equals(mstatus1))) {
                updateProgressDialog(R.string.progress_erasing);
                final IMountService mountService = getMountService();
                final String extStoragePath = Environment
                                                            .getInternalStorageDirectory().toString();
                final String extStoragePathnotPhone = Environment
                                                            .getExternalStorageDirectory().toString();
                if (mountService != null) {
                    new Thread() {
                        public void run() {
                            boolean success = false;
                            try {
                                mountService.formatVolume(extStoragePath);
                                success = true;
                            } catch (Exception e) {
                                Toast.makeText(PhoneStorageFormatter.this,
                                R.string.format_error,
                                Toast.LENGTH_LONG).show();
                            }
                            if (success) {
                                if (mFactoryReset) {
                                    String mstatus = Environment
                                                            .getExternalStorageState();
                                    if (DBG)
                                        Log.d(TAG, "ExternalStorage Status is: "
                                                        + Environment.getExternalStorageState());
                                    if (Environment.MEDIA_NOFS.equals(mstatus)
                                    || Environment.MEDIA_UNMOUNTED.equals(mstatus)
                                    || Environment.MEDIA_UNMOUNTABLE.equals(mstatus)) {
                                        IMountService myMountService = getMountService();
                                        if (myMountService != null) {
                                            if (DBG)
                                                Log.d(TAG, "myMountService is Ready");
                                            try {
                                                myMountService.formatVolume(extStoragePathnotPhone);
                                            } catch (Exception e) {
                                                return;
                                            }
                                        }
                                        if (DBG)
                                            Log.d(TAG, "myMountService Out");
                                    } else {
                                        try {
                                            // mountService.unmountVolume(extStoragePathnotPhone, true);
                                                mountService.formatVolume(extStoragePathnotPhone);
                                        } catch (Exception e) {
                                            mFormatAll = false;
                                            return;
                                        }
                                    }
                                    sendBroadcast(new Intent("android.intent.action.MASTER_CLEAR"));
                                    // Intent handling is asynchronous -- assume
                                    // it will happen soon.
                                    stopSelf();
                                    return;
                                }
                            }
                            // If we didn't succeed, or aren't doing a full
                            // factory reset, then it is time to remount the storage.
                            if (!success && mAlwaysReset) {
                                sendBroadcast(new Intent("android.intent.action.MASTER_CLEAR"));
                            } else {
                                try {
                                    mountService.mountVolume(extStoragePath);
                                } catch (RemoteException e) {
                                    Log.w(TAG, "Failed talking with mount service", e);
                                }
                            }
                            stopSelf();
                            return;
                        }
                    }.start();
                } else {
                    Log.w("MediaFormat", "Unable to locate IMountService");
                }
            } else if (Environment.MEDIA_BAD_REMOVAL.equals(status)) {
                fail(R.string.media_bad_removal);
            } else if (Environment.MEDIA_CHECKING.equals(status)) {
                fail(R.string.media_checking);
            } else if (Environment.MEDIA_REMOVED.equals(status)) {
                fail(R.string.media_removed);
            } else if (Environment.MEDIA_SHARED.equals(status)) {
                fail(R.string.media_shared);
            } else {
                fail(R.string.media_unknown_state);
                Log.w(TAG, "Unknown storage state: " + status);
                stopSelf();
            }
        }else {
            if (Environment.MEDIA_MOUNTED.equals(status)
                || Environment.MEDIA_MOUNTED_READ_ONLY.equals(status)) {
                updateProgressDialog(R.string.progress_unmounting);
                IMountService mountService = getMountService();
                String extStoragePath = Environment.getInternalStorageDirectory().toString();
                try {
                    mountService.unmountVolume(extStoragePath, true, true);
                } catch (RemoteException e) {
                    Log.w(TAG, "Failed talking with mount service", e);
                }
            } else if (Environment.MEDIA_NOFS.equals(status)
                        || Environment.MEDIA_UNMOUNTED.equals(status)
                        || Environment.MEDIA_UNMOUNTABLE.equals(status)) {
                updateProgressDialog(R.string.progress_erasing);
                final IMountService mountService = getMountService();
                final String extStoragePath = Environment.getInternalStorageDirectory().toString();

                if (mountService != null) {
                    new Thread() {
                        public void run() {
                            boolean success = false;
                            try {
                                mountService.formatVolume(extStoragePath);
                                success = true;
                            } catch (Exception e) {
                                Toast.makeText(PhoneStorageFormatter.this,
                                                        R.string.format_error,
                                                        Toast.LENGTH_LONG).show();
                            }
                            if (success) {
                                if (mFactoryReset) {
                                    sendBroadcast(new Intent("android.intent.action.MASTER_CLEAR"));
                                    // Intent handling is asynchronous -- assume
                                    // it will happen soon.
                                    stopSelf();
                                    return;
                                }
                            }
                            // If we didn't succeed, or aren't doing a full
                            // factory reset, then it is time to remount the storage.
                            if (!success && mAlwaysReset) {
                                sendBroadcast(new Intent("android.intent.action.MASTER_CLEAR"));
                            } else {
                                try {
                                    mountService.mountVolume(extStoragePath);
                                } catch (RemoteException e) {
                                    Log.w(TAG, "Failed talking with mount service", e);
                                }
                            }
                            stopSelf();
                            return;
                        }
                    }.start();
                } else {
                    Log.w("MediaFormat", "Unable to locate IMountService");
                }
            } else if (Environment.MEDIA_BAD_REMOVAL.equals(status)) {
                fail(R.string.media_bad_removal);
            } else if (Environment.MEDIA_CHECKING.equals(status)) {
                fail(R.string.media_checking);
            } else if (Environment.MEDIA_REMOVED.equals(status)) {
                fail(R.string.media_removed);
            } else if (Environment.MEDIA_SHARED.equals(status)) {
                fail(R.string.media_shared);
            } else {
                fail(R.string.media_unknown_state);
                Log.w(TAG, "Unknown storage state: " + status);
                stopSelf();
            }
        }
    }

    public void updateProgressDialog(int msg) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.getWindow().setType(
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
            mProgressDialog.show();
        }

        mProgressDialog.setMessage(getText(msg));
    }

    IMountService getMountService() {
        if (mMountService == null) {
            IBinder service = ServiceManager.getService("mount");
            if (service != null) {
                mMountService = IMountService.Stub.asInterface(service);
            } else {
                Log.e(TAG, "Can't get mount service");
            }
        }
        return mMountService;
    }
}
