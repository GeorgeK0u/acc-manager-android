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
import com.example.accmanager.utils.ViewHandler;
import com.example.accmanager.utils.XmlHandler;
import com.google.android.material.slider.RangeSlider;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class EditAccPopupDialog extends Dialog
{
    private Random Rand;
    private ClipboardManager Clipboard;
    private LinearLayout GenMenuLayout;
    private TextView GenPwdSliderValueTv;
    private EditText AccNameInput, ExtraInfoInput, PwdInput;
    private Button CopyPwdBtn, GenMenuBtn, GenBtn, UpdateBtn;
    private RangeSlider GenPwdSlider;
    private CheckBox DisabledCbx;
    private String[] AccDetails, EditAccDetails;
    private ArrayList<ArrayList<Object>> GenPwdCharacters;
    private ArrayList<Character> SimilarCharacters;
    private ArrayList<CheckBox> CharGroupCbxList;
    private String prevAccName;
    private boolean showGenMenu;
    private Gson gson;

    @RequiresApi(api = Build.VERSION_CODES.N)
    public EditAccPopupDialog(@NonNull Context context) {
        super(context);
        setContentView(R.layout.edit_acc_dialog_layout);
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
        // update btn
        UpdateBtn = findViewById(R.id.updateBtn);
        UpdateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                UpdateAcc();
            }
        });
        // get edit acc details
        EditAccDetails = ViewHandler.MainActivityRef.GetSelectedRowAccDetails();
        AccNameInput.setText(EditAccDetails[0]);
        ExtraInfoInput.setText(EditAccDetails[1]);
        PwdInput.setText(EditAccDetails[2]);
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

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void UpdateAcc()
    {
        String accName = AccNameInput.getText().toString();
        String extraInfo = ExtraInfoInput.getText().toString();
        String pwd = PwdInput.getText().toString();
        // format empty fields
        if (extraInfo.equals(""))
        {
            extraInfo = "-";
        }
        if (pwd.equals(""))
        {
            pwd = "-";
        }
        AccDetails = new String[] { accName, extraInfo, pwd };
        // acc details are the same
        if (Arrays.equals(AccDetails, EditAccDetails))
        {
            System.out.println("No reason to update. Acc details are the same");
            dismiss();
            return;
        }
        // empty acc name
        if (accName.equals(""))
        {
            Toast.makeText(getContext(), "Account name is empty", Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayList<String> AccNames = XmlHandler.GetAccNames();
        prevAccName = EditAccDetails[0];
        // acc name exists and not due to itself
        if (AccNames.contains(accName) && !accName.equals(prevAccName))
        {
            Toast.makeText(getContext(), "Account name already exists", Toast.LENGTH_SHORT).show();
            return;
        }
        // check if password exists
        if (!pwd.equals("-"))
        {
            String prevPwd = EditAccDetails[2];
            // password exists and not because of itself
            if (XmlHandler.GetPwds().contains(pwd) && !pwd.equals(prevPwd))
            {
                // show password exists alert dialog
                new QuestionAlertDialog(
                    getContext(),
                    "This password already exists",
                    "Using the same password for multiple accounts isn't recommended.\\nContinue ?",
                    false,
                    () -> { Update(); },
                        null
                );
                return;
            }
        }
        Update();
    }

    public void Update()
    {
        // create msg
        String encPrevAccName = Cryptor.Encrypt(prevAccName);
        ArrayList<Object> Msg = new ArrayList<>(Arrays.asList(Client.SYNC_BC, "U", encPrevAccName));
        // get updated acc details
        for (int i = 0; i < 3; i++) {
            if (AccDetails[i].equals(EditAccDetails[i])) {
                continue;
            }
            String encIndex = Cryptor.Encrypt(String.valueOf(i));
            String encDetail = Cryptor.Encrypt(AccDetails[i]);
            // add updated details arr to msg
            Msg.add(new ArrayList<>(Arrays.asList(encIndex, encDetail)));
        }
        // convert to json string
        String msgJsonString = gson.toJson(Msg);
        // send broadcast sync msg
        Client.SendBroadcastMsg(msgJsonString);
        // close dialog
        dismiss();
    }

    public static void Init()
    {

    }
}