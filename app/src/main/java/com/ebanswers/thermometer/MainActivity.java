package com.ebanswers.thermometer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.kongqw.serialportlibrary.Device;
import com.kongqw.serialportlibrary.SerialPortFinder;
import com.kongqw.serialportlibrary.SerialPortManager;
import com.kongqw.serialportlibrary.listener.OnOpenSerialPortListener;
import com.kongqw.serialportlibrary.listener.OnSerialPortDataListener;
import com.orhanobut.logger.Logger;
import com.yhao.floatwindow.FloatWindow;
import com.yhao.floatwindow.MoveType;
import com.yhao.floatwindow.PermissionListener;
import com.yhao.floatwindow.ViewStateListener;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import utils.DataConversion;
import utils.FileProviderUtils;
import utils.HexUtils;
import utils.NumberToEnglishUtils;
import utils.SPUtils;
import utils.TxtUtils;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    private static final String TAG = "MainActivity";
    SerialPortManager mSerialPortManager;

    /**
     * 语音朗读
     */
    protected TextToSpeech mTextToSpeech = null;

    boolean readTemp = false;//读取状态标识 false表示未处理读取状态
    boolean atCDboolean = true;//悬浮窗展示标识 true表示可展示

    Timer timer;
    TimerTask timerTask;

    int authorization = 0;
    TextView tvTemp;//温度

    ConstraintLayout constraintLayout;

    MyRvAdapter myRvAdapter;

    private SurfaceView surfaceView;//预览摄像头数据容器
    private SurfaceHolder surfaceHolder;
    private Camera camera;//摄像头
    private RecyclerView recyclerView;

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
    Button button_Start;
    Button button_SpeakPower;


    EditText editText_portspeed;
    EditText editText_windowWidth;
    EditText editText_windowHeight;
    EditText editText_temp;

    public static MainActivity sMainActivity;

    int createtime = 0;

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

    String SerialPort = "ttyS3";//串口
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
    private String lang = "zh";

    private Context othercontext;
    private SharedPreferences sp;
    boolean appIntsalled = false;

    private BroadcastReceiver mInstallAppBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (Build.VERSION.SDK_INT <= 26) {
                return;}

            if (intent != null && TextUtils.equals(Intent.ACTION_PACKAGE_ADDED, intent.getAction())) {

                if (intent.getData() != null) {

                    String packageName = intent.getData().getSchemeSpecificPart();

                    if ("com.iflytek.speechcloud".equals(packageName)) {
                        Log.d(TAG, "onReceive: 动态注册广播");
                        Intent intentTTS = new Intent();
                        intentTTS.setAction("com.android.settings.TTS_SETTINGS");
                        intentTTS.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intentTTS);
                        Toast.makeText(context, "请选择“科大讯飞语音引擎3.0”", Toast.LENGTH_LONG).show();
                    }

                }

            }

        }

    };

    @Override
    protected void onResume() {
        super.onResume();
        if ("com.iflytek.speechcloud".equals(mTextToSpeech.getDefaultEngine())) {
        } else {
            Log.d(TAG, "onResume: 重设语音引擎"+getNewTextToSpeech().getDefaultEngine());

            if ("com.iflytek.speechcloud".equals(mTextToSpeech.getDefaultEngine())) {
                mTextToSpeech = getNewTextToSpeech();
            } else {
                Toast.makeText(sMainActivity, "请选择“科大讯飞语音引擎3.0”", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Logger.d("启动次数" + createtime);
        createtime++;
        super.onCreate(savedInstanceState);
        mTextToSpeech = getNewTextToSpeech();

        Log.d(TAG, "onCreate: 语音引擎"+ mTextToSpeech.getDefaultEngine());

        setContentView(R.layout.activity_main);

        getAuthorization();//授权

        registerInstallAppBroadcastReceiver();
        //设置系统亮度
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + this.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                this.startActivity(intent);
            } else {
                //有了权限，具体的动作
//                Settings.System.putInt(getContentResolver(),
//                        Settings.System.SCREEN_BRIGHTNESS, this);
//                data2 = intToString(progress, 255);
//                tvSunlightValue.setText(data2 + "%");
            }
        }


        sMainActivity = this;

        button_SpeakPower = findViewById(R.id.button_speakpower);
        button_SpeakPower.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                if (!getInstalledPackages()) {
//                    installApp();
//                    return;
//                }

                Intent intent = new Intent();
                intent.setAction("com.android.settings.TTS_SETTINGS");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });
        //语音语言
        button_lang = findViewById(R.id.button_lang);
        button_lang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                installApp();
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

        //重置全局按钮
        button_reset = findViewById(R.id.button_reset);
        button_reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FloatWindow.destroy("camera");
                SPUtils.clear(MainActivity.this);
                if (null != timerTask) {
                    timerTask.cancel();
                }
                if (null != timer) {
                    timer.cancel();
                }
                mSerialPortManager.closeSerialPort();

                Toast.makeText(othercontext, "正在删除配置，请稍候", Toast.LENGTH_SHORT).show();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                        System.exit(0);
                    }
                }, 2000);


            }
        });

        //温度设置按钮.暂时不启用
