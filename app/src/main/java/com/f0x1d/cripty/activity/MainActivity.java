package com.f0x1d.cripty.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.f0x1d.cripty.R;
import com.f0x1d.cripty.fragment.MainFragment;
import com.f0x1d.cripty.receiver.CopyTextReceiver;
import com.f0x1d.cripty.utils.ThemeUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.PrintWriter;
import java.io.StringWriter;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences defPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeUtils.getCurrentTheme());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE))
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 228);
        }
        defPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread t, Throwable e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                String stackTrace = sw.toString();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    String name = "Crashes";
                    int importance = NotificationManager.IMPORTANCE_DEFAULT;
                    NotificationChannel channel = new NotificationChannel(getPackageName() + ".crashed", name, importance);
                    channel.enableVibration(true);
                    channel.enableLights(true);
                    NotificationManager notificationManager = MainActivity.this.getSystemService(NotificationManager.class);
                    notificationManager.createNotificationChannel(channel);
                }

                Notification.Builder builder = new Notification.Builder(getApplicationContext());
                builder.setSmallIcon(R.drawable.ic_bug_report_black_24dp);
                builder.setContentTitle("Cripty crashed!");
                builder.setContentText(stackTrace);
                builder.addAction(new Notification.Action(0, getString(R.string.copy), PendingIntent.getBroadcast(
                        getApplicationContext(), 228, new Intent(MainActivity.this, CopyTextReceiver.class).putExtra("text", stackTrace), 0)));
                builder.setStyle(new Notification.BigTextStyle().bigText(stackTrace));
                builder.setAutoCancel(true);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    builder.setChannelId(getPackageName() + ".notifications");

                NotificationManager notificationManager = (NotificationManager) MainActivity.this.getSystemService(NOTIFICATION_SERVICE);
                notificationManager.notify(Integer.MIN_VALUE, builder.build());

                e.printStackTrace();

                defaultHandler.uncaughtException(t, e);
            }
        });

        if (!getDefaultPreferences().getBoolean("showed_faq", false)) {
            WebView webView = new WebView(this);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.loadUrl("file:///android_asset/faq.html");

            new MaterialAlertDialogBuilder(this)
                    .setCancelable(false)
                    .setTitle("FAQ")
                    .setView(webView)
                    .setPositiveButton(R.string.ok, ((dialog, which) -> getDefaultPreferences().edit().putBoolean("showed_faq", true).apply()))
                    .show();
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, MainFragment.newInstance())
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() != 0)
            getSupportFragmentManager().popBackStack();
        else {
            moveTaskToBack(true);
            super.onBackPressed();
        }
    }

    public SharedPreferences getDefaultPreferences() {
        return defPrefs;
    }

    public boolean hasPermissions(String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }
}
