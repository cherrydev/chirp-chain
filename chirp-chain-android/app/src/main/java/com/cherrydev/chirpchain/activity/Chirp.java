package com.cherrydev.chirpchain.activity;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cherrydev.chirpchain.ChirpMessageService;
import com.cherrydev.chirpchain.Intents;
import com.cherrydev.chirpchain.R;
import com.cherrydev.chirpchain.message.ChirpMessage;
import com.cherrydev.chirpchain.message.ChirpMessageAdaptor;
import com.cherrydev.chirpchain.message.ChirpMessageIntent;
import com.cherrydev.chirpchain.message.TestMessageGenerator;
import com.cherrydev.chirpchain.modules.ActivityModule;
import com.cherrydev.chirpchain.util.FakeMessageQueue;
import com.cherrydev.chirpchain.util.IpDialogCallback;
import com.cherrydev.chirpchain.views.ProgressView;
import com.fizzbuzz.android.dagger.InjectingActivity;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import dagger.Module;

public class Chirp extends InjectingActivity implements IpDialogCallback {


    @Inject
    TestMessageGenerator testMessageGenerator;

    private Handler handler;

    private ChirpMessageService messageService;

    private BroadcastReceiver broadcastReceiver;

    private ChirpMessageAdaptor listAdaptor;

    private FakeMessageQueue messageQueue;

    @InjectView(R.id.messageListView)
    ListView messageListView;

    @InjectView(R.id.emptyView)
    View emptyView;

    @InjectView(R.id.editViewMessageText)
    EditText messageText;

    @InjectView(R.id.statusContainer)
    LinearLayout statusContainer;


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startLockTask();
        setContentView(R.layout.activity_chirp);
        ButterKnife.inject(this);
        messageQueue = new FakeMessageQueue();
        List<ChirpMessage> messages = new ArrayList<>();

        listAdaptor = new ChirpMessageAdaptor(this, messages);


        messageListView.setAdapter(listAdaptor);
        messageListView.setEmptyView(emptyView);
        Intent serviceIntent = new Intent(this, ChirpMessageService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ChirpMessageIntent messageIntent = new ChirpMessageIntent(intent);
                listAdaptor.add(messageIntent.getMessage());
                messageListView.smoothScrollToPosition(listAdaptor.getCount() - 1);
            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter(Intents.INTENT_NEW_MESSAGE));
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Toast.makeText(Chirp.this, "Reloading messages", Toast.LENGTH_SHORT).show();
                listAdaptor.clear();
                listAdaptor.addAll(messageService.getAllMessages());
            }
        }, new IntentFilter(Intents.INTENT_RELOAD_MESSAGES));

        registerForContextMenu(messageListView);
        registerForContextMenu(emptyView);

        messageText.setOnEditorActionListener((v, actionId, event) -> {
            composeNewMessage(messageText.getText().toString());
            return false;
        });
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 300);
        ProgressView progress = new ProgressView(this);
        statusContainer.addView(progress, params);
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                float progressAmount = intent.getFloatExtra("progress", 0f);
                FakeMessageQueue.Mode mode = FakeMessageQueue.Mode.valueOf(intent.getStringExtra("mode"));
                progress.setProgress(progressAmount);
            }
        }, new IntentFilter(Intents.INTENT_PROGRESS_UPDATE));
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        getMenuInflater().inflate(R.menu.menu_chirp, menu);
    }

    public void onClearMessages(MenuItem menu) {
        Toast.makeText(this, "Clearing messages", Toast.LENGTH_SHORT).show();
        messageService.clearMessages();
    }

    public void onStartMessages(MenuItem menu) {
        messageService.startGenerating();
    }

    public void onStopMessages(MenuItem menu) {
        messageService.stopGenerating();
    }

    public void onMakeServer(MenuItem menu) {
        SharedPreferences preferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        preferences.edit().putBoolean("isServer", true).commit();
        messageService.resetNetwork();
    }

    public void onMakeClient(MenuItem menu) {
        IpDialogFragment dialog = new IpDialogFragment();
        dialog.show(getFragmentManager(), "serverIp");
    }

    public void onResetNetwork(MenuItem menu) {
        messageService.resetNetwork();
    }


    @Override
    public void setIpAddress(String address) {
        SharedPreferences preferences = getSharedPreferences("prefs", Context.MODE_PRIVATE);
        preferences.edit()
                .putString("serverAddress", address)
                .putBoolean("isServer", false)
                .commit();
        messageService.resetNetwork();
    }


    public void composeNewMessage(String messageText) {
        ChirpMessage newMessage = messageService.startNewMessage(messageText);
        Intent destIntent = new Intent(getApplicationContext(), ChooseDestination.class);
        destIntent.putExtra(Intents.INTENT_EXTRA_MESSAGE, Parcels.wrap(newMessage));
        startActivity(destIntent);
        Toast.makeText(this, "Composing: " + messageText, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(broadcastReceiver);
        unbindService(serviceConnection);
    }

    private void onServiceConnected() {
        listAdaptor.addAll(messageService.getAllMessages());
    }

    @Override protected List<Object> getModules() {
        List<Object> modules = super.getModules();
        modules.add(new ChirpActivityModule());
        return modules;
    }



    @Module(injects = Chirp.class, addsTo = ActivityModule.class)
    public class ChirpActivityModule {

    }

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ChirpMessageService.ChirpMessageServiceBinder binder = (ChirpMessageService.ChirpMessageServiceBinder) service;
            messageService = binder.getService();
            Chirp.this.onServiceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };


   public static class IpDialogFragment extends DialogFragment
   {
       @Override
       public Dialog onCreateDialog(Bundle savedInstanceState) {

           AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
           // Get the layout inflater
           LayoutInflater inflater = getActivity().getLayoutInflater();

           // Inflate and set the layout for the dialog
           // Pass null as the parent view because its going in the dialog layout
           builder.setView(inflater.inflate(R.layout.ip_dialog, null))
                   // Add action buttons
                   .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                       @Override
                       public void onClick(DialogInterface dialog, int id) {
                           EditText ipText = (EditText) IpDialogFragment.this.getDialog().findViewById(R.id.ipText);
                           ((IpDialogCallback)getActivity()).setIpAddress(ipText.getText().toString());
                       }
                   })
                   .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                       public void onClick(DialogInterface dialog, int id) {
                       }
                   });
           return builder.create();
       }
   }
}