//        button_tempset = findViewById(R.id.button_tempset);
//        button_tempset.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                SPUtils.put(MainActivity.this, "temperatureNumber", editText_temp.getText().toString().trim());//异常体温下限
//            }
//        });

        //摄像头：开启
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

        //温度：显示
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

        //语音播报：开启
        button_voice = findViewById(R.id.button_speak);
        button_voice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if ("com.iflytek.speechcloud".equals(mTextToSpeech.getDefaultEngine())) {

                } else {
                    Toast.makeText(sMainActivity, "请选择“科大讯飞语音引擎3.0”", Toast.LENGTH_SHORT).show();
                }
                if (voiceOpen) {
                    button_voice.setText("语音播报：关闭");
                } else {
                    button_voice.setText("语音播报：开启");
                }
                voiceOpen = voiceOpen ? false : true;
                SPUtils.put(MainActivity.this, "voice", voiceOpen);//语音
            }
        });

        editText_windowWidth = findViewById(R.id.editText_width);
        editText_windowHeight = findViewById(R.id.editText_height);
//        editText_temp = findViewById(R.id.editText_temp);//温度设置
        //悬浮窗宽高 保存按钮
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


        //波特率
        editText_portspeed = findViewById(R.id.editText_portspeed);

        //串口扫描
        SerialPortFinder serialPortFinder = new SerialPortFinder();
        devices = serialPortFinder.getDevices();
        final ArrayList<String> spName = new ArrayList<>();
        for (Device device : devices) {
            spName.add(device.getName());
        }

        //串口
        tvSp = findViewById(R.id.textView_sp);

        //退出应用按钮
        button_exit = findViewById(R.id.button_exit);
        button_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FloatWindow.destroy("camera");
                if (null != timerTask) {
                    timerTask.cancel();
                }
                if (null != timer) {
                    timer.cancel();
                }
                mSerialPortManager.closeSerialPort();

                finish();
            }
        });


        recyclerView = findViewById(R.id.sp_rv);
        myRvAdapter = new MyRvAdapter(spName);
        recyclerView.setAdapter(myRvAdapter);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        //-----悬浮窗-----------------
        surfaceView = new SurfaceView(getApplicationContext());
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);//摄像头帧容器初始化

        constraintLayout = (ConstraintLayout) LayoutInflater.from(this).inflate(R.layout.window_float, null);
        constraintLayout.addView(surfaceView);//悬浮窗view初始化

        tvTemp = new TextView(getApplicationContext());
        tvTemp.setText("");
        tvTemp.setTextColor(getResources().getColor(R.color.colortemp));
        tvTemp.setTextSize(50);//悬浮窗温度初始化

        constraintLayout.addView(tvTemp);

//--------------------------开始读取温度---------------------------------
        button_Start = findViewById(R.id.button_readtemp);//开始读取温度按钮
        button_Start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//        打开串口
