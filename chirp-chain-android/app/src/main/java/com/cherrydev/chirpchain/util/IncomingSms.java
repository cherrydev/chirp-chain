package com.cherrydev.chirpchain.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import com.cherrydev.chirpchain.Intents;
import com.cherrydev.chirpchain.message.ChirpMessage;

import org.parceler.Parcels;

import java.util.Date;

/**
 * Created by alannon on 2015-02-28.
 */
public class IncomingSms extends BroadcastReceiver {

    final SmsManager sms = SmsManager.getDefault();
    @Override
    public void onReceive(Context context, Intent intent) {

        // Retrieves a map of extended data from the intent.
        final Bundle bundle = intent.getExtras();

        try {

            if (bundle != null) {

                final Object[] pdusObj = (Object[]) bundle.get("pdus");

                for (int i = 0; i < pdusObj.length; i++) {

                    SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
                    String phoneNumber = currentMessage.getDisplayOriginatingAddress();

                    String senderNum = phoneNumber;
                    String message = currentMessage.getDisplayMessageBody();

                    Log.i("SmsReceiver", "senderNum: " + senderNum + "; message: " + message);

                    ChirpMessage chirpMessage = new ChirpMessage();
                    chirpMessage.setTo("From:" + senderNum);
                    chirpMessage.setDate(new Date());
                    chirpMessage.setText(message);
                    Intent messageReceivedIntent = new Intent(Intents.INTENT_SMS_MESSAGE);
                    messageReceivedIntent.putExtra(Intents.INTENT_EXTRA_MESSAGE, Parcels.wrap(chirpMessage));
                    context.sendBroadcast(messageReceivedIntent);

                } // end for loop
            } // bundle is null

        } catch (Exception e) {
            Log.e("SmsReceiver", "Exception smsReceiver" +e);

        }
    }
}
