package receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * author: yukaida
 * desc: this receiver use to start application when system boot
 * */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Log.d(TAG, "onReceive: receive boot");
            Intent startIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
            context.startActivity(startIntent);
        }
    }
}
