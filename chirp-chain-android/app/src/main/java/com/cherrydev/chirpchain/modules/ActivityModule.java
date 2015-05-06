package com.cherrydev.chirpchain.modules;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;

import com.cherrydev.chirpchain.activity.ChooseDestination;
import com.fizzbuzz.android.dagger.InjectingActivityModule;
import com.fizzbuzz.android.dagger.Injector;

import net._01001111.text.LoremIpsum;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by alannon on 2015-02-20.
 */
@Module(
        injects = {ChooseDestination.class},
        addsTo = InjectingActivityModule.class, library = true)
public class ActivityModule {

    private Activity activity;
    private Injector injector;

    public ActivityModule(android.app.Activity activity, Injector injector) {

        this.activity = activity;
        this.injector = injector;
    }

    @Provides @Singleton @InjectingActivityModule.Activity
    public LayoutInflater provideInflator() {
        return (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Provides @Singleton
    public LoremIpsum provideLorem() { return new LoremIpsum(); }
}
