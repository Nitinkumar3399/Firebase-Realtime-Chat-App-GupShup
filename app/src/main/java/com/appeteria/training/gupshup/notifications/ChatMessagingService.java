package com.appeteria.training.gupshup.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.appeteria.training.gupshup.Common.Constants;
import com.appeteria.training.gupshup.Common.Util;
import com.appeteria.training.gupshup.R;
import com.appeteria.training.gupshup.login.LoginActivity;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class ChatMessagingService extends FirebaseMessagingService {

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s); // // sending token to server here

        Util.updateDeviceToken(this, s);

    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = remoteMessage.getData().get(Constants.NOTIFICATION_TITLE);
        String message = remoteMessage.getData().get(Constants.NOTIFICATION_MESSAGE);

        Intent intentChat = new Intent(this, LoginActivity.class);
        PendingIntent pendingIntent  = PendingIntent.getActivity(this, 0, intentChat, PendingIntent.FLAG_ONE_SHOT);

        final NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        final NotificationCompat.Builder notificationBuilder;

        if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.O)
        {
            NotificationChannel channel = new NotificationChannel(Constants.CHANNEL_ID,
                    Constants.CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);

            channel.setDescription(Constants.CHANNEL_DESC);
            notificationManager.createNotificationChannel(channel);
            notificationBuilder = new NotificationCompat.Builder(this, Constants.CHANNEL_ID);
        }
        else
        notificationBuilder = new NotificationCompat.Builder(this);

            notificationBuilder.setSmallIcon(R.drawable.ic_chat);
        notificationBuilder.setColor(getResources().getColor(R.color.colorPrimary));
        notificationBuilder.setContentTitle(title);
        notificationBuilder.setAutoCancel(true);
        notificationBuilder.setSound(defaultSoundUri);
        notificationBuilder.setContentIntent(pendingIntent);

        if(message.startsWith("https://firebasestorage."))
        {
            try{
             final NotificationCompat.BigPictureStyle bigPictureStyle = new NotificationCompat.BigPictureStyle();

                Glide.with(this)
                        .asBitmap()
                        .load(message)
                        .into(new CustomTarget<Bitmap>(200, 100) {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                bigPictureStyle.bigPicture(resource);
                                notificationBuilder.setStyle(bigPictureStyle);
                                notificationManager.notify(999, notificationBuilder.build());
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {

                            }
                        });


            }
            catch (Exception ex)
            {
                notificationBuilder.setContentText("New File Received");
            }
        }
        else {
            notificationBuilder.setContentText(message);
            notificationManager.notify(999, notificationBuilder.build());
        }



    }
}







