package com.ebanswers.thermometer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.provider.FontsContract;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;
import com.kongqw.serialportlibrary.Device;
import com.kongqw.serialportlibrary.SerialPortFinder;
import com.kongqw.serialportlibrary.SerialPortManager;
import com.kongqw.serialportlibrary.listener.OnOpenSerialPortListener;
import com.kongqw.serialportlibrary.listener.OnSerialPortDataListener;
import com.tencent.bugly.crashreport.CrashReport;
import com.yhao.floatwindow.FloatWindow;
import com.yhao.floatwindow.MoveType;
import com.yhao.floatwindow.PermissionListener;
import com.yhao.floatwindow.Screen;
import com.yhao.floatwindow.ViewStateListener;

import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import utils.DataConversion;
import utils.HexUtils;
import utils.KqwSpeechSynthesizer;
import utils.LanguageUtil;
import utils.NumberToEnglishUtils;
import utils.SPUtils;
import utils.ToastCustom;
import utils.TxtUtils;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private static final String TAG = "MainActivity";
    SerialPortManager mSerialPortManager;
    Button button;
    boolean readTemp = false;
    Timer timer;
    TimerTask timerTask;
    boolean atCDboolean = true;
    ImageView imageView;
    private SharedPreferences mSharedPreferences;
    private SurfaceView surfaceView;//预览摄像头
    private SurfaceHolder surfaceHolder;
    private Button buttoncamera;//拍照按钮
    private Camera camera;//摄像头

    private RecyclerView recyclerView;

    int authorization = 0;
    TextView tvTemp;//温度
    KqwSpeechSynthesizer mKqwSpeechSynthesizer;
    ConstraintLayout constraintLayout;

    MyRvAdapter myRvAdapter;

    TextView tvSp;
    Button button_exit;

    Button button_camera;
    Button button_temp;
    Button button_voice;
    Button button_save;
    Button button_restoreCamera;
    Button button_tempset;
    Button button_reset;
    Button button_lang;


    EditText editText_portspeed;
    EditText editText_windowWidth;
    EditText editText_windowHeight;
    EditText editText_temp;

    public static MainActivity sMainActivity;
    ArrayList<Device> devices;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case 0://显示
                    if (FloatWindow.get("camera") != null)
                        FloatWindow.get("camera").show();
                    break;
                case 1://隐藏
                    if (FloatWindow.get("camera") != null) {
                        FloatWindow.get("camera").hide();
                        SPUtils.put(MainActivity.this, "windowX", FloatWindow.get("camera").getX());//悬浮窗位置x 默认300
                        SPUtils.put(MainActivity.this, "windowY", FloatWindow.get("camera").getY());//悬浮窗位置x 默认300
                    }
                    break;
                case 2://修改位置
                    if (FloatWindow.get("camera") != null)
                        FloatWindow.get("camera").updateX(500);
                    break;
            }
        }
    };

    int SerialPort = 0;//串口
    int Baud = 9600;//波特率
    boolean cameraOpen = true;//摄像头
    boolean temperatureShow = true;//温度
    boolean voiceOpen = true;//语音播报
    boolean success = false;

    //悬浮窗相关
    int windowWidth = 300;
    int windowHeight = 400;
    int windowX = 800;
    int windowY = 300;

    double tempNumber = 37.3;
//    @Override
//    public void onClick(View v) {
//        switch (v.getId()) {
////            case R.id.button_camera:
////
////                break;
////            case R.id.button_temp:
////
////                break;
////            case R.id.button_speak:
////
////                break;
////            case R.id.button_save:
////
////                break;
//
//        }
//    }

    private Context othercontext;
    private SharedPreferences sp;
    boolean appIntsalled = false;

    private String lang = "zh";
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != timerTask) {
            timerTask.cancel();
        }
        if (null != timer) {
            timer.cancel();
        }
        mSerialPortManager.closeSerialPort();
        mKqwSpeechSynthesizer.stop();
        FloatWindow.destroy("camera");
        mKqwSpeechSynthesizer.stop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //---------系统已安装”com.dsplayer”,” com.ebanswers.auxtool”和” com.ebanswers.aotoshutdown”这个三个APP---------
        appIntsalled = isAvilible(this, "com.dsplayer")
                && isAvilible(this, "com.ebanswers.auxtool")
                && isAvilible(this, "com.ebanswers.aotoshutdown");
        Log.d(TAG, "授权 应用安装：" + appIntsalled);

