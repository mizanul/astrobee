/*
 * Decompiled with CFR 0.152.
 */
package jp.jaxa.iss.kibo.rpc.api.types;

import gov.nasa.arc.astrobee.types.Point;
import org.jboss.netty.buffer.ChannelBuffer;
import sensor_msgs.PointCloud2;

public class PointCloud
implements Cloneable {
    private int width;
    private int height;
    private int rowStep;
    private int pointStep;
    private ChannelBuffer rawData;

    public PointCloud(PointCloud2 pointCloud2) {
        this.width = pointCloud2.getWidth();
        this.height = pointCloud2.getHeight();
        this.rowStep = pointCloud2.getRowStep();
        this.pointStep = pointCloud2.getPointStep();
        this.rawData = pointCloud2.getData();
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public Point[] getPointArray() {
        Point[] pointArray = new Point[this.height * this.width];
        for (int ix = 0; ix < this.width; ++ix) {
            for (int iy = 0; iy < this.height; ++iy) {
                float x = this.rawData.getFloat(iy * this.rowStep + this.pointStep * ix);
                float y = this.rawData.getFloat(iy * this.rowStep + this.pointStep * ix + 4);
                float z = this.rawData.getFloat(iy * this.rowStep + this.pointStep * ix + 8);
                pointArray[iy * this.width + ix] = new Point((double)x, (double)y, (double)z);
            }
        }
        return pointArray;
    }

    public PointCloud clone() throws CloneNotSupportedException {
        PointCloud cloned = (PointCloud)super.clone();
        return cloned;
    }
}

