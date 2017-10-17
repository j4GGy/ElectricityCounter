package twapps.electricitycounter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

public class AddEntryDialog extends DialogFragment {

    private final static String TAG = AddEntryDialog.class.getSimpleName();

    private EditText editTextValue;
    private View contentView;

    public interface ResultCallback {
        void onPositiveResult(double value, Date date);
        void onNegativeResult();
    }

    private ResultCallback callback;

    public void setCallback(ResultCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = getActivity().getLayoutInflater();
        contentView = inflater.inflate(R.layout.dialog_add_entry, null);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        editTextValue = (EditText) contentView.findViewById(R.id.editTextValue);

        final Date date = new Date();

        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        ((TextView) contentView.findViewById(R.id.date)).setText(dateFormat.format(date));

        builder.setTitle("Dateneingabe")
                .setView(contentView)
                .setNegativeButton("Abbruch", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (callback != null) {
                            callback.onNegativeResult();
                        }
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("Speichern", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (callback != null) {
                            double value = -1.0;
                            try {
                                value = Double.parseDouble(editTextValue.getText().toString());
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                            callback.onPositiveResult(value, date);
                        }
                        dialog.dismiss();
                    }
                });

        return builder.create();
    }
}
