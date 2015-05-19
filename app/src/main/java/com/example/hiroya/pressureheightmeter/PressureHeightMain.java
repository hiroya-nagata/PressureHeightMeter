package com.example.hiroya.pressureheightmeter;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.List;


public class PressureHeightMain extends Activity implements SensorEventListener{

    SensorManager sensorManager;

    static final int filter_n = 250;
    float[] prevPressure;
    float currentPressure;

    boolean resetPrevPressureFlag; //trueなら過去のデータを現在の測定値でクリア

    boolean isZeroed; //基準点の測定が済んだかどうか
    float refPressure; //基準点の気圧
    float temperature; //気温

    TextView textPressure;
    TextView textHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pressure_height_main);

        final Button b = (Button)findViewById(R.id.button);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refPressure = currentPressure;
                isZeroed = true;
            }
        });

        final EditText temperatureEdit = (EditText)findViewById(R.id.editText);
        temperatureEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try{
                    temperature = Float.parseFloat(s.toString());
                }catch(NumberFormatException e){
                    //なにもしない
                }
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            @Override
            public void afterTextChanged(Editable s){}
        });

        textPressure = (TextView)findViewById(R.id.textPressure);
        textHeight = (TextView)findViewById(R.id.textHeight);
        textHeight.setText("?.?? m");

        prevPressure = new float[filter_n];
        temperature = 20.0f;
        isZeroed = false;
    }

    @Override
    protected void onResume(){
        super.onResume();
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        List<Sensor> sensores = sensorManager.getSensorList(Sensor.TYPE_PRESSURE);
        if(sensores.size() == 0){
            textPressure.setText("?.?? hPa");
            return;
        }
        sensorManager.registerListener(this, sensores.get(0), SensorManager.SENSOR_DELAY_GAME);

        resetPrevPressureFlag = true;
    }

    @Override
    protected void onPause(){
        super.onPause();
        if(sensorManager != null){
            sensorManager.unregisterListener(this);
            sensorManager = null;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent e){
        if(resetPrevPressureFlag){
            for(int i = 0; i < filter_n; i++)prevPressure[i] = e.values[0];
            resetPrevPressureFlag = false;
        }
        System.arraycopy(prevPressure, 0, prevPressure, 1, filter_n - 1);
        prevPressure[0] = e.values[0];

        float sum = 0.0f;
        for(int i = 0; i < filter_n; i++) {
            sum += prevPressure[i] * Math.pow(0.97f, i);
        }
        currentPressure = sum / 33.3169f;
        textPressure.setText(String.format("%.2f", currentPressure) + " hPa");

        if(isZeroed){ //基準点の測定が終わってたら高度を計算・表示
            double height;
            height = (Math.pow(refPressure / currentPressure, 1.0 / 5.257) - 1.0) * (temperature + 275.15) / 0.0065;
            if(Math.abs(height) > 1.0){
                textHeight.setText(String.format("%+.2f", height) + " m");
            }else{
                textHeight.setText(String.format("%+.2f", 100.0*height) + " cm");
            }
        }
   }
    @Override
    public void onAccuracyChanged(Sensor sen, int acc){}
}
