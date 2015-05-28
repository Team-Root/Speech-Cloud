package edu.spu.teamroot.voicecloud;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public class ExclusionActivity extends ActionBarActivity {

    /*
     * Member variables
     */

    ListView mListView;
    Button mAddButton;
    ArrayAdapter<String> adapter;
    List<String> wordArrayList;

    /*
     * Methods
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        this.setTheme(R.style.ExclusionTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exclusion);

        mListView = (ListView) findViewById(R.id.exclusionList);

        wordArrayList = new ArrayList<String>(ExclusionList.getInstance().excludeList.keySet());
        adapter = new ArrayAdapter<String>(this, R.layout.exclusion_row, wordArrayList);
        mListView.setAdapter(adapter);

        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1,
                                    int position, long arg3) {
                removeItemFromList(position);
            }
        });

        mAddButton = (Button) findViewById(R.id.add_button);
        mAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openAdd();
            }
        });
    }

    protected void removeItemFromList(final int position) {
        final int deletePosition = position;

        AlertDialog.Builder alert = new AlertDialog.Builder(
                ExclusionActivity.this);

        alert.setTitle("Delete");
        alert.setMessage("Do you want delete this item?");
        alert.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String word = wordArrayList.get(deletePosition);
                wordArrayList.remove(word);
                ExclusionList.getInstance().removeWord(word);
                adapter.notifyDataSetChanged();
                adapter.notifyDataSetInvalidated();

            }
        });
        alert.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // TODO Auto-generated method stub
                dialog.dismiss();
            }
        });

        alert.show();

    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    void openAdd()
    {
        AlertDialog.Builder addalert = new AlertDialog.Builder(ExclusionActivity.this);
        final LayoutInflater inflater = this.getLayoutInflater();

        addalert.setTitle("Add Words");

        // Set up the input
        final EditText et = new EditText(ExclusionActivity.this);
        et.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 30);
        et.setPadding(50, 50, 50, 50);
        addalert.setView(et);

        addalert.setPositiveButton("ADD", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                String input = et.getText().toString();
                if (null != input && input.length() > 0) {

                    // Split up input string into words
                    String[] words = input.toLowerCase().split(" ");

                    // Add words entered to exclusion list
                    for (String word : words) {
                        // Clean up words and remove punctuation
                        word = word.replaceAll("[,.!\n ]", "");
                        if (!word.isEmpty()) {
                            ExclusionList.getInstance().addWord(word);
                            wordArrayList.add(word);
                        }
                    }

                    adapter.notifyDataSetChanged();
                }
            }
        });
        addalert.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

            }
        });
        addalert.show();
    }
}