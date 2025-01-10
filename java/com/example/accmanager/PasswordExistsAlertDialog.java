package com.example.accmanager;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.example.accmanager.utils.ViewHandler;

public class PasswordExistsAlertDialog extends AlertDialog
{
    public PasswordExistsAlertDialog(Context context, String forDialog)
    {
        super(context);
        setCancelable(false);
        setTitle("This password already exists");
        setMessage("Using the same password for multiple accounts isn't recommended.\\nContinue ?");
        setButton(BUTTON_POSITIVE, "Yes", new OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    if (forDialog.equals("Add"))
                    {
                        ViewHandler.MainActivityRef.addAccPopupDialog.Save();
                    }
                    else
                    {
                        ViewHandler.MainActivityRef.editAccPopupDialog.Update();
                    }
                    dismiss();
                }
            }
        );
        setButton(BUTTON_NEGATIVE, "No", new OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dismiss();
                }
            }
        );
    }
}
