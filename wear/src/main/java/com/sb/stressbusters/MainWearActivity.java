package com.sb.stressbusters;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;

public class MainWearActivity extends WearableActivity implements SensorEventListener {
    private final int shortAverageCount = 10; // 10 seconds
    private final int longAverageCount = 300; // 5 minutes
    private final int longAverageGap = 60; // 1 minute
    private final int minAlarmRate = 80; // 100 bpm
    private final double longShortAlarmRatio = 0.25; // 20%

    private TextView mTextView;
    private TextView mTextViewShort;
    private TextView mTextViewLong;
    private ImageButton btnStart;
    private ImageButton btnPause;
    private Drawable imgStart;
    private SensorManager mSensorManager;
    private Sensor mHeartRateSensor;
    private int index;
    private ArrayList<Integer> measurements;
    private ArrayList<Integer> lastMeasurements;
    private boolean gotIt;
    private int longSum;
    private int shortSum;
    private double longAverage;
    private double shortAverage;
    private boolean registered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.heartRateText);
                mTextViewLong = (TextView) stub.findViewById(R.id.heartRateTextLong);
                mTextViewShort = (TextView) stub.findViewById(R.id.heartRateTextShort);
                btnStart = (ImageButton) stub.findViewById(R.id.btnStart);
                btnPause = (ImageButton) stub.findViewById(R.id.btnPause);

                btnStart.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        btnStart.setVisibility(ImageButton.GONE);
                        btnPause.setVisibility(ImageButton.VISIBLE);
                        // mTextView.setText("Please wait...");
                        startMeasure();
                    }
                });

                btnPause.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        btnPause.setVisibility(ImageButton.GONE);
                        btnStart.setVisibility(ImageButton.VISIBLE);
                        mTextView.setText("--");
                        stopMeasure();
                    }
                });

            }
        });

        setAmbientEnabled();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        registered = false;

    }

    private void startMeasure() {
        index = 0;
        gotIt = false;
        longSum = 0;
        longAverage = 0;
        shortSum = 0;
        shortAverage = 0;
        measurements = new ArrayList<Integer>();
        lastMeasurements = new ArrayList<Integer>();

        if (!registered) {
            registered = mSensorManager.registerListener(this, mHeartRateSensor, 1000000);
            Log.d("Sensor Status:", " Sensor registered: " + (registered ? "yes" : "no"));
        }

        mTextView.setText("please wait");
        gotIt = false;

    }

    private void stopMeasure() {
        gotIt = true;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float mHeartRateFloat = event.values[0];

        int mHeartRate = Math.round(mHeartRateFloat);
        measurements.add(mHeartRate);
        lastMeasurements.add(mHeartRate);
        shortSum += mHeartRate;

        while (measurements.size() > longAverageCount) {
            longSum -= measurements.get(0);
            measurements.remove(0);
        }

        if (measurements.size() > longAverageGap) {
            longSum += measurements.get(measurements.size() - longAverageGap);
            longAverage = longSum / (measurements.size() - longAverageGap);
            mTextViewLong.setText("" + longAverage);
        }

        while (lastMeasurements.size() > shortAverageCount) {
            shortSum -= lastMeasurements.get(0);
            lastMeasurements.remove(0);
        }

        shortAverage = shortSum / lastMeasurements.size();

        mTextViewShort.setText("" + shortAverage);
        mTextView.setText(Integer.toString(mHeartRate));

        if ((!gotIt) && (shortAverage > minAlarmRate) && (longAverage > 0) &&
                (shortAverage > longAverage * (1 + longShortAlarmRatio))) {
            Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            long[] vibrationPattern = {0, 5000};
            final int indexInPatternToRepeat = -1; //-1 - don't repeat
            vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);
        }

        Log.d("Date:", " " + shortAverage + " " + longAverage + " " + shortSum + " " + longSum);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

}
