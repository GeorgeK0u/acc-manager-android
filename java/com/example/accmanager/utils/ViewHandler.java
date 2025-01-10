package com.example.accmanager.utils;

import com.example.accmanager.AddAccPopupDialog;
import com.example.accmanager.EditAccPopupDialog;
import com.example.accmanager.MainActivity;

public class ViewHandler
{
    public static MainActivity MainActivityRef;

    public static void OnLogin()
    {
        AddAccPopupDialog.Init();
        EditAccPopupDialog.Init();
    }
}