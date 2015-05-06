package com.cherrydev.chirpchain;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import com.cherrydev.chirpchain.message.ChirpMessage;
import com.cherrydev.chirpchain.message.ChirpMessageIntent;
import com.cherrydev.chirpchain.message.TestMessageGenerator;
import com.cherrydev.chirpchain.network.SocketClient;
import com.cherrydev.chirpchain.network.SocketServer;
import com.cherrydev.chirpchain.util.FakeMessageQueue;
import com.fizzbuzz.android.dagger.InjectingService;
import com.j256.ormlite.android.AndroidConnectionSource;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import org.parceler.Parcels;

import java.sql.SQLException;
import java.util.List;

import javax.inject.Inject;

public class ChirpMessageService extends InjectingService {

    private final IBinder binder = new ChirpMessageServiceBinder();
    private Handler handler;
    private boolean continueGenerating;
    private int testMessageGenerateInterval = 5000;
    private ConnectionSource ormConnectionSource;
    private Dao<ChirpMessage, Integer> messageDao;

    private SocketServer socketServer;
    private SocketClient socketClient;
    private FakeMessageQueue messageQueue;

    @Inject
    TestMessageGenerator messageGenerator;

    public ChirpMessageService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent.getAction().equals(Intents.INTENT_ACTION_SEND_MESSAGE)) {
            ChirpMessage m = Parcels.unwrap(intent.getParcelableExtra(Intents.INTENT_EXTRA_MESSAGE));
            sendMessage(m);
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        openDatabase();
        socketServer = new SocketServer(this);
        socketClient = new SocketClient(this);
        messageQueue = new FakeMessageQueue();
        messageQueue.setProgressCallback(this::sendUpdateNotification);
        messageQueue.setCompleteCallback(this::onMessageComplete);
        resetNetwork();
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ChirpMessage message = Parcels.unwrap(intent.getParcelableExtra(Intents.INTENT_EXTRA_MESSAGE));
                sendMessage(message);
            }
        }, new IntentFilter(Intents.INTENT_SMS_MESSAGE));
    }

    public void stopGenerating() {
        continueGenerating = false;
        handler.removeCallbacks(this::generateTestMessage);
    }

    public void startGenerating() {
        continueGenerating = true;
        handler.post(this::generateTestMessage);
    }

    public void resetNetwork() {
        socketClient.stop();
        socketServer.stop();
        SharedPreferences prefs = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        if (prefs.getBoolean("isServer", false)) {
            handler.post(() -> socketServer.start());
        }
        else {
            String ip = prefs.getString("serverAddress", null);
            if (ip != null && ip.length() > 0) {
                handler.post(() -> socketClient.clientConnect(ip));
            }
        }
    }

    public void clearMessages() {
        try {
            messageDao.delete(messageDao.queryForAll());
            sendReloadMessageNotification();
        }
        catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public List<ChirpMessage> getAllMessages() {
        try {
            return messageDao.queryForAll();
        }
        catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }

    public ChirpMessage startNewMessage(String messageText) {

        ChirpMessage message = messageGenerator.createEmptyMessage();
        message.setText(messageText);
        return message;
    }

    public void sendMessage(ChirpMessage message) {
        messageQueue.enqueueMessageForSend(message);
    }

    public void onMessageComplete(ChirpMessage message, FakeMessageQueue.Mode mode) {
        // FIXME: Get rid of 'send' once we're actually sending between devices
        if (mode == FakeMessageQueue.Mode.receive || mode == FakeMessageQueue.Mode.send ) {
            // save it
            try {
                messageDao.create(message);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            sendMessageNotification(message);
        }
    }

    private void openDatabase() {
        //this.deleteDatabase("ChirpChain");
        SQLiteOpenHelper openHelper = new SQLiteOpenHelper(this, "ChirpChain", null, 1) {
            @Override
            public void onCreate(SQLiteDatabase db) {

            }

            @Override
            public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

            }

        };
        ormConnectionSource = new AndroidConnectionSource(openHelper);
        try {
            TableUtils.createTableIfNotExists(ormConnectionSource, ChirpMessage.class);
            messageDao = DaoManager.createDao(ormConnectionSource, ChirpMessage.class);
        }
        catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
    }


    private void generateTestMessage() {
        if (continueGenerating) {
            ChirpMessage m = messageGenerator.generateMessage();
            try {
                messageDao.create(m);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            sendMessageNotification(m);
            handler.postDelayed(this::generateTestMessage, testMessageGenerateInterval);
        }
    }

    private void sendUpdateNotification(ChirpMessage message, FakeMessageQueue.Mode mode, float progress) {
        Intent i = new Intent(Intents.INTENT_PROGRESS_UPDATE);
        i.putExtra("mode", mode.name());
        i.putExtra("progress", progress);
        sendBroadcast(i);
    }

    private void sendMessageNotification(ChirpMessage message) {
        Intent i = new ChirpMessageIntent(message);
        sendBroadcast(i);
    }

    private void sendReloadMessageNotification() {
        Intent i = new Intent(Intents.INTENT_RELOAD_MESSAGES);
        sendBroadcast(i);
    }

    public class ChirpMessageServiceBinder extends Binder {
        public ChirpMessageService getService() {
            return ChirpMessageService.this;
        }
    }
}