//-------------------读取com.dsplayer APP的sharedpreferences文件，若PLAYER_ID属性是否包含有效整数值-------------------------------
        try {
            othercontext = createPackageContext("com.dsplayer", Context.CONTEXT_IGNORE_SECURITY);
            Log.d(TAG, "授权:" + othercontext.getPackageName());
        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "授权:" + e);
            e.printStackTrace();
        }

        try {
            //根据Context取得对应的SharedPreferences
            sp = othercontext.getSharedPreferences("ebanswers_preferences", Context.MODE_WORLD_WRITEABLE);
            Log.d(TAG, "授权sp：" + sp.getAll().toString());
            authorization = sp.getInt("PLAYER_ID", 0);
            Log.d(TAG, "授权sp：" + authorization);

        } catch (Exception e) {
            Log.d(TAG, "授权sp：" + e);
        }

        try {
            Context dsplayerAppContext = getApplicationContext().createPackageContext("com.dsplayer", Context.MODE_WORLD_WRITEABLE);
            authorization = dsplayerAppContext.getSharedPreferences("ebanswers_preferences", Context.MODE_WORLD_WRITEABLE).getInt("PLAYER_ID", 0);
            Log.d(TAG, "授权sp2: " + dsplayerAppContext.getSharedPreferences("ebanswers_preferences", Context.MODE_WORLD_WRITEABLE).getAll().toString());
            Log.d(TAG, "授权sp2：" + authorization);

        } catch (Exception e) {

        }


        try {
//                String content = TxtUtils.readFromXML("/storage/emulated/0/ebanswers/appconfig.js");
            String content = TxtUtils.readFromXML("/sdcard/ebanswers/appconfig.js");
            Log.d(TAG, "授权sp2: " + content);
            String jsonString = content.substring(15);
            Log.d(TAG, "授权sp2: " + jsonString);
//                String jsonString2 =content.substring(0, jsonString.length() - 1);
            String jsonString2 = jsonString.trim().replace(";", "");
//                if (("PlayerId")) {
//
//                }
            Log.d(TAG, "授权sp2  jsonString2: " + jsonString2);
            JSONObject jsonObject = new JSONObject(jsonString2);
            int id = jsonObject.getInt("PlayerId");
            Log.d(TAG, "授权sp2 id: " + id);

            if (id > 10) {
                authorization = 1;
            }


        } catch (Exception e) {
        }


