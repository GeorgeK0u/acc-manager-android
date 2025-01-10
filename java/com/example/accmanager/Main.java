package com.example.accmanager;

import android.app.Application;

import com.example.accmanager.utils.Cryptor;
import com.example.accmanager.utils.XmlHandler;

public class Main extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();
        Init();
    }

    private void Init()
    {
        Cryptor.Init();
        XmlHandler.Init();
    }
}