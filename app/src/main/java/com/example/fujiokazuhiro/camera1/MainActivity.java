package com.example.fujiokazuhiro.camera1;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {

    // プレビュー用テクスチャーID
    private static final int PREVIEW_TEXTURE_ID = 1;

    // OpenCVライブラリロード済みフラグ
    private boolean OpenCVLoadFlg = false;

    // カメラ
    private Camera myCamera;

    // カメラビュー用のサーフェイス
    private SurfaceView mySurfaceView;

    // 地磁気・加速度センサー 
    private SensorManager mySensor;

    // 地磁気・加速度センサー情報
    private static final int MATRIX_SIZE = 16;
    private static final int DIMENSION = 3;
    private float[] magneticValues = new float[DIMENSION];
    private float[] accelerometerValues = new float[DIMENSION];
    private float[] orientationValues = new float[DIMENSION];

    // カメラビュー用バッファ
    private byte mBuffer[];

    // カメラビュー用テクスチャー
    private SurfaceTexture mSurfaceTexture;

    // カメラビュー用画素情報（ダブルバッファ)
    private Mat[] mFrameChain = null;

    // カメラビュー用画素情報の切替インデックス
    private int mChainIdx = 0;

    // カメラビュー用RGB画素情報
    private Mat mRgba;

    // カメラビュー用Bitmap情報
    private Bitmap mCacheBitmap;

    // カメラビュー用描画スレッド
    private Thread mThreadCameraView;

    // カメラビュー用描画スレッド停止フラグ
    private boolean mStopThreadCameraViewFlg;

    /**
     * OpenCVのロード通知イベント
     */
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    OpenCVLoadFlg = true;
                    mySurfaceView.setVisibility(View.VISIBLE);
                    break;

                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    /**
     * カメラのプレビューの描画スレッド
     */
    private class CameraWorker implements Runnable {

        public void run() {
            do {

                synchronized (mySurfaceView) {
                    try {
                        mySurfaceView.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                if (!mStopThreadCameraViewFlg) {
                    if (mFrameChain != null) {
                        if (!mFrameChain[mChainIdx].empty()) {
                            drawCameraView(mFrameChain[mChainIdx]);
                        }
                        mChainIdx = 1 - mChainIdx;
                    }
                }

            } while (!mStopThreadCameraViewFlg);
        }
    }

    /**
     * カメラのプレビューイベント処理
     */
    private PreviewCallback mPreviewCallback =
            new PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    // wait()で待機している描画スレッドに処理を渡す
                    synchronized (mySurfaceView) {
                        mFrameChain[1 - mChainIdx].put(0, 0, data);
                        mySurfaceView.notify();
                    }
                    if (myCamera != null) {
                        // バッファはonPreviewFrameイベントの度に設定しないといけない
                        myCamera.addCallbackBuffer(mBuffer);
                    }
                }
            };

    /**
     * カメラのイベント処理
     */
    private PictureCallback mPictureListener =

            new PictureCallback() {

                @Override
                public void onPictureTaken(byte[] data, Camera camera) {

                    // データを生成する
                    Bitmap tmp_bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    int width = tmp_bitmap.getWidth();
                    int height = tmp_bitmap.getHeight();

                    // 画像データを回転する
                    int rad_y = radianToDegree(orientationValues[2]);
                    Matrix matrix = new Matrix();
                    if ((rad_y > -45 && rad_y <= 0) || (rad_y > 0 && rad_y <= 45)) {
                        matrix.setRotate(90);
                    } else if (rad_y > 45 && rad_y <= 135) {
                        matrix.setRotate(180);
                    } else if ((rad_y > 135 && rad_y <= 180) || (rad_y >= -180 && rad_y <= -135)) {
                        matrix.setRotate(-90);
                    } else if (rad_y > -135 && rad_y <= -45) {
                        matrix.setRotate(0);
                    }
                    Bitmap bitmap = Bitmap.createBitmap(tmp_bitmap, 0, 0, width, height, matrix, true);

                    // ギャラリーに保存
                    String name = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", java.util.Locale.JAPAN).format(new Date()) + ".jpg";
                    MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, name, null);

                    Toast.makeText(MainActivity.this, "保存しました。", Toast.LENGTH_SHORT).show();

                    // カメラを再開
                    myCamera.startPreview();
                    myCamera.setPreviewCallback(mPreviewCallback);
                }
            };

    /**
     * カメラ用サーフェイスのイベント処理
     */
    private SurfaceHolder.Callback mSurfaceListener =

            new SurfaceHolder.Callback() {

                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    if (myCamera != null) {
                        closeCamera();
                    }
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                    if (myCamera != null) {
                        closeCamera();
                    }
                    openCamera();
                    if (myCamera != null) {
                        setupCameraView(width, height);
                    }
                }
            };

    /**
     * オートフォーカスのイベント処理
     */
    private AutoFocusCallback mAutoFocusListener =
            new AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                }
            };

    /**
     * センサー制御のイベント処理
     */
    private SensorEventListener mSensorEventListener =

            new SensorEventListener() {

                @Override
                public void onSensorChanged(SensorEvent event) {

                    // Nexus7では常にSENSOR_STATUS_UNRELIABLEになるのでチェックしない
                /*
                if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
                    return;
                */

                    switch (event.sensor.getType()) {
                        case Sensor.TYPE_MAGNETIC_FIELD:
                            // 地磁気センサ
                            magneticValues = event.values.clone();
                            break;
                        case Sensor.TYPE_ACCELEROMETER:
                            // 加速度センサ(Nexus7ではサポート外？)
                            accelerometerValues = event.values.clone();
                            break;
                    }

                    if (magneticValues != null && accelerometerValues != null) {
                        float[] rotationMatrix = new float[MATRIX_SIZE];
                        float[] inclinationMatrix = new float[MATRIX_SIZE];
                        float[] remapedMatrix = new float[MATRIX_SIZE];

                        // 加速度センサと地磁気センタから回転行列を取得
                        SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, accelerometerValues, magneticValues);

                        SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, remapedMatrix);
                        SensorManager.getOrientation(remapedMatrix, orientationValues);
                    }
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // カメラプレビューの設定
        mySurfaceView = (SurfaceView)findViewById(R.id.surface_view);
        SurfaceHolder holder = mySurfaceView.getHolder();
        holder.addCallback(mSurfaceListener);
        mySurfaceView.setVisibility(View.INVISIBLE);

        // センサーを取得する
        mySensor = (SensorManager)getSystemService(SENSOR_SERVICE);

        Button buttonOK = (Button)this.findViewById(R.id.buttonSave);
        buttonOK.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                buttonSave_onClick();
            }
        });

    }

    /***
     * カメラの使用開始
     */
    private void openCamera() {
        if (!OpenCVLoadFlg) {
            return;
        }
        myCamera = Camera.open();
        mStopThreadCameraViewFlg = false;
        mThreadCameraView = new Thread(new CameraWorker());
        mThreadCameraView.start();
    }

    /***
     * カメラの使用終了
     */
    private void closeCamera() {

        mStopThreadCameraViewFlg = true;

        // wait()で待機している描画スレッドに処理を渡す
        synchronized (mySurfaceView) {
            mySurfaceView.notify();
        }

        try {
            if (mThreadCameraView != null) {
                mThreadCameraView.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            mThreadCameraView =  null;
        }

        myCamera.stopPreview();
        myCamera.setPreviewCallback(null);
        myCamera.release();
        myCamera = null;

        if (mFrameChain != null) {
            mFrameChain[0].release();
            mFrameChain[1].release();
            mFrameChain = null;
        }

        if (mRgba != null) {
            mRgba.release();
            mRgba = null;
        }

        if (mCacheBitmap != null) {
            mCacheBitmap.recycle();
            mCacheBitmap = null;
        }

    }

    /***
     * カメラビューの表示サイズ＆表示開始設定
     */
    private void setupCameraView(int width, int height) {

        myCamera.stopPreview();

        Camera.Parameters parameters = myCamera.getParameters();

        // 画面の向きを設定
        boolean portrait = isPortrait();
        if (portrait) {
            myCamera.setDisplayOrientation(90);
        } else {
            myCamera.setDisplayOrientation(0);
        }

        // 対応するプレビューサイズ・保存サイズを取得する
        List<Camera.Size> pictureSizes = parameters.getSupportedPictureSizes();
        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();

        // 最大解像度を選択
        Size pictureSize = getOptimalPictureSize(pictureSizes);
        // 写真サイズに近くて最も大きいプレビューサイズを選択する
        Size previewSize = getOptimalPreviewSize(previewSizes, pictureSize.width, pictureSize.height);

        parameters.setPreviewSize(previewSize.width, previewSize.height);
        parameters.setPictureSize(pictureSize.width, pictureSize.height);

        // サーフェイスのサイズをカメラのプレビューサイズと同じ比率に設定
        LayoutParams layoutParams = mySurfaceView.getLayoutParams();
        double preview_raito = (double)previewSize.width / (double)previewSize.height;
        if (width > height) {
            // 横長
            int new_height = (int)(width / preview_raito);
            if (new_height <= height) {
                layoutParams.height = height;
            } else {
                int new_width = (int)(height * preview_raito);
                layoutParams.width = new_width;
            }
        } else {
            // 縦長
            int new_width = (int)(height / preview_raito);
            if (new_width <= width) {
                layoutParams.width = new_width;
            } else {
                int new_height = (int)(width * preview_raito);
                layoutParams.height = new_height;
            }
        }
        mySurfaceView.setLayoutParams(layoutParams);

        int size = parameters.getPreviewSize().width * parameters.getPreviewSize().height;
        size  = size * ImageFormat.getBitsPerPixel(parameters.getPreviewFormat()) / 8;
        mBuffer = new byte[size];

        mFrameChain = new Mat[2];
        mFrameChain[0] = new Mat(parameters.getPreviewSize().height + (parameters.getPreviewSize().height/2), parameters.getPreviewSize().width, CvType.CV_8UC1);
        mFrameChain[1] = new Mat(parameters.getPreviewSize().height + (parameters.getPreviewSize().height/2), parameters.getPreviewSize().width, CvType.CV_8UC1);

        mRgba = new Mat();

        mCacheBitmap = Bitmap.createBitmap(parameters.getPreviewSize().width, parameters.getPreviewSize().height, Bitmap.Config.ARGB_8888);

        myCamera.addCallbackBuffer(mBuffer);
        myCamera.setPreviewCallbackWithBuffer(mPreviewCallback);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                // Android 3.0 (API 11)以降ならサーフェイス用のテクスチャーにプレビューを表示する
                mSurfaceTexture = new SurfaceTexture(PREVIEW_TEXTURE_ID);
                myCamera.setPreviewTexture(mSurfaceTexture);
            } else {
                myCamera.setPreviewDisplay(null);
            }
        } catch (Exception e) {}

        // パラメータを設定してカメラを再開
        myCamera.setParameters(parameters);

        myCamera.startPreview();
    }

    /***
     * カメラビューの表示処理
     */
    private void drawCameraView(Mat mat) {

        Canvas canvas = mySurfaceView.getHolder().lockCanvas();

        if (canvas != null) {

            canvas.drawColor(0, android.graphics.PorterDuff.Mode.CLEAR);

            Imgproc.cvtColor(mat, mRgba, Imgproc.COLOR_YUV2BGR_NV12, 4);

            Utils.matToBitmap(mRgba, mCacheBitmap);

            // 画像データを回転する
            int rad_y = radianToDegree(orientationValues[2]);
            Matrix matrix = new Matrix();
            if ((rad_y > -45 && rad_y <= 0) || (rad_y > 0 && rad_y <= 45)) {
                matrix.setRotate(90);
                matrix.postTranslate(mCacheBitmap.getHeight(), 0);
                matrix.postScale((float)canvas.getWidth() / (float)mCacheBitmap.getHeight(), (float)canvas.getHeight() / (float)mCacheBitmap.getWidth());
            } else if (rad_y > 45 && rad_y <= 135) {
                matrix.setRotate(180);
                matrix.postTranslate(mCacheBitmap.getWidth(), mCacheBitmap.getHeight());
                matrix.postScale((float)canvas.getWidth() / (float)mCacheBitmap.getWidth(), (float)canvas.getHeight() / (float)mCacheBitmap.getHeight());
            } else if ((rad_y > 135 && rad_y <= 180) || (rad_y >= -180 && rad_y <= -135)) {
                matrix.setRotate(-90);
                matrix.postTranslate(0, mCacheBitmap.getWidth());
                matrix.postScale((float)canvas.getWidth() / (float)mCacheBitmap.getHeight(), (float)canvas.getHeight() / (float)mCacheBitmap.getWidth());
            } else if (rad_y > -135 && rad_y <= -45) {
                matrix.setRotate(0);
                matrix.postScale((float)canvas.getWidth() / (float)mCacheBitmap.getWidth(), (float)canvas.getHeight() / (float)mCacheBitmap.getHeight());
            }

            canvas.drawBitmap(mCacheBitmap, matrix, null);

            Paint mPaint = new Paint();
            mPaint.setColor(Color.BLUE);
            mPaint.setTextSize(20);

            String timeString = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", java.util.Locale.JAPAN).format(new Date());
            canvas.drawText(timeString, 20, 30, mPaint);

            mySurfaceView.getHolder().unlockCanvasAndPost(canvas);
        }

    }

    @Override
    public void onResume() {

        super.onResume();

        // 地磁気センサ
        mySensor.registerListener(mSensorEventListener,
                mySensor.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_UI);

        // 加速度センサ
        mySensor.registerListener(mSensorEventListener,
                mySensor.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);

        // OpenCVのロード
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);

    }

    @Override
    public void onPause() {
        super.onPause();
        mySensor.unregisterListener(mSensorEventListener);
    }

    /**
     * 画面の向きを取得する(縦ならtrue)
     */
    private boolean isPortrait() {
        return (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT);
    }

    /**
     * 大きすぎない写真サイズを選択する(300万画素以下、4:3のもの)
     */
    private Size getOptimalPictureSize(List<Size> sizes) {

        double targetRatio = (double)4 / 3;

        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            int gasosu = size.width * size.height;
            if (gasosu < (300 * 10000) && Math.abs(ratio - targetRatio) < 0.1) {
                return size;
            }
        }

        return sizes.get(0);
    }

    /**
     * 写真サイズに近くて最も大きいプレビューサイズを選択する
     */
    private Size getOptimalPreviewSize(List<Size> sizes, int w, int h) {

        if (sizes == null) {
            return null;
        }

        double targetRatio = (double) w / h;

        for (Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) < 0.1) {
                return size;
            }
        }

        return sizes.get(0);
    }

    /**
     * ラジアンで計測した角度を、相当する度に変換する
     */
    private int radianToDegree(float rad) {
        return (int)Math.floor(Math.toDegrees(rad));
    }

    /**
     * 画面タッチ時でオートフォーカス
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            Camera.Parameters params = myCamera.getParameters();
            if (!params.getFocusMode().equals(Camera.Parameters.FOCUS_MODE_FIXED)) {
                myCamera.autoFocus(mAutoFocusListener);
            }
        }
        return true;
    }

    /**
     * 写真保存
     */
    protected void buttonSave_onClick() {
        myCamera.setPreviewCallback(null);
        myCamera.takePicture(null, null, mPictureListener);
    }

}