/*
 * Copyright (C) 2013 PA Updater (Simon Matzeder and Parthipan Ramesh)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.paranoid.updater;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.format.Time;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class AlarmReceiver extends BroadcastReceiver {

    private final String SOMEACTION = "com.paranoid.updater.ACTION";
    private Context Context;
    private int localVer;

    @Override
    public void onReceive(Context context, Intent intent) {
        Context = context.getApplicationContext();
        Time now = new Time();
        now.setToNow();

        String action = intent.getAction();
        if (SOMEACTION.equals(action)) {
            Log.i("AlarmReceiver", "Reiceived Alarm");

            GetGooVersion GooTask = new GetGooVersion();
            GooTask.execute();
        }
    }

    class GetGooVersion extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... noargs) {
            return Functions.CheckGoo(Context);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result.equals("err")) {
                Log.e("AlarmReceiver", "Unable to check for updates! No connection or goo down!");
            } else {
                int gooVer = Integer.valueOf(result);
                Log.i("AlarmReceiver", "gooVer: " + gooVer);
                try { //Get local ro.goo.version
                    Process proc;
                    proc = Runtime.getRuntime().exec(new String[]{"/system/bin/getprop", "ro.goo.version"});
                    BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));

                    String resultstring = reader.readLine();

                    if (resultstring.equals("") || resultstring.equals(null)) {
                        Log.i("Local Parser", "Not running on PA!!!");
                        localVer = gooVer + 1;
                    } else {
                        localVer = Integer.valueOf(resultstring);
                    }
                    Log.i("AlarmReceiver", "localVer: " + localVer);
                } catch (IOException e) {
                    Log.e("Local Parser", "Error parsing local version!");
                    Log.e("Local Parser", e.toString());
                }
                if (gooVer > localVer) {
                    Log.i("AlarmReceiver", "Newer Version available! Show Notification!");

                    Notification.Builder nBuilder;
                    nBuilder = new Notification.Builder(Context);

                    Intent resultIntent = new Intent(Context, MainActivity.class);
                    resultIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(Context);

                    stackBuilder.addParentStack(MainActivity.class);

                    stackBuilder.addNextIntent(resultIntent);
                    PendingIntent resultPendingIntent =
                            stackBuilder.getPendingIntent(
                                    0,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            );
                    nBuilder.setContentIntent(resultPendingIntent);
                    nBuilder.setSmallIcon(R.drawable.ic_launcher);
                    nBuilder.setContentTitle("PA Updater");
                    nBuilder.setContentText("New Update available!");
                    nBuilder.setAutoCancel(true);
                    nBuilder.setOngoing(false);
                    NotificationManager mNotificationManager =
                            (NotificationManager) Context.getSystemService(android.content.Context.NOTIFICATION_SERVICE);
                    mNotificationManager.notify(1, nBuilder.build());
                }
            }
        }
    }

}
