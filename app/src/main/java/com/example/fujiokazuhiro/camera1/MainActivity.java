package com.example.fujiokazuhiro.camera1;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static android.R.attr.headerBackground;
import static android.R.attr.lines;
import static android.R.attr.process;
import static org.opencv.android.Utils.bitmapToMat;


public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private CameraBridgeViewBase mCameraView;
    private Mat mOutputFrame;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    mCameraView.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCameraView = (CameraBridgeViewBase)findViewById(R.id.camera_view);
        mCameraView.setCvCameraViewListener(this);

    }

    @Override
    public void onPause() {
        if (mCameraView != null) {
            mCameraView.disableView();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_4, this, mLoaderCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mCameraView != null) {
            mCameraView.disableView();
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        // Mat(int rows, int cols, int type)
        // rows(行): height, cols(列): width
        mOutputFrame = new Mat(height, width, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped() {
        mOutputFrame.release();
    }

//    public int p = 0;
//    public final int processingcycle = 3;
//    public Mat outputFrame;
//    @Override
//    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
//        p++;
//        Mat image = inputFrame.rgba();
//        Mat edge = new Mat();
//        Mat lines = new Mat();
//        if(p == 1) {
//            Imgproc.cvtColor(image, edge, Imgproc.COLOR_RGB2GRAY);
//            Imgproc.Canny(edge, edge, 80, 100);
//            Imgproc.HoughLinesP(edge, lines, 1, Math.PI / 180, 50, 100, 1000);
//            outputFrame = fncDrwLine(lines,image);
//        }else if(p == processingcycle){
//            p -= processingcycle;
//        }
//
//        return outputFrame;
//    }


    // ここに画像処理
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat image = inputFrame.rgba();
        Mat gray = new Mat(3, 3, CvType.CV_32FC2);
        Mat dst_bin = new Mat(3, 3, CvType.CV_32FC2);
        Mat hierarchy = new Mat(3, 3, CvType.CV_32FC2);
        Resources r = getResources();
        Bitmap bmp_frame = BitmapFactory.decodeResource(r, R.drawable.image);
//        bmp_frame = Bitmap.createScaledBitmap(bmp_frame, 640, 480, false);
        Mat mat = new Mat(3,3,CvType.CV_32FC2);
        Core.add(mat, image, image);
        Imgproc.pyrDown(image, image);
        Imgproc.pyrUp(image, image);
        Imgproc.cvtColor(image , gray, Imgproc.COLOR_RGB2GRAY);
        Imgproc.threshold(gray,dst_bin, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        Imgproc.findContours(dst_bin,contours,hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE  );
//        Imgproc.cvtColor(gray , gray, Imgproc.COLOR_GRAY2BGRA,4);
//        Imgproc.drawContours(gray, contours, -1,  new Scalar(255, 0, 0), 1);


//        for(int i = 0; i < contours.size(); i++){
//                    Imgproc.polylines(image, contours, true, new Scalar(0, 255, 0), 2);
//        }


        Log.d("tag_contours.size()", "" + contours.size());

        ArrayList<MatOfPoint2f> approx = new ArrayList<MatOfPoint2f>();
        ArrayList<MatOfPoint> out = new ArrayList<MatOfPoint>();

        //輪郭が四角形かどうかの判定
        for(int i = 0; i < contours.size(); i++) {
            MatOfPoint2f mop2f1 = new MatOfPoint2f();
            MatOfPoint2f mop2f2 =new MatOfPoint2f();
            MatOfPoint mop = new MatOfPoint();
            contours.get(i).convertTo(mop2f1, CvType.CV_32FC2);

            //直線近似
            Imgproc.approxPolyDP(mop2f1,mop2f2, 10.0, true);
            mop2f2.convertTo(mop, CvType.CV_32FC2);
            out.add(mop);
            approx.add(mop2f2);
            double area =Imgproc.contourArea(approx.get(i));
            Log.d("tag_area", "" + area);
            Log.d("tag_approx.size()", "" + approx.size());

            //近似が４点かつ面積が一定以上
            if(approx.size() == 4 && area > 200) {
                Imgproc.polylines(image, contours, true, new Scalar(0, 255, 0), 2);

//                try {
//                    Thread.sleep(1000);
//                }catch (InterruptedException e){}

//                approx.clear();
            }

        }
        return image;
    }


//    private Mat fncDrwLine(Mat lin,Mat img) {
//        double[] data;
//        Point pt1 = new Point();
//        Point pt2 = new Point();
//        for (int i = 0; i < lin.cols(); i++){
//            data = lin.get(0, i);
//            pt1.x = data[0];
//            pt1.y = data[1];
//            pt2.x = data[2];
//            pt2.y = data[3];
//            Log.d("log", "pt1.x : " + pt1.x + " pt1.y : " + pt1.y + "pt2.x : " + pt2.x + "pt2.y : " + pt2.y);
//            Imgproc.line(img, pt1, pt2, new Scalar(255, 0, 0), 10);
//        }
//        return img;
//    }

//    public void DrawLine(Mat image){
//        double[] data;
//        double rho, theta;
//        Point pt1 = new Point();
//        Point pt2 = new Point();
//        double x0, y0;
//
//        for (int i = 0; i == lines.cols(); i++){
//            data = lines.get(0, i);
//            rho = data[0];
//            theta = data[1];
//            double cosTheta = Math.cos(theta);
//            double sinTheta = Math.sin(theta);
//            pt1.x = rho / cosTheta;
//            pt1.y = 0;
//            pt2.x = 0;
//            pt2.y = rho / sinTheta;
//
//            Core.line(image, pt1, pt2, new Scalar(255, 0, 0), 1);
//        }
//    }
}