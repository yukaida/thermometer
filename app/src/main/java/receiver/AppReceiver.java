package receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AppReceiver extends BroadcastReceiver {
	private static final String TAG = "AppReceiver";
	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "onReceive: 静态注册"+intent.getData().getSchemeSpecificPart());
		if (intent.getAction().equals(Intent.ACTION_PACKAGE_ADDED)) {
            String packageName = intent.getData().getSchemeSpecificPart();
            System.out.println(packageName);
            if(packageName.equals("com.iflytek.speechcloud")){
            	gotoTTS(context);
            }
        }
	}

	private void gotoTTS(Context context){
		Intent intent = new Intent();
		intent.setAction("com.android.settings.TTS_SETTINGS");
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}
}
