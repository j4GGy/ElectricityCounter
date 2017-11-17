package twapps.electricitycounter;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_EXPORT = 1;

    //TODO: export
    //TODO: widget + reminder

    private CounterData counterData;
    private File storageFile;
    private boolean isPortraitDisplay = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v("activity", "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

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

        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);
        isPortraitDisplay = size.x <= size.y;

        boolean portrait = isDisplayInPortraitMode();
        Log.v(TAG, "isDisplayInPortraitMode: " + portrait);
        findViewById(R.id.header_delta_time).setVisibility(portrait ? View.GONE : View.VISIBLE);
        findViewById(R.id.header_delta_energy).setVisibility(portrait ? View.GONE : View.VISIBLE);
        findViewById(R.id.header_energy_per_day).setVisibility(portrait ? View.GONE : View.VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.overflow_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_export:
                Intent saveIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                saveIntent.addCategory(Intent.CATEGORY_OPENABLE);
                saveIntent.setType("text/csv");
                DateFormat df = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.GERMANY);
                saveIntent.putExtra(Intent.EXTRA_TITLE, "electricity_data_"+df.format(Calendar.getInstance().getTime())+".csv");
                startActivityForResult(saveIntent, REQUEST_EXPORT);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_EXPORT:
                if(resultCode == RESULT_OK && data != null && data.getData() != null) {
                    final Uri uri = data.getData();
                    final Handler handler = new Handler();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Looper.prepare();
                            ParcelFileDescriptor pfd = null;
                            try {
                                pfd = getContentResolver().openFileDescriptor(uri, "w");
                            } catch (Exception e) {
                                Log.e(TAG, "Exception obtaining FileDescriptor from URI:");
                                e.printStackTrace();
                            }
                            FileOutputStream fileOutputStream = new FileOutputStream(pfd.getFileDescriptor());
                            if(counterData.saveDataToCSV(fileOutputStream)) {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "Exported to CSV", Toast.LENGTH_LONG).show();
                                    }
                                });
                            } else {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "Exporting to CSV failed", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }
                        }
                    }).start();
                }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void addNewEntry() {
        AddEntryDialog dialog = new AddEntryDialog();
        dialog.setCallback(new AddEntryDialog.ResultCallback() {
            @Override
            public void onPositiveResult(double value, Date date) {
                counterData.add(new CounterData.Item(date.getTime(), value));
                counterData.saveDataToFile(storageFile);
                updateList();
            }
            @Override
            public void onNegativeResult() {}
        });
        dialog.show(getFragmentManager(), "tag");
    }

    private boolean isDisplayInPortraitMode() {
        return isPortraitDisplay;
    }

    private void updateList() {
        ((CounterDataAdapter) ((ListView) findViewById(R.id.listView)).getAdapter()).notifyDataSetChanged();
    }

    private double getHourDifference(long t1, long t2) {
        return (t2 - t1) / 1000.0 / 3600.0;
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
            return this.counterData.get(this.counterData.size() - 1 - position);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.item_list, parent, false);
            }

            final CounterData.Item item = getItem(position);

            final Date date = new Date(item.timestamp);

            TextView tv = (TextView) convertView.findViewById(R.id.date);
            tv.setText(dateFormat.format(date));
            tv = (TextView) convertView.findViewById(R.id.time);
            tv.setText(timeFormat.format(date));
            tv = (TextView) convertView.findViewById(R.id.value);
            tv.setText(String.format(Locale.getDefault(), "%.2f", item.value));

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                    builder.setTitle("Löschen?")
                            .setMessage(String.format(Locale.getDefault(), "Wollen Sie wirklich diesen Eintrag löschen?\n\nZeitpunkt: %s %s\nZählerstand: %.2f",
                                    dateFormat.format(date), timeFormat.format(date), item.value))
                            .setNegativeButton("Nein", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .setPositiveButton("Ja", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    counterData.remove(position);
                                    counterData.saveDataToFile(storageFile);
                                    updateList();
                                    dialog.dismiss();
                                }
                            });
                    builder.create().show();
                }
            });

            if(isDisplayInPortraitMode()) {
                convertView.findViewById(R.id.delta_time).setVisibility(View.GONE);
                convertView.findViewById(R.id.delta_energy).setVisibility(View.GONE);
                convertView.findViewById(R.id.energy_per_day).setVisibility(View.GONE);
            } else {
                if(position == this.counterData.size() - 1) {
                    tv = (TextView) convertView.findViewById(R.id.delta_time);
                    tv.setVisibility(View.VISIBLE);
                    tv.setText("-");


                    tv = (TextView) convertView.findViewById(R.id.delta_energy);
                    tv.setVisibility(View.VISIBLE);
                    tv.setText("-");

                    tv = (TextView) convertView.findViewById(R.id.energy_per_day);
                    tv.setVisibility(View.VISIBLE);
                    tv.setText("-");
                } else {
                    double timeDifference = getHourDifference(getItem(position + 1).timestamp, item.timestamp);
                    double valueDifference = item.value - getItem(position + 1).value;

                    tv = (TextView) convertView.findViewById(R.id.delta_time);
                    tv.setVisibility(View.VISIBLE);
                    tv.setText(String.format(Locale.getDefault(), "%.2f", timeDifference));

                    tv = (TextView) convertView.findViewById(R.id.delta_energy);
                    tv.setVisibility(View.VISIBLE);
                    tv.setText(String.format(Locale.getDefault(), "%.2f", valueDifference));

                    tv = (TextView) convertView.findViewById(R.id.energy_per_day);
                    tv.setVisibility(View.VISIBLE);
                    tv.setText(String.format(Locale.getDefault(), "%.2f", 24.0 * valueDifference / timeDifference));
                }
            }

            return convertView;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }
    }
}
