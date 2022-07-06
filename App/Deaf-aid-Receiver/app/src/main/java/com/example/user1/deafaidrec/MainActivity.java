package com.example.user1.deafaidrec;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Environment;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements SensorEventListener,View.OnClickListener {

    SensorManager manager;
    Sensor sensor;
    TextView allTextView;
    int stopflag = 0;
    int maxpoints = 500;
    int varn = (int) maxpoints/10;
    int count = 0;
    int upcount, downcount = 0;

    // time threshold
    int upcountmax = 20;
    int downcountmax = 200;
    int IDrangecountmin = 20;
    int[] onegap = new int[]{0, 30};
    int[] zerogap = new int[]{40, 60};
    int[] SOFgap = new int[]{100, 150};

    boolean upcounting, downcounting = false;
    boolean IDrangeflag = false;
    int f01, l01 = 0;
    int axisFlag1 = 0;
    int axisFlag2 = 1;

    // point num of mean filter
    int n = 10;
    double threshold = 100;
    double[] xyValues = new double[maxpoints];
    double[] xyMeanValues = new double[maxpoints];
    double[] xValues = new double[maxpoints];
    double[] yValues = new double[maxpoints];
    double[] zValues = new double[maxpoints];
    double[] zeroneValues = new double[maxpoints];

    LineChart mChart1;
    LineChart mChart2;
    LineChart mChart3;
    LineChart mChart4;

    YAxis Yaxis1, Yaxis1R;
    YAxis Yaxis2, Yaxis2R;
    YAxis Yaxis3, Yaxis3R;
    YAxis Yaxis4, Yaxis4R;

    String[] names = new String[]{"Axis-1", "Axis-2", "Combined Channel","01 Channel"};
    int[] colors = new int[]{Color.RED, Color.GREEN, Color.BLUE,Color.BLACK};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        allTextView = (TextView)findViewById(R.id.allValue);
        Button stopbutton = (Button) findViewById(R.id.buttonofstop);
        stopbutton.setOnClickListener((View.OnClickListener) this);

        manager = (SensorManager)getSystemService(SENSOR_SERVICE);
        sensor = manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);

        mChart1 = (LineChart) findViewById(R.id.lineChart1);
        mChart2 = (LineChart) findViewById(R.id.lineChart2);
        mChart3 = (LineChart) findViewById(R.id.lineChart3);
        mChart4 = (LineChart) findViewById(R.id.lineChart4);

        Yaxis1R = mChart1.getAxisRight();
        Yaxis1R.setEnabled(false);

        Yaxis2R = mChart2.getAxisRight();
        Yaxis2R.setEnabled(false);

        Yaxis3R = mChart3.getAxisRight();
        Yaxis3R.setEnabled(false);

        Yaxis4R = mChart4.getAxisRight();
        Yaxis4R.setEnabled(false);
        Yaxis4 = mChart4.getAxisLeft();

        mChart1.setDescription("");
        mChart1.setData(new LineData());
        mChart2.setDescription("");
        mChart2.setData(new LineData());
        mChart3.setDescription("");
        mChart3.setData(new LineData());
        mChart4.setDescription("");
        mChart4.setData(new LineData());
    }

    @Override
    public void onClick(View v) {
        stopflag = 1-stopflag;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(stopflag == 1) {
            double xValue = event.values[0];
            double yValue = event.values[1];
            double zValue = event.values[2];
            double[] xyzValue = {xValue, yValue, zValue};

            xValues = addData(xValues,xValue);
            yValues = addData(yValues,yValue);
            zValues = addData(zValues,zValue);

            // select axis
            if (count == 0) {
                Log.d("Select Axis","on");
                double xVar = variancen(Arrays.copyOfRange(xValues, xValues.length-varn, xValues.length));
                double yVar = variancen(Arrays.copyOfRange(yValues, yValues.length-varn, yValues.length));
                double zVar = variancen(Arrays.copyOfRange(zValues, zValues.length-varn, zValues.length));
                double[] vars = {xVar, yVar, zVar};
                double minuVar = minValue(vars);
                if (minuVar == xVar) {
                    axisFlag1 = 1;
                    axisFlag2 = 2;
                } else if (minuVar == yVar) {
                    axisFlag1 = 0;
                    axisFlag2 = 2;
                } else if (minuVar == zVar) {
                    axisFlag1 = 0;
                    axisFlag2 = 1;
                }
            }

            double xyValue = Math.abs(xyzValue[axisFlag1] * xyzValue[axisFlag2]);
            xyValues = addData(xyValues, xyValue);

            double xyMean = meann(xyValues);
            xyMeanValues = addData(xyMeanValues,xyMean);

            if (count == 100) {
                threshold = maxEntropy(Arrays.copyOfRange(xyMeanValues, xyMeanValues.length-varn, xyMeanValues.length));
            }

            int Value01;
            if (xyMean > threshold){
                Value01 = 1;
            }
            else{
                Value01 = 0;
            }
            f01 = l01;
            l01 = Value01;
            zeroneValues = addData(zeroneValues,Value01);

            // IDrange detection
            if (f01 == 0 && l01 == 1 && !IDrangeflag){
                upcounting = true;
            }
            if (f01 == 1 && l01 == 0 && !IDrangeflag){
                if (IDrangecountmin < upcount){
                    IDrangeflag = true;
                }
                upcounting = false;
                upcount = 0;
            }

            // data detection
            if (f01 == 0 && l01 == 1 && IDrangeflag){
                upcounting = true;
            }
            if (f01 == 1 && l01 == 0 && IDrangeflag){
                if (upcount > upcountmax){
                    if (downcounting) {
                        downcounting = false;
                        if (downcount > onegap[0] && downcount <= onegap[1])
                            allTextView.append("1");
                        else if (downcount > zerogap[0] && downcount <= zerogap[1])
                            allTextView.append("0");
                        else if (downcount > SOFgap[0] && downcount <= SOFgap[1])
                            allTextView.append("SOF");
                    }
                    else {
                        downcounting = true;
                        downcount = 0;
                    }
                }
                upcounting = false;
            }
            if (downcount > downcountmax){
                allTextView.append("EOF");
                IDrangeflag = false;
                downcounting = false;
                downcount = 0;
            }

            if (upcounting){upcount += 1;}
            if (downcounting){downcount += 1;}

            // plot
            LineData data1 = mChart1.getLineData();
            LineData data2 = mChart2.getLineData();
            LineData data3 = mChart3.getLineData();
            LineData data4 = mChart4.getLineData();

            if (data1 != null && data2 != null && data3 != null) {
                count = count +1;
                ILineDataSet set1 = data1.getDataSetByIndex(0);
                ILineDataSet set2 = data2.getDataSetByIndex(0);
                ILineDataSet set3 = data3.getDataSetByIndex(0);
                ILineDataSet set4 = data4.getDataSetByIndex(0);
                if (set1 == null) {
                    set1 = createSet(names[0], colors[0]);
                    data1.addDataSet(set1);
                }
                if (set2 == null) {
                    set2 = createSet(names[1], colors[1]);
                    data2.addDataSet(set2);
                }
                if (set3 == null) {
                    set3 = createSet(names[2], colors[2]);
                    data3.addDataSet(set3);
                }
                if (set4 == null) {
                    set4 = createSet(names[3], colors[3]);
                    data4.addDataSet(set4);
                }

                data1.addEntry(new Entry(set1.getEntryCount(), (float) xyzValue[axisFlag1]), 0);
                data1.notifyDataChanged();
                data2.addEntry(new Entry(set2.getEntryCount(), (float) xyzValue[axisFlag2]), 0);
                data2.notifyDataChanged();
                data3.addEntry(new Entry(set3.getEntryCount(), (float) xyMean), 0);
                data3.notifyDataChanged();
                data4.addEntry(new Entry(set4.getEntryCount(), Value01), 0);
                data4.notifyDataChanged();

                if (count%5 == 0) {
                    mChart1.notifyDataSetChanged();
                    mChart1.setVisibleXRangeMaximum(maxpoints);
                    mChart1.moveViewToX(data1.getEntryCount());

                    mChart2.notifyDataSetChanged();
                    mChart2.setVisibleXRangeMaximum(maxpoints);
                    mChart2.moveViewToX(data2.getEntryCount());

                    mChart3.notifyDataSetChanged();
                    mChart3.setVisibleXRangeMaximum(maxpoints);
                    mChart3.moveViewToX(data3.getEntryCount());

                    mChart4.notifyDataSetChanged();
                    mChart4.setVisibleXRangeMaximum(maxpoints);
                    mChart4.moveViewToX(data4.getEntryCount());
                    Yaxis4.setAxisMaxValue((float) 1);
                    Yaxis4.setAxisMinValue((float) 0);
                }
            }
            if (count > 500) {
                data1 = new LineData();
                data2 = new LineData();
                data3 = new LineData();
                data4 = new LineData();
                Log.d("count","count = 0");
                count = 0;
            }
        }
    }

    private LineDataSet createSet(String label, int color) {
        LineDataSet set = new LineDataSet(null, label);
        set.setLineWidth(1.5f);
        set.setColor(color);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        return set;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        manager.unregisterListener(this);
    }

    private double[] addData(double[] data, double newdata){
        for (int i = 0;i < n-1; i++){
            data[i] = data[i+1];
        }
        data[n-1] = newdata;
        return data;
    }

    public double meann(double[] data){
        double result, datasum = 0;
        if (data.length > 1) {
            for (int i = 0; i < n; i++) {
                datasum = datasum + data[i];
            }
            result = datasum / n;
        }
        else result = data[0];
        return result;
    }

    public double meanData(double[] data){
        double result, datasum = 0;
        for (int i = 0; i < data.length; i++) {
            datasum = datasum + data[i];
        }
        result = datasum / data.length;
        return result;
    }

    public double variancen(double[] x) {
        int m=x.length;
        double dAve = meann(x);
        double dVar = 0;
        for(int i = 0;i < m; i++){
            dVar += (x[i]-dAve)*(x[i]-dAve);
        }
        return dVar/(m-1);
    }

    public double minValue(double[] data){
        double minData = data[0];
        for(int i = 1; i < data.length; i++){
            if(data[i] < minData){
                minData = data[i];
            }
        }
        return minData;
    }

    public double maxValue(double[] data){
        double maxData = data[0];
        for(int i = 1; i < data.length; i++){
            if(data[i] > maxData){
                maxData = data[i];
            }
        }
        return maxData;
    }

    public double maxEntropy(double[] data){
        double r = (double) 0.01/varn;
        double threshold = maxValue(data)+r;
        int K = (int) Math.ceil((threshold-r)/r);
        Log.d("K", String.valueOf(K));
        double[] p = new double[K];
        for(int i = 0; i < data.length; i++){
            for(int j = 0; j < K; j++){
                if(data[i] == 0) p[0]+=1/data.length;
                if(data[i] > j*r && data[i] <= (j+1)*r){
                    p[j]+=1/data.length;
                    break;
                }
            }
        }
        if (K > 3) {
            double[] H = new double[K];
            for (int k = 0; k < K; k++) {
                double H0 = 0;
                double H1 = 0;
                double p0_total = meanData(Arrays.copyOfRange(p, 0, k + 1)) * (k + 1);
                for (int i = 0; i < k + 1; i++) {
                    H0 += p[i] / p0_total * Math.log(p[i] / p0_total) / Math.log(2);
                }
                double p1_total = meanData(Arrays.copyOfRange(p, k + 1, K)) * (K - k - 1);
                for (int i = k + 1; i < K; i++) {
                    H1 += p[i] / p1_total * Math.log(p[i] / p1_total) / Math.log(2);
                }
                H[k] = -H0 - H1;
            }

            double maxH = maxValue(H);
            threshold = (Arrays.binarySearch(H, maxH) + 1) * r;
        }
        Log.d("threshold", String.valueOf(threshold));
        return threshold;
    }

    private LineData newLineData(double[] data){
        LineData setData = new LineData();
        for (int i = 0; i < maxpoints; i++){
            Log.i("TAG", String.valueOf(i));
            setData.addEntry(new Entry(i, (float) data[i]),0);
        }
        Log.i("TAG", "newLineData: "+data.length);
        return setData;
    }

}