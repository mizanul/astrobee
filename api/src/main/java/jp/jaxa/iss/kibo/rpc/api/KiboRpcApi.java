/*
 * Decompiled with CFR 0.152.
 */
package jp.jaxa.iss.kibo.rpc.api;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;
import gov.nasa.arc.astrobee.AstrobeeException;
import gov.nasa.arc.astrobee.AstrobeeRuntimeException;
import gov.nasa.arc.astrobee.Kinematics;
import gov.nasa.arc.astrobee.PendingResult;
import gov.nasa.arc.astrobee.Result;
import gov.nasa.arc.astrobee.Robot;
import gov.nasa.arc.astrobee.RobotFactory;
import gov.nasa.arc.astrobee.android.gs.MessageType;
import gov.nasa.arc.astrobee.android.gs.StartGuestScienceService;
import gov.nasa.arc.astrobee.ros.DefaultRobotFactory;
import gov.nasa.arc.astrobee.ros.RobotConfiguration;
import gov.nasa.arc.astrobee.types.ActionType;
import gov.nasa.arc.astrobee.types.FlashlightLocation;
import gov.nasa.arc.astrobee.types.FlightMode;
import gov.nasa.arc.astrobee.types.PlannerType;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.PoweredComponent;
import gov.nasa.arc.astrobee.types.Quaternion;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import jp.jaxa.iss.kibo.rpc.api.GetterNode;
import jp.jaxa.iss.kibo.rpc.api.SetterNode;
import jp.jaxa.iss.kibo.rpc.api.sub.GameManager;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

