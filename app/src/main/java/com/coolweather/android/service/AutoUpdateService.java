package com.coolweather.android.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.coolweather.android.gson.Weather;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {
    public AutoUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWeather();
        updateBingPic();
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        int eightHour = 8 * 60 * 60 * 1000;
        long triggerAtTime = SystemClock.elapsedRealtime() + eightHour;
        Intent i = new Intent(this, AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        alarmManager.cancel(pi);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
        return super.onStartCommand(intent, flags, startId);
    }

    private void updateBingPic() {
        String bingPicURL = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(bingPicURL, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String bing_pic = response.body().string();
                SharedPreferences.Editor spe = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                spe.putString("bing_pic", bing_pic);
                spe.apply();
            }
        });
    }

    private void updateWeather() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = sp.getString("weather", null);
        if (weatherString != null) {
            Weather weather = Utility.handleWeatherResponse(weatherString);
            String weatherId = weather.basic.weatherId;
            String weatherURL = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=45d9c5c6bf64497199ca1aebb5754c4a";
            HttpUtil.sendOkHttpRequest(weatherURL, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String weatherResponse = response.body().string();
                    Weather weather = Utility.handleWeatherResponse(weatherResponse);
                    if (weather != null && weather.status.equals("ok")) {
                        SharedPreferences.Editor spe = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        spe.putString("weather", weatherResponse);
                        spe.apply();
                    }
                }
            });
        }
    }
}
