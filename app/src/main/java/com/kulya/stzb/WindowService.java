package com.kulya.stzb;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;

import java.util.Calendar;

import androidx.core.app.NotificationCompat;

public class WindowService extends Service {
    private static final String TAG = "MainService";
    private Thread timeThread;
    private NumberPicker picker;
    private NumberPicker picker2;
    private EditText a;
    private Button b;
    private Button c;
    private Button countdown;
    Button imageButton1;
    LinearLayout toucherLayout;
    LinearLayout CountdownLayout;
    WindowManager.LayoutParams params;
    WindowManager windowManager;
    int statusBarHeight = 0;
    private Boolean isShowing = true;
    private boolean isTaskA = false;
    private boolean isTaskB = false;
    private boolean isTaskC = false;
    private boolean isFirst = true;
    private int Countdown;
    int minute;
    int second;
    int hour;

    class TimeThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    Message msg = new Message();
                    if (isTaskA) msg.what = 2;
                    else if (isTaskB) msg.what = 3;
                    else if (isTaskC) msg.what = 4;
                    else msg.what = 1;
                    mHandler.sendMessage(msg);
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    long sysTime = System.currentTimeMillis();
                    CharSequence sysTimeStr = DateFormat.format("hh:mm:ss", sysTime);

                    Calendar calendar = Calendar.getInstance();
                     minute = calendar.get(Calendar.MINUTE);
                     second = calendar.get(Calendar.SECOND);
                     hour = calendar.get(Calendar.HOUR_OF_DAY);
                    try {
                        imageButton1.setText(String.format("%02d", hour) + ":"
                                +String.format("%02d", minute) + ":" + String.format("%02d", second)); //更新时
                    } catch (Exception e) {
                    }
                    break;
                case 2:
                    Calendar calendar2 = Calendar.getInstance();
                     minute = calendar2.get(Calendar.MINUTE);
                     second = calendar2.get(Calendar.SECOND);
                    int time = minute * 60 + second;
                    if (isFirst) {
                        Countdown = picker.getValue() * 60 + picker2.getValue();
                        if (Countdown >= time)
                            Countdown = Countdown - time;
                        else
                            Countdown = Countdown - time + 3600;
                        isFirst = false;
                    }
                    if (Countdown == 0) {
                        isFirst = true;
                        isTaskA = false;
                        break;
                    }
                    minute=Countdown/60;
                    second=Countdown%60;
                    Countdown--;
                    try {
                        imageButton1.setText(String.format("%02d", minute) + ":" + String.format("%02d", second)); //更新时
                    } catch (Exception e) {
                    }

                    break;
                case 3:
                    break;
                case 4:
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        startService();
        createToucher();
        timeThread = new TimeThread();
        timeThread.start();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void createToucher() {

        //赋值WindowManager&LayoutParam.
        params = new WindowManager.LayoutParams();
        windowManager = (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
        //设置type.系统提示型窗口，一般都在应用程序窗口之上.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        }
        //设置效果为背景透明.
        params.format = PixelFormat.RGBA_8888;
        //设置flags.不可聚焦及不可使用按钮对悬浮窗进行操控.
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        //  params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
        //获取dpi
        DisplayMetrics metric = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metric);
        final int width = metric.widthPixels;     // 屏幕宽度（像素）
        final int height = metric.heightPixels;   // 屏幕高度（像素）
        int densityDpi = metric.densityDpi;
        //设置窗口初始停靠位置.
        params.gravity = Gravity.LEFT | Gravity.TOP;
        //px与dp的换算为px = dp * (dpi / 160).
        //注意运算规则！！！！！！！
        final int window_x = WindowManager.LayoutParams.WRAP_CONTENT;
        final int window_y = WindowManager.LayoutParams.WRAP_CONTENT;
        params.width = window_x;
        params.height = window_y;
        //获取浮动窗口视图所在布局.
        toucherLayout = (LinearLayout) LayoutInflater.from(getApplication()).inflate(R.layout.floatwindow, null);
        //添加toucherlayout
        //  CountdownLayout = (LinearLayout) LayoutInflater.from(getApplication()).inflate(R.layout.floatmenu, null);
        windowManager.addView(toucherLayout, params);
        //主动计算出当前View的宽高信息.
        toucherLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        //用于检测状态栏高度.
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        } else
            statusBarHeight = 0;
        final int point[] = {-1, -1};
        //浮动窗口按钮.
        imageButton1 = toucherLayout.findViewById(R.id.imageButton1);
        b = toucherLayout.findViewById(R.id.b);
        c = toucherLayout.findViewById(R.id.c);
        picker = toucherLayout.findViewById(R.id.numberpicker);
        picker2 = toucherLayout.findViewById(R.id.numberpicker2);
        countdown = toucherLayout.findViewById(R.id.countdown);
        CountdownLayout = toucherLayout.findViewById(R.id.hy);
        picker.setMaxValue(59);
        picker.setMinValue(0);
        picker.setValue(0);
        picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);//不可手动修改

        picker2.setMaxValue(59);
        picker2.setMinValue(0);
        picker2.setValue(0);
        picker2.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        imageButton1.setOnClickListener(new itemOnclick());
        countdown.setOnClickListener(new itemOnclick());
        // a.setOnEditorActionListener(new itemOnclick());
        b.setOnClickListener(new itemOnclick());
        c.setOnClickListener(new itemOnclick());
        imageButton1.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (params.x < 0) params.x = 0;
                else if (params.x > width - window_x) params.x = width - window_x;
                if (params.y < 0) params.y = 0;
                else if (params.y > height - window_y - statusBarHeight)
                    params.y = height - window_y - statusBarHeight;
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        point[0] = (int) event.getRawX() - params.x;
                        point[1] = (int) event.getRawY() - params.y - statusBarHeight;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        params.x = (int) event.getRawX() - point[0];
                        params.y = (int) event.getRawY() - point[1] - statusBarHeight;
                        windowManager.updateViewLayout(toucherLayout, params);
                        break;
                    default:
                        break;
                }
                return false;
            }
        });

    }

    private void startService() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel("default", "name", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(notificationChannel);
        }
        Notification notification = new NotificationCompat.Builder(this, "default")
                .setContentTitle("前台服务")//设置标题
                .setContentText("率土助手正在运行")//设置内容
                .setWhen(System.currentTimeMillis())//设置显示通知被创建的时间
                .setSmallIcon(R.mipmap.ic_launcher)//设置通知的小图标
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))//设置通知的大图标
                .setAutoCancel(true)//通知自动取消
                .build();
        //显示通知，每条通知的id都要不同
        startForeground(1, notification);
    }

    class itemOnclick implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.imageButton1:
                    if (!isShowing) {
                        CountdownLayout.setVisibility(View.VISIBLE);
                        // b.setVisibility(View.VISIBLE);
                        // c.setVisibility(View.VISIBLE);
                    } else {
                        CountdownLayout.setVisibility(View.GONE);
                        b.setVisibility(View.GONE);
                        c.setVisibility(View.GONE);
                    }
                    isShowing = !isShowing;
                    break;
                case R.id.b:
                    break;
                case R.id.c:
                    break;
                case R.id.countdown:
                    isTaskA = true;
                    isFirst = true;
                    // Toast.makeText(WindowService.this,"64544",Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }

    }

    @Override
    public void onDestroy() {
        //用imageButton检查悬浮窗还在不在，这里可以不要。优化悬浮窗时要用到。
        if (imageButton1 != null) {
            windowManager.removeView(toucherLayout);
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
        // throw new UnsupportedOperationException("Not yet implemented");
    }

}
