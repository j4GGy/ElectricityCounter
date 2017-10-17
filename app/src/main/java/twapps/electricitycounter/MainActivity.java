package twapps.electricitycounter;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    //TODO: export
    //TODO: widget + reminder

    private CounterData counterData;
    private File storageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //setup data backend
        storageFile = new File(getFilesDir(), "counter_data.csv");
        counterData = new CounterData(storageFile);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addNewEntry();
            }
        });

        ListView listView = (ListView) findViewById(R.id.listView);
        CounterDataAdapter adapter = new CounterDataAdapter(counterData);
        listView.setAdapter(adapter);
    }

    private void addNewEntry() {
        AddEntryDialog dialog = new AddEntryDialog();
        dialog.setCallback(new AddEntryDialog.ResultCallback() {
            @Override
            public void onPositiveResult(double value, Date date) {
                counterData.add(new CounterData.Item(date.getTime(), value));
                counterData.saveDataToFile(storageFile);
                ((CounterDataAdapter) ((ListView) findViewById(R.id.listView)).getAdapter()).notifyDataSetChanged();
            }

            @Override
            public void onNegativeResult() {}
        });
        dialog.show(getFragmentManager(), "tag");

    }

    private class CounterDataAdapter extends BaseAdapter {

        private CounterData counterData;
        private DateFormat dateFormat;
        private DateFormat timeFormat;

        public CounterDataAdapter(CounterData data) {
            this.counterData = data;
            dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
            timeFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
        }

        @Override
        public int getCount() {
            return this.counterData.size();
        }

        @Override
        public CounterData.Item getItem(int position) {
            return this.counterData.get(position);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.item_list, parent, false);
            }

            CounterData.Item item = getItem(position);

            Date date = new Date(item.timestamp);

            TextView tv = (TextView) convertView.findViewById(R.id.date);
            tv.setText(dateFormat.format(date));
            tv = (TextView) convertView.findViewById(R.id.time);
            tv.setText(timeFormat.format(date));
            tv = (TextView) convertView.findViewById(R.id.value);
            tv.setText(String.format(Locale.getDefault(), "%.2f", item.value));

            //TODO: tap on entry to remove

            return convertView;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }
}
