package com.example.accmanager;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.accmanager.utils.Client;
import com.example.accmanager.utils.Cryptor;
import com.example.accmanager.utils.MyTableLayout;
import com.example.accmanager.utils.ViewHandler;
import com.example.accmanager.utils.XmlHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity {
    public AddAccPopupDialog addAccPopupDialog;
    public EditAccPopupDialog editAccPopupDialog;
    private TextView ConnStatusTv, NoAccsTv;
    private EditText SearchInput;
    private CheckBox MatchSearchCaseCheckbox;
    private Spinner SortResultsDropdown;
    private Button SettingsBtn, AscDescBtn, AddAccBtn, ManualSyncBtn, EditAccBtn, DelAccBtn, ChangeAllPwdVisBtn;
    private MyTableLayout AccTable;
    private ArrayList<ArrayList<String>> Accs;
    // sort option
    private final int TIME_ADDED_SORT_OPTION_INDEX = 0, ALPHABETICAL_SORT_OPTION_INDEX = 1, NUMBER_OF_FILLED_FIELDS_SORT_OPTION_INDEX = 2;
    private int selectedSortOptionIndex, sortColIndex;
    // client conn status
    public static final String CLIENT_CONNECTING = "Connecting...", CLIENT_CONNECTED = "Connected", CLIENT_NOT_CONNECTED = "Not Connected";
    // sort order
    private final String ASC_ORDER = "Asc", DESC_ORDER = "Desc";
    // no accs text
    private final String NO_ACCS = "No Accounts", NO_SEARCH_RESULTS = "No Search Results";
    private String sortOrder;
    private static String curConnStatus;
    private boolean autoSortSelection, maskAllPwds;
    private static boolean startConn;
    private Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        Init();
    }

    private void Init()
    {
        // get main activity ref
        if (ViewHandler.MainActivityRef == null)
        {
            // get main activity ref
            ViewHandler.MainActivityRef = this;
        }
        // widgets
        // conn status
        ConnStatusTv = findViewById(R.id.connStatusTv);
        // start conn once
        if (!startConn)
        {
            // set text to connecting
            SetConnStatus(CLIENT_CONNECTING);
            // start conn loop on a seperate thread
            new Thread(() -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    System.out.println("Cannot start conn loop");
                    return;
                }
                Client.StartConn();
            }).start();
            startConn = true;
        }
        else
        {
            // set to latest status
            ConnStatusTv.setText(curConnStatus);
        }
        // settings
        SettingsBtn = findViewById(R.id.settingsBtn);
        //search
        // search input
        SearchInput = findViewById(R.id.searchInput);
        SearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
            {
                String searchQuery = charSequence.toString();
                UpdateResults(searchQuery);
            }
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }

            @Override
            public void afterTextChanged(Editable editable) { }
        });
        // match case
        MatchSearchCaseCheckbox = findViewById(R.id.matchSearchCaseCheckbox);
        MatchSearchCaseCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String searchQuery = SearchInput.getText().toString();
                UpdateResults(searchQuery);
            }
        });
        // acc table
        AccTable = findViewById(R.id.accTable);
        // sort column
        // on header click
        TableRow TableHeaderRow = (TableRow)AccTable.getChildAt(0);
        for (int i = 0; i < 3; i++)
        {
            TextView TableHeaderTv = (TextView)TableHeaderRow.getChildAt(i);
            TableHeaderTv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int clickedColIndex = TableHeaderRow.indexOfChild(TableHeaderTv);
                    sortColIndex = clickedColIndex;
                    // not alphabetical sorting
                    if (selectedSortOptionIndex != 1)
                    {
                        return;
                    }
                    // update sort results
                    // get current table results
                    ArrayList<ArrayList<String>> AllAccDetails = AccTable.GetAll();
                    // sort results
                    SortResults(AllAccDetails);
                    // clear table
                    AccTable.Clear();
                    // add back sorted results
                    AccTable.AddRows(AllAccDetails, maskAllPwds);
                }
            });
        }
        sortColIndex = 0;
        // sort results
        // sort dropdown
        SortResultsDropdown = findViewById(R.id.sortResultsDropdown);
        String[] SortResultsOptions = new String[] { "Time Added", "Alphabetically", "Number of Filled Fields" };
        ArrayAdapter SortResultsDropdownAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item, SortResultsOptions);
        SortResultsDropdownAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        SortResultsDropdown.setAdapter(SortResultsDropdownAdapter);
        SortResultsDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int selIndex, long l)
            {
                if (autoSortSelection)
                {
                    autoSortSelection = false;
                    return;
                }
                // update selected sort option
                selectedSortOptionIndex = selIndex;
                // get current table results
                ArrayList<ArrayList<String>> AllAccDetails = AccTable.GetAll();
                // sort results
                SortResults(AllAccDetails);
                // clear table
                AccTable.Clear();
                // add back sorted results
                AccTable.AddRows(AllAccDetails, maskAllPwds);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) { }
        });
        autoSortSelection = true;
        // asc/desc
        AscDescBtn = findViewById(R.id.ascDescBtn);
        AscDescBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // update
                sortOrder = (sortOrder.equals(ASC_ORDER)) ? DESC_ORDER : ASC_ORDER;
                AscDescBtn.setText(sortOrder);
                // get current table results
                ArrayList<ArrayList<String>> AllAccDetails = AccTable.GetAll();
                // sort results
                Collections.reverse(AllAccDetails);
                // clear table
                AccTable.Clear();
                // add back sorted results
                AccTable.AddRows(AllAccDetails, maskAllPwds);
            }
        });
        sortOrder = ASC_ORDER;
        // actions
        // add acc btn
        AddAccBtn = findViewById(R.id.addAccBtn);
        AddAccBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                if (!Client.IsConnected())
                {
                    Toast.makeText(getApplicationContext(), "Not connected", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (Client.GetManualSyncInProgress())
                {
                    Toast.makeText(getApplicationContext(), "Manual sync in progress", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    System.out.println("Cannot show AddAccPopupDialog");
                    return;
                }
                addAccPopupDialog = new AddAccPopupDialog(MainActivity.this);
                addAccPopupDialog.show();
            }
        });
        ManualSyncBtn = findViewById(R.id.manualSyncBtn);
        ManualSyncBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Client.SendManualSyncMsg();
            }
        });
        // edit acc btn
        EditAccBtn = findViewById(R.id.editAccBtn);
        EditAccBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!Client.IsConnected())
                {
                    Toast.makeText(getApplicationContext(), "Not connected", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (Client.GetManualSyncInProgress())
                {
                    Toast.makeText(getApplicationContext(), "Manual sync in progress", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (AccTable.GetSelectedRowAccIndex() == -1)
                {
                    Toast.makeText(getApplicationContext(), "No account selected", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    System.out.println("Cannot show AddAccPopupDialog");
                    return;
                }
                editAccPopupDialog = new EditAccPopupDialog(MainActivity.this);
                editAccPopupDialog.show();
            }
        });
        // del acc btn
        DelAccBtn = findViewById(R.id.delAccBtn);
        DelAccBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!Client.IsConnected())
                {
                    Toast.makeText(getApplicationContext(), "Not connected", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (Client.GetManualSyncInProgress())
                {
                    Toast.makeText(getApplicationContext(), "Manual sync in progress", Toast.LENGTH_SHORT).show();
                    return;
                }
                int accIndex = AccTable.GetSelectedRowAccIndex();
                if (accIndex == -1)
                {
                    Toast.makeText(getApplicationContext(), "No account selected", Toast.LENGTH_SHORT).show();
                    return;
                }
                // get selected row acc details
                String[] AccDetails = AccTable.GetSelectedRowAccDetails();
                String accName = AccDetails[0];
                // delete confirmation
                new QuestionAlertDialog(
                    MainActivity.this,
                    "Delete Confirmation",
                    "Are you sure you want to delete " + accName + " ?",
                    false,
                    () -> {
                        // encrypt acc name
                        String encAccName = Cryptor.Encrypt(accName);
                        // create msg
                        ArrayList<String> Msg = new ArrayList<>(Arrays.asList(Client.SYNC_BC, "D", encAccName));
                        String msgJsonString  = gson.toJson(Msg);
                        // send sync broadcast delete msg
                        Client.SendBroadcastMsg(msgJsonString);
                    },
                    null
                );
            }
        });
        // change all pwds vis btn
        ChangeAllPwdVisBtn = findViewById(R.id.changeAllPwdVisBtn);
        ChangeAllPwdVisBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                ChangePwdsVisibility();
            }
        });
        maskAllPwds = true;
        // no accs text
        NoAccsTv = findViewById(R.id.noAccsTv);
        // get all accs
        Accs = XmlHandler.GetAccs(true);
        // load accs
        UpdateResults("");
        gson = new GsonBuilder().setLenient().create();
    }

    private void ChangePwdsVisibility()
    {
        maskAllPwds = !maskAllPwds;
        // get table rows
        ArrayList<ArrayList<String>> AllTableAccDetails = AccTable.GetAll();
        // clear table
        AccTable.Clear();
        // add updated rows
        AccTable.AddRows(AllTableAccDetails, maskAllPwds);
    }

    private void SetNoAccsText(String text)
    {
        if (NoAccsTv.getText().toString().equals(text))
        {
            return;
        }
        NoAccsTv.setText(text);
    }
    private void SetNoAccsVisibility(int vis)
    {
        if (NoAccsTv.getVisibility() == vis)
        {
            return;
        }
        NoAccsTv.setVisibility(vis);
    }

    private boolean DisplayAcc(int accIndex, ArrayList<String> AccDetails)
    {
        if (accIndex == -0)
        {
            String searchQuery = SearchInput.getText().toString();
            searchQuery = MatchSearchCaseCheckbox.isChecked() ? searchQuery : searchQuery.toLowerCase();
            String accName = AccDetails.get(0);
            accName = MatchSearchCaseCheckbox.isChecked() ? accName : accName.toLowerCase();
            String extraInfo = AccDetails.get(1);
            extraInfo = MatchSearchCaseCheckbox.isChecked() ? extraInfo : extraInfo.toLowerCase();
            if (accName.contains(searchQuery) || extraInfo.contains(searchQuery))
            {
                return true;
            }
            return false;
        }
        // check if can be added to current table results
        if (accIndex != -1)
        {
            return true;
        }
        return false;
    }

    public void InsertTableRow(int accIndex, ArrayList<String> AccDetails, int allAccsIndex)
    {
        // add to current table results
        if (DisplayAcc(accIndex, AccDetails))
        {
            // update no accs
            SetNoAccsVisibility(View.GONE);
            // add to current results
            if (accIndex == -1 || accIndex == -0) {
                AccTable.AddRow(AccDetails, maskAllPwds);
            }
            // re-add to current results in same row
            else
            {
                AccTable.InsertRow(accIndex+1, AccDetails, maskAllPwds);
            }
        }
        // update all accs list
        if (allAccsIndex == -1)
        {
            Accs.add(AccDetails);
        }
        else
        {
            Accs.add(allAccsIndex, AccDetails);
        }
    }

    public void AddTableRow(ArrayList<String> AccDetails, int allAccsIndex)
    {
        InsertTableRow(-0, AccDetails, allAccsIndex);
    }

    public void UpdateTableRow(ArrayList<Object> Msg, int allAccsIndex)
    {
        String prevAccName = String.valueOf(Msg.remove(0));
        // get all acc details
        ArrayList<ArrayList<String>> EncUpdatedAccDetails = new ArrayList<>();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            System.out.println("Phone not compatible with forEach lambda expression");
            return;
        }
        Msg.forEach((updatedAccDetailArr) -> EncUpdatedAccDetails.add((ArrayList<String>)updatedAccDetailArr));
        ArrayList<String> AccDetails = new ArrayList<>(Arrays.asList(null, null, null));
        for (ArrayList<String> EncUpdatedAccDetailArr : EncUpdatedAccDetails)
        {
            // updated detail
            int updatedIndex = Integer.parseInt(Cryptor.Decrypt(EncUpdatedAccDetailArr.get(0)));
            String updatedDetail = Cryptor.Decrypt(EncUpdatedAccDetailArr.get(1));
            AccDetails.set(updatedIndex, updatedDetail);
        }
        for (int i = 0; i < AccDetails.size(); i++)
        {
            if (AccDetails.get(i) == null)
            {
                // same valued
                ArrayList<String> Acc = Accs.get(allAccsIndex);
                AccDetails.set(i, Acc.get(i));
            }
        }
        // get table acc index
        int accIndex = -1;
        ArrayList<ArrayList<String>> AllTableAccDetails = AccTable.GetAll();
        for (int i = 1; i < AllTableAccDetails.size(); i++)
        {
            String tableAccName = AllTableAccDetails.get(i).get(0);
            if (!tableAccName.equals(prevAccName))
            {
                continue;
            }
            accIndex = i;
        }
        // remove table row
        RemoveTableRow("", accIndex, allAccsIndex);
        // add updated
        InsertTableRow(accIndex, AccDetails, allAccsIndex);
    }

    public void RemoveTableRow(String accName, int accIndex, int allAccsIndex)
    {
        // check if acc exists in current table results
        if (accIndex == -0)
        {
            ArrayList<ArrayList<String>> AllTableAccDetails = AccTable.GetAll();
            for (int i = 0; i < AllTableAccDetails.size(); i++)
            {
                String curAccName = AllTableAccDetails.get(i).get(0);
                if (!curAccName.equals(accName))
                {
                    continue;
                }
                accIndex = i;
            }
            // not found in cur table results
            if (accIndex == -0)
            {
                accIndex = -1;
            }
        }
        // remove from current table results
        if (accIndex != -1)
        {
            // remove from current results
            AccTable.RemoveRow(accIndex+1);
            System.out.println("Removed from " + (accIndex+1));
            // show no accs text
            if (AccTable.GetAll().size() == 0)
            {
                // update no accs text
                String searchQuery = SearchInput.getText().toString();
                SetNoAccsText((searchQuery.equals("")) ? NO_ACCS : NO_SEARCH_RESULTS);
                // show no accs text
                SetNoAccsVisibility(View.VISIBLE);
            }
        }
        // update all accs list
        Accs.remove(allAccsIndex);
    }

    public void SetConnStatus(String status)
    {
        ConnStatusTv.setText(status);
        curConnStatus = status;
    }

    private void SortResults(ArrayList<ArrayList<String>> Results)
    {
//        System.out.println(selectedSortOptionIndex);
        switch (selectedSortOptionIndex)
        {
            case TIME_ADDED_SORT_OPTION_INDEX:
                SortResultsByTimeAdded(Results);
                break;

            case ALPHABETICAL_SORT_OPTION_INDEX:
                SortResultsAlphabetically(Results);
                break;

            case NUMBER_OF_FILLED_FIELDS_SORT_OPTION_INDEX:
                SortResultsByNumberOfFilledFields(Results);
                break;
        }
    }

    private void SortResultsByTimeAdded(ArrayList<ArrayList<String>> Results)
    {
        ArrayList<ArrayList<String>> SortedResults = new ArrayList<>();
        for (int i = 0; i < Accs.size(); i++)
        {
            // it's based on the premise that the acc name will be unique
            ArrayList<String> TimeAddedAccDetails = Accs.get(i);
            int timeAddedAccIndex = Results.indexOf(TimeAddedAccDetails);
            if (timeAddedAccIndex == -1)
            {
                continue;
            }
            SortedResults.add(Results.get(timeAddedAccIndex));
        }
        Results.clear();
        Results.addAll(SortedResults);
        if (sortOrder.equals(DESC_ORDER))
        {
            Collections.reverse(Results);
        }
    }

    private void SortResultsAlphabetically(ArrayList<ArrayList<String>> Results)
    {
        class CompareAlphabetically implements Comparator<ArrayList<String>>
        {
            @Override
            public int compare(ArrayList<String> a1, ArrayList<String> a2)
            {
                // sort alphabetically on current sort column
                String c1 = a1.get(sortColIndex);
                String c2 = a2.get(sortColIndex);
                return (sortOrder.equals(ASC_ORDER)) ? c1.compareTo(c2) : c2.compareTo(c1);
            }
        }
        Collections.sort(Results, new CompareAlphabetically());
    }

    private void SortResultsByNumberOfFilledFields(ArrayList<ArrayList<String>> Results)
    {
        class CompareByNumberOfFilledFields implements Comparator<ArrayList<String>>
        {
            @Override
            public int compare(ArrayList<String> a1, ArrayList<String> a2)
            {
                int n1 = Collections.frequency(a1, "-");
                int n2 = Collections.frequency(a2, "-");
                return (sortOrder.equals(ASC_ORDER)) ? n2 - n1 : n1 - n2;
            }
        }
        Collections.sort(Results, new CompareByNumberOfFilledFields());
    }

    private void UpdateResults(String searchQuery)
    {
        // check casing
        searchQuery = (MatchSearchCaseCheckbox.isChecked()) ? searchQuery : searchQuery.toLowerCase();
        // get results
        ArrayList<ArrayList<String>> SearchResultAccsDetails = new ArrayList<>();
        for (int i = 0; i < Accs.size(); i++)
        {
            ArrayList<String> AccDetails = Accs.get(i);
            for (int j = 0; j < 2; j++)
            {
                String accDetail = AccDetails.get(j);
                // check casing
                accDetail = (MatchSearchCaseCheckbox.isChecked()) ? accDetail : accDetail.toLowerCase();
                if (!accDetail.contains(searchQuery))
                {
                    continue;
                }
                SearchResultAccsDetails.add(AccDetails);
                break;
            }
        }
        if (selectedSortOptionIndex > 0)
        {
            // sort results
            SortResults(SearchResultAccsDetails);
        }
        else if (sortOrder.equals(DESC_ORDER))
        {
            Collections.reverse(SearchResultAccsDetails);
        }
        // clear table
        AccTable.Clear();
        if (SearchResultAccsDetails.size() > 0)
        {
            // hide no accs text
            SetNoAccsVisibility(View.GONE);
            // add results
            AccTable.AddRows(SearchResultAccsDetails, maskAllPwds);
        }
        else
        {
            // update no accs text
            SetNoAccsText((searchQuery.equals("")) ? NO_ACCS : NO_SEARCH_RESULTS);
            // show no accs text
            SetNoAccsVisibility(View.VISIBLE);
        }
    }

    public String[] GetSelectedRowAccDetails()
    {
        return AccTable.GetSelectedRowAccDetails();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        ViewHandler.MainActivityRef = null;
        // screen rotation
        if (isChangingConfigurations())
        {
            Toast.makeText(this, "onDestroy called due to screen rotation", Toast.LENGTH_SHORT).show();
        }
        // app exit
        else
        {
            // send close msg
            Client.SendCloseSocketMsg();
        }
    }
}