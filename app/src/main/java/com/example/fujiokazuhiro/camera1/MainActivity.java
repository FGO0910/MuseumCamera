package com.example.fujiokazuhiro.camera1;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;

import android.graphics.Camera;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import static android.R.attr.headerBackground;
import static android.R.attr.lines;
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

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
        Mat image = inputFrame.rgba();
        Mat edge = new Mat();
        Mat lines = new Mat();
        Imgproc.cvtColor(image , edge, Imgproc.COLOR_RGB2GRAY);
        Imgproc.Canny(edge, edge, 80, 100);
        Imgproc.HoughLinesP(edge, lines, 1, Math.PI / 180 , 50, 100 ,1000);
        fncDrwLine(lines,image);
        return image;
    }

//    @Override
//    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
////        try{
////            Thread.sleep(5);
////            }catch(InterruptedException e){}
//        Mat src = inputFrame.rgba();
//        Mat hierarchy=Mat.zeros(new Size(5,5), CvType.CV_8UC1);
//        Mat invsrc=src.clone();
//        Core.bitwise_not(src, invsrc);
////        Imgproc.cvtColor(image, edge, Imgproc.COLOR_RGB2GRAY);
////            Imgproc.Canny(edge, edge, 80, 100);
//        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
//        Imgproc.findContours(invsrc,contours,hierarchy, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE  );
////        for(int i = 0; i < contours.size(); i++) {
////            ArrayList<MatOfPoint2f> approx = new ArrayList<MatOfPoint2f>();
////            Imgproc.approxPolyDP(approx.get(i), approx.get(i), 50.0, true);
////        }
////        Log.d("log", lines.cols() + "");
////        Log.d("log", lines.rows() + "");
////            Mat print  = fncDrwLine(lines, image);
////        double area =Imgproc.contourArea(lines);
////        if(lines.size() == 4 && area > 300){
////            Log.d("succesfull!");
////        }
////        Log.d("log", lines.size() + "");
//        return src;
//    }
    private Mat fncDrwLine(Mat lin,Mat img) {
        double[] data;
        Point pt1 = new Point();
        Point pt2 = new Point();
        for (int i = 0; i < lin.cols(); i++){
            data = lin.get(0, i);
//            double rho = data[0];
//            double theta = data[1];
//
//            double cosTheta = Math.cos(theta);
//            double sinTheta = Math.sin(theta);
            pt1.x = data[0];
            pt1.y = data[1];
            pt2.x = data[2];
            pt2.y = data[3];
            Log.d("log", "pt1.x : " + pt1.x + " pt1.y : " + pt1.y + "pt2.x : " + pt2.x + "pt2.y : " + pt2.y);
            Imgproc.line(img, pt1, pt2, new Scalar(255, 0, 0), 10);
        }
        return img;
    }

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