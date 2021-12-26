package com.example.shoppinglistapp2;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executors;

public class App extends Application {
    private final int NUMBER_OF_THREADS = 4;
    public final ListeningExecutorService backgroundExecutorService
            = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(NUMBER_OF_THREADS));

    private static WeakReference<Context> mContext;

    @Override
    public void onCreate(){
        super.onCreate();
        mContext = new WeakReference<>(this);
    }

    public static Context getContext() {
        return mContext.get();
    }

    public static Resources getRes() {
        return getContext().getResources();
    }
}
