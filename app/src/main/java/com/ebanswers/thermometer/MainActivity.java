package com.ebanswers.thermometer;

import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechUtility;
import com.kongqw.serialportlibrary.Device;
import com.kongqw.serialportlibrary.SerialPortFinder;
import com.kongqw.serialportlibrary.SerialPortManager;
import com.kongqw.serialportlibrary.listener.OnOpenSerialPortListener;
import com.kongqw.serialportlibrary.listener.OnSerialPortDataListener;
import com.yhao.floatwindow.FloatWindow;
import com.yhao.floatwindow.MoveType;
import com.yhao.floatwindow.PermissionListener;
import com.yhao.floatwindow.Screen;
import com.yhao.floatwindow.ViewStateListener;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import utils.DataConversion;
import utils.HexUtils;
import utils.KqwSpeechSynthesizer;

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

    TextView tvTemp;//温度
    KqwSpeechSynthesizer mKqwSpeechSynthesizer;
    ConstraintLayout constraintLayout;

    MyRvAdapter myRvAdapter;

    TextView tvSp;
    int portPosition = 0;
    Button button_exit;
    public static MainActivity sMainActivity;
    ArrayList<Device> devices;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case 0://显示
                    FloatWindow.get("camera").show();

                    break;
                case 1://隐藏
                    FloatWindow.get("camera").hide();

                    break;
                case 2://修改位置
                    FloatWindow.get("camera").updateX(500);
                    break;
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mKqwSpeechSynthesizer.stop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SerialPortFinder serialPortFinder = new SerialPortFinder();
        devices = serialPortFinder.getDevices();

        imageView = new ImageView(getApplicationContext());
        imageView.setImageResource(R.drawable.ic_launcher_background);

        tvSp = findViewById(R.id.textView_sp);
        tvSp.setText("串口：默认"+devices.get(0).getName());
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
        recyclerView.setLayoutManager(new GridLayoutManager(this,3));

        SpeechUtility.createUtility(this, SpeechConstant.APPID +"=5ec5e6f6");

        mKqwSpeechSynthesizer = new KqwSpeechSynthesizer(this);

        surfaceView = new SurfaceView(getApplicationContext());
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        constraintLayout = (ConstraintLayout) LayoutInflater.from(this).inflate(R.layout.window_float, null);
//        view.
        constraintLayout.addView(surfaceView);

        tvTemp = new TextView(getApplicationContext());
        tvTemp.setText("");
        tvTemp.setTextColor(getResources().getColor(R.color.colortemp));
        tvTemp.setTextSize(50);

//        tvTemp.setLayoutParams();

        constraintLayout.addView(tvTemp);
        
        FloatWindow
                .with(getApplicationContext())
                .setView(constraintLayout)
                .setWidth(300)                               //设置控件宽高
                .setHeight(Screen.width, 0.2f)
                .setX(300)                                   //设置控件初始位置
                .setY(Screen.height, 0.3f)
                .setDesktopShow(true)                        //桌面显示
                .setViewStateListener(mViewStateListener)    //监听悬浮控件状态改变
                .setPermissionListener(mPermissionListener)  //监听权限申请结果
                .setMoveType(MoveType.active)
                .setTag("camera")
                .setMoveStyle(500, new AccelerateInterpolator())  //贴边动画时长为500ms，加速插值器
                .build();
        

        button = findViewById(R.id.button_readtemp);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String sp=tvSp.getText().toString();

//        打开串口
//        参数1：串口
//        参数2：波特率
//        返回：串口打开是否成功
                if (button.getText().equals("开始读取温度")) {
                    boolean openSerialPort = mSerialPortManager.openSerialPort(devices.get(portPosition).getFile(), 115200);
                    Log.d(TAG, "onCreate: SerialPort open :--" + devices.get(4) + "--succeed ? " + openSerialPort);
                    if (!openSerialPort) {
                        Toast.makeText(MainActivity.this, "串口打开失败,请切换串口或检查设备", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Toast.makeText(MainActivity.this, "打开成功,开始读取温度", Toast.LENGTH_SHORT).show();

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
        Message messagehide = new Message();
        messagehide.what = 1;
        handler.sendMessage(messagehide);

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

    private void analysisTemp(final double temp) {
        if (35 < temp && temp < 40) {
            if (atCDboolean) {
                //语言播报/打开摄像头/显示温度
                atCDboolean = false;
                showCamera();
//                int i=mSpeech.speak(temp+"度"+"体温正常",TextToSpeech.QUEUE_ADD, null);
                if (35 < temp && temp < 37.3) {
                    mKqwSpeechSynthesizer.start(temp + "度" + "体温正常");
                } else {
                    mKqwSpeechSynthesizer.start(temp + "度" + "体温异常");
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvTemp.setText(temp+"℃");
                    }
                });
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

}

