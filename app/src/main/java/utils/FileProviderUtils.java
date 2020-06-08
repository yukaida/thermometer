package utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.io.File;

/**
 * @author newtrekWang
 * @fileName FileProviderUtils
 * @createDate 2019/2/14 11:47
 * @email 408030208@qq.com
 * @desc FileProvider工具类,用于适配Android7.0的文件访问变更
 */
public final class FileProviderUtils {
    /**
     * 获取合适的File Uri
     * @param context
     * @param file
     * @return
     */
    private static final String TAG = "FileProviderUtils";
    public static Uri getUriForFile(Context context, File file) {
        Uri fileUri = null;
        if (Build.VERSION.SDK_INT >= 24) {
            fileUri = getUriForFile24(context, file);
        } else {
            fileUri = Uri.fromFile(file);
        }
        return fileUri;
    }

    /**
     * 获取File Uri from 安卓7及以上版本
     * @param context
     * @param file
     * @return
     */
    public static Uri getUriForFile24(Context context, File file) {
        Uri fileUri = android.support.v4.content.FileProvider.getUriForFile(context,
                "com.ebanswers.thermometer.FileProvider",
                file);
        return fileUri;
    }

    /**
     * 用户安装apk场景
     * @param context
     * @param intent
     * @param type
     * @param file
     * @param writeAble
     */
    public static void setIntentDataAndType(Context context,
                                            Intent intent,
                                            String type,
                                            File file,
                                            boolean writeAble) {
        if (Build.VERSION.SDK_INT >= 24) {
            intent.setDataAndType(getUriForFile(context, file), type);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
            if (writeAble) {
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                context.startActivity(intent);
            }
        } else {
            Log.d(TAG, "setIntentDataAndType: 低于7.0");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setDataAndType(Uri.fromFile(file), type);
            context.startActivity(intent);
        }

    }
}