//        参数1：串口
//        参数2：波特率
//        返回：串口打开是否成功
                if (button_Start.getText().equals("开始读取温度")) {
                    if (!editText_portspeed.getText().toString().trim().isEmpty()) {
                        Baud = Integer.valueOf(editText_portspeed.getText().toString().trim());
                    }
                    int position = spName.indexOf(tvSp.getText().toString());
                    boolean openSerialPort = mSerialPortManager.openSerialPort(devices.get(position).getFile(), Baud);
                    Log.d(TAG, "onCreate: SerialPort open :--" + SerialPort + "--succeed ? " + openSerialPort);
                    if (!openSerialPort) {
                        Toast.makeText(MainActivity.this, "串口打开失败,请切换串口,波特率或检查设备", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Toast.makeText(MainActivity.this, "打开成功,开始读取温度,串口" + SerialPort + "波特率" + Baud, Toast.LENGTH_SHORT).show();
                    SPUtils.put(MainActivity.this, "Baud", Baud);//波特率 默认9600
                    SPUtils.put(MainActivity.this, "SerialPort", SerialPort);//波特率 默认9600
                    SPUtils.put(MainActivity.this, "success", true);//是否成功运行过
                }


                if (readTemp) {
                    readTemp = false;
                    button_Start.setText("继续");
                } else {
                    readTemp = true;
                    button_Start.setText("停止读取温度");
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


        if (!(Boolean) SPUtils.get(this, "success", false)) {
            //首次启动
            SPUtils.put(this, "SerialPort", "ttyS3");//串口 position
            SPUtils.put(this, "success", false);//是否成功运行过
            SPUtils.put(this, "language", "zh");//语言
            tvSp.setText("ttyS3");
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

            Log.d(TAG, "onCreate: 本地配置 首次启动 ---串口" + spName.indexOf("ttyS3") + "波特率" + 9600);

        } else {
            //打开应用之前成功启动过
            SerialPort = (String) SPUtils.get(this, "SerialPort", "ttyS3");
            Baud = (int) SPUtils.get(this, "Baud", 9600);

            tvSp.setText(SerialPort);
            editText_portspeed.setText(Baud + "");

            cameraOpen = (Boolean) SPUtils.get(this, "camera", true);
            temperatureShow = (Boolean) SPUtils.get(this, "temperature", true);
            voiceOpen = (Boolean) SPUtils.get(this, "voice", true);
            button_camera.setText(cameraOpen ? "摄像头：开启" : "摄像头：关闭");
            button_temp.setText(temperatureShow ? "温度：显示" : "温度：隐藏");
            button_voice.setText(voiceOpen ? "语音播报：开启" : "语音播报：关闭");

            lang = (String) SPUtils.get(this, "language", "zh");
            if (!"zh".equals(lang)) {
                button_lang.setText("语音语言：English");
                lang = "en";
                SPUtils.put(MainActivity.this, "language", "en");//语言
            }

            windowX = (int) SPUtils.get(this, "windowX", 800);//悬浮窗位置x 默认800
            windowY = (int) SPUtils.get(this, "windowY", 300);//悬浮窗位置x 默认300
            windowWidth = (int) SPUtils.get(this, "windowWidth", 300);//悬浮窗宽 默认300
            windowHeight = (int) SPUtils.get(this, "windowHeight", 400);//悬浮窗高 默认400

            editText_windowWidth.setHint("悬浮窗宽" + windowWidth);
            editText_windowHeight.setHint("高：" + windowHeight);

            Log.d(TAG, "本地配置 " + "\n" + "串口" + SerialPort + "\n"
                    + "波特率" + Baud + "\n"
                    + "摄像头" + cameraOpen + "\n"
                    + "温度" + temperatureShow + "\n"
                    + "语音播报" + voiceOpen + "\n"
                    + "悬浮窗宽：" + windowWidth + "_高:" + windowHeight + "_X：" + windowX + "_Y" + windowY);

            if (button_Start.getText().equals("开始读取温度")) {
//                if (!editText_portspeed.getText().toString().trim().isEmpty()) {
//                    Baud = Integer.valueOf(editText_portspeed.getText().toString().trim());
//                }
                int position = spName.indexOf(tvSp.getText().toString());
                boolean openSerialPort = mSerialPortManager.openSerialPort(devices.get(position).getFile(), Baud);
                Log.d(TAG, "onCreate: SerialPort open :--" + SerialPort + "--succeed ? " + openSerialPort);
                if (!openSerialPort) {
                    Toast.makeText(MainActivity.this, "串口打开失败,请切换串口,波特率或检查设备", Toast.LENGTH_SHORT).show();
                    SPUtils.put(MainActivity.this, "success", false);//是否成功运行过
                    return;
                }
                Toast.makeText(MainActivity.this, "打开成功,开始读取温度,串口" + SerialPort + "波特率" + Baud, Toast.LENGTH_SHORT).show();
                SPUtils.put(MainActivity.this, "Baud", Baud);//波特率 默认9600
                SPUtils.put(MainActivity.this, "SerialPort", SerialPort);//波特率 默认9600
                SPUtils.put(MainActivity.this, "success", true);//是否成功运行过
                if (readTemp) {
                    readTemp = false;
                    button_Start.setText("继续");
                } else {
                    readTemp = true;
                    button_Start.setText("停止读取温度");
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
        }

        try {
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
        } catch (Exception e) {
            Logger.e("悬浮窗", e);
        }


        Message messagehide = new Message();
        messagehide.what = 1;
        handler.sendMessage(messagehide);//隐藏悬浮窗


        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        startActivity(home);


        if (((Boolean) SPUtils.get(this, "isFirst", true)) && !getInstalledPackages()) {
            installApp();
        }

    }

    //--------------------↑↑↑---------onCreate-------↑↑↑---------------------
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            moveTaskToBack(false);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean getInstalledPackages() {
        List<PackageInfo> packages = getApplicationContext().getPackageManager().getInstalledPackages(0);
        for (int i = 0; i < packages.size(); i++) {
            PackageInfo packageInfo = packages.get(i);
            if (packageInfo.packageName.equals("com.iflytek.speechcloud")) {
                return true;
            }
        }
        return false;
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
        FloatWindow.destroy("camera");
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
//                            mKqwSpeechSynthesizer.start(temp + "度" + "体温正常");
                            mTextToSpeech.speak(temp + "度" + "体温正常", TextToSpeech.QUEUE_FLUSH, null);
                        } else {
//                            mKqwSpeechSynthesizer.start(temp + "度" + "体温异常");
                            mTextToSpeech.speak(temp + "度" + "体温异常", TextToSpeech.QUEUE_FLUSH, null);
                        }

                    } else {

                        if (35 < temp && temp < tempNumber) {
//                            mKqwSpeechSynthesizer.start("thirty" + NumberToEnglishUtils.getEnglish(Integer.valueOf(String.valueOf(temp).substring(1, 2))) + "point" + NumberToEnglishUtils.getEnglish(Integer.valueOf(String.valueOf(temp).substring(3))) + " degrees, normal temperature");
                            mTextToSpeech.speak("thirty" + NumberToEnglishUtils.getEnglish(Integer.valueOf(String.valueOf(temp).substring(1, 2))) + "point" + NumberToEnglishUtils.getEnglish(Integer.valueOf(String.valueOf(temp).substring(3))) + " degrees, normal temperature", TextToSpeech.QUEUE_FLUSH, null);
                        } else {
//                            mKqwSpeechSynthesizer.start(temp + "degrees, abnormal temperature");
                            mTextToSpeech.speak("thirty" + NumberToEnglishUtils.getEnglish(Integer.valueOf(String.valueOf(temp).substring(1, 2))) + "point" + NumberToEnglishUtils.getEnglish(Integer.valueOf(String.valueOf(temp).substring(3))) + " degrees, abnormal temperature", TextToSpeech.QUEUE_FLUSH, null);
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
                                tv.setTextColor(getResources().getColor(R.color.white));
                                tv.setTextSize(50);
                                toastView.setGravity(Gravity.CENTER);
//                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
//                                        LinearLayout.LayoutParams.WRAP_CONTENT);
                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                                        LinearLayout.LayoutParams.WRAP_CONTENT);
                                params.setMargins(0, 0, 0, 60);
                                tv.setLayoutParams(params);
//                                tv.setGravity(Gravity.CENTER);
                                toast.setView(toastView);
                                toastView.addView(tv);
                                if (35 < temp && temp < tempNumber) {
                                    toastView.setBackground(getResources().getDrawable(R.drawable.toast_center_green));
                                    tv.setText("正常体温： " + temp + "℃");
                                } else {
                                    toastView.setBackground(getResources().getDrawable(R.drawable.toast_center_red));
                                    tv.setText("异常体温： " + temp + "℃");
                                }

                            }
                        });

                    }


                    if (35 < temp && temp < tempNumber) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvTemp.setText("正常体温：" + temp + "℃");
                                tvTemp.setTextColor(getResources().getColor(R.color.greenyellow));
                            }
                        });
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                tvTemp.setText("异常体温：" + temp + "℃");
                                tvTemp.setTextColor(getResources().getColor(R.color.red));
                            }
                        });
                    }


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

        try {
            Camera.Parameters params = camera.getParameters();
            params.setRecordingHint(true);
            camera.setParameters(params);
            camera.startPreview();
        } catch (Exception e) {
            Logger.e("摄像头", e);
            Toast.makeText(sMainActivity, "请检查摄像头设备", Toast.LENGTH_SHORT).show();
        }

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
            Toast.makeText(MainActivity.this, "启动摄像头失败,请检查设备或开启摄像头权限", Toast.LENGTH_SHORT).show();
