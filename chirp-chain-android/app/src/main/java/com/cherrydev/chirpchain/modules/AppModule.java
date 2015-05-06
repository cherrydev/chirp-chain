package com.cherrydev.chirpchain.modules;

import com.cherrydev.chirpchain.ChirpMessageService;
import com.cherrydev.chirpchain.ChirpchainApplication;
import com.fizzbuzz.android.dagger.InjectingApplication;

import net._01001111.text.LoremIpsum;

import dagger.Module;
import dagger.Provides;

/**
 * Created by alannon on 2015-02-20.
 */

@Module(injects = {ChirpchainApplication.class, LoremIpsum.class, ChirpMessageService.class}, addsTo = InjectingApplication.InjectingApplicationModule.class)
public class AppModule {
    @Provides
    public LoremIpsum provideLorem() { return new LoremIpsum(); }
}