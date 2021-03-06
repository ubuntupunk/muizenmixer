/*
 * This file is part of MuizeDroid's MuizenMixer
 *
 * Based upon Amproid by Peter Papp
 *
 * Please visit https://github.com/ubuntupunk/ for details
 *
 * Muizedroid is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Muizedroid is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Muizedroid. If not, see http://www.gnu.org/licenses/
 */

package com.ppphun.muizedroid.mixer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.BitmapFactory;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;


public class PresenceForever extends Service
{

    private final static int    NOTIFICATION_ID         = 1100;
    private final static String NOTIFICATION_CHANNEL_ID = "MixerPresenceForeverNotificationChannel";

    private final AutoBroadcastReceiver     autoBroadcastReceiver = new AutoBroadcastReceiver();
    private       NotificationManagerCompat notificationManager   = null;


    @Override
    public void onCreate()
    {
        super.onCreate();

        registerReceiver(autoBroadcastReceiver, new IntentFilter("android.app.action.ENTER_CAR_MODE"));
        notificationManager = NotificationManagerCompat.from(this);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.setLockscreenVisibility(NotificationCompat.VISIBILITY_SECRET);
            notificationChannel.setSound(null, null);
            notificationManager.createNotificationChannel(notificationChannel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID).setContentIntent(PendingIntent.getActivity(this, 0, new Intent(this, MixerMainActivity.class), PendingIntent.FLAG_CANCEL_CURRENT)).setContentText(getString(R.string.persistence_notification)).setContentTitle(getString(R.string.app_name)).setSmallIcon(R.drawable.ic_launcher).setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher)).setOngoing(true).setPriority(Notification.PRIORITY_HIGH).setSound(null).setVisibility(NotificationCompat.VISIBILITY_SECRET);

        startForeground(NOTIFICATION_ID, notificationBuilder.build());
    }


    @Override
    public void onDestroy()
    {
        unregisterReceiver(autoBroadcastReceiver);

        if (notificationManager != null) {
            notificationManager.cancelAll();
        }

        super.onDestroy();
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        // let no one bind to this service
        return null;
    }


    private final class AutoBroadcastReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            Intent intentMixer = new Intent(PresenceForever.this, MixerService.class);
            intentMixer.putExtra("PresenceForever", true);
            startService(intentMixer);
        }
    }
}
