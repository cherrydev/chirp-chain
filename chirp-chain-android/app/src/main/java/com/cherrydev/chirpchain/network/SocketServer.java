package com.cherrydev.chirpchain.network;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import com.cherrydev.chirpchain.message.ChirpMessage;
import com.cherrydev.chirpchain.message.ChirpMessage$$Parcelable;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.callback.DataCallback;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.WebSocket;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import org.parceler.Parcels;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alannon on 2015-02-28.
 */
public class SocketServer {
    AsyncHttpServer server = new AsyncHttpServer();

    List<WebSocket> _sockets = new ArrayList<>();
    private Context context;

    public SocketServer(Context context) {

        this.context = context;
    }

    public void start() {

        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        Toast.makeText(context, "IP address:" + ip, Toast.LENGTH_LONG).show();
        server.websocket("/live", "chirpchat", new AsyncHttpServer.WebSocketRequestCallback() {
            @Override
            public void onConnected(final WebSocket webSocket, AsyncHttpServerRequest request) {
                _sockets.add(webSocket);
                Toast.makeText(context, "Connected to client", Toast.LENGTH_SHORT).show();
                //Use this to clean up any references to your websocket
                webSocket.setClosedCallback(new CompletedCallback() {
                    @Override
                    public void onCompleted(Exception ex) {
                        try {
                            if (ex != null)
                                Log.e("WebSocket", "Error");
                        } finally {
                            _sockets.remove(webSocket);
                        }
                    }
                });

                webSocket.setStringCallback(new WebSocket.StringCallback() {
                    @Override
                    public void onStringAvailable(String s) {
                        if ("Hello Server".equals(s))
                            webSocket.send("Welcome Client!");
                    }
                });
                webSocket.setDataCallback(new DataCallback() {
                    @Override
                    public void onDataAvailable(DataEmitter emitter, ByteBufferList bb) {
                        byte[] bytes = bb.getAllByteArray();
                        bb.recycle();
                        Parcel p = Parcel.obtain();
                        p.unmarshall(bytes, 0, bytes.length);
                        p.setDataPosition(0);
                        Parcelable.Creator<ChirpMessage$$Parcelable> creator = ChirpMessage$$Parcelable.CREATOR;
                        Parcelable parcelable = creator.createFromParcel(p);
                        ChirpMessage message = Parcels.unwrap(parcelable);
                        webSocket.send("Message was:" + message.getText());
                    }
                });
            }
        });
        server.listen(5000);
    }

    public void stop() {
        server.stop();
    }
}
