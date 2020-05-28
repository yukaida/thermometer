package utils;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.DisplayMetrics;

import com.ebanswers.thermometer.MyApplication;

import java.util.Locale;


public class LanguageUtil {
    private static final String TAG = "LanguageUtil";
    private static LanguageUtil instance;
    private Resources mResources;

    private LanguageUtil() {
        mResources = MyApplication.getInstance().getResources();
    }

    public static LanguageUtil getInstance() {
        if (instance == null) {
            synchronized (LanguageUtil.class) {
                if (instance == null) {
                    instance = new LanguageUtil();
                }
            }
        }
        return instance;
    }

    /**
     * @return 中文zh  英文en
     */
    public String getSystemLanguage() {

            return Locale.getDefault().getLanguage();

    }

    public void logswitchLanguage(String language) {
        Configuration configuration = mResources.getConfiguration();
        DisplayMetrics displayMetrics = mResources.getDisplayMetrics();
        switch (language) {
            case "zh":
                configuration.locale = Locale.SIMPLIFIED_CHINESE;
                break;
            case "en":
                configuration.locale = Locale.ENGLISH;
                break;
            case "auto":
                configuration.locale = Locale.ENGLISH;
                break;
        }
        mResources.updateConfiguration(configuration, displayMetrics);
    }



    public boolean isChinease() {
        return "zh".equals(getSystemLanguage());
    }

    public String getStringById(int id) {
        return mResources.getString(id);
    }


}
