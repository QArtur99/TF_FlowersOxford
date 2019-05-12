package com.artf.flowersoxford;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

public class PredictAsyncTask extends AsyncTask<Integer, Integer, Integer> {

    private MainActivityInt mainActivity;
    private ImageClassifier imageClassifier;
    private Bitmap bitmap;


    PredictAsyncTask(MainActivityInt mainActivity, Bitmap bitmap, ImageClassifier imageClassifier) {
        this.mainActivity = mainActivity;
        this.imageClassifier = imageClassifier;
        this.bitmap = bitmap;
    }

    @Override
    protected Integer doInBackground(Integer... params) {
        Bitmap resized_image = ImageUtils.processBitmap(bitmap, 224);
        imageClassifier.classifyFrame(resized_image);

        Log.e("Sign Language", "PREDICTED LABEL : " + imageClassifier.prediction);
        return 0;
    }

    @Override
    protected void onPostExecute(Integer result) {
        mainActivity.onPredictPostExecute();
    }

}
