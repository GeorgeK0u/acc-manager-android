package com.example.accmanager;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.example.accmanager.utils.ViewHandler;

//public class PasswordExistsAlertDialog extends AlertDialog
//{
//        setTitle("This password already exists");
//        setMessage("Using the same password for multiple accounts isn't recommended.\\nContinue ?");
// yes clicked
//if (forDialog.equals("Add"))
//    {
//        ViewHandler.MainActivityRef.addAccPopupDialog.Save();
//    }
//                    else
//    {
//        ViewHandler.MainActivityRef.editAccPopupDialog.Update();
//    }
//    dismiss();
    // no clicked
//                    dismiss();

    //    public PasswordExistsAlertDialog(Context context, String title, String msg, boolean cancelable,  onYesClicked,  onNoClicked)

@FunctionalInterface
interface OnClickAction {
    void onClick();
}

public class QuestionAlertDialog extends AlertDialog
{
    public QuestionAlertDialog(Context context, String title, String msg, boolean cancelable, OnClickAction Yes, OnClickAction No)
    {
        super(context);
        setCancelable(cancelable);
        setTitle(title);
        setMessage(msg);
        setButton(BUTTON_POSITIVE, "Yes", new OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Yes.onClick();
                }
            }
        );
        setButton(BUTTON_NEGATIVE, "No", new OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    // no closes the dialog
                    if (No == null)
                    {
                        dismiss();
                        return;
                    }
                    No.onClick();
                }
            }
        );
        show();
    }
}
