package com.cherrydev.chirpchain.message;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by alannon on 2015-02-20.
 */
public class ChirpMessageAdaptor extends ArrayAdapter<ChirpMessage> {
    private List<ChirpMessage> messageList;

    public ChirpMessageAdaptor(Context context) {
        this(context, new ArrayList<ChirpMessage>());
    }

    public ChirpMessageAdaptor(Context context, List<ChirpMessage> messageList) {
        super(context, android.R.layout.simple_list_item_2, messageList);
        this.messageList = messageList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = ((LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(android.R.layout.simple_list_item_2, parent, false);
        }
        TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
        TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);
        text1.setText(messageList.get(position).getText());
        text2.setText(messageList.get(position).getTo());
        return convertView;
    }
}
