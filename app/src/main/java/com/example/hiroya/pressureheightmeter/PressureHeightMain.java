package com.example.hiroya.pressureheightmeter;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;


public class PressureHeightMain extends Activity implements SensorEventListener{

    private SensorManager sensorManager;

    private float currentPressure;

    private boolean isZeroed; //基準点の測定が済んだかどうか
    private float refPressure; //基準点の気圧
    private float temperature; //気温
    private float filterStr; //フィルタの効き

    private TextView textPressure;
    private TextView textHeight;
    private TextView textFilterStr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pressure_height_main);

        final Button b = (Button) findViewById(R.id.button);
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

        ((SeekBar)findViewById(R.id.filterStrength)).setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser){
                filterStr = seekBar.getProgress() / 100.0f;
                textFilterStr.setText(seekBar.getProgress() + "%");
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar){}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar){}
        });

        textPressure = (TextView)findViewById(R.id.textPressure);
        textHeight = (TextView)findViewById(R.id.textHeight);
        textHeight.setText("?.?? m");
        textFilterStr = (TextView)findViewById(R.id.textFilterStr);

        temperature = 20.0f;
        filterStr = 0.0f;
        isZeroed = false;
    }

    @Override
    protected void onResume(){
        super.onResume();
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_PRESSURE);
        if(sensors.size() == 0){
            textPressure.setText("?.?? hPa");
            return;
        }
        sensorManager.registerListener(this, sensors.get(0), SensorManager.SENSOR_DELAY_GAME);
        Toast.makeText(this, "Sensor ON", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause(){
        super.onPause();
        if(sensorManager != null){
            sensorManager.unregisterListener(this);
            Toast.makeText(this, "Sensor OFF", Toast.LENGTH_SHORT).show();
            sensorManager = null;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent e){
        currentPressure = e.values[0] * (1.0f - filterStr) + currentPressure * filterStr;
        textPressure.setText(String.format("%.2f", currentPressure) + " hPa");

        if(isZeroed){ //基準点の測定が終わってたら高度を計算・表示
            double height;
            height = (Math.pow(refPressure / currentPressure, 1.0 / 5.257) - 1.0) * (temperature + 275.15) / 0.0065;
            if(Math.abs(height) > 1.0){
                textHeight.setText(String.format("%+.2f", height) + " m");
            }else{
                textHeight.setText(String.format("%+.2f", 100.0 * height) + " cm");
            }
        }
   }
    @Override
    public void onAccuracyChanged(Sensor sen, int acc){}
}
