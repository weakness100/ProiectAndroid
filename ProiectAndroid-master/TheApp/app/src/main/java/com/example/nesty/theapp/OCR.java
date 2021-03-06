package com.example.nesty.theapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.SparseArray;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by nesty on 11/2/2017.
 */

public class OCR extends AppCompatActivity {


    private Context _context = null;
    private static final String TAG = OCR.class.getSimpleName();
    private int nr=0;
    public OCR()
    {
    }
    static{ System.loadLibrary("opencv_java"); }





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        _context = getApplicationContext(); //activity has a context now
    }


    @Override
    public void onStart() {
        super.onStart();
        Intent intent = getIntent();
        String bitmapFilePath = intent.getStringExtra("bitmapFilePath");
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inSampleSize = 2;
        options.inScreenDensity = DisplayMetrics.DENSITY_LOW;
        Bitmap bitmap = BitmapFactory.decodeFile(bitmapFilePath, options);
        crop(bitmap);


        finish();
    }

    public static Bitmap rotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    private void debugSaveBitmapToFile(Bitmap bitmap)
    {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = timeStamp + ".jpg";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_DCIM);
        File image = null;
        try {
            image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(image.getAbsolutePath());
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out); // bmp is your Bitmap instance
            // PNG is a lossless format, the compression factor (100) is ignored
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private static Mat crop1(Mat matImage, Rect rect)
    {
        return matImage.submat(rect.y, rect.y + rect.height, rect.x, rect.x + rect.width);
    }
    void SaveToText(String temp)
    {


        File root = getExternalFilesDir(Environment.DIRECTORY_DCIM);


        try {

            if (!root.exists()) {
                root.mkdirs();
            }
            File gpxfile= new File(root,nr +".txt");
            nr++;
            FileWriter writer = new FileWriter(gpxfile);
            writer.append(temp);
            writer.flush();
            writer.close();
            //Toast.makeText(ok,"Saved", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void crop(Bitmap bitmap)
    {

        Mat matImage = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC3);
        Utils.bitmapToMat(bitmap, matImage);
        Mat gray = new Mat(matImage.size(), CvType.CV_8UC1);
        Imgproc.cvtColor(matImage, gray, Imgproc.COLOR_RGB2GRAY, 4);
        Mat edge = new Mat();
        Mat dst = new Mat();
        Imgproc.Canny(gray, edge, 100, 200);
        Imgproc.cvtColor(edge, dst, Imgproc.COLOR_GRAY2RGBA, 4);
        Bitmap resultBitmap1 = Bitmap.createBitmap(dst.cols(), dst.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(dst, resultBitmap1);

        Mat bw = new Mat();
        Imgproc.threshold(edge, bw, 128, 1,Imgproc.THRESH_OTSU);

        Imgproc.medianBlur(bw, bw, 5);


        Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_OPEN, new Size(5, 5));
        Imgproc.dilate(bw, bw, kernel,  new Point(0,1), 19);
        Imgproc.erode(bw, bw, kernel,  new Point(0,1), 3);
        Imgproc.dilate(bw, bw, kernel,  new Point(0,1), 5);

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Mat hierarchy = new Mat();


        Imgproc.findContours(bw,contours,hierarchy,Imgproc.RETR_LIST,Imgproc.CHAIN_APPROX_SIMPLE);
        ArrayList<Rect> boundingRects = new ArrayList<Rect>();

        for(MatOfPoint contour: contours)
        {
            Rect boundingRect = Imgproc.boundingRect(contour);
            boundingRects.add(boundingRect);


        }
        ArrayList<Mat> croppedImages = new ArrayList<Mat>();
        ArrayList<Mat> croImages = new ArrayList<Mat>();
        Mat ma = new Mat();
        int w=0;
        float x1=0;
        float y1=0;
        float x2=0;
        float y2=0;
        for(Rect rect : boundingRects)
        {
            if(rect.height>500 && rect.width>500) {
                x2=x1;
                y2=y1;
                x1=rect.x;
                y1=rect.y;
                Mat croppedImage = crop1(matImage, rect);
                croppedImages.add(croppedImage);
            }

        }
        List<Mat> croppedImage2=croppedImages;
        int kk=0;
        for(Mat mat2 : croppedImage2)
        {
            Mat mat4=mat2;
            if(x2>x1 && kk==0)
            {
                for(Mat mat3:croppedImage2)
                {
                    mat4=mat3;
                }
                Mat mat5 =mat2;
                mat2=mat4;
                mat4=mat5;
                kk++;
            }
        }
        for(Mat mat : croppedImage2)
        {

            Bitmap resultBitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(mat,resultBitmap);

            String text = convertBitmapToText(resultBitmap);



                    debugSaveBitmapToFile(resultBitmap);
                    SaveToText(text);
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("detectedText", text);
                    setResult(Activity.RESULT_OK, resultIntent);





        }

    }


    private String convertBitmapToText(Bitmap bitmap) {




        TextRecognizer textRecognizer = new TextRecognizer.Builder(_context).build();
        try {
            if (!textRecognizer.isOperational()) {
                new AlertDialog.
                        Builder(_context).
                        setMessage("Text recognizer could not be set up on your device").show();

            }

            Frame frame = new Frame.Builder().setBitmap(bitmap).build();
            SparseArray<TextBlock> origTextBlocks = textRecognizer.detect(frame);
            List<TextBlock> textBlocks = new ArrayList<>();
            for (int i = 0; i < origTextBlocks.size(); i++) {
                TextBlock textBlock = origTextBlocks.valueAt(i);
                textBlocks.add(textBlock);
            }
            Collections.sort(textBlocks, new Comparator<TextBlock>() {
                @Override
                public int compare(TextBlock o1, TextBlock o2) {
                    int diffOfTops = o1.getBoundingBox().top - o2.getBoundingBox().top;
                    int diffOfLefts = o1.getBoundingBox().left - o2.getBoundingBox().left;
                    if (diffOfTops != 0) {
                        return diffOfTops;
                    }
                    return diffOfLefts;
                }
            });

            StringBuilder detectedText = new StringBuilder();
            for (TextBlock textBlock : textBlocks) {
                if (textBlock != null && textBlock.getValue() != null) {
                    detectedText.append(textBlock.getValue());

                    //detectedText.append("\n");
                }
            }
            String b= new String();
            b=detectedText.toString();
            b=b.replaceAll("\n"," ");
            return b.toString();

        } finally {
            textRecognizer.release();
        }
    }
    public void retn()
    {
        nr=0;
    }


}
