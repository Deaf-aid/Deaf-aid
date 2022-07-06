package com.example.deafaidtrans;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    private List<String> listDevice = new ArrayList<String>();
    private TextView textDevice;
    private ArrayAdapter<String> adapterDevice;

    private List<String> listFre = new ArrayList<String>();
    private List<String> listIDrangeL = new ArrayList<String>();
    private List<String> listIDrangeR = new ArrayList<String>();
    private TextView textFre;

    private Spinner spinnertext;
    private EditText textmsg;
    public static String transmsg;

    private Button transmitt_button;
    private ToneGenerator tone;
    public static double resonantFre = 19.4*1000;
    public static double leftFre = 19*1000;
    public static double rightFre = 20*1000;

    public void onCreate(Bundle savedlnstanceState) {
        super.onCreate(savedlnstanceState);
        setContentView(R.layout.activity_main);
        textmsg = (EditText) findViewById(R.id.msg);
        transmsg = textmsg.getText().toString();
        transmitt_button = findViewById(R.id.Transmit);
        tone = new ToneGenerator();

        listDevice.add("Huawei P40"); listFre.add("19.9"); listIDrangeL.add("19"); listIDrangeR.add("20");
        listDevice.add("Huawei P40 Pro"); listFre.add("27.48"); listIDrangeL.add("19"); listIDrangeR.add("20");
        listDevice.add("Samsung Galaxy S8"); listFre.add("19.4"); listIDrangeL.add("19"); listIDrangeR.add("20");
        listDevice.add("Google Pixel 4"); listFre.add("23.1"); listIDrangeL.add("19"); listIDrangeR.add("20");

        textDevice = (TextView) findViewById(R.id.DeviceName);
        textFre = (TextView) findViewById(R.id.ResonantFre);
        spinnertext = (Spinner) findViewById(R.id.spinner1);
        adapterDevice = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, listDevice);
        adapterDevice.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnertext.setAdapter(adapterDevice);
        spinnertext.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                textDevice.setText(adapterDevice.getItem(position));
                textFre.setText("Resonant frequency: " + listFre.get(position) + " kHz");
                resonantFre = Double.valueOf(listFre.get(position))*1000;
                leftFre = Double.valueOf(listIDrangeL.get(position))*1000;
                rightFre = Double.valueOf(listIDrangeR.get(position))*1000;
                parent.setVisibility(View.VISIBLE);
            }

            public void onNothingSelected(AdapterView<?> argO) {
                textDevice.setText("NONE");
                argO.setVisibility(View.VISIBLE);
            }
        });

        spinnertext.setOnTouchListener(new Spinner.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                v.setVisibility(View.INVISIBLE);
                return false;
            }
        });

        spinnertext.setOnFocusChangeListener(new Spinner.OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                v.setVisibility(View.VISIBLE);
            }
        });
        ButterKnife.bind(this);
    }

    @OnClick(R.id.Transmit)
    void onClickCheck(View view) {
        transmsg = textmsg.getText().toString();
        boolean is01 = is01only(transmsg);
        if (is01) {
            tone.startTone();
            transmitt_button.setText("Start Transmitting");
            Toast.makeText(MainActivity.this, "The massage is transmitted once.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(MainActivity.this, "Please input 0 or 1 only!", Toast.LENGTH_LONG).show();
        }
    }

    private boolean is01only(String input){
        Pattern pattern = Pattern.compile("[0-1]*");
        return pattern.matcher(input).matches();
    }
}
