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
//    @Override
//    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
//        Mat image = inputFrame.rgba();
//        Mat gray = new Mat(3, 3, CvType.CV_32FC2);
//        Mat dst_bin = new Mat(3, 3, CvType.CV_32FC2);
//        Mat hierarchy = new Mat(3, 3, CvType.CV_32FC2);
//        Resources r = getResources();
//        Bitmap bmp_frame = BitmapFactory.decodeResource(r, R.drawable.image);
////        bmp_frame = Bitmap.createScaledBitmap(bmp_frame, 640, 480, false);
//        Mat mat = new Mat(3,3,CvType.CV_32FC2);
//        Core.add(mat, image, image);
//        Imgproc.pyrDown(image, image);
//        Imgproc.pyrUp(image, image);
//        Imgproc.cvtColor(image , gray, Imgproc.COLOR_RGB2GRAY);
//        Imgproc.threshold(gray,dst_bin, 0, 255, Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
//        ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
//        Imgproc.findContours(dst_bin,contours,hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE  );
////        Imgproc.cvtColor(gray , gray, Imgproc.COLOR_GRAY2BGRA,4);
////        Imgproc.drawContours(gray, contours, -1,  new Scalar(255, 0, 0), 1);
//
//
////        for(int i = 0; i < contours.size(); i++){
////                    Imgproc.polylines(image, contours, true, new Scalar(0, 255, 0), 2);
////        }
//
//
//        Log.d("tag_contours.size()", "" + contours.size());
//
//        ArrayList<MatOfPoint2f> approx = new ArrayList<MatOfPoint2f>();
//        ArrayList<MatOfPoint> out = new ArrayList<MatOfPoint>();
//
//        //輪郭が四角形かどうかの判定
//        for(int i = 0; i < contours.size(); i++) {
//            MatOfPoint2f mop2f1 = new MatOfPoint2f();
//            MatOfPoint2f mop2f2 =new MatOfPoint2f();
//            MatOfPoint mop = new MatOfPoint();
//            contours.get(i).convertTo(mop2f1, CvType.CV_32FC2);
//
//            //直線近似
//            Imgproc.approxPolyDP(mop2f1,mop2f2, 10.0, true);
//            mop2f2.convertTo(mop, CvType.CV_32FC2);
//            out.add(mop);
//            approx.add(mop2f2);
//            double area =Imgproc.contourArea(approx.get(i));
//            Log.d("tag_area", "" + area);
//            Log.d("tag_approx.size()", "" + approx.size());
//
//            //近似が４点かつ面積が一定以上
//            if(approx.size() == 4 && area > 200) {
//                Imgproc.polylines(image, contours, true, new Scalar(0, 255, 0), 2);
//
////                try {
////                    Thread.sleep(1000);
////                }catch (InterruptedException e){}
//
////                approx.clear();
//            }
//
//        }
//        return image;
//    }

    //画像合成お試し
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat mat1 = new Mat();
        Mat mat2 = new Mat();
        Resources r = getResources();
        Bitmap bmp_frame1 = BitmapFactory.decodeResource(r, R.drawable.image);
        bmp_frame1 = Bitmap.createScaledBitmap(bmp_frame1, 1280, 720, false);
        Bitmap bmp_frame2 = BitmapFactory.decodeResource(r, R.drawable.image);
        bmp_frame2 = Bitmap.createScaledBitmap(bmp_frame2, 500, 500, false);
        Utils.bitmapToMat(bmp_frame1, mat1);
        Utils.bitmapToMat(bmp_frame2, mat2);

        paste(mat1, mat2, 10, 10, 500, 500);

        return mat1;


    }

    public void paste(Mat dst, Mat src, int x, int y, int width, int height){
        int w, h, u, v, px, py;
        Mat resized_img = new Mat();
        Size size = new Size(width, height);
        Imgproc.resize(src, resized_img, size);

        if (x >= dst.cols() || y >= dst.rows()){}
        else{
            if(x >= 0){
                w = Math.min(dst.cols() - x, resized_img.cols());
                u = 0;
            }else{
                w = Math.min(Math.max(resized_img.cols() + x, 0), dst.cols());
                u = Math.min(-x, resized_img.cols() - 1);
            }
            if(y >= 0){
                h = Math.min(dst.rows() - y, resized_img.rows());
                v = 0;
            }else{
                h = Math.min(Math.max(resized_img.rows() + y, 0), dst.rows());
                v = Math.min(-y, resized_img.rows() - 1);
            }

            px = Math.max(x, 0);
            py = Math.max(y, 0);

            Mat roi_dst = new Mat(dst, new Rect(px, py, w, h));
            Mat roi_resized = new Mat(resized_img, new Rect(u, v, w, h));
            roi_resized.copyTo(roi_dst);
        }
    }



}
