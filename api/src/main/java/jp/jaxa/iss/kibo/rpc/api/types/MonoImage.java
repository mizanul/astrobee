/*
 * Decompiled with CFR 0.152.
 */
package jp.jaxa.iss.kibo.rpc.api.types;

import android.graphics.Bitmap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import sensor_msgs.Image;

public class MonoImage {
    private byte[] data;
    private int height;
    private int width;
    private ChannelBuffer dataBuffer;

    public MonoImage(Image image) {
        this.height = image.getHeight();
        this.width = image.getWidth();
        this.dataBuffer = image.getData();
        this.data = new byte[this.width * this.height];
        this.dataBuffer.getBytes(this.dataBuffer.readerIndex(), this.data);
    }

    public Mat getMat() {
        Mat mat = new Mat(this.height, this.width, CvType.CV_8UC1);
        mat.put(0, 0, this.data);
        return mat;
    }

    public Bitmap getBitmap() {
        Mat mat = this.getMat();
        Bitmap bitmap = Bitmap.createBitmap((int)mat.cols(), (int)mat.rows(), (Bitmap.Config)Bitmap.Config.ARGB_8888);
        Utils.matToBitmap((Mat)mat, (Bitmap)bitmap);
        return bitmap;
    }
}