//-----------------------------------------------------------------------------------------------
        button_lang = findViewById(R.id.button_lang);
        button_lang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ("语音语言：中文".equals(button_lang.getText().toString().trim())) {
                    button_lang.setText("语音语言：English");
                    lang = "en";
                    SPUtils.put(MainActivity.this, "language", "en");//语言
                } else {
                    button_lang.setText("语音语言：中文");
                    lang = "zh";
                    SPUtils.put(MainActivity.this, "language", "zh");//语言
                }

            }
        });

        button_reset = findViewById(R.id.button_reset);
        button_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SPUtils.clear(MainActivity.this);
                if (null != timerTask) {
                    timerTask.cancel();
                }
                if (null != timer) {
                    timer.cancel();
                }
                mSerialPortManager.closeSerialPort();
                mKqwSpeechSynthesizer.stop();
                FloatWindow.destroy("camera");
                finish();
            }
        });
        button_exit = findViewById(R.id.button_exit);
        button_tempset = findViewById(R.id.button_tempset);
        button_tempset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SPUtils.put(MainActivity.this, "temperatureNumber", editText_temp.getText().toString().trim());//异常体温下限
            }
        });

        button_camera = findViewById(R.id.button_camera);
        button_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (cameraOpen) {
                    button_camera.setText("摄像头：关闭");
                } else {
                    button_camera.setText("摄像头：开启");
                }
                cameraOpen = cameraOpen ? false : true;
                SPUtils.put(MainActivity.this, "camera", cameraOpen);//摄像头
            }
        });

        button_temp = findViewById(R.id.button_temp);
        button_temp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (temperatureShow) {
                    button_temp.setText("温度：隐藏");


                } else {
                    button_temp.setText("温度：显示");
                }
                temperatureShow = temperatureShow ? false : true;
                SPUtils.put(MainActivity.this, "temperature", temperatureShow);//温度
            }
        });

        editText_windowWidth = findViewById(R.id.editText_width);
        editText_windowHeight = findViewById(R.id.editText_height);
        editText_temp = findViewById(R.id.editText_temp);//温度设置

        button_save = findViewById(R.id.button_save);
        button_save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!editText_windowWidth.getText().toString().isEmpty()) {
                    windowWidth = Integer.valueOf(editText_windowWidth.getText().toString().trim());
                    SPUtils.put(MainActivity.this, "windowWidth", windowWidth);//悬浮窗宽 默认400

                    if (!editText_windowHeight.getText().toString().isEmpty()) {
                        windowHeight = Integer.valueOf(editText_windowHeight.getText().toString().trim());
                        SPUtils.put(MainActivity.this, "windowHeight", windowHeight);//悬浮窗宽 默认400

                        Toast.makeText(MainActivity.this, "保存成功,请点击“退出应用”,并重启应用", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
//重置悬浮窗
        button_restoreCamera = findViewById(R.id.button_restorecamera);
        button_restoreCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                windowWidth = 300;
                windowHeight = 400;
                windowX = 800;
                windowY = 300;
                SPUtils.put(MainActivity.this, "windowWidth", 300);//悬浮窗宽 默认300
                SPUtils.put(MainActivity.this, "windowHeight", 400);//悬浮窗宽 默认400
                SPUtils.put(MainActivity.this, "windowX", 800);//悬浮窗位置x 默认800
                SPUtils.put(MainActivity.this, "windowY", 300);//悬浮窗位置y 默认300
                Toast.makeText(MainActivity.this, "重置成功,请点击“退出应用”,并重启应用", Toast.LENGTH_SHORT).show();

            }
        });

        button_voice = findViewById(R.id.button_speak);
        button_voice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (voiceOpen) {
                    button_voice.setText("语音播报：关闭");
                } else {
                    button_voice.setText("语音播报：开启");
                }
                voiceOpen = voiceOpen ? false : true;
                SPUtils.put(MainActivity.this, "voice", voiceOpen);//语音
            }
        });

        editText_portspeed = findViewById(R.id.editText_portspeed);

        SerialPortFinder serialPortFinder = new SerialPortFinder();
        devices = serialPortFinder.getDevices();

        imageView = new ImageView(getApplicationContext());
        imageView.setImageResource(R.drawable.ic_launcher_background);

        tvSp = findViewById(R.id.textView_sp);
        tvSp.setText("串口：" + devices.get(SerialPort).getName());
        button_exit = findViewById(R.id.button_exit);
        button_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != timerTask) {
                    timerTask.cancel();
                }
                if (null != timer) {
                    timer.cancel();
                }
                mSerialPortManager.closeSerialPort();
                mKqwSpeechSynthesizer.stop();
                FloatWindow.destroy("camera");
                finish();
            }
        });


        sMainActivity = this;

        recyclerView = findViewById(R.id.sp_rv);

        final ArrayList<String> spName = new ArrayList<>();
        for (Device device : devices) {
            spName.add(device.getName());
        }
        myRvAdapter = new MyRvAdapter(spName);
        recyclerView.setAdapter(myRvAdapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=5ec5e6f6");

        mKqwSpeechSynthesizer = new KqwSpeechSynthesizer(this);//语音引擎初始化

        surfaceView = new SurfaceView(getApplicationContext());
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);//摄像头帧容器初始化

        constraintLayout = (ConstraintLayout) LayoutInflater.from(this).inflate(R.layout.window_float, null);
