package com.cherrydev.chirpchain.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.Layout;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.cherrydev.chirpchain.ChirpMessageService;
import com.cherrydev.chirpchain.Intents;
import com.cherrydev.chirpchain.R;
import com.cherrydev.chirpchain.message.ChirpMessage;
import com.cherrydev.chirpchain.message.ChirpMessageAdaptor;
import com.cherrydev.chirpchain.message.ChirpMessageIntent;
import com.cherrydev.chirpchain.message.TestMessageGenerator;
import com.cherrydev.chirpchain.modules.ActivityModule;
import com.fizzbuzz.android.dagger.InjectingActivity;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import dagger.Module;

public class ChooseDestination extends InjectingActivity {

    private ChirpMessage message;

    private int selectedDestination = -1;

    @InjectView(R.id.destinationList)
    ListView destinationList;

    @InjectView(R.id.sendButton)
    ImageButton sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            message = Parcels.unwrap(savedInstanceState.getParcelable(Intents.INTENT_EXTRA_MESSAGE));
        }
        else {
            message = Parcels.unwrap(getIntent().getParcelableExtra(Intents.INTENT_EXTRA_MESSAGE));
        }
        setContentView(R.layout.activity_choose_destination);
        ButterKnife.inject(this);
        String[] destinations = {
            "Some cool camp",
            "Some other camp",
            "Center camp",
            "Vertigo"
        };
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.destination_list_item, android.R.id.text1,destinations) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                int visibility = selectedDestination == position ? View.VISIBLE : View.INVISIBLE;
                v.findViewById(android.R.id.checkbox).setVisibility(visibility);
                return v;
            }
        };
        destinationList.setAdapter(adapter);
        destinationList.setOnItemClickListener((parent, view, position, id) -> {
            selectedDestination = position;
            sendButton.setEnabled(true);
            adapter.notifyDataSetInvalidated();
        });
        sendButton.setEnabled(false);
        sendButton.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(Intents.INTENT_ACTION_SEND_MESSAGE, null, getApplicationContext(), ChirpMessageService.class);
            serviceIntent.putExtra(Intents.INTENT_EXTRA_MESSAGE, Parcels.wrap(message));
            startService(serviceIntent);
            finish();
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(Intents.INTENT_EXTRA_MESSAGE, Parcels.wrap(message));
    }

    @Override protected List<Object> getModules() {
        List<Object> modules = super.getModules();
        modules.add(new ChooseDestinationModule());
        return modules;
    }


    @Module(injects = ChooseDestination.class, addsTo = ActivityModule.class)
    public class ChooseDestinationModule {

    }

}
