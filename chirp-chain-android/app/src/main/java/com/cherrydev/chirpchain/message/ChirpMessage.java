package com.cherrydev.chirpchain.message;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.parceler.Parcel;
import org.parceler.ParcelClass;
import org.parceler.ParcelConverter;
import org.parceler.ParcelPropertyConverter;

import java.util.Date;

/**
 * Created by alannon on 2015-02-20.
 */

@DatabaseTable(tableName = "ChirpMessage")
@Parcel
public class ChirpMessage {

    @DatabaseField
    String text;

    @DatabaseField
    String to;

    @DatabaseField
    @ParcelPropertyConverter(ChirpMessage.DateParcelConverter.class)
    Date date;

    @DatabaseField(generatedId=true)
    int messageId;

    @DatabaseField
    int sourceStationId;

    @DatabaseField
    int destStationId;

    public ChirpMessage(String text, String to, Date date, int messageId, int sourceStationId, int destStationId) {
        this();
        this.text = text;
        this.to = to;
        this.date = date;
        this.sourceStationId = sourceStationId;
        this.destStationId = destStationId;
    }

    public ChirpMessage() {

    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public int getSourceStationId() {
        return sourceStationId;
    }

    public void setSourceStationId(int sourceStationId) {
        this.sourceStationId = sourceStationId;
    }

    public int getDestStationId() {
        return destStationId;
    }

    public void setDestStationId(int destStationId) {
        this.destStationId = destStationId;
    }

    public static class DateParcelConverter implements ParcelConverter<Date> {

        @Override
        public void toParcel(Date input, android.os.Parcel parcel) {
            parcel.writeLong(input.getTime());
        }

        @Override
        public Date fromParcel(android.os.Parcel parcel) {
            return new Date(parcel.readLong());
        }
    }
}