//            if (null != timerTask) {
//                timerTask.cancel();
//            }
//            if (null != timer) {
//                timer.cancel();
//            }
//            mSerialPortManager.closeSerialPort();
//            mKqwSpeechSynthesizer.stop();
//            FloatWindow.destroy("camera");
//            finish();
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

    private void getAuthorization() {
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
            Logger.e("授权", e);
        }


//-----------------------------------------------------------------------------------------------
    }

    /**
     * 返回实例化的TTS对象
     *
     * @return
     */
    protected TextToSpeech getNewTextToSpeech() {
        return new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                // TODO Auto-generated method stub
                if (status == TextToSpeech.SUCCESS) {
                    //设置朗读语言
                    int supported = mTextToSpeech.setLanguage(Locale.CHINESE);
//                    if ((supported != TextToSpeech.LANG_AVAILABLE) && (supported != TextToSpeech.LANG_COUNTRY_AVAILABLE)) {
//                        Toast.makeText(MainActivity.this, "不支持当前语言", Toast.LENGTH_SHORT).show();
//                    }
                }
            }
        });
    }


    private void installApp() {
        Log.d(TAG, "installApp: 讯飞引擎安装");


        String type = "application/vnd.android.package-archive";
        try {
            //从assets读取文件流
            InputStream is = getClass().getResourceAsStream(
                    "/assets/tts_service_1.0.apk");
            //将该文件流写入到本应用程序的私有数据区this.getFilesDir().getPath();
            FileOutputStream fos;
            if (Build.VERSION.SDK_INT >= 24) {
                 fos = getApplicationContext().openFileOutput(
                        "mptv.apk", Context.MODE_PRIVATE);
            } else {
                 fos = getApplicationContext().openFileOutput(
                        "mptv.apk", Context.MODE_PRIVATE
                                + Context.MODE_WORLD_READABLE);
            }


            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            fos.flush();
            is.close();
            fos.close();

            File f = new File(getApplication().getFilesDir().getPath()
                    + "/mptv.apk");
            Log.d(TAG, "installApk29: " + f.getAbsolutePath());
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);

            FileProviderUtils.setIntentDataAndType(this, intent, type, f, true);


            SPUtils.put(this, "isFirst", false);
        } catch (Exception e) {
            Log.d(TAG, "installApk29: " + e);
            e.printStackTrace();
        }


    }


    private void registerInstallAppBroadcastReceiver() {

        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);

        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);

        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);

        intentFilter.addDataScheme("package");

        this.registerReceiver(mInstallAppBroadcastReceiver, intentFilter);

    }


}

