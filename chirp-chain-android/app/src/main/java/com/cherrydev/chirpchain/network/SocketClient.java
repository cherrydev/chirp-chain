package com.cherrydev.chirpchain.network;

import android.content.Context;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.widget.Toast;

import com.cherrydev.chirpchain.message.ChirpMessage;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;

import org.parceler.Parcels;

import java.util.Date;

/**
 * Created by alannon on 2015-02-28.
 */
public class SocketClient {

    private WebSocket socket;
    private Context context;
    private Handler handler;

    public SocketClient(Context context) {

        this.context = context;
        handler = new Handler();
    }

    public void clientConnect(String server) {
        AsyncHttpClient.getDefaultInstance().websocket("http://" + server + ":5000/live", "chirpchat", new AsyncHttpClient.WebSocketConnectCallback() {
            @Override
            public void onCompleted(Exception ex, WebSocket webSocket) {
                if (ex != null) {
                    ex.printStackTrace();
                    return;
                }
                SocketClient.this.socket = webSocket;
                handler.post(() -> Toast.makeText(context, "Client connected!", Toast.LENGTH_SHORT).show());
                ChirpMessage m = new ChirpMessage("Hello", "To you", new Date(), 0, 0, 0);
                Parcel p = Parcel.obtain();
                Parcels.wrap(m).writeToParcel(p, 0);
                byte[] bytes = p.marshall();
                p.recycle();
                webSocket.send(bytes);
                webSocket.setStringCallback(new WebSocket.StringCallback() {
                    public void onStringAvailable(String s) {
                        System.out.println("I got a string: " + s);
                    }
                });
                webSocket.setDataCallback(new DataCallback() {
                    public void onDataAvailable(DataEmitter emitter, ByteBufferList byteBufferList) {
                        System.out.println("I got some bytes!");
                        // note that this data has been read
                        byteBufferList.recycle();
                    }
                });
            }


        });
    }

    public void stop() {
        if (socket != null) {
            socket.close();
        }
    }
}
