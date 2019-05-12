package com.artf.flowersoxford;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.MPPointF;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements MainActivityInt, OnSeekBarChangeListener,
        OnChartValueSelectedListener {

    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;
    private final RectF mOnValueSelectedRectF = new RectF();
    Button button, searchButton;
    Bitmap bitmap;
    ImageView imageView;

    private HorizontalBarChart chart;
    private ImageClassifier classifier;
    private Typeface tfRegular;
    private Typeface tfLight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        button = findViewById(R.id.camera_button);
        imageView = findViewById(R.id.camera_image);
        searchButton = findViewById(R.id.search_button);

        try {
            classifier = new ImageClassifier(this);
            classifier.useCPU();
        } catch (IOException e) {
            Log.e("Flowers Oxford", "FAILED TO INIT MODEL " + e.getMessage());
        }

        button.setOnClickListener(view -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, REQUEST_CAMERA);

        });

        searchButton.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_GALLERY);
        });

        loadChart();
    }

    private void loadChart() {
        tfRegular = Typeface.createFromAsset(getAssets(), "OpenSans-Regular.ttf");
        tfLight = Typeface.createFromAsset(getAssets(), "OpenSans-Light.ttf");

        chart = findViewById(R.id.chart1);
        chart.setOnChartValueSelectedListener(this);
        // chart.setHighlightEnabled(false);

        chart.setDrawBarShadow(false);
        chart.setDrawValueAboveBar(true);
        chart.getDescription().setEnabled(false);

        chart.setMaxVisibleValueCount(5);
        chart.setPinchZoom(false);

        // draw shadows for each bar that show the maximum value
        // chart.setDrawBarShadow(true);

        chart.setDrawGridBackground(false);
        XAxis xl = chart.getXAxis();
        xl.setPosition(XAxis.XAxisPosition.BOTTOM);
        xl.setTypeface(tfLight);
        xl.setDrawAxisLine(true);
        xl.setDrawGridLines(false);
        xl.setGranularity(10f);
        xl.setTextSize(12);
        xl.setValueFormatter(newFormat());

        YAxis yl = chart.getAxisLeft();
        yl.setTypeface(tfLight);
        yl.setDrawAxisLine(true);
        yl.setDrawGridLines(true);
        yl.setAxisMinimum(0f);

        YAxis yr = chart.getAxisRight();
        yr.setTypeface(tfLight);
        yr.setDrawAxisLine(true);
        yr.setDrawGridLines(false);
        yr.setAxisMinimum(0f);

        chart.setFitBars(true);
        chart.animateY(2500);

        Legend l = chart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setFormSize(0.01f);
        l.setXEntrySpace(0.01f);

        loadData();
    }

    public ValueFormatter newFormat() {
        return new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                List<Recognition> valuesPred = classifier.topKPred;
                if(valuesPred.isEmpty()) {
                    String valueString = String.valueOf((int) value + 1);
                    return classifier.getClass2Name().get(valueString);
                }else{
                    return valuesPred.get((int) value/10).title;
                }
            }
        };
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CAMERA || requestCode == REQUEST_GALLERY) {
                switch (requestCode) {
                    case REQUEST_CAMERA:
                        Bitmap bmp = (Bitmap) data.getExtras().get("data");
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();

                        if (bmp != null)
                            bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);

                        byte[] byteArray = stream.toByteArray();
                        bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                        break;
                    case REQUEST_GALLERY:
                        try {
                            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), data.getData());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                }


                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                    predict(bitmap);
                }
            }
        } else {
            imageView.setImageResource(R.drawable.nopic);
        }
    }

    public void predict(final Bitmap bitmap) {
        new PredictAsyncTask(this, bitmap, classifier).execute();
    }

    public void onPredictPostExecute() {
        loadData();
    }

    public void loadData() {
        setData(5, 100);
        chart.setFitBars(true);
        chart.invalidate();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

    private void setData(int count, float range) {

        float barWidth = 9f;
        float spaceForBar = 10f;
        ArrayList<BarEntry> values = new ArrayList<>();
        List<Recognition> valuesPred = classifier.topKPred;

        if(valuesPred.isEmpty()){
            for (int i = 0; i < count; i++) {
                float val = (float) (Math.random() * range);
                values.add(new BarEntry(i * spaceForBar, val));
            }
        }else {
            for (int i = 0; i < valuesPred.size(); i++) {
                values.add(new BarEntry(i * spaceForBar, valuesPred.get(i).confidence));
            }
        }

        BarDataSet set1;

        if (chart.getData() != null && chart.getData().getDataSetCount() > 0) {
            set1 = (BarDataSet) chart.getData().getDataSetByIndex(0);
            set1.setValues(values);
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();
        } else {
            set1 = new BarDataSet(values, "");
            set1.setDrawIcons(false);
            set1.setDrawValues(true);
            set1.setBarBorderWidth(1.f);

            ArrayList<IBarDataSet> dataSets = new ArrayList<>();
            dataSets.add(set1);

            BarData data = new BarData(dataSets);
            data.setValueTextSize(10f);
            data.setDrawValues(true);
            data.setValueTypeface(tfLight);
            data.setBarWidth(barWidth);
            chart.setData(data);
        }

    }

    public void saveToGallery() {
//        saveToGallery(chart, "HorizontalBarChartActivity");
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onValueSelected(Entry e, Highlight h) {

        if (e == null)
            return;

        RectF bounds = mOnValueSelectedRectF;
        chart.getBarBounds((BarEntry) e, bounds);

        MPPointF position = chart.getPosition(e, chart.getData().getDataSetByIndex(h.getDataSetIndex())
                .getAxisDependency());

        Log.i("bounds", bounds.toString());
        Log.i("position", position.toString());

        MPPointF.recycleInstance(position);
    }

    @Override
    public void onNothingSelected() {

    }
}
