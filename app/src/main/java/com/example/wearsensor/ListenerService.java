package com.example.wearsensor;

import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.nio.charset.Charset;

public class ListenerService extends WearableListenerService {

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        final byte[] data = messageEvent.getData();
        if (data != null){
            final String info = new String(data, Charset.forName("UTF-8"));
            final Intent infoIntent = new Intent("custom-event-name");
            infoIntent.putExtra("info",info);
            LocalBroadcastManager.getInstance(this).sendBroadcast(infoIntent);
        }
    }
}