//        view.
        constraintLayout.addView(surfaceView);//悬浮窗view初始化

        tvTemp = new TextView(getApplicationContext());
        tvTemp.setText("");
        tvTemp.setTextColor(getResources().getColor(R.color.colortemp));
        tvTemp.setTextSize(50);//悬浮窗温度初始化

//        tvTemp.setLayoutParams();

        constraintLayout.addView(tvTemp);

//--------------------------开始读取温度---------------------------------
        button = findViewById(R.id.button_readtemp);//开始读取温度按钮
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//        打开串口
//        参数1：串口
//        参数2：波特率
//        返回：串口打开是否成功
                if (button.getText().equals("开始读取温度")) {
                    if (!editText_portspeed.getText().toString().trim().isEmpty()) {
                        Baud = Integer.valueOf(editText_portspeed.getText().toString().trim());
                    }
                    boolean openSerialPort = mSerialPortManager.openSerialPort(devices.get(SerialPort).getFile(), Baud);
                    Log.d(TAG, "onCreate: SerialPort open :--" + devices.get(SerialPort).getName() + "--succeed ? " + openSerialPort);
                    if (!openSerialPort) {
                        Toast.makeText(MainActivity.this, "串口打开失败,请切换串口,波特率或检查设备", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Toast.makeText(MainActivity.this, "打开成功,开始读取温度,串口" + devices.get(SerialPort).getName() + "波特率" + Baud, Toast.LENGTH_SHORT).show();
                    SPUtils.put(MainActivity.this, "Baud", Baud);//波特率 默认9600
                    SPUtils.put(MainActivity.this, "SerialPort", SerialPort);//波特率 默认9600
                    SPUtils.put(MainActivity.this, "success", true);//是否成功运行过
                }


                if (readTemp) {
                    readTemp = false;
                    button.setText("继续");
                } else {
                    readTemp = true;
                    button.setText("停止读取温度");
                }

                if (readTemp) {
                    timer = new Timer();
                    timerTask = new TimerTask() {
                        @Override
                        public void run() {
                            boolean sendBytes = mSerialPortManager.sendBytes(HexUtils.Hex2Bytes("A0A1AA"));
                        }
                    };
                    timer.schedule(timerTask, 0, 1000);
                } else {
                    if (null != timerTask) {
                        timerTask.cancel();
                    }
                    if (null != timer) {
                        timer.cancel();
                    }
                }
            }
        });
//---------------------------------------------------------------
        //串口管理器
        mSerialPortManager = new SerialPortManager();

        //打开串口监听
        mSerialPortManager.setOnOpenSerialPortListener(new OnOpenSerialPortListener() {
            @Override
            public void onSuccess(File device) {

            }

            @Override
            public void onFail(File device, Status status) {

            }
        });

        //数据通信监听
        mSerialPortManager.setOnSerialPortDataListener(new OnSerialPortDataListener() {
            @Override
            public void onDataReceived(byte[] bytes) {
                String dataReceived = DataConversion.encodeHexString(bytes);
                Log.d(TAG, "onDataReceived: " + dataReceived);
                double temp = getTempWithHex(dataReceived);
                analysisTemp(temp);
                Log.d(TAG, "temperature: " + temp);
            }

            @Override
            public void onDataSent(byte[] bytes) {
                Log.d(TAG, "onDataSent: " + DataConversion.encodeHexString(bytes));
            }
        });

