package com.example.fujiokazuhiro.camera1;

import org.opencv.core.Mat;

/**
 * Created by fujiokazuhiro on 2016/10/26.
 */

public interface CvCameraViewFrame {
    // 4チャンネルRGBAカラーのMatインスタンスを返す
    public Mat rgba();

    // 1チャンネルグレースケールのMatインスタンスを返す
    public Mat gray();
}
