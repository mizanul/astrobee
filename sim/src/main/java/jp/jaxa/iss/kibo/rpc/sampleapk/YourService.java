package jp.jaxa.iss.kibo.rpc.sampleapk;

import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;

import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;

// utils
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee
 */

public class YourService extends KiboRpcService {
    @Override
    protected void runPlan1(){
        // The mission starts.
        api.startMission();

        // Move to a point.
        Point point = new Point(10.9d, -9.92284d, 5.195d);
        Quaternion quaternion = new Quaternion(0f, 0f, -0.707f, 0.707f);
        api.moveTo(point, quaternion, false);

        // Get a camera image.
//        Mat image = api.getMatNavCam();

        /* *********************************************************************** */
        /* Write your code to recognize type and number of items in the each area! */
        /* *********************************************************************** */
        api.getActiveTargets();

        // When you recognize items, let’s set the type and number.
        //api.setAreaInfo(1, "item_name", 1);

        /* **************************************************** */
        /* Let's move to the each area and recognize the items. */
        /* **************************************************** */

        // When you move to the front of the astronaut, report the rounding completion.
        //api.reportRoundingCompletion();

        /* ********************************************************** */
        /* Write your code to recognize which item the astronaut has. */
        /* ********************************************************** */

        // Let's notify the astronaut when you recognize it.
        //api.notifyRecognitionItem();

        /* ******************************************************************************************************* */
        /* Write your code to move Astrobee to the location of the target item (what the astronaut is looking for) */
        /* ******************************************************************************************************* */

        // Take a snapshot of the target item.
        //api.takeTargetItemSnapshot();
    }

    @Override
    protected void runPlan2(){
        // write your plan 2 here
    }

    @Override
    protected void runPlan3(){
        // write your plan 3 here
    }

//    private static final String TAG = "AstrobeeObjectDetection";
//    private static final String MODEL_NAME = "model.tflite";

//    private void analyzeImage() {
//        try {
//            // Load TensorFlow Lite model
//            ObjectDetectorOptions options = ObjectDetectorOptions.builder()
//                    .setMaxResults(5)
//                    .build();
//
//            ObjectDetector objectDetector = ObjectDetector.createFromFileAndOptions(
//                    this, MODEL_NAME, options);
//
//            // Load image from assets
//            InputStream imageStream = getAssets().open("test_image.png");
//            Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
//
//            // Convert Bitmap to TensorImage
//            TensorImage tensorImage = TensorImage.fromBitmap(bitmap);
//
//            // Run object detection
//            List<Detection> results = objectDetector.detect(tensorImage);
//
//            // Count objects of a specific class (e.g., class ID 1)
//            int objectCount = 0;
//            for (Detection detection : results) {
//                if (detection.getCategories().get(0).getIndex() == 1) { // Replace with your specific class index
//                    objectCount++;
//                }
//            }
//
//            // Display the result
//            Toast.makeText(this, "Number of objects detected: " + objectCount, Toast.LENGTH_LONG).show();
//            Log.d(TAG, "Number of objects detected: " + objectCount);
//
//        } catch (IOException e) {
//            Log.e(TAG, "Error loading model or image", e);
//        }
//    }



}