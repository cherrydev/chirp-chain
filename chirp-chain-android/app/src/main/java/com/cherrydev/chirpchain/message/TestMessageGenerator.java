package com.cherrydev.chirpchain.message;

import net._01001111.text.LoremIpsum;

import java.util.Date;
import java.util.Random;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by alannon on 2015-02-20.
 */

@Singleton
public class TestMessageGenerator {
    private static final int[] stationIds = {1,2,3,4,5};
    private final LoremIpsum loremIpsum;
    private int lastMessageId = 0;
    private Random random = new Random();


    @Inject
    public TestMessageGenerator(LoremIpsum loremIpsum) {
        this.loremIpsum = loremIpsum;
    }

    public ChirpMessage generateMessage() {
        int sourceId = stationIds[random.nextInt(stationIds.length)];
        int destId = stationIds[random.nextInt(stationIds.length)];
        while (sourceId == destId) {
            destId = stationIds[random.nextInt(stationIds.length)];
        }
        ChirpMessage m = new ChirpMessage(
                loremIpsum.sentence(),
                loremIpsum.randomWord(),
                new Date(),
                ++lastMessageId,
                sourceId,
                destId
        );
        return m;
    }

    public ChirpMessage createEmptyMessage() {
        ChirpMessage m = new ChirpMessage();
        m.setMessageId(++lastMessageId);
        m.setDate(new Date());
        return m;
    }
}