//        发送数据
//        参数：发送数据 byte[]
//        返回：发送是否成功
//        boolean sendBytes = mSerialPortManager.sendBytes(HexUtils.Hex2Bytes("A0A1AA"));


        if (!(Boolean) SPUtils.get(this, "success", false)) {
            SPUtils.put(this, "SerialPort", spName.indexOf("ttyS3"));//串口 position
            SPUtils.put(this, "success", false);//是否成功运行过
            SPUtils.put(this, "language", "zh");//语言
            tvSp.setText(spName.get(spName.indexOf("ttyS3")));
            SPUtils.put(this, "Baud", 9600);//波特率 默认9600
            editText_portspeed.setText("9600");
            SPUtils.put(this, "camera", true);//摄像头 默认开启
            SPUtils.put(this, "temperature", true);//温度 默认显示
            SPUtils.put(this, "voice", true);//语音播报 默认开启
            SPUtils.put(this, "windowWidth", 300);//悬浮窗宽 默认300
            SPUtils.put(this, "windowHeight", 400);//悬浮窗宽 默认400
            SPUtils.put(this, "windowX", 800);//悬浮窗位置x 默认800
            SPUtils.put(this, "windowY", 300);//悬浮窗位置y 默认300
            SPUtils.put(this, "temperatureNumber", 37.3);//异常体温下限

//            HashMap<String, Integer> mapConfig = new HashMap<>();
//            mapConfig.put("SerialPort", 0);
//            mapConfig.put("Baud", 9600);
//            mapConfig.put("camera", 1);
//            mapConfig.put("temperature", 1);
//            mapConfig.put("voice", 1);
            Log.d(TAG, "onCreate: 本地配置 首次启动 ---");

        } else {

            SerialPort = (int) SPUtils.get(this, "SerialPort", 4);
//

            Baud = (int) SPUtils.get(this, "Baud", 9600);
            editText_portspeed.setText(Baud + "");

            lang = (String) SPUtils.get(this, "language", "zh");
            if (!"zh".equals(lang)) {
                button_lang.setText("语音语言：English");
                lang = "en";
                SPUtils.put(MainActivity.this, "language", "en");//语言
            }
            cameraOpen = (Boolean) SPUtils.get(this, "camera", true);
            temperatureShow = (Boolean) SPUtils.get(this, "temperature", true);
            voiceOpen = (Boolean) SPUtils.get(this, "voice", true);

            windowX = (int) SPUtils.get(this, "windowX", 800);//悬浮窗位置x 默认800
            windowY = (int) SPUtils.get(this, "windowY", 300);//悬浮窗位置x 默认300
            windowWidth = (int) SPUtils.get(this, "windowWidth", 300);//悬浮窗宽 默认300
            windowHeight = (int) SPUtils.get(this, "windowHeight", 400);//悬浮窗高 默认400

//            tempNumber = (double) SPUtils.get(this, "temperatureNumber", 37.3);//温度下限
//            if (tempNumber != 37.3) {
//                editText_temp.setText(tempNumber + "");
//            }


            tvSp.setText("串口：" + devices.get(SerialPort).getName());
            button_camera.setText(cameraOpen ? "摄像头：开启" : "摄像头：关闭");
            button_temp.setText(temperatureShow ? "温度：显示" : "温度：隐藏");
            button_voice.setText(voiceOpen ? "语音播报：开启" : "语音播报：关闭");
            editText_portspeed.setText(Baud + "");

            Log.d(TAG, "本地配置 " + "\n" + "串口" + SerialPort + "\n"
                    + "波特率" + Baud + "\n"
                    + "摄像头" + cameraOpen + "\n"
                    + "温度" + temperatureShow + "\n"
                    + "语音播报" + voiceOpen + "\n"
                    + "悬浮窗宽：" + windowWidth + "_高:" + windowHeight + "_X：" + windowX + "_Y" + windowY);


            if (button.getText().equals("开始读取温度")) {
                if (!editText_portspeed.getText().toString().trim().isEmpty()) {
                    Baud = Integer.valueOf(editText_portspeed.getText().toString().trim());
                }
                boolean openSerialPort = mSerialPortManager.openSerialPort(devices.get(SerialPort).getFile(), Baud);
                Log.d(TAG, "onCreate: SerialPort open :--" + devices.get(SerialPort).getName() + "--succeed ? " + openSerialPort);
                if (!openSerialPort) {
                    Toast.makeText(MainActivity.this, "串口打开失败,请切换串口,波特率或检查设备", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(MainActivity.this, "打开成功,开始读取温度,串口" + devices.get(SerialPort).getName() + "波特率" + Baud, Toast.LENGTH_SHORT).show();
                SPUtils.put(MainActivity.this, "Baud", Baud);//波特率 默认9600
                SPUtils.put(MainActivity.this, "SerialPort", SerialPort);//波特率 默认9600
            }


            if (readTemp) {
                readTemp = false;
                button.setText("继续");
            } else {
                readTemp = true;
                button.setText("停止读取温度");
            }

            if (readTemp) {
                timer = new Timer();
                timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        boolean sendBytes = mSerialPortManager.sendBytes(HexUtils.Hex2Bytes("A0A1AA"));
                    }
                };
                timer.schedule(timerTask, 0, 1000);
            } else {
                if (null != timerTask) {
                    timerTask.cancel();
                }
                if (null != timer) {
                    timer.cancel();
                }
            }
        }

        editText_windowWidth.setHint("悬浮窗宽" + windowWidth);
        editText_windowHeight.setHint("高：" + windowHeight);

        //-------------------悬浮窗--------------------------------------
        FloatWindow
                .with(getApplicationContext())
                .setView(constraintLayout)
                .setWidth(windowWidth)                               //设置控件宽高
                .setHeight(windowHeight)
                .setX(windowX)                                   //设置控件初始位置
                .setY(windowY)
                .setDesktopShow(true)                        //桌面显示
                .setViewStateListener(mViewStateListener)    //监听悬浮控件状态改变
                .setPermissionListener(mPermissionListener)  //监听权限申请结果
                .setMoveType(MoveType.active)
                .setTag("camera")
                .setMoveStyle(500, new AccelerateInterpolator())  //贴边动画时长为500ms，加速插值器
                .build();//悬浮窗对象实例化

        Message messagehide = new Message();
        messagehide.what = 1;
        handler.sendMessage(messagehide);//隐藏悬浮窗

        if (success) {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            startActivity(home);
        }


