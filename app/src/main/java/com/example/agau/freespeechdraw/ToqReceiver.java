package com.example.agau.freespeechdraw;

/**
 * Created by agau on 10/7/14.
 */
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ToqReceiver extends BroadcastReceiver{
    @Override
    public void onReceive(Context context, Intent intent) {
        // Launch the demo app activity to complete the install of the deck of cards applet
        Intent launchIntent= new Intent(context, DrawActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(launchIntent);
    }
}
