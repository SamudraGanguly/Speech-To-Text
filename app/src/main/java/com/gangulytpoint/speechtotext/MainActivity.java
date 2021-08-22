package com.gangulytpoint.speechtotext;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    File file;
    FileWriter fileWriter;
    SpeechRecognizer speech;
    Intent i;
    EditText tv;
    AlertDialog alert,sd;
    Button btn,btnOpen,btnNew;
    ImageView mic;
    View mainView;
    boolean listOpened;
    int c;
    public static String s;
    String reqFile, fileName;
    boolean switchedOn;
    static {
        s = "";
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        enableRunTimePermission();
        tv = findViewById(R.id.editText);
        tv.setCursorVisible(true);
        tv.setText("");
        btn = findViewById(R.id.button);
        btnOpen = findViewById(R.id.button2);
        btnNew = findViewById(R.id.button3);
        mic = findViewById(R.id.imageView);
        mic.setColorFilter(Color.TRANSPARENT);
        listOpened = false;
        switchedOn = false;
        mainView = findViewById(R.id.mainView);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (speech != null) {
                    speech.cancel();
                    speech.destroy();
                    mic.setColorFilter(Color.TRANSPARENT);
                    switchedOn = false;
                }
                String text = tv.getText().toString().trim();
                if (text.equals(""))
                    Toast.makeText(MainActivity.this, "Please write something", Toast.LENGTH_LONG).show();
                else {
                    if (fileName != null) {
                        try {
                            File f = new File(getFilesDir() + "/SpeechToText");
                            if (!f.exists())
                                Files.createDirectory(Paths.get(f.getPath()));
                            file = new File(f + "/" + fileName);
                            if (!file.exists())
                                Files.createFile(Paths.get(file.getPath()));
                            fileWriter = new FileWriter(file, false);
                            fileWriter.write(text);
                            fileWriter.flush();
                            fileWriter.close();
                            Toast.makeText(MainActivity.this, "File saved successfully", Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                        transaction.replace(R.id.frag, new SaveDialogFragment(MainActivity.this, text)).commit();
                    }
                }
            }
        });
        btnOpen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fileName = null;
                open();
            }
        });
        btnNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fileName = null;
                tv.setText("");
            }
        });
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("GIVE RECORD AUDIO PERMISSION TO GOOGLE APP");
        builder.setMessage("In order to use this app, your Google App must have the Record Audio permission");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:com.google.android.googlequicksearchbox"));
                startActivityForResult(intent,1);
            }
        });
        AlertDialog.Builder shareDelete = new AlertDialog.Builder(MainActivity.this);
        shareDelete.setTitle("CHOOSE");
        shareDelete.setPositiveButton("SHARE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                try {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    File f = new File(getFilesDir() + "/SpeechToText/" + reqFile);
                    if (f.exists()) {
                        shareIntent.setType("application/txt");
                        Context context = getApplicationContext();
                        Uri fileUri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", f);
                        shareIntent.putExtra(Intent.EXTRA_STREAM,fileUri);
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        shareIntent.setType("*/*");
                        startActivity(Intent.createChooser(shareIntent,"Share File..."));
                    }
                }
                catch (Exception e) {
                    Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        });
        shareDelete.setNegativeButton("DELETE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                try {
                    File f = new File(getFilesDir() + "/SpeechToText/" + reqFile);
                    f.delete();
                    open();
                }
                catch(Exception e) {
                    Toast.makeText(MainActivity.this, "Failed to delete file\n" + e, Toast.LENGTH_LONG).show();
                }
            }
        });
        alert = builder.create();
        alert.setCancelable(false);
        alert.setCanceledOnTouchOutside(false);
        sd = shareDelete.create();
        sd.setCanceledOnTouchOutside(false);
        mic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (switchedOn) {
                    if (speech != null) {
                        speech.cancel();
                        speech.destroy();
                        switchedOn = false;
                        mic.setColorFilter(Color.TRANSPARENT);
                    }
                }
                else {
                    if(SpeechRecognizer.isRecognitionAvailable(getApplicationContext())) {
                        speech = SpeechRecognizer.createSpeechRecognizer(getApplication());
                        go();
                        switchedOn = true;
                        mic.setColorFilter(Color.RED);
                    }
                    else
                        Toast.makeText(MainActivity.this, "Your device does not support Speech Recognition", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    void go() {
        c = 0;
        i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,"ABC");
        i.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,5);

        speech.setRecognitionListener(new MyListener());
        speech.startListening(i);
    }
    class MyListener implements RecognitionListener {
        @Override
        public void onBeginningOfSpeech() {

        }
        @Override
        public void onBufferReceived(byte[] b) {
            Log.d("ABCD","Inside onBufferReceived()");
        }

        @Override
        public void onRmsChanged(float f) {
            Log.d("ABCD","Inside onRmsChanged()");
            c++;
            if(c>=500)
            {
                go();
            }
        }

        @Override
        public void onPartialResults(Bundle bbb) {
            Log.d("ABCD","Inside onPartialResults()");
        }

        @Override
        public void onEndOfSpeech() {
            Log.d("ABCD","Inside onEndOfSpeech()");
        }

        @Override
        public void onError(int n) {
            Log.d("ABCD","Inside onError(). ERROR "+n);
            if(n==6||n==8) {
                Toast.makeText(MainActivity.this, "TRY LATER", Toast.LENGTH_LONG).show();
                speech.cancel();
                speech.destroy();
                speech = null;
            }
            if(n==3) {
                if(!MainActivity.this.isFinishing())
                    alert.show();
            }
            if(n==9)
                enableRunTimePermission();
            else {
                go();
            }
        }

        @Override
        public void onReadyForSpeech(Bundle bbb) {
            Log.d("ABCD","Inside onReadyForSpeech()");
        }

        @Override
        public void onResults(Bundle results) {
            Log.d("ABCD","Inside onResults()");
            String str = "";
            ArrayList data = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            str=data.get(0).toString();
            str = str.replace("Answer","ANSWER");
            str = str.replace("রিকার","ঋ-কার");
            tv.append(str + " ");
            go();
        }

        public void onEvent(int n, Bundle bbb) {
            Log.d("ABCD","Inside onEvent()");
        }
    }
    public void enableRunTimePermission() {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(MainActivity.this, "AUDIO PERMISSION IS NEEDED", Toast.LENGTH_LONG).show();
            } else {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            }
        }
    }
    public void open() {
        try {
            tv.setText(null);
            File f = new File(getFilesDir() + "/SpeechToText");
            String[] fileNames = f.list();
            ArrayList<String> arrayList = new ArrayList<>();
            if (fileNames == null) {
                Toast.makeText(MainActivity.this, "There is no text file", Toast.LENGTH_LONG).show();
            }
            else {
                for (String name : fileNames) {
                    if (name.endsWith(".txt")) {
                        arrayList.add(name);
                    }
                }
                LayoutInflater inflater = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
                View v = inflater.inflate(R.layout.activity_list,null);
                ArrayAdapter adapter = new ArrayAdapter(MainActivity.this, android.R.layout.simple_list_item_1, arrayList);
                ListView listView = v.findViewById(R.id.listView);
                listView.setAdapter(adapter);
                setContentView(v);
                listOpened = true;
                listView.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
                listView.setItemsCanFocus(true);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        try {
                            listOpened = false;
                            fileName = arrayList.get(i);
                            File file = new File(getFilesDir() + "/SpeechToText/" + fileName);
                            FileReader fileReader = new FileReader(file);
                            BufferedReader br = new BufferedReader(fileReader);
                            String str;
                            str = br.readLine();
                            setContentView(mainView);
                            while (str != null) {
                                tv.append(str + "\n");
                                str = br.readLine();
                            }
                        } catch (Exception e) {
                            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
                listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                        if (!MainActivity.this.isFinishing()) {
                            reqFile = arrayList.get(i);
                            sd.show();
                        }
                        return true;
                    }
                });
            }
        }
        catch (Exception e) {
            Log.d("NPException",e.toString());
            Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_LONG).show();
        }
    }
    @Override
    public void onBackPressed() {
        if (listOpened) {
            setContentView(mainView);
            listOpened = false;
        }
        else {
            super.onBackPressed();
        }
    }
    @Override
    public void onStop() {
        if (speech != null) {
            speech.cancel();
            speech.destroy();
            speech = null;
            mic.setColorFilter(Color.TRANSPARENT);
            switchedOn = false;
        }
        super.onStop();
    }
    @Override
    public void onDestroy() {
        if (speech != null) {
            speech.cancel();
            speech.destroy();
            speech = null;
        }
        super.onDestroy();
        System.exit(0);
    }
}