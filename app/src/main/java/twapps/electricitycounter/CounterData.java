package twapps.electricitycounter;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.IllegalFormatException;
import java.util.List;

public class CounterData {

    public static final String TAG = CounterData.class.getSimpleName();

    public static class Item {
        private static final String SEPARATOR = ";";

        long timestamp;
        double value;

        public Item(long timestamp, double value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        public static String toString(Item item) {
            StringBuilder sb = new StringBuilder();
            sb.append(item.timestamp);
            sb.append(SEPARATOR);
            sb.append(item.value);
            return sb.toString();
        }

        public String toString() {
            return toString(this);
        }

        public static Item valueOf(String s) throws IOException{
            Item item = new Item(-1,-1);

            String[] data = s.split(SEPARATOR);
            if(data.length != 2) {
                throw new IOException("Invalid CounterData.Item format: " + s);
            } else {
                try {
                    item.timestamp = Long.parseLong(data[0]);
                    item.value = Double.parseDouble(data[1]);
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid CounterData.Item format: " + s);
                }
            }
            return item;
        }
    }

    private File dataFile;
    private List<Item> data;

    public CounterData(File storage) {
        data = new ArrayList<>();
        dataFile = storage;

        loadDataFromFile(dataFile);
    }

    public int size() {
        return (data == null) ? 0 : data.size();
    }

    public void add(Item item) {
        if(data == null) {
            data = new ArrayList<>();
        }

        for(int i=0; i<data.size(); i++){
            if(data.get(i).timestamp > item.timestamp) {
                data.add(i, item);
                return;
            }
        }
        data.add(item);
    }

    public Item get(int index) {
        return (data == null) ? null : data.get(index);
    }

    public void remove(int index) {
        if(data != null) {
            data.remove(index);
        }
    }

    public void loadDataFromFile(File fileIn) {
        BufferedReader bufferedReader;

        data.clear();

        try {
            bufferedReader = new BufferedReader(new FileReader(fileIn));

            while(bufferedReader.ready()) {
                data.add(Item.valueOf(bufferedReader.readLine()));
            }

            bufferedReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (IOException e1) {
            e1.printStackTrace();
        }

    }

    public void saveDataToFile(File fileOut) {
        Log.v(TAG, "saveDateToFile()");
        BufferedWriter bufferedWriter;

        if(!fileOut.exists()) {
            try {
                if(!fileOut.createNewFile()) {
                    Log.e(TAG, "Output file does not exist but can't create either: " + fileOut.getPath());
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            bufferedWriter = new BufferedWriter(new FileWriter(fileOut));
            for(Item item : data) {
                bufferedWriter.write(item.toString());
                bufferedWriter.write(System.lineSeparator());
            }
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