//        Intent i = new Intent("com.ebanswers.startDog");
//        i.putExtra("package", getPackageName(getApplicationContext()));
//        i.putExtra("callbackActivity", "MainActivity");
//        sendBroadcast(i);
        CrashReport.initCrashReport(getApplicationContext(), "70c3427e4d", false);

    }

    public static synchronized String getPackageName(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    context.getPackageName(), 0);
            return packageInfo.packageName;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //解析传入的16进制指令,返回温度,如 A0A10021081A01AA   33.8
    private double getTempWithHex(String hexString) {
        try {
            if (hexString.length() == 16) {
                String plus_minus = hexString.substring(4, 6);
                String integer = hexString.substring(6, 8);
                String decimal = hexString.substring(8, 10);
                Log.d(TAG, "getTempWithHex: plus_minus:" + plus_minus + "\n"
                        + "integer:" + integer + "\n"
                        + "decimal:" + decimal
                );
                double intergerDouble = DataConversion.hexToDec(integer);
                double decimalLong = DataConversion.hexToDec(decimal);
                double decimalDouble = decimalLong / 10;
                double temp = intergerDouble + decimalDouble;
                return plus_minus.equals("00") ? temp : -(temp);
            }
        } catch (Exception e) {
            return 100;
        }
        return 100;
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //要执行的操作
                atCDboolean = true;
                Message messagehide = new Message();
                messagehide.what = 1;
                handler.sendMessage(messagehide);
            }
        }, 2000);//2秒后执行Runnable中的run方法
    }

    private void analysisTemp(final double temp) {
        if (authorization == 0 || !appIntsalled) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(sMainActivity, "未授权！请注册播放端后重启系统。", Toast.LENGTH_SHORT).show();
                }
            });
        } else if (35 < temp && temp < 40) {
            if (atCDboolean) {
                atCDboolean = false;

                Log.d(TAG, "摄像头: " + cameraOpen + "  语音：" + voiceOpen + "  温度" + temperatureShow);

                if (voiceOpen) {//语音播报


//
                    if ("zh".equals(lang)) {

                        if (35 < temp && temp < tempNumber) {
                            mKqwSpeechSynthesizer.start(temp + "度" + "体温正常");
                        } else {
                            mKqwSpeechSynthesizer.start(temp + "度" + "体温异常");
                        }

                    } else {

                        if (35 < temp && temp < tempNumber) {
                            mKqwSpeechSynthesizer.start("thirty"+ NumberToEnglishUtils.getEnglish(Integer.valueOf(String.valueOf(temp).substring(1,2))) +"point"+ NumberToEnglishUtils.getEnglish(Integer.valueOf(String.valueOf(temp).substring(3))) + " degrees, normal temperature");
                        } else {
                            mKqwSpeechSynthesizer.start(temp + "degrees, abnormal temperature");
                        }

                    }

                }

                if (temperatureShow) {//显示温度

                    if (!cameraOpen) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        "", Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
//                                ToastCustom.makeText(temp + "℃",getApplicationContext());

                                LinearLayout toastView = (LinearLayout) toast.getView();
                                WindowManager wm = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
                                DisplayMetrics outMetrics = new DisplayMetrics();
                                wm.getDefaultDisplay().getMetrics(outMetrics);
                                TextView tv = new TextView(MainActivity.this);

                                tv.setTextSize(50);
                                toastView.setGravity(Gravity.CENTER);
                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT);
                                params.setMargins(0, 0, 0, 50);
                                tv.setLayoutParams(params);
                                toast.setView(toastView);
                                toastView.addView(tv);
                                tv.setText(temp + "℃");
                            }
                        });

                    }


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvTemp.setText(temp + "℃");
                        }
                    });

                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvTemp.setText(temp + "℃");
                            tvTemp.setText("");
                        }
                    });
                }

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //要执行的操作
                        atCDboolean = true;
                        Message messagehide = new Message();
                        messagehide.what = 1;
                        handler.sendMessage(messagehide);
                    }
                }, 5000);//5秒后执行Runnable中的run方法

                if (cameraOpen) { //打开 摄像头
                    showCamera();
                }
            }
        }

    }

    private boolean atCD() {
        if (atCDboolean) {
            atCDboolean = false;
            return atCDboolean;
        } else {
            atCDboolean = true;

            return atCDboolean;
        }
    }


    private void showCamera() {
        Message messageshow = new Message();
        messageshow.what = 0;
        handler.sendMessage(messageshow);
    }


    private PermissionListener mPermissionListener = new PermissionListener() {
        @Override
        public void onSuccess() {
            Log.d(TAG, "onSuccess");
        }

        @Override
        public void onFail() {
            Log.d(TAG, "onFail");
        }
    };

    private ViewStateListener mViewStateListener = new ViewStateListener() {
        @Override
        public void onPositionUpdate(int x, int y) {
            Log.d(TAG, "onPositionUpdate: x=" + x + " y=" + y);
        }

        @Override
        public void onShow() {
            Log.d(TAG, "onShow");
        }

        @Override
        public void onHide() {
            Log.d(TAG, "onHide");
        }

        @Override
        public void onDismiss() {
            Log.d(TAG, "onDismiss");
        }

        @Override
        public void onMoveAnimStart() {
            Log.d(TAG, "onMoveAnimStart");
        }

        @Override
        public void onMoveAnimEnd() {
            Log.d(TAG, "onMoveAnimEnd");
        }

        @Override
        public void onBackToDesktop() {
            Log.d(TAG, "onBackToDesktop");
        }
    };

    private void initCamera() {
        Camera.Parameters params = camera.getParameters();
        params.setRecordingHint(true);
        camera.setParameters(params);
        camera.startPreview();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera = Camera.open(0);
            camera.setPreviewDisplay(surfaceHolder);
        } catch (Exception e) {
            if (null != camera) {
                camera.release();
                camera = null;
            }
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "启动摄像头失败,请开启摄像头权限", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        initCamera();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (null != camera) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }


    private boolean isAvilible(Context context, String packageName) {
        final PackageManager packageManager = context.getPackageManager();

        // 获取所有已安装程序的包信息
        List<PackageInfo> pinfo = packageManager.getInstalledPackages(0);
        for (int i = 0; i < pinfo.size(); i++) {
            // 循环判断是否存在指定包名
            if (pinfo.get(i).packageName.equalsIgnoreCase(packageName)) {
                return true;
            }

        }
        return false;
    }

}

