package com.cherrydev.chirpchain.message;

import android.content.Intent;

import com.cherrydev.chirpchain.Intents;

import java.util.Date;

/**
 * Created by alannon on 2015-02-21.
 */
public class ChirpMessageIntent extends Intent {

    private static final String EXTRA_MESSAGE = "message";
    private static final String EXTRA_SOURCE = "source";
    private static final String EXTRA_DEST = "dest";
    private static final String EXTRA_TO = "to";
    private static final String EXTRA_DATE = "date";
    private static final String EXTRA_ID = "id";

    private ChirpMessage message;

    public ChirpMessageIntent(ChirpMessage message) {
        super(Intents.INTENT_NEW_MESSAGE);
        writeMessage(message, this);
        this.message = message;
    }

    public ChirpMessageIntent(Intent copyIntent) {
        super(copyIntent);
        this.message = readMessage(this);
    }

    public ChirpMessage getMessage() {
        return message;
    }

    public void setMessage(ChirpMessage message) {
        this.message = message;
        writeMessage(message, this);
    }

    public static void writeMessage(ChirpMessage message, Intent intent) {
        intent.putExtra(EXTRA_MESSAGE, message.getText());
        intent.putExtra(EXTRA_TO, message.getTo());
        intent.putExtra(EXTRA_SOURCE, message.getSourceStationId());
        intent.putExtra(EXTRA_DEST, message.getDestStationId());
        intent.putExtra(EXTRA_DATE, message.getDate().getTime());
        intent.putExtra(EXTRA_ID, message.getMessageId());
    }

    public static ChirpMessage readMessage(Intent intent) {
        return new ChirpMessage(intent.getStringExtra(EXTRA_MESSAGE),
                intent.getStringExtra(EXTRA_TO),
                new Date(intent.getLongExtra(EXTRA_DATE, 0)),
                intent.getIntExtra(EXTRA_ID, 0),
                intent.getIntExtra(EXTRA_SOURCE, 0),
                intent.getIntExtra(EXTRA_DEST, 0));
    }
}
