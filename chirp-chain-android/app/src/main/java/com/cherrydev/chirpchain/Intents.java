package com.cherrydev.chirpchain;

/**
 * Created by alannon on 2015-02-21.
 */
public class Intents {
    public static final String INTENT_PREFIX = "com.cherrydev.chirpchain.";
    public static final String INTENT_NEW_MESSAGE = INTENT_PREFIX + "newMessage";
    public static final String INTENT_SMS_MESSAGE = INTENT_PREFIX + "smsMessage";
    public static final String INTENT_RELOAD_MESSAGES = INTENT_PREFIX + "reloadMessages";
    public static final String INTENT_PROGRESS_UPDATE = INTENT_PREFIX + "progressUpdate";
    public static final String INTENT_ACTION_SEND_MESSAGE = INTENT_PREFIX + "action.sendMessage";

    public static final String INTENT_EXTRA_MESSAGE = INTENT_PREFIX + "extra.message";
}