public final class KiboRpcApi
extends Activity {
    private GetterNode getterNode;
    private SetterNode setterNode;
    private GameManager gameManager;
    private static final URI ROS_MASTER_URI = URI.create("http://llp:11311");
    private static final String EMULATOR_ROS_HOSTNAME = "hlp";
    private static final String NODE_NAME = "kibo_rpc_api";
    private static final String TARGET_IMAGES_SAVE_DIR = "/immediate/JudgeImages";
    private static final String DEBUG_IMAGES_SAVE_DIR = "/immediate/DebugImages";
    private static KiboRpcApi instance = null;
    private RobotConfiguration robotConfiguration = new RobotConfiguration();
    private RobotFactory factory;
    private Robot robot;
    private StartGuestScienceService gsService = null;
    private PlannerType plannerType = null;
    private final DateFormat df = new SimpleDateFormat("yyyyMMdd hhmmssSSS");
    private final String PAYLOAD_START = "Mission Start";
    private final String PAYLOAD_FINISH = "Mission Finish";
    private final String UNDOCK_START = "Undock Start";
    private final String UNDOCK_FINISH = "Undock Finish";
    private final String UNDOCK_ERROR = "Undock failed";
    private final String TARGET_SNAPSHOT_START = "Taking snapshots of Target start";
    private final String TARGET_TAKING_SNAPSHOT = "Taking a snapshot of Target";
    private final String TARGET_SNAPSHOT_FINISH = "Taking snapshots of Target finish";
    private final String START_MOVING_TO_GOAL = "Start moving to the goal";
    private final String SIGNAL_LIGHT_PERCHING_ARM = "Turn on the signal light and deploy the perching arm";
    private final String AAR_VERSION = "3.1.0";
    private final int APPROACH_TIMES_SNAPSHOT_SIMULATION = 1;
    private final int APPROACH_TIMES_SNAPSHOT_ISS = 2;
    private final int APPROACH_INTEVAL = 1000;
    private final int LASER_WAIT_TIME = 2000;
    private final int TIMER_CHECK_RESOLUTION = 100;
    private final int UNDOCK_TIMEOUT_ON_SIM = 100;
    private final int MAX_IMAGES = 50;
    private final int MAX_IMAGE_SIZE = 1228800;
    private final float ARM_PAN = 0.0f;
    private final float DEPLOY_ARM_TILT = 90.0f;
    private final float STOW_ARM_TILT = 180.0f;
    private final int BLINK_NUM = 2;
    private final int WAIT_AFTER_BLINK = 1000;
    private final int WAIT_AFTER_GRIPPER_OPEN = 2000;
    private final float FLASH_LIGHT_MIN = 0.0f;
    private final float FLASH_LIGHT_REPORT_MISSION_COMPLETION = 0.05f;
    private final double[] NAVCAM_CAMERA_MATRIX_SIMULATION = new double[]{523.10575, 0.0, 635.434258, 0.0, 534.765913, 500.335102, 0.0, 0.0, 1.0};
    private final double[] NAVCAM_CAMERA_MATRIX_ISS = new double[]{608.8073, 0.0, 632.53684, 0.0, 607.61439, 549.08386, 0.0, 0.0, 1.0};
    private final double[] NAVCAM_DISTORTION_COEFFICIENTS_SIMULATION = new double[]{-0.164787, 0.020375, -0.001572, -3.69E-4, 0.0};
    private final double[] NAVCAM_DISTORTION_COEFFICIENTS_ISS = new double[]{-0.212191, 0.073843, -9.18E-4, 0.00189, 0.0};
    private final double[] DOCKCAM_CAMERA_MATRIX_SIMULATION = new double[]{661.783002, 0.0, 595.212041, 0.0, 671.508662, 489.094196, 0.0, 0.0, 1.0};
    private final double[] DOCKCAM_CAMERA_MATRIX_ISS = new double[]{753.51021, 0.0, 631.11512, 0.0, 751.3611, 508.69621, 0.0, 0.0, 1.0};
    private final double[] DOCKCAM_DISTORTION_COEFFICIENTS_SIMULATION = new double[]{-0.215168, 0.044354, 0.003615, 0.005093, 0.0};
    private final double[] DOCKCAM_DISTORTION_COEFFICIENTS_ISS = new double[]{-0.411405, 0.17724, -0.017145, 0.006421, 0.0};
    private final List<Point> POINT_POSITIONS = new ArrayList<Point>(){
        {
            this.add(new Point(9.815, -9.806, 4.293));
            this.add(new Point(11.2746, -9.92284, 5.2988));
            this.add(new Point(10.612, -9.0709, 4.48));
            this.add(new Point(10.71, -7.7, 4.48));
            this.add(new Point(10.51, -6.7185, 5.1804));
            this.add(new Point(11.114, -7.9756, 5.3393));
            this.add(new Point(11.355, -8.9929, 4.7818));
            this.add(new Point(11.369, -8.5518, 4.48));
            this.add(new Point(11.143, -6.7607, 4.9654));
        }
    };
    private final float DISTANCE_THRESHOLD = 0.3f;
    private boolean calledMoveTo = false;
    private boolean mPlaying = false;
    private boolean finishing = false;
    private int nearbyTargetId = 0;
    private boolean laserActivationFlag = false;

    private void sendExceptionMessage(String errmsg, Exception err) {
        Log.v((String)"KiboRpcApi", (String)"[Start] sendExceptionMessage");
        try {
            JSONObject data = new JSONObject();
            data.put("error", (Object)("[" + err.getClass().getName() + "] " + errmsg));
            Log.e((String)"KiboRpcApi", (String)errmsg, (Throwable)err);
            this.gsService.sendData(MessageType.JSON, "data", data.toString());
        }
        catch (JSONException e) {
            this.gsService.sendData(MessageType.JSON, "data", ((Object)((Object)e)).getClass().getName());
            Log.e((String)"KiboRpcApi", (String)"[sendExceptionMessage] Failed to send error message.", (Throwable)e);
        }
        Log.v((String)"KiboRpcApi", (String)"[Finish] sendExceptionMessage");
    }

    private KiboRpcApi(StartGuestScienceService startGuestScienceService) {
        Log.v((String)"KiboRpcApi", (String)"[Start] KiboRpcApi");
        Log.i((String)"KiboRpcApi", (String)"[AARVersion] AAR version 3.1.0");
        this.configureRobot();
        this.factory = new DefaultRobotFactory(this.robotConfiguration);
        this.gsService = startGuestScienceService;
        try {
            this.robot = this.factory.getRobot();
            this.getterNode = GetterNode.getInstance();
            this.setterNode = SetterNode.getInstance();
            this.gameManager = GameManager.getInstance(startGuestScienceService);
            this.gameManager.start();
        }
        catch (AstrobeeException e) {
            this.sendExceptionMessage("[Constructor] Error with Astrobee", (Exception)((Object)e));
        }
        catch (InterruptedException e) {
            this.sendExceptionMessage("[Constructor] Connection Interrupted", e);
        }
        Log.v((String)"KiboRpcApi", (String)"[Finish] KiboRpcApi");
    }

    private void configureRobot() {
        Log.v((String)"KiboRpcApi", (String)"[Start] configureRobot");
        this.robotConfiguration.setMasterUri(ROS_MASTER_URI);
        this.robotConfiguration.setHostname(EMULATOR_ROS_HOSTNAME);
        this.robotConfiguration.setNodeName(NODE_NAME);
        Log.v((String)"KiboRpcApi", (String)"[Finish] configureRobot");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private Result getCommandResult(PendingResult pending, boolean printRobotPosition, int timeout) {
        Log.v((String)"KiboRpcApi", (String)"[Start] getCommandResult");
        Result result = null;
        int counter = 0;
        try {
            while (!pending.isFinished()) {
                if (timeout >= 0) {
                    Log.v((String)"KiboRpcApi", (String)"[getCommandResult] Setting timeout");
                    if (counter > timeout) {
                        Log.v((String)"KiboRpcApi", (String)"[getCommandResult] return null");
                        Object var7_10 = null;
        return null;
                    }
                }
                if (printRobotPosition) {
                    Log.v((String)"KiboRpcApi", (String)"[getCommandResult] Meanwhile, let's get the positioning along the trajectory");
                    Kinematics k = this.getterNode.getCurrentKinematics();
                    if (k.getPosition() != null) {
                        Log.i((String)"KiboRpcApi", (String)("[getCommandResult] Current Position: " + k.getPosition().toString()));
                    }
                    if (k.getOrientation() != null) {
                        Log.i((String)"KiboRpcApi", (String)("[getCommandResult] Current Orientation: " + k.getOrientation().toString()));
                    }
                }
                pending.getResult(1000L, TimeUnit.MILLISECONDS);
                ++counter;
            }
            result = pending.getResult();
            this.printLogCommandResult(result);
        }
        catch (AstrobeeException e) {
            this.sendExceptionMessage("[getCommandResult] Error with Astrobee", (Exception)((Object)e));
        }
        catch (InterruptedException e) {
            this.sendExceptionMessage("[getCommandResult] Connection Interrupted", e);
        }
        catch (TimeoutException e) {
            this.sendExceptionMessage("[getCommandResult] Timeout connection", e);
        }
        finally {
            Log.v((String)"KiboRpcApi", (String)"[Finish] getCommandResult");
            return result;
        }
    }

    private void printLogCommandResult(Result result) {
        Log.i((String)"KiboRpcApi", (String)"[Start] printLogCommandResult");
        if (result != null) {
            if (result.getStatus() != null) {
                Log.i((String)"KiboRpcApi", (String)("[printLogCommandResult] Command status: " + result.getStatus().toString()));
            }
            if (!result.hasSucceeded()) {
                Log.e((String)"KiboRpcApi", (String)("[printLogCommandResult] Command message: " + result.getMessage()));
            }
            Log.i((String)"KiboRpcApi", (String)"[printLogCommandResult] Done.");
            Log.i((String)"KiboRpcApi", (String)"[Finish] printLogCommandResult");
        } else {
            Log.e((String)"KiboRpcApi", (String)"[printLogCommandResult] Invalid result.");
        }
    }

    private Result stopAllMotion() {
        Log.v((String)"KiboRpcApi", (String)"[Start] stopAllMotion");
        PendingResult pendingResult = null;
        try {
            pendingResult = this.robot.stopAllMotion();
        }
        catch (AstrobeeRuntimeException e) {
            this.sendExceptionMessage("[stopAllMotion] Node not ready or dead.", (Exception)((Object)e));
            return null;
        }
        Result result = this.getCommandResult(pendingResult, false, -1);
        Log.v((String)"KiboRpcApi", (String)"[Finish] stopAllMotion");
        return result;
    }

    private boolean setPlanner(PlannerType plannerType) {
        Log.v((String)"KiboRpcApi", (String)"[Start] setPlanner");
        PendingResult pendingResult = null;
        try {
            pendingResult = this.robot.setPlanner(plannerType);
        }
        catch (AstrobeeRuntimeException e) {
            this.sendExceptionMessage("[setPlanner] Node not ready or dead.", (Exception)((Object)e));
            return false;
        }
        Result result = this.getCommandResult(pendingResult, false, 5);
        if (result != null) {
            if (result.hasSucceeded()) {
                this.plannerType = plannerType;
                Log.v((String)"KiboRpcApi", (String)("[setPlanner] Planner is set to " + plannerType));
            }
            Log.v((String)"KiboRpcApi", (String)"[Finish] setPlanner");
            return result.hasSucceeded();
        }
        Log.e((String)"KiboRpcApi", (String)"[setPlanner] Invalid result.");
        return false;
    }

    public void shutdownFactory() {
        Log.v((String)"KiboRpcApi", (String)"[Start] shutdownFactory");
        this.factory.shutdown();
        Log.v((String)"KiboRpcApi", (String)"[Finish] shutdownFactory");
    }

    public static KiboRpcApi getInstance(StartGuestScienceService startGuestScienceService) {
        Log.v((String)"KiboRpcApi", (String)"[Start] getInstance");
        if (instance == null) {
            Log.v((String)"KiboRpcApi", (String)"[getInstance] Make instance");
            instance = new KiboRpcApi(startGuestScienceService);
        }
        Log.v((String)"KiboRpcApi", (String)"[Finish] getInstance");
        return instance;
    }

    private Kinematics getTrustedRobotKinematics() {
        Log.i((String)"KiboRpcApi", (String)"[Start] getTrustedRobotKinematics");
        Log.i((String)"KiboRpcApi", (String)"[getTrustedRobotKinematics] Waiting for robot to acquire position.");
        Kinematics k = null;
        long start_point = System.currentTimeMillis();
        long end_point = System.currentTimeMillis();
        while (end_point - start_point < 30000L) {
            k = this.getterNode.getCurrentKinematics();
            if (k.getConfidence() == Kinematics.Confidence.GOOD) {
                Log.v((String)"KiboRpcApi", (String)"[getTrustedRobotKinematics] Break loop");
                break;
            }
            k = null;
            try {
                Thread.sleep(1000L);
            }
            catch (InterruptedException e) {
                Log.e((String)"KiboRpcApi", (String)"[getTrustedRobotkinematics] It was not possible to get a trusted kinematics. Sorry.");
                return null;
            }
            end_point = System.currentTimeMillis();
        }
        Log.i((String)"KiboRpcApi", (String)"[Finish] getTrustedRobotKinematics");
        return k;
    }

    public Kinematics getRobotKinematics() {
        Log.i((String)"KiboRpcApi", (String)"[Start] getRobotKinematics");
        Log.i((String)"KiboRpcApi", (String)"[getRobotKinematics] Waiting for robot to acquire position.");
        Log.i((String)"KiboRpcApi", (String)"[Finish] getRobotKinematics");
        return this.getterNode.getCurrentKinematics();
    }

    public Bitmap getBitmapNavCam() {
        Log.i((String)"KiboRpcApi", (String)"[Start] getBitmapNavCam");
        Bitmap ret = this.getterNode.getBitmapNavCam();
        if (ret == null) {
            Log.i((String)"KiboRpcApi", (String)"[getBitmapNavCam] ret is null.");
            Log.e((String)"KiboRpcApi", (String)"[getBitmapNavCam] It was not possible to get a Bitmap from Nav Cam.");
        }
        Log.i((String)"KiboRpcApi", (String)"[Finish] getBitmapNavCam");
        return ret;
    }

    public Bitmap getBitmapDockCam() {
        Log.i((String)"KiboRpcApi", (String)"[Start] getBitmapDockCam");
        Bitmap ret = this.getterNode.getBitmapDockCam();
        if (ret == null) {
            Log.i((String)"KiboRpcApi", (String)"[getBitmapDockCam] ret is null.");
            Log.e((String)"KiboRpcApi", (String)"[getBitmapDockCam] It was not possible to get a Bitmap from Dock Cam.");
        }
        Log.i((String)"KiboRpcApi", (String)"[Finish] getBitmapDockCam");
        return ret;
    }

    public Mat getMatNavCam() {
        Log.i((String)"KiboRpcApi", (String)"[Start] getMatNavCam");
        Mat ret = this.getterNode.getMatNavCam();
        if (ret == null) {
            Log.i((String)"KiboRpcApi", (String)"[getMatNavCam] ret is null.");
            Log.e((String)"KiboRpcApi", (String)"[getMatNavCam] It was not possible to get a Mat from Nav Cam.");
        }
        Log.i((String)"KiboRpcApi", (String)"[Finish] getMatNavCam");
        return ret;
    }

    public Mat getMatDockCam() {
        Log.i((String)"KiboRpcApi", (String)"[Start] getMatDockCam");
        Mat ret = this.getterNode.getMatDockCam();
        if (ret == null) {
            Log.i((String)"KiboRpcApi", (String)"[getMatDockCam] ret is null.");
            Log.e((String)"KiboRpcApi", (String)"[getMatDockCam] It was not possible to get a Mat from Dock Cam.");
        }
        Log.i((String)"KiboRpcApi", (String)"[Finish] getMatDockCam");
        return ret;
    }

    public Result flashlightControlFront(float brightness) {
        Log.i((String)"KiboRpcApi", (String)"[Start] flashlightControlFront");
        Log.i((String)"KiboRpcApi", (String)("Parameters: brightness == " + brightness));
        PendingResult pendingResult = null;
        try {
            pendingResult = this.robot.setFlashlightBrightness(FlashlightLocation.FRONT, brightness);
        }
        catch (AstrobeeRuntimeException e) {
            this.sendExceptionMessage("[flashlightControlFront] Node not ready or dead.", (Exception)((Object)e));
            return null;
        }
        this.robot.setFlashlightBrightness(FlashlightLocation.FRONT, brightness);
        Log.i((String)"KiboRpcApi", (String)"[Finish] flashlightControlFront");
        return this.getCommandResult(pendingResult, false, -1);
    }

    public Result flashlightControlBack(float brightness) {
        Log.i((String)"KiboRpcApi", (String)"[Start] flashlightControlBack");
        Log.i((String)"KiboRpcApi", (String)("Parameters: brightness == " + brightness));
        PendingResult pendingResult = null;
        try {
            pendingResult = this.robot.setFlashlightBrightness(FlashlightLocation.BACK, brightness);
        }
        catch (AstrobeeRuntimeException e) {
            this.sendExceptionMessage("[flashlightControlBack] Node not ready or dead.", (Exception)((Object)e));
            return null;
        }
        Log.i((String)"KiboRpcApi", (String)"[Finish] flashlightControlBack");
        return this.getCommandResult(pendingResult, false, -1);
    }

    public Result moveTo(Point goalPoint, Quaternion orientation, boolean printRobotPosition) {
        Log.i((String)"KiboRpcApi", (String)"[Start] moveTo");
        Log.i((String)"KiboRpcApi", (String)("Parameters: goalPoint == " + goalPoint + ", orientation == " + orientation + ", printRobotPosition == " + printRobotPosition));
        if (goalPoint == null) {
            Log.i((String)"KiboRpcApi", (String)("[moveTo] goalPoint == " + goalPoint));
            Log.e((String)"KiboRpcApi", (String)"[moveTo] goalPoint is invalid.");
            return null;
        }
        if (orientation == null) {
            Log.i((String)"KiboRpcApi", (String)("[moveTo] orientation == " + orientation));
            Log.e((String)"KiboRpcApi", (String)"[moveTo] orientation is invalid.");
            return null;
        }
        if (!this.setPlanner(PlannerType.TRAPEZOIDAL)) {
            Log.e((String)"KiboRpcApi", (String)"[moveTo] Cannot set planner.");
            return null;
        }
        Log.i((String)"KiboRpcApi", (String)("[moveTo] Planner is set to " + this.plannerType.toString() + "."));
        Result result = this.stopAllMotion();
        if (result == null) {
            Log.i((String)"KiboRpcApi", (String)("[moveTo] result == " + result));
            Log.e((String)"KiboRpcApi", (String)"[moveTo] Cannot stop all motion.");
            return null;
        }
        if (result.hasSucceeded()) {
            Log.i((String)"KiboRpcApi", (String)("[moveTo] Planner is " + this.plannerType.toString() + "."));
            Log.i((String)"KiboRpcApi", (String)"[moveTo] Moving the bee.");
            PendingResult pendingResult = null;
            try {
                pendingResult = this.robot.simpleMove6DOF(goalPoint, orientation);
            }
            catch (AstrobeeRuntimeException e) {
                this.sendExceptionMessage("[moveTo] Node not ready or dead.", (Exception)((Object)e));
                return null;
            }
            result = this.getCommandResult(pendingResult, printRobotPosition, -1);
            if (this.getterNode.getOnSimulation()) {
                Log.v((String)"KiboRpcApi", (String)("[moveTo] OnSimulation: " + this.getterNode.getOnSimulation()));
                if (this.isActiveTarget()) {
                    Log.v((String)"KiboRpcApi", (String)"[moveTo] Set LaserActivationFlag: true");
                    this.laserActivationFlag = true;
                }
            }
            this.calledMoveTo = true;
        }
        Log.i((String)"KiboRpcApi", (String)"[Finish] moveTo");
        return result;
    }

    public Result relativeMoveTo(Point goalPoint, Quaternion orientation, boolean printRobotPosition) {
        Log.i((String)"KiboRpcApi", (String)"[Start] relativeMoveTo");
        Log.i((String)"KiboRpcApi", (String)("Parameters: goalPoint == " + goalPoint + ", orientation == " + orientation + ", printRobotPosition == " + printRobotPosition));
        if (goalPoint == null) {
            Log.i((String)"KiboRpcApi", (String)("[relativeMoveTo] goalPoint == " + goalPoint));
            Log.e((String)"KiboRpcApi", (String)"[relativeMoveTo] goalPoint is invalid.");
            return null;
        }
        if (orientation == null) {
            Log.i((String)"KiboRpcApi", (String)("[relativeMoveTo] orientation == " + orientation));
            Log.e((String)"KiboRpcApi", (String)"[relativeMoveTo] orientation is invalid.");
            return null;
        }
        Kinematics k = this.getTrustedRobotKinematics();
        if (k == null) {
            Log.i((String)"KiboRpcApi", (String)("[relativeMoveTo] k == " + k));
            Log.e((String)"KiboRpcApi", (String)"[relativeMoveTo] Cannot get robot kinematics.");
            return null;
        }
        Point currPosition = k.getPosition();
        Point endPoint = new Point(currPosition.getX() + goalPoint.getX(), currPosition.getY() + goalPoint.getY(), currPosition.getZ() + goalPoint.getZ());
        Log.i((String)"KiboRpcApi", (String)"[Finish] relativeMoveTo");
        return this.moveTo(endPoint, orientation, printRobotPosition);
    }

    private boolean isActiveTarget() {
        Log.v((String)"KiboRpcApi", (String)"[Start] isActiveTarget");
        boolean isActive = false;
        Kinematics k = this.getTrustedRobotKinematics();
        if (k == null) {
            Log.i((String)"KiboRpcApi", (String)("[isActiveTarget] k == " + k));
            Log.e((String)"KiboRpcApi", (String)"[isActiveTarget] Cannot get robot kinematics.");
            return false;
        }
        Point currPosition = k.getPosition();
        if (currPosition == null) {
            Log.i((String)"KiboRpcApi", (String)("[isActiveTarget] currPosition == " + currPosition));
            Log.e((String)"KiboRpcApi", (String)"[isActiveTarget] Cannot get current position.");
            return false;
        }
        double smallerDistance = Double.MAX_VALUE;
        List<Integer> activeTargets = this.gameManager.getActiveTargets();
        for (int activeTargetId : activeTargets) {
            Point checkPoint = new Point(currPosition.getX() - this.POINT_POSITIONS.get(activeTargetId).getX(), currPosition.getY() - this.POINT_POSITIONS.get(activeTargetId).getY(), currPosition.getZ() - this.POINT_POSITIONS.get(activeTargetId).getZ());
            double distance = Math.sqrt(Math.pow(checkPoint.getX(), 2.0) + Math.pow(checkPoint.getY(), 2.0) + Math.pow(checkPoint.getZ(), 2.0));
            if (!(distance <= (double)0.3f) || !(distance <= smallerDistance)) continue;
            Log.v((String)"KiboRpcApi", (String)("[isActiveTarget] Near active target [target_id: " + activeTargetId + "]"));
            Log.v((String)"KiboRpcApi", (String)("[isActiveTarget] Set nearbyTargetId [target_id: " + activeTargetId + "]"));
            smallerDistance = distance;
            this.nearbyTargetId = activeTargetId;
            isActive = true;
        }
        Log.v((String)"KiboRpcApi", (String)"[Finish] isActiveTarget");
        return isActive;
    }

    public Result laserControl(boolean state) {
        PendingResult pendingResult;
        block6: {
            Log.i((String)"KiboRpcApi", (String)"[Start] laserControl");
            Log.i((String)"KiboRpcApi", (String)("Parameters: state == " + state));
            if (this.getterNode.getOnSimulation() && !this.laserActivationFlag) {
                Log.v((String)"KiboRpcApi", (String)("[laserControl] OnSimulation: " + this.getterNode.getOnSimulation()));
                Log.v((String)"KiboRpcApi", (String)("[laserControl] laserActivationFlag: " + this.laserActivationFlag));
                Log.i((String)"KiboRpcApi", (String)"[Finish] laserControl (SKIP)");
                return null;
            }
            pendingResult = null;
            try {
                if (state) {
                    Log.v((String)"KiboRpcApi", (String)"[laserControl] Power on");
                    pendingResult = this.robot.powerOnItem(PoweredComponent.LASER_POINTER);
                    try {
                        Thread.sleep(2000L);
                        break block6;
                    }
                    catch (InterruptedException e) {
                        Log.e((String)"KiboRpcApi", (String)"[laserControl] It was not possible to get a trusted kinematics. Sorry.");
                        return null;
                    }
                }
                Log.v((String)"KiboRpcApi", (String)"[laserControl] Power off");
                pendingResult = this.robot.powerOffItem(PoweredComponent.LASER_POINTER);
            }
            catch (AstrobeeRuntimeException e) {
                this.sendExceptionMessage("[laserControl] Node not ready or dead.", (Exception)((Object)e));
                return null;
            }
        }
        Log.i((String)"KiboRpcApi", (String)"[Finish] laserControl");
        return this.getCommandResult(pendingResult, false, -1);
    }

    private Result perchingArmControl(boolean state) {
        Log.v((String)"KiboRpcApi", (String)"[Start] perchingArmControl");
        Log.i((String)"KiboRpcApi", (String)("Parameters: state == " + state));
        PendingResult pendingResult = null;
        try {
            if (state) {
                Log.v((String)"KiboRpcApi", (String)"[perchingArmControl] deploy");
                pendingResult = this.robot.armPanAndTilt(0.0f, 90.0f, ActionType.BOTH);
            } else {
                Log.v((String)"KiboRpcApi", (String)"[perchingArmControl] stow");
                pendingResult = this.robot.armPanAndTilt(0.0f, 180.0f, ActionType.BOTH);
            }
        }
        catch (AstrobeeRuntimeException e) {
            this.sendExceptionMessage("[perchingArmControl] Node not ready or dead.", (Exception)((Object)e));
            return null;
        }
        Log.v((String)"KiboRpcApi", (String)"[Finish] perchingArmControl");
        return this.getCommandResult(pendingResult, false, -1);
    }

    private Result gripperControl(boolean open) {
        Log.v((String)"KiboRpcApi", (String)"[Start] gripperControl");
        Log.i((String)"KiboRpcApi", (String)("Parameters: open == " + open));
        PendingResult pendingResult = null;
        try {
            pendingResult = this.robot.gripperControl(open);
        }
        catch (AstrobeeRuntimeException e) {
            this.sendExceptionMessage("[gripperControl] Node not ready or dead.", (Exception)((Object)e));
            return null;
        }
        Log.v((String)"KiboRpcApi", (String)"[Finish] gripperControl");
        return this.getCommandResult(pendingResult, false, -1);
    }

    private void setSignalStateCorrectPattern() {
        Log.v((String)"KiboRpcApi", (String)"[Start] setSignalStateCorrectPattern");
        this.setterNode.setSignalState((byte)4, 2);
        this.setterNode.setSignalState((byte)5, 1);
        this.setterNode.setSignalState((byte)3, 1);
        Log.v((String)"KiboRpcApi", (String)"[Finish] setSignalStateCorrectPattern");
    }

    private void setSignalStateIncorrectPattern() {
        Log.v((String)"KiboRpcApi", (String)"[Start] setSignalStateIncorrectPattern");
        this.setterNode.setSignalState((byte)7, 2);
        Log.v((String)"KiboRpcApi", (String)"[Finish] setSignalStateIncorrectPattern");
    }

    public boolean startMission() {
        Log.i((String)"KiboRpcApi", (String)"[Start] startMission");
        boolean isSuccess = false;
        try {
            JSONObject data = new JSONObject();
            String time = this.df.format(new Date(System.currentTimeMillis()));
            data.put("t_stamp_undock", (Object)time);
            data.put("status", (Object)"Undock Start");
            this.gsService.sendData(MessageType.JSON, "data", data.toString());
            Log.i((String)"KiboRpcApi", (String)("Undock Start: " + time));
            PendingResult pendingResult = this.robot.undock();
            int timeout = 100;
            if (!this.getterNode.getOnSimulation()) {
                Log.d((String)"KiboRpcApi", (String)"set timeout -1");
                timeout = -1;
            }
            Result result = this.getCommandResult(pendingResult, false, timeout);
            data = new JSONObject();
            if (result != null && result.hasSucceeded()) {
                data.put("status", (Object)"Undock Finish");
                isSuccess = true;
                time = this.df.format(new Date(System.currentTimeMillis()));
                Log.i((String)"KiboRpcApi", (String)("Undock Finish: " + time));
            } else {
                data.put("failed", (Object)"Undock failed");
                time = this.df.format(new Date(System.currentTimeMillis()));
                Log.e((String)"KiboRpcApi", (String)("Undock failed: " + time));
            }
            this.gsService.sendData(MessageType.JSON, "data", data.toString());
            if (isSuccess) {
                time = this.df.format(new Date(System.currentTimeMillis()));
                data = new JSONObject();
                data.put("t_stamp_start", (Object)time);
                data.put("status", (Object)"Mission Start");
                this.gsService.sendData(MessageType.JSON, "data", data.toString());
                Log.i((String)"KiboRpcApi", (String)("Mission Start: " + time));
            }
            if (!this.calledMoveTo) {
                this.gameManager.startMission();
            } else {
                Log.v((String)"KiboRpcApi", (String)"[startMission] astrobee is moved. Unable to retry the start mission");
            }
        }
        catch (JSONException e) {
            this.sendExceptionMessage("[startMission] Internal error occurred. Unable to serialize data to JSON.", (Exception)((Object)e));
            return false;
        }
        catch (Exception e) {
            this.sendExceptionMessage("[startMission] Internal error occurred. Unable to send data to gds.", e);
            return false;
        }
        Log.i((String)"KiboRpcApi", (String)"[Finish] startMission");
        return isSuccess;
    }

    @Deprecated
    private Result setQuietMode() {
        try {
            Log.v((String)"KiboRpcApi", (String)"[Start] setQuietMode");
            Log.i((String)"KiboRpcApi", (String)"[setQuietMode] Change the flight mode to Quiet mode.");
            PendingResult pendingResult = this.robot.setOperatingLimits("iss_quiet", FlightMode.QUIET, 0.02f, 0.002f, 0.0174f, 0.0174f, 0.25f);
            Result result = this.getCommandResult(pendingResult, true, -1);
            Log.i((String)"KiboRpcApi", (String)"[setQuietMode] Changed flight mode to Quiet mode.");
            Log.v((String)"KiboRpcApi", (String)"[Finish] setQuietMode");
            return result;
        }
        catch (AstrobeeRuntimeException e) {
            this.sendExceptionMessage("[setQuietMode] Node not ready or dead.", (Exception)((Object)e));
            return null;
        }
    }

    @Deprecated
    private Result setLomoMode() {
        try {
            Log.v((String)"KiboRpcApi", (String)"[Start] setLomoMode");
            Log.i((String)"KiboRpcApi", (String)"[setLomoMode] Change the flight mode to Lomo mode.");
            PendingResult pendingResult = this.robot.setOperatingLimits("iss_lomo", FlightMode.NOMINAL, 0.15f, 0.0175f, 0.0873f, 0.1745f, 0.25f);
            Result result = this.getCommandResult(pendingResult, true, -1);
            Log.i((String)"KiboRpcApi", (String)"[setLomoMode] Changed flight mode to Lomo mode.");
            Log.v((String)"KiboRpcApi", (String)"[Finish] setLomoMode");
            return result;
        }
        catch (AstrobeeRuntimeException e) {
            this.sendExceptionMessage("[setLomoMode] Node not ready or dead.", (Exception)((Object)e));
            return null;
        }
    }

    public void notifyGoingToGoal() {
        Log.i((String)"KiboRpcApi", (String)"[Start] notifyGoingToGoal");
        try {
            Log.v((String)"KiboRpcApi", (String)"[notifyGoingToGoal] Start moving to the goal");
            JSONObject data = new JSONObject();
            data.put("status", (Object)"Start moving to the goal");
            this.gsService.sendData(MessageType.JSON, "data", data.toString());
            if (!this.getterNode.getOnSimulation()) {
                this.setSignalStateIncorrectPattern();
                this.setterNode.setSignalState((byte)17, 1);
            }
        }
        catch (JSONException e) {
            this.sendExceptionMessage("[notifyGoingToGoal] Internal error was occurred. Unable to serialize data to JSON.", (Exception)((Object)e));
            return;
        }
        catch (Exception e) {
            this.sendExceptionMessage("[notifyGoingToGoal] Internal error was occurred. Unable to send data to gds.", e);
            return;
        }
        Log.i((String)"KiboRpcApi", (String)"[Finish] notifyGoingToGoal");
    }

    public boolean reportMissionCompletion(String report) {
        try {
            Log.i((String)"KiboRpcApi", (String)"[Start] reportMissionCompletion");
            Log.i((String)"KiboRpcApi", (String)("Parameters: report == " + report));
            if (!this.finishing) {
                Log.v((String)"KiboRpcApi", (String)"[reportMissionCompletion] Make finish message");
                JSONObject data = new JSONObject();
                data.put("t_stamp_finish", (Object)this.df.format(new Date(System.currentTimeMillis())));
                data.put("status", (Object)"Mission Finish");
                this.gsService.sendData(MessageType.JSON, "data", data.toString());
                this.finishing = true;
            }
        }
        catch (JSONException e) {
            this.sendExceptionMessage("[reportMissionCompletion] Internal error was occurred. Unable to serialize data to JSON.", (Exception)((Object)e));
            return false;
        }
        catch (Exception e) {
            this.sendExceptionMessage("[reportMissionCompletion] Internal error was occurred. Unable to send data to gds.", e);
            return false;
        }
        if (!this.getterNode.getOnSimulation()) {
            Log.v((String)"KiboRpcApi", (String)"[reportMissionCompletion] getOnSimulation: False");
            Result stopResult = this.stopAllMotion();
            if (stopResult == null || !stopResult.hasSucceeded()) {
                Log.i((String)"KiboRpcApi", (String)("[reportMissionCompletion] stopResult == " + stopResult));
                Log.e((String)"KiboRpcApi", (String)"[reportMissionCompletion] Cannot stop all motion.");
            }
        }
        try {
            if (!this.mPlaying) {
                Log.v((String)"KiboRpcApi", (String)"[reportMissionCompletion] Do getRobotKinematics");
                Kinematics kinematics = this.getRobotKinematics();
                if (kinematics == null || kinematics.getPosition() == null || kinematics.getOrientation() == null) {
                    Log.e((String)"KiboRpcApi", (String)"[reportMissionCompletion] It was not possible to get a kinematics.");
                    if (kinematics == null) {
                        Log.e((String)"KiboRpcApi", (String)"[reportMissionCompletion] kinematics is null.");
                    }
                    if (kinematics.getPosition() == null) {
                        Log.e((String)"KiboRpcApi", (String)"[reportMissionCompletion] kinematics.getPosition() is null.");
                    }
                    if (kinematics.getOrientation() == null) {
                        Log.e((String)"KiboRpcApi", (String)"[reportMissionCompletion] kinematics.getOrientation() is null.");
                    }
                    return false;
                }
                String positionX = String.format("%.3f", kinematics.getPosition().getX());
                String positionY = String.format("%.3f", kinematics.getPosition().getY());
                String positionZ = String.format("%.3f", kinematics.getPosition().getZ());
                String orientationX = String.format("%.3f", Float.valueOf(kinematics.getOrientation().getX()));
                String orientationY = String.format("%.3f", Float.valueOf(kinematics.getOrientation().getY()));
                String orientationZ = String.format("%.3f", Float.valueOf(kinematics.getOrientation().getZ()));
                String orientationW = String.format("%.3f", Float.valueOf(kinematics.getOrientation().getW()));
                JSONObject data = new JSONObject();
                data.put("point_pos", (Object)("[ posX : " + positionX + ", posY : " + positionY + ", posZ " + positionZ + ", quaX: " + orientationX + ", quaY: " + orientationY + ", quaZ: " + orientationZ + ", quaW: " + orientationW + " ]"));
                data.put("t_stamp_signal_light", (Object)this.df.format(new Date(System.currentTimeMillis())));
                data.put("status", (Object)"Turn on the signal light and deploy the perching arm");
                data.put("qr_info", (Object)report);
                this.gsService.sendData(MessageType.JSON, "data", data.toString());
                for (int i = 0; i < 2; ++i) {
                    Log.v((String)"KiboRpcApi", (String)("[reportMissionCompletion] Count: " + i));
                    this.flashlightControlFront(0.05f);
                    this.flashlightControlBack(0.05f);
                    this.flashlightControlFront(0.0f);
                    this.flashlightControlBack(0.0f);
                }
                Thread.sleep(1000L);
                if (!this.getterNode.getOnSimulation()) {
                    Log.v((String)"KiboRpcApi", (String)"[reportMissionCompletion] getOnSimulation: False");
                    String correctReport = this.gameManager.getCorrectReport();
                    Log.v((String)"KiboRpcApi", (String)("[reportMissionCompletion] correctReport: " + correctReport));
                    if (correctReport != null && correctReport.equals(report)) {
                        Result closeArmResult;
                        Result openGripResult;
                        Log.v((String)"KiboRpcApi", (String)"[reportMissionCompletion] report is correct.");
                        Log.v((String)"KiboRpcApi", (String)"[reportMissionCompletion] Set SignalState");
                        this.setSignalStateCorrectPattern();
                        Result openArmResult = this.perchingArmControl(true);
                        if (openArmResult == null) {
                            Log.i((String)"KiboRpcApi", (String)("[reportMissionCompletion] arm deploy Result == " + openArmResult));
                            Log.e((String)"KiboRpcApi", (String)"[reportMissionCompletion] Cannot deploy arm.");
                        }
                        if ((openGripResult = this.gripperControl(true)) == null) {
                            Log.i((String)"KiboRpcApi", (String)("[reportMissionCompletion] gripper open Result == " + openGripResult));
                            Log.e((String)"KiboRpcApi", (String)"[reportMissionCompletion] Cannot open gripper.");
                        }
                        Thread.sleep(2000L);
                        Result closeGripResult = this.gripperControl(false);
                        if (closeGripResult == null) {
                            Log.i((String)"KiboRpcApi", (String)("[reportMissionCompletion] gripper close Result == " + closeGripResult));
                            Log.e((String)"KiboRpcApi", (String)"[reportMissionCompletion] Cannot close gripper.");
                        }
                        if ((closeArmResult = this.perchingArmControl(false)) == null) {
                            Log.i((String)"KiboRpcApi", (String)("[reportMissionCompletion] arm stow Result == " + closeArmResult));
                            Log.e((String)"KiboRpcApi", (String)"[reportMissionCompletion] Cannot stow arm.");
                        }
                    } else {
                        Log.v((String)"KiboRpcApi", (String)"[reportMissionCompletion] report is incorrect.");
                        Log.v((String)"KiboRpcApi", (String)"[reportMissionCompletion] Set SignalState");
                        this.setSignalStateIncorrectPattern();
                    }
                    Log.v((String)"KiboRpcApi", (String)"[reportMissionCompletion] Set SignalState");
                    this.setterNode.setSignalState((byte)17, 1);
                    Result stopResult = this.stopAllMotion();
                    if (stopResult == null || !stopResult.hasSucceeded()) {
                        Log.i((String)"KiboRpcApi", (String)("[reportMissionCompletion] stopResult == " + stopResult));
                        Log.e((String)"KiboRpcApi", (String)"[reportMissionCompletion] Cannot stop all motion.");
                    }
                }
                this.mPlaying = true;
            }
        }
        catch (Exception e) {
            this.sendExceptionMessage("[reportMissionCompletion] Internal error was occurred.", e);
            this.mPlaying = false;
            return false;
        }
        Log.i((String)"KiboRpcApi", (String)"[Finish] reportMissionCompletion");
        return true;
    }

    public void takeTargetSnapshot(int targetId) {
        Log.i((String)"KiboRpcApi", (String)"[Start] takeTargetSnapshot");
        Log.i((String)"KiboRpcApi", (String)("Parameters: targetId == " + targetId));
        try {
            List<Integer> activeTargets = this.gameManager.getActiveTargets();
            int phaseNumber = this.gameManager.getCurrentPhaseNumber();
            boolean result = true;
            if (this.getterNode.getOnSimulation() && activeTargets.contains(targetId) && this.nearbyTargetId == targetId || !this.getterNode.getOnSimulation() && activeTargets.contains(targetId)) {
                int approach_time = 1;
                if (!this.getterNode.getOnSimulation()) {
                    Log.v((String)"KiboRpcApi", (String)"[takeTargetSnapshot] getOnSimulation: False");
                    Log.v((String)"KiboRpcApi", (String)"[takeTargetSnapshot] Set approach_time");
                    approach_time = 2;
                }
                result = this.takeSnapshot(phaseNumber, targetId, approach_time);
            } else if (!activeTargets.contains(targetId)) {
                JSONObject data = new JSONObject();
                Log.e((String)"KiboRpcApi", (String)"[takeTargetSnapshot] Fail to deactivate the target.");
                data.put("snapshot_inactive", (Object)String.format("Target is inactive [ target_id: %d ]", targetId));
                data.put("target_info", (Object)this.getTargetInfoObject(phaseNumber, targetId));
                this.gsService.sendData(MessageType.JSON, "data", data.toString());
            }
            if (this.getterNode.getOnSimulation() && this.nearbyTargetId != targetId) {
                Log.e((String)"KiboRpcApi", (String)String.format("[takeTargetSnapshot] target is unmatch. Expected target is %d.", this.nearbyTargetId));
            }
            if (result) {
                this.laserControl(false);
                this.nearbyTargetId = 0;
                this.laserActivationFlag = false;
            }
        }
        catch (JSONException e) {
            this.sendExceptionMessage("[takeTargetSnapshot] Internal error was occurred. Unable to serialize data to JSON.", (Exception)((Object)e));
            return;
        }
        catch (Exception e) {
            this.sendExceptionMessage("[takeTargetSnapshot] Internal error was occurred. Unable to send data to gds.", e);
            return;
        }
        Log.i((String)"KiboRpcApi", (String)"[Finish] takeTargetSnapshot");
    }

    private boolean takeSnapshot(int phaseNumber, int targetId, int approach_times) {
        try {
            Log.v((String)"KiboRpcApi", (String)"[Start] takeSnapshot");
            JSONObject data = null;
            for (int i = 1; i <= approach_times; ++i) {
                Log.v((String)"KiboRpcApi", (String)("[takeSnapshot] Count: " + i));
                long startTime = System.currentTimeMillis();
                if (i == 1) {
                    data = new JSONObject();
                    Log.v((String)"KiboRpcApi", (String)"[takeSnapshot] Make start message");
                    data.put("t_stamp_snapshot", (Object)this.df.format(new Date(System.currentTimeMillis())));
                    data.put("status", (Object)"Taking snapshots of Target start");
                    data.put("target_info", (Object)this.getTargetInfoObject(phaseNumber, targetId));
                    this.gsService.sendData(MessageType.JSON, "data", data.toString());
                    if (!this.gameManager.targetDeactivation(targetId)) {
                        Log.e((String)"KiboRpcApi", (String)"[takeSnapshot] Fail to deactivate the target.");
                        return false;
                    }
                }
                data = new JSONObject();
                Log.v((String)"KiboRpcApi", (String)"[takeSnapshot] Make target snapshot");
                data.put("status", (Object)"Taking a snapshot of Target");
                data.put("target_info", (Object)this.getTargetInfoObject(phaseNumber, targetId));
                this.gsService.sendData(MessageType.JSON, "data", data.toString());
                if (!this.getterNode.getOnSimulation()) {
                    Log.v((String)"KiboRpcApi", (String)"[takeSnapshot] getOnSimulation: False");
                    Log.v((String)"KiboRpcApi", (String)"[takeSnapshot] Save bitmap");
                    Bitmap image = this.getBitmapNavCam();
                    this.saveBitmap(image, String.valueOf(phaseNumber), String.valueOf(targetId));
                }
                long currentTime = System.currentTimeMillis();
                while (currentTime - startTime <= 1000L) {
                    if (this.getterNode.getOnSimulation()) {
                        Log.v((String)"KiboRpcApi", (String)"[takeSnapshot] getOnSimulation: True");
                        Log.v((String)"KiboRpcApi", (String)"[takeSnapshot] Sleep inteval");
                        Thread.sleep(10L);
                    } else {
                        Log.v((String)"KiboRpcApi", (String)"[takeSnapshot] getOnSimulation: False");
                        Log.v((String)"KiboRpcApi", (String)"[takeSnapshot] Sleep inteval");
                        Thread.sleep(10L);
                    }
                    currentTime = System.currentTimeMillis();
                }
            }
            data = new JSONObject();
            Log.v((String)"KiboRpcApi", (String)"[takeSnapshot] Make target finish message");
            data.put("status", (Object)"Taking snapshots of Target finish");
            data.put("target_info", (Object)this.getTargetInfoObject(phaseNumber, targetId));
            this.gsService.sendData(MessageType.JSON, "data", data.toString());
            Log.v((String)"KiboRpcApi", (String)"[Finish] takeSnapshot");
            return true;
        }
        catch (JSONException e) {
            this.sendExceptionMessage("[takeSnapshot] Internal error was occurred. Unable to serialize data to JSON.", (Exception)((Object)e));
            return false;
        }
        catch (SecurityException e) {
            this.sendExceptionMessage("[takeSnapshot] Internal error was occurred. Unable to access directory.", e);
            return false;
        }
        catch (IOException e) {
            this.sendExceptionMessage("[takeSnapshot] Internal error was occurred. Unable to make image files.", e);
            return false;
        }
        catch (NullPointerException e) {
            this.sendExceptionMessage("[takeSnapshot] Internal error was occurred. Could not get Nav cam image.", e);
            return false;
        }
        catch (Exception e) {
            this.sendExceptionMessage("[takeSnapshot] Internal error was occurred. Unable to send data to gds.", e);
            return false;
        }
    }

    private JSONObject getTargetInfoObject(int phaseNumber, int targetId) throws JSONException {
        JSONObject data = new JSONObject();
        data.put("target_id", targetId);
        data.put("phase_number", phaseNumber);
        return data;
    }

    private void saveBitmap(Bitmap saveImage, String phaseNumber, String targetId) throws SecurityException, IOException, NullPointerException {
        Log.v((String)"KiboRpcApi", (String)"[Start] saveBitmap");
        String filepath = this.gsService.getGuestScienceDataBasePath() + TARGET_IMAGES_SAVE_DIR;
        File file = new File(filepath);
        if (!file.exists()) {
            Log.v((String)"KiboRpcApi", (String)"[saveBitmap] Make save directory");
            file.mkdir();
        }
        Date mDate = new Date();
        SimpleDateFormat fileNameDate = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String fileName = "Phase_" + phaseNumber + "_Target_" + targetId + "_" + fileNameDate.format(mDate) + ".png";
        String AttachName = file.getAbsolutePath() + "/" + fileName;
        FileOutputStream out = new FileOutputStream(AttachName);
        saveImage.compress(Bitmap.CompressFormat.PNG, 100, (OutputStream)out);
        out.flush();
        out.close();
        Log.v((String)"KiboRpcApi", (String)"[Finish] saveBitmap");
    }

    public double[][] getNavCamIntrinsics() {
        Log.i((String)"KiboRpcApi", (String)"[Start] getNavCamIntrinsics");
        double[][] camera_param = new double[2][];
        if (this.getterNode.getOnSimulation()) {
            Log.v((String)"KiboRpcApi", (String)"[getNavCamIntrinsics] getOnSimulation: True");
            Log.v((String)"KiboRpcApi", (String)"[getNavCamIntrinsics] Set simulation param");
            camera_param[0] = this.NAVCAM_CAMERA_MATRIX_SIMULATION;
            camera_param[1] = this.NAVCAM_DISTORTION_COEFFICIENTS_SIMULATION;
        } else {
            Log.v((String)"KiboRpcApi", (String)"[getNavCamIntrinsics] Set ISS param");
            camera_param[0] = this.NAVCAM_CAMERA_MATRIX_ISS;
            camera_param[1] = this.NAVCAM_DISTORTION_COEFFICIENTS_ISS;
        }
        Log.i((String)"KiboRpcApi", (String)"[Finish] getNavCamIntrinsics");
        return camera_param;
    }

    public double[][] getDockCamIntrinsics() {
        Log.i((String)"KiboRpcApi", (String)"[Start] getDockCamIntrinsics");
        double[][] camera_param = new double[2][];
        if (this.getterNode.getOnSimulation()) {
            Log.v((String)"KiboRpcApi", (String)"[getDockCamIntrinsics] getOnSimulation: True");
            Log.v((String)"KiboRpcApi", (String)"[getDockCamIntrinsics] Set simulation param");
            camera_param[0] = this.DOCKCAM_CAMERA_MATRIX_SIMULATION;
            camera_param[1] = this.DOCKCAM_DISTORTION_COEFFICIENTS_SIMULATION;
        } else {
            Log.v((String)"KiboRpcApi", (String)"[getDockCamIntrinsics] Set ISS param");
            camera_param[0] = this.DOCKCAM_CAMERA_MATRIX_ISS;
            camera_param[1] = this.DOCKCAM_DISTORTION_COEFFICIENTS_ISS;
        }
        Log.i((String)"KiboRpcApi", (String)"[Finish] getDockCamIntrinsics");
        return camera_param;
    }

    public void saveBitmapImage(Bitmap image, String imageName) {
        try {
            Log.i((String)"KiboRpcApi", (String)"[Start] saveBitmapImage");
            boolean checkArgs = true;
            if (image == null) {
                Log.e((String)"KiboRpcApi", (String)"[saveBitmapImage] image is null");
                checkArgs = false;
            }
            if (imageName == null) {
                Log.e((String)"KiboRpcApi", (String)"[saveBitmapImage] imageName is null");
                checkArgs = false;
            }
            if (checkArgs) {
                Log.i((String)"KiboRpcApi", (String)("Parameters: image, imageName == " + imageName));
                if (this.getterNode.getOnSimulation()) {
                    Log.v((String)"KiboRpcApi", (String)"[saveBitmapImage] getOnSimulation: True");
                    Log.v((String)"KiboRpcApi", (String)"[saveBitmapImage] Check directry");
                    String filepath = this.gsService.getGuestScienceDataBasePath() + DEBUG_IMAGES_SAVE_DIR;
                    File file = new File(filepath);
                    if (!file.exists()) {
                        Log.v((String)"KiboRpcApi", (String)"[saveBitmapImage] Make save directry");
                        file.mkdir();
                    }
                    File[] list = file.listFiles();
                    int width = image.getWidth();
                    int height = image.getHeight();
                    int size = width * height;
                    if (list.length >= 50) {
                        Log.e((String)"KiboRpcApi", (String)"[saveBitmapImage] Can't save more than 50 images");
                    } else if (size > 1228800) {
                        Log.e((String)"KiboRpcApi", (String)"[saveBitmapImage] The size is too large.");
                    } else {
                        Log.v((String)"KiboRpcApi", (String)"[saveBitmapImage] Save bitmap image");
                        this.saveImage(image, imageName, file);
                    }
                } else {
                    Log.v((String)"KiboRpcApi", (String)"[saveBitmapImage] getOnSimulation: False");
                }
            }
            Log.i((String)"KiboRpcApi", (String)"[Finish] saveBitmapImage");
        }
        catch (Exception e) {
            this.sendExceptionMessage("[saveBitmapImage] Internal error was occurred.", e);
        }
    }

    public void saveMatImage(Mat image, String imageName) {
        try {
            Log.i((String)"KiboRpcApi", (String)"[Start] saveMatImage");
            boolean checkArgs = true;
            if (image == null) {
                Log.e((String)"KiboRpcApi", (String)"[saveMatImage] image is null");
                checkArgs = false;
            }
            if (imageName == null) {
                Log.e((String)"KiboRpcApi", (String)"[saveMatImage] imageName is null");
                checkArgs = false;
            }
            if (checkArgs) {
                Log.i((String)"KiboRpcApi", (String)("Parameters: image, imageName == " + imageName));
                if (this.getterNode.getOnSimulation()) {
                    Log.v((String)"KiboRpcApi", (String)"[saveMatImage] getOnSimulation: True");
                    Log.v((String)"KiboRpcApi", (String)"[saveMatImage] Check directry");
                    String filepath = this.gsService.getGuestScienceDataBasePath() + DEBUG_IMAGES_SAVE_DIR;
                    File file = new File(filepath);
                    if (!file.exists()) {
                        Log.v((String)"KiboRpcApi", (String)"[saveMatImage] make save directory");
                        file.mkdir();
                    }
                    File[] list = file.listFiles();
                    int width = image.width();
                    int height = image.height();
                    int size = width * height;
                    if (list.length >= 50) {
                        Log.e((String)"KiboRpcApi", (String)"[saveMatImage] Can't save more than 50 images.");
                    } else if (size > 1228800) {
                        Log.e((String)"KiboRpcApi", (String)"[saveMatImage] The size is too large.");
                    } else {
                        Log.v((String)"KiboRpcApi", (String)"[saveMatImage] Save mat image");
                        Bitmap bitmapImage = Bitmap.createBitmap((int)image.width(), (int)image.height(), (Bitmap.Config)Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap((Mat)image, (Bitmap)bitmapImage);
                        this.saveImage(bitmapImage, imageName, file);
                    }
                } else {
                    Log.v((String)"KiboRpcApi", (String)"[saveMatImage] getOnSimulation: False");
                }
            }
            Log.i((String)"KiboRpcApi", (String)"[Finish] saveMatImage");
        }
        catch (Exception e) {
            this.sendExceptionMessage("[saveMatImage] Internal error was occurred.", e);
        }
    }

    private void saveImage(Bitmap image, String imageName, File file) throws SecurityException, IOException, NullPointerException {
        String AttachName = file.getAbsolutePath() + "/" + imageName;
        Log.v((String)"KiboRpcApi", (String)AttachName);
        FileOutputStream out = new FileOutputStream(AttachName);
        image.compress(Bitmap.CompressFormat.PNG, 100, (OutputStream)out);
        out.flush();
        out.close();
    }

    public List<Long> getTimeRemaining() {
        Log.i((String)"KiboRpcApi", (String)"[Start] getTimeRemaining");
        List<Long> list = this.gameManager.getTimeRemaining();
        Log.i((String)"KiboRpcApi", (String)"[Finish] getTimeRemaining");
        return list;
    }

    public List<Integer> getActiveTargets() {
        Log.i((String)"KiboRpcApi", (String)"[Start] getActiveTargets");
        List<Integer> list = this.gameManager.getActiveTargets();
        Log.i((String)"KiboRpcApi", (String)"[Finish] getActiveTargets");
        return list;
    }
}

