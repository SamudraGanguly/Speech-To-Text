package com.gangulytpoint.speechtotext;

import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SaveDialogFragment extends DialogFragment {
    MainActivity ob;
    EditText et;
    TextView tvSave, tvCancel;
    File file;
    String text;
    SaveDialogFragment(MainActivity ob, String text) {
        this.ob = ob;
        this.text = text;
    }
    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup container, Bundle savedInstanceState) {
        layoutInflater = getLayoutInflater();
        return layoutInflater.inflate(R.layout.savedialogfragment, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        et = view.findViewById(R.id.et);
        tvSave = view.findViewById(R.id.savebutton);
        tvCancel = view.findViewById(R.id.btn);
        tvSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    File f = new File(ob.getFilesDir() + "/SpeechToText");
                    if (!f.exists())
                        Files.createDirectory(Paths.get(f.getPath()));
                    String name = et.getText().toString().trim();
                    if (name.equals("")) {
                        Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_LONG).show();
                    }
                    else {
                        file = new File(f + "/" + name + ".txt");
                        if (file.exists())
                            Toast.makeText(getContext(), "File already exists", Toast.LENGTH_SHORT).show();
                        else {
                            Files.createFile(Paths.get(file.getPath()));
                            FileWriter fileWriter = new FileWriter(file, true);
                            fileWriter.write(text);
                            fileWriter.flush();
                            fileWriter.close();
                            Toast.makeText(getContext(), "File saved successfully", Toast.LENGTH_LONG).show();
                        }
                    }
                }
                catch (Exception e) {
                    Toast.makeText(getContext(), e.toString(), Toast.LENGTH_SHORT).show();
                }
                finally {
                    dismiss();
                }
            }
        });
        tvCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
    }
}
