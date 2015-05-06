package com.cherrydev.chirpchain;

import android.app.Application;

import com.cherrydev.chirpchain.modules.AppModule;
import com.fizzbuzz.android.dagger.InjectingApplication;

import java.util.Arrays;
import java.util.List;

import dagger.Module;
import dagger.ObjectGraph;

/**
 * Created by alannon on 2015-02-20.
 */
public class ChirpchainApplication extends InjectingApplication {
    public ChirpchainApplication() {
        addSeedModules(Arrays.asList( (Object) new AppModule()));
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }
}
