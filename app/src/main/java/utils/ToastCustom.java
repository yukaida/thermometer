package utils;

import android.content.Context;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.ebanswers.thermometer.R;


/**
 * Created by air on 2017/9/2.
 */

public class ToastCustom {
    public static final int LENGTH_SHORT = Toast.LENGTH_LONG;
    public static final int LENGTH_LONG = Toast.LENGTH_LONG;

    Toast toast;
    Context mContext;
    TextView toastTextField;
    private static ToastCustom toastCustom;

    public ToastCustom(Context context) {
        mContext = context;
        toast = new Toast(mContext);
        toast.setGravity(Gravity.CENTER,0,120);// 位置会比原来的Toast偏上一些
        View toastRoot = View.inflate(context, R.layout.toast_view, null);
        toastTextField = (TextView) toastRoot.findViewById(R.id.toast_text);
        TextPaint tp = toastTextField.getPaint();
        toastTextField.setTextSize(18);
        tp.setFakeBoldText(true);
        toast.setView(toastRoot);
    }

    public void setDuration(int d) {
        toast.setDuration(d);
    }

    public void setText(String t) {
        toastTextField.setText(t);
    }

    public static ToastCustom makeText(Context context, String text, int duration) {
//        if(toastCustom == null ) {
//        }
        toastCustom = new ToastCustom(context);
        toastCustom.setText(text);
        toastCustom.setDuration(duration);
        return toastCustom;
    }


    public static ToastCustom makeText(Context context, int textid, int duration) {
//        if(toastCustom == null ) {
//        }
        toastCustom = new ToastCustom(context);
        toastCustom.setText(context.getResources().getString(textid));
        toastCustom.setDuration(duration);
        return toastCustom;
    }

    public static ToastCustom makeText(String text,Context context) {
        return makeText(context, text, ToastCustom.LENGTH_LONG);
    }
//
//    public static ToastCustom makeText(int text) {
//        return makeText(KitchenDiaryApplication.getInstance(), text, ToastCustom.LENGTH_LONG);
//    }

    public void show() {
        toast.show();
    }

}
