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
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import java.security.PrivilegedAction;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;

public class WindowService extends Service {
    private static final String TAG = "MainService";
    //要引用的布局文件.
    ConstraintLayout toucherLayout;
    //布局参数.
    WindowManager.LayoutParams params;
    //实例化的WindowManager.
    WindowManager windowManager;
    Button imageButton1;
    //状态栏高度.（接下来会用到）
    int statusBarHeight = -1;

    class TimeThread extends Thread {
        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(1000);
                    Message msg = new Message();
                    msg.what = 1;  //消息(一个整型值)
                    mHandler.sendMessage(msg);// 每隔1秒发送一个msg给mHandler
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
                    imageButton1.setText(sysTimeStr); //更新时
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
        new TimeThread().start();
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
        //获取dpi
        DisplayMetrics metric = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metric);
        int width = metric.widthPixels;     // 屏幕宽度（像素）
        int height = metric.heightPixels;   // 屏幕高度（像素）
        float density = metric.density;      // 屏幕密度（0.75 / 1.0 / 1.5）
        int densityDpi = metric.densityDpi;
        //设置窗口初始停靠位置.
        params.gravity = Gravity.LEFT | Gravity.TOP;
        //设置悬浮窗口长宽数据.
        //注意，这里的width和height均使用px而非dp.这里我偷了个懒
        //如果你想完全对应布局设置，需要先获取到机器的dpi
        //px与dp的换算为px = dp * (dpi / 160).
        final int window_x = 90 * (densityDpi / 160);
        final int window_y = 30 * (densityDpi / 160);
        params.width = window_x;
        params.height = window_y;
        //获取浮动窗口视图所在布局.
        toucherLayout = (ConstraintLayout) LayoutInflater.from(getApplication()).inflate(R.layout.window, null);
//        //添加toucherlayout
        try {
            windowManager.addView(toucherLayout, params);
        } catch (Exception e) {

        }
        //主动计算出当前View的宽高信息.
        toucherLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        //用于检测状态栏高度.
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(resourceId);
        }
         final int point[] = new int[2];
        //浮动窗口按钮.
        imageButton1 = toucherLayout.findViewById(R.id.imageButton1);
        imageButton1.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        point[0] = (int) event.getRawX()-params.x-window_x / 2;
                        point[1] = (int) event.getRawY()-params.y-statusBarHeight-window_y / 2;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        params.x = (int) event.getRawX() - window_x / 2 - point[0];
                        //这就是状态栏偏移量用的地方
                        params.y = (int) event.getRawY() - window_y / 2 - point[1] - statusBarHeight;
                        windowManager.updateViewLayout(toucherLayout, params);
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
