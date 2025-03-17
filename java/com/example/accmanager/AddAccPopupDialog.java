package com.example.accmanager;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.example.accmanager.utils.Client;
import com.example.accmanager.utils.Cryptor;
import com.example.accmanager.utils.XmlHandler;
import com.google.android.material.slider.RangeSlider;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class AddAccPopupDialog extends Dialog
{
    private Random Rand;
    private ClipboardManager Clipboard;
    private LinearLayout GenMenuLayout;
    private TextView GenPwdSliderValueTv;
    private EditText AccNameInput, ExtraInfoInput, PwdInput;
    private Button CopyPwdBtn, GenMenuBtn, GenBtn, SaveBtn;
    private RangeSlider GenPwdSlider;
    private CheckBox DisabledCbx;
    private ArrayList<ArrayList<Object>> GenPwdCharacters;
    private ArrayList<Character> SimilarCharacters;
    private ArrayList<CheckBox> CharGroupCbxList;
    private String accName, extraInfo, pwd;
    private boolean showGenMenu;
    private Gson gson;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public AddAccPopupDialog(@NonNull Context context) {
        super(context);
        setContentView(R.layout.add_acc_dialog_layout);
        // dynamically crop a bit from the popup left, right sides
        // get popup dialog window
        Window PopupDialogWindow;
        try
        {
            PopupDialogWindow = getWindow();
            if (PopupDialogWindow == null)
            {
                throw new Exception("Popup dialog window was not found");
            }
        }
        catch (Exception e)
        {
            System.out.println("Exception: " + e);
            return;
        }
        // get display metrics of popup dialog window
        DisplayMetrics displayMetrics = new DisplayMetrics();
        PopupDialogWindow.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        // get initial popup dialog width in px
        int width = displayMetrics.widthPixels;
        // set the popup dialog width based on initial
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.copyFrom(PopupDialogWindow.getAttributes());
        layoutParams.width = (int)(width * 0.9/1);
        PopupDialogWindow.setAttributes(layoutParams);
        // widgets
        // acc name
        AccNameInput = findViewById(R.id.accNameInput);
        ExtraInfoInput = findViewById(R.id.extraInfoInput);
        PwdInput = findViewById(R.id.pwdInput);
        // copy pwd btn
        CopyPwdBtn = findViewById(R.id.copyPwdBtn);
        CopyPwdBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.Q)
            @Override
            public void onClick(View view) {
                String pwd = PwdInput.getText().toString();
                Clipboard.setPrimaryClip(ClipData.newPlainText("text", pwd));
            }
        });
        Clipboard = (ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        // gen menu layout
        GenMenuLayout = findViewById(R.id.genMenu);
        // gen pwd slider
        GenPwdSlider = findViewById(R.id.genPwdSlider);
        GenPwdSlider.addOnChangeListener(new RangeSlider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull RangeSlider slider, float value, boolean fromUser) {
                int valueInt = (int)value;
                // update gen pwd slider value text
                GenPwdSliderValueTv.setText(String.valueOf(valueInt));
            }
        });
        // gen pwd slider value text
        GenPwdSliderValueTv = findViewById(R.id.genPwdSliderValue);
        // set slider starting value
        float startingValue = 12f;
        GenPwdSlider.setValues(startingValue);
        GenPwdSliderValueTv.setText(String.valueOf((int)startingValue));
        // char checkbox groups
        CharGroupCbxList = new ArrayList<>();
        CharGroupCbxList.add(findViewById(R.id.capitalAZCbx));
        CharGroupCbxList.add(findViewById(R.id.lowerAZCbx));
        CharGroupCbxList.add(findViewById(R.id.digitsCbx));
        CharGroupCbxList.add(findViewById(R.id.symbolsCbx));
        CharGroupCbxList.forEach((CharGroupCbx) ->
            {
                // start all checked
                CharGroupCbx.setChecked(true);
                // on click
                CharGroupCbx.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // prevent last char group checkbox from being unchecked
                        // check checked count
                        int checkedCount = GetCheckedCharGroupCbxCount();
                        // disable only checkbox
                        if (checkedCount == 1) {
                            DisabledCbx = GetOnlyCheckedCbx();
                            // disable
                            DisabledCbx.setEnabled(false);
                        }
                        // re-enable disabled checkbox
                        else if (checkedCount == 2 && DisabledCbx != null) {
                            DisabledCbx.setEnabled(true);
                            DisabledCbx = null;
                        }
                    }
                });
            }
        );
        // gen btn
        GenBtn = findViewById(R.id.genBtn);
        GenBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                GenPwd();
            }
        });
        Rand = new Random();
        // gen chars
        GenPwdCharacters = new ArrayList<>();
        GenPwdCharacters.add(new ArrayList<>(Arrays.asList(65, 90)));
        GenPwdCharacters.add(new ArrayList<>(Arrays.asList(97, 122)));
        GenPwdCharacters.add(new ArrayList<>(Arrays.asList(48, 57)));
        GenPwdCharacters.add(new ArrayList<>(Arrays.asList('!', '@', '#', '$', '%', '^', '*', '(', ')', '-', '_', '=', '+', '[', '{', ']', '}', '\\', ';', ':', ',', '<', '.', '>', '/', '?')));
        // similar chars
        SimilarCharacters = new ArrayList<>(Arrays.asList('I', 'l'));
        // gen menu btn
        GenMenuBtn = findViewById(R.id.genMenuBtn);
        GenMenuBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View view) {
                showGenMenu = !showGenMenu;
                GenMenuLayout.setVisibility(showGenMenu ? View.VISIBLE : View.GONE);
                if (showGenMenu && PwdInput.getText().toString().equals(""))
                {
                    GenPwd();
                }
            }
        });
        showGenMenu = false;
        // save btn
        SaveBtn = findViewById(R.id.saveBtn);
        SaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                SaveAcc();
            }
        });
        gson = new GsonBuilder().setLenient().create();
    }

    private int GetCheckedCharGroupCbxCount()
    {
        int count = 0;
        for (CheckBox Cbx : CharGroupCbxList)
        {
            if (!Cbx.isChecked())
            {
                continue;
            }
            count++;
        }
        return count;
    }

    private CheckBox GetOnlyCheckedCbx()
    {
        for (CheckBox Cbx : CharGroupCbxList)
        {
            if (!Cbx.isChecked())
            {
                continue;
            }
            return Cbx;
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void GenPwd()
    {
        int pwdLen = (int)(float)GenPwdSlider.getValues().get(0);
        String pwd = "";
        ArrayList<ArrayList<Object>> CheckedCharGroupList = new ArrayList<>();
        // get checked char groups
        for (int i = 0; i < CharGroupCbxList.size(); i++)
        {
            CheckBox CharGroupCbx = CharGroupCbxList.get(i);
            if (!CharGroupCbx.isChecked())
            {
                continue;
            }
            CheckedCharGroupList.add(GenPwdCharacters.get(i));
        }
        // generate
        for (int i = 0; i < pwdLen; i++)
        {
            int listIndex = Rand.nextInt(CheckedCharGroupList.size());
            ArrayList<Object> CharList = CheckedCharGroupList.get(listIndex);
            char ch;
            if (CharList.get(0).getClass() == Integer.class)
            {
                // convert object arr to integer arr
                ArrayList<Integer> RangeCharList = new ArrayList<>();
                CharList.forEach((el) -> RangeCharList.add(Integer.parseInt(el.toString())));
                ch = GetRandCharFromRangeList(RangeCharList);
            }
            else
            {
                int chIndex = Rand.nextInt(CharList.size());
                ch = (char)CharList.get(chIndex);
            }
            pwd += ch;
        }
        // pwd exists
        if (XmlHandler.GetPwds().contains(pwd))
        {
            GenPwd();
            return;
        }
        // fill pwd
        PwdInput.setText(pwd);
    }

    private int GetRandRange(int min, int max)
    {
        int maxTmp = max - min + 1;
        return Rand.nextInt(maxTmp) + min;
    }

    private char GetRandCharFromRangeList(ArrayList<Integer> RangeList)
    {
        int start = RangeList.get(0);
        int end = RangeList.get(1);
        while (true)
        {
            int ascii = GetRandRange(start, end);
            char ch = (char)ascii;
            if (SimilarCharacters.contains(ch))
            {
                continue;
            }
            return ch;
        }
    }

    private void SaveAcc()
    {
        accName = AccNameInput.getText().toString();
        // empty acc name
        if (accName.equals(""))
        {
            Toast.makeText(getContext(), "Account name is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        // acc name exists
        ArrayList<String> AccNames = XmlHandler.GetAccNames();
        if (AccNames.contains(accName))
        {
            Toast.makeText(getContext(), "Account name already exists", Toast.LENGTH_SHORT).show();
            return;
        }
        // format empty fields
        extraInfo = ExtraInfoInput.getText().toString();
        if (extraInfo.equals(""))
        {
            extraInfo = "-";
        }
        pwd = PwdInput.getText().toString();
        if (pwd.equals(""))
        {
            pwd = "-";
        }
        else
        {
            // password exists
            if (XmlHandler.GetPwds().contains(pwd))
            {
                // show password exists alert dialog
                new QuestionAlertDialog(
                    getContext(),
                    "This password already exists",
                    "Using the same password for multiple accounts isn't recommended.\\nContinue ?",
                     false,
                        () -> { Save(); },
                        null
                );
                return;
            }
        }
        Save();
    }

    public void Save()
    {
        // create msg
        String encAccName = Cryptor.Encrypt(accName);
        String encExtraInfo = Cryptor.Encrypt(extraInfo);
        String encPwd = Cryptor.Encrypt(pwd);
        ArrayList<String> Msg = new ArrayList(Arrays.asList(Client.SYNC_BC, "C", encAccName, encExtraInfo, encPwd));
        // convert msg to json string
        String msgJsonString = gson.toJson(Msg);
        // send broadcast sync msg
        Client.SendBroadcastMsg(msgJsonString);
        // clear fields
        AccNameInput.setText("");
        ExtraInfoInput.setText("");
        PwdInput.setText("");
        // hide generate menu
        if (showGenMenu)
        {
            GenMenuBtn.performClick();
        }
        // focus on acc name
        AccNameInput.requestFocus();
    }

    public static void Init()
    {

    }
}