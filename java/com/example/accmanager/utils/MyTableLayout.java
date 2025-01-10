package com.example.accmanager.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.example.accmanager.R;

import java.util.ArrayList;
import java.util.Arrays;

public class MyTableLayout extends TableLayout
{
    private ArrayList<String> UnmaskedPwds;
    private View PrevTableClickedCell;
    private int editAccIndex;
    private ClipboardManager Clipboard;

    public MyTableLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        UnmaskedPwds = new ArrayList<>();
        editAccIndex = -1;
        Clipboard = (ClipboardManager)getContext().getSystemService(Context.CLIPBOARD_SERVICE);
    }

    public ArrayList<ArrayList<String>> GetAll()
    {
        ArrayList<ArrayList<String>> AllAccDetails = new ArrayList<>();
        // first child is the header row
        for (int i = 1; i < getChildCount(); i++)
        {
            TableRow Row = (TableRow)getChildAt(i);
            String accName = ((TextView)Row.getChildAt(0)).getText().toString();
            String extraInfo = ((TextView)Row.getChildAt(1)).getText().toString();
            // table has 1 more row for the header
            String pwd = UnmaskedPwds.get(i-1);
            AllAccDetails.add(new ArrayList<>(Arrays.asList(accName, extraInfo, pwd)));
        }
        return AllAccDetails;
    }

    public void InsertRow(int index, ArrayList<String> AccDetails, boolean maskAllPwds)
    {
        // create row
        TableRow AccRow = new TableRow(getContext());
        AccRow.setWeightSum(3);
        // create cells
        for (int i = 0; i < 3; i++)
        {
            String accDetail = AccDetails.get(i);
            if (i == 2)
            {
                // store unmasked pwd
                if (index == -1) {
                    UnmaskedPwds.add(accDetail);
                }
                else {
                    // first child is the header row
                    UnmaskedPwds.add(index-1, accDetail);
                }
                // mask pwd
                if (maskAllPwds)
                {
                    accDetail = Cryptor.Mask(accDetail);
                }
            }
            TextView AccCellTv = CreateRowCell(accDetail, maskAllPwds);
            AccRow.addView(AccCellTv);
        }
        // TODO resize row height to max height column
        // add row to table
        if (index == -1)
        {
            addView(AccRow);
        }
        else
        {
            addView(AccRow, index);
        }
    }

    public void AddRow(ArrayList<String> AccDetails, boolean maskAllPwds)
    {
        InsertRow(-1, AccDetails, maskAllPwds);
    }

    public void AddRows(ArrayList<ArrayList<String>> AccDetailArr, boolean maskAllPwds)
    {
        for (ArrayList<String> AccDetails : AccDetailArr)
        {
            AddRow(AccDetails, maskAllPwds);
        }
    }

    private TextView CreateRowCell(String cellText, boolean maskAllPwds)
    {
        TextView AccCellTv = new TextView(getContext());
        AccCellTv.setLayoutParams(new TableRow.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        AccCellTv.setPadding(5, 5, 5, 5);
        AccCellTv.setBackgroundResource(R.drawable.table_cell_border_shape);
        AccCellTv.setText(cellText);
        AccCellTv.setTextSize(TypedValue.COMPLEX_UNIT_PT, 8);
        AccCellTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (PrevTableClickedCell != null)
                {
                    PrevTableClickedCell.setBackgroundResource(R.drawable.table_cell_border_shape);
                }
                view.setBackgroundColor(Color.rgb(216, 229, 255));
                PrevTableClickedCell = view;
                // allow edit
                View Row = ((View)view.getParent());
                editAccIndex = indexOfChild(Row);
            }
        });
        AccCellTv.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                TextView LongClickedCellTv = (TextView)view;
                TableRow Row = (TableRow)LongClickedCellTv.getParent();
                int rowIndex = ((TableLayout)Row.getParent()).indexOfChild(Row);
                // get data
                String data;
                // masked pwd cell
                if (maskAllPwds && Row.indexOfChild(LongClickedCellTv) == 2)
                {
                    // get unmasked pwd
                    // first table row is header
                    data = UnmaskedPwds.get(rowIndex-1);
                }
                else
                {
                    data = LongClickedCellTv.getText().toString();
                }
                // copy data
                Clipboard.setPrimaryClip(ClipData.newPlainText("text", data));
                return true;
            }
        });
        return AccCellTv;
    }
    public void RemoveRow(int index)
    {
        removeViewAt(index);
        // remove stored pwd
        // first child is the header row
        UnmaskedPwds.remove(index-1);
    }

    public void Clear()
    {
        // first child is the header row
        for (int i = getChildCount()-1; i >= 1; i--)
        {
            RemoveRow(i);
        }
    }

    public int GetSelectedRowAccIndex()
    {
        return editAccIndex;
    }

    public String[] GetSelectedRowAccDetails()
    {
        TableRow Row = (TableRow)getChildAt(editAccIndex);
        String accName = ((TextView)Row.getChildAt(0)).getText().toString();
        String extraInfo = ((TextView)Row.getChildAt(1)).getText().toString();
        // table has 1 more row for the header
        String pwd = UnmaskedPwds.get(editAccIndex-1);
        return new String[] { accName, extraInfo, pwd };
    }
}
