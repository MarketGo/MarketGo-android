package com.application.CloudVision;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.application.BuildConfig;
import com.application.R;
import com.application.activity.GameResultActivity;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.TextAnnotation;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.lang.String;

import static java.security.AccessController.getContext;

/* Google Cloud Vision call  & use
 * 지수 작성
 *
 * 20190603 first try: key error & diverse error.....
 * 20190604 3rd try: it works but recognize fail
 */
public class CloudVision extends AppCompatActivity {
    private static final String CLOUD_VISION_API_KEY = BuildConfig.API_KEY;
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    private static final int MAX_LABEL_RESULTS = 10;

    private static final String TAG = CloudVision.class.getSimpleName();
    private Bitmap handwriteBitmap; // 아가들 글씨
    private String recogRes = new String("");

    private TextView visionRes;
    private ImageView handwriteImage;

    String quizString1;
    String quizString2;
    String mSpeakTerm;

    protected void onCreate(Bundle savedInstanceSate) {
        super.onCreate(savedInstanceSate);
        setContentView(R.layout.activity_cloudvision);

        Intent intent = getIntent();
        quizString1 = intent.getStringExtra("quizString1");
        quizString2 = intent.getStringExtra("quizString2");
        mSpeakTerm = intent.getStringExtra("speakTerm");

        byte[] arr = intent.getByteArrayExtra("handwriteImage");
        handwriteBitmap = BitmapFactory.decodeByteArray(arr, 0, arr.length);

        visionRes = findViewById(R.id.visionResult);
        handwriteImage = findViewById(R.id.handwriteImage);

        recogRes = callCloudVision(handwriteBitmap);

        handwriteImage.setImageBitmap(handwriteBitmap);

        hideNavigationBar();


    }

    /* Google Cloud Vision */
    private Vision.Images.Annotate prepareAnnotationRequest(Bitmap bitmap) throws IOException {

        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        VisionRequestInitializer requestInitializer =

                new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                    /**
                     * We override this so we can inject important identifying fields into the HTTP
                     * headers. This enables use of a restricted cloud platform API key.
                     */

                    @Override
                    protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                            throws IOException {
                        super.initializeVisionRequest(visionRequest);

                        String packageName = getPackageName();
                        visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                        String sig = PackageManagerUtils.getSignature(getPackageManager(), packageName);

                        visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
                    }
                };

        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
        builder.setVisionRequestInitializer(requestInitializer);

        Vision vision = builder.build();

        BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                new BatchAnnotateImagesRequest();

        batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
            AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

            // Add the image
            Image base64EncodedImage = new Image();

            // Convert the bitmap to a JPEG
            // Just in case it's a format that Android understands but Cloud Vision
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();

            // Base64 encode the JPEG
            base64EncodedImage.encodeContent(imageBytes);
            annotateImageRequest.setImage(base64EncodedImage);

            // add the features we want - 글자 인식
            annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                Feature textDetection = new Feature();
                textDetection.setType("TEXT_DETECTION");
                textDetection.setMaxResults(MAX_LABEL_RESULTS);
                add(textDetection);
            }});

            // Add the list of one thing to the request
            add(annotateImageRequest);
        }});

        Vision.Images.Annotate annotateRequest =
                vision.images().annotate(batchAnnotateImagesRequest);

        // Due to a bug: requests to Vision API containing large images fail when GZipped.
        annotateRequest.setDisableGZipContent(true);
        Log.d(TAG, "created Cloud Vision request object, sending request");

        return annotateRequest;
    }


    /* lable detection method */
    private static class LableDetectionTask extends AsyncTask<Object, Void, String> {
        private final WeakReference<CloudVision> mActivityWeakReference;
        private Vision.Images.Annotate mRequest;

        LableDetectionTask(CloudVision activity, Vision.Images.Annotate annotate) {
            mActivityWeakReference = new WeakReference<>(activity);
            mRequest = annotate;
        }

        @Override
        protected String doInBackground(Object... params) {
            try {
                Log.d(TAG, "created Cloud Vision request object, sending request");

                BatchAnnotateImagesResponse response = mRequest.execute();
                return convertResponseToString(response);

            } catch (GoogleJsonResponseException e) {
                Log.d(TAG, "failed to make API request because " + e.getContent());

            } catch (IOException e) {
                Log.d(TAG, "failed to make API request because of other IOException " +
                        e.getMessage());
            }

            return "인식실패";
        }

        protected void onPostExecute(String result) {
            CloudVision activity = mActivityWeakReference.get();

            if (activity != null && !activity.isFinishing()) {
                TextView handwriteRecogRes = activity.findViewById(R.id.visionResult);
                handwriteRecogRes.setText(result);
            }
        }
    }

    private static String convertResponseToString(BatchAnnotateImagesResponse response) {
        String message;
        List<EntityAnnotation> labels = response.getResponses().get(0).getTextAnnotations();

        if (labels != null) {
            message = labels.get(0).getDescription();
        } else {
            message = "nothing";
        }

        return message.toLowerCase().replace("\n", "");
    }

    /* CloudVision 호출 부*/
    private String callCloudVision(final Bitmap bitmap) {

        // Switch text to loading
        // Do the real work in an async task, because we need to use the network anyway
        String res = "";

        try {
            AsyncTask<Object, Void, String> labelDetectionTask = new LableDetectionTask(this, prepareAnnotationRequest(bitmap));

            try {
                res = labelDetectionTask.execute().get();

            } catch (Exception e) {
                e.printStackTrace();
            }


        } catch (IOException e) {
            Log.d(TAG, "failed to make API request because of other IOException " +
                    e.getMessage());
        }
        return res;
    }

    // 이전으로 돌아가기
    public void onBackButtonClick(View v) {
        finish();
    }

    /* 다음 화살표*/
    public void goToResultButtonClick(View v) {

        Intent intent = new Intent(CloudVision.this, GameResultActivity.class);
        intent.putExtra("quizString1", quizString1);
        intent.putExtra("quizString2", quizString2);
        intent.putExtra("speakTerm", mSpeakTerm);
        intent.putExtra("writeTerm", recogRes);

        startActivity(intent);
    }

    private void hideNavigationBar() {
        int uiOptions = getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;
        newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }
}
