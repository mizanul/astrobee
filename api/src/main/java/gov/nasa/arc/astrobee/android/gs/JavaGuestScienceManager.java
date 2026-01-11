/* Copyright (c) 2017, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 *
 * All rights reserved.
 *
 * The Astrobee platform is licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package gov.nasa.arc.astrobee.android.gs;

import java.util.Date;

import gov.nasa.arc.astrobee.ros.RobotConfiguration;
import gov.nasa.arc.astrobee.ros.NodeExecutorHolder;
import gov.nasa.arc.astrobee.android.gs.MessageType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.node.NodeConfiguration;
import gov.nasa.arc.astrobee.Kinematics;

import org.ros.message.Time;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONObject;
import org.json.JSONArray;

import jp.jaxa.iss.kibo.rpc.api.types.ImuResult;
import jp.jaxa.iss.kibo.rpc.api.types.MonoImage;
import jp.jaxa.iss.kibo.rpc.api.types.PointCloud;

import android.graphics.Bitmap;
import org.opencv.core.Mat;

import org.ros.message.Duration;

import java.util.ArrayList;
import java.util.List;

import jp.jaxa.iss.kibo.rpc.api.sub.Phase;
import jp.jaxa.iss.kibo.rpc.api.GetterNode;

/**
 * Construct Manager in your main (JavaGuestScienceManager will make the
 * NodeMain (ie GetterNode)
 * Implement AppInterface
 * Give AppIntImpl to the JavaGuestScienceManager
 * JavaGuestScienceManager calls AppImpl when it needs to tell it something
 * AppImpl has to be able to send commands through this.
 */
public class JavaGuestScienceManager {
    private final Log logger = LogFactory.getLog(JavaGuestScienceManager.class);
    private Phase[] Phases;
    private int targetPhaseIndex;
    private final Object mLock;
    private boolean isMissionStart;
    private final DateFormat df;

    GetterNode m_nodeMain;
    StartGuestScienceService m_app;

    private Time tmActiveStart;
    private Time tmActiveLimit;

    private Time tmMissionStart;
    private Time tmMissionLimit;

    private int phaseNum;
    private boolean isLoop;
    private boolean isPhaseOver;
    private Time tmLastPhaseInfoNotice;


    public JavaGuestScienceManager() {
        logger.info("JavaGuestScienceManager() ctor");
        this.targetPhaseIndex = -1;
        this.isMissionStart = false;
        this.mLock = new Object();
        this.df = new SimpleDateFormat("yyyyMMdd hhmmssSSS");
        this.tmActiveStart = null;
         this.tmActiveLimit = null;
         this.tmMissionStart = null;
        this.tmMissionLimit = null;

        this.phaseNum = 0;
        this.isLoop = true;
        this.isPhaseOver = false;
        this.tmLastPhaseInfoNotice = null;

        RobotConfiguration robotConfiguration = new RobotConfiguration();
        final NodeConfiguration nodeConfiguration = robotConfiguration.build();
        m_nodeMain = GetterNode.getInstance();
        NodeExecutorHolder.getExecutor().execute(m_nodeMain, nodeConfiguration);

        logger.info("JavaGuestScienceManager() ctor finished");
    }

    public boolean acceptApplication(StartGuestScienceService app) {
        m_app = app;
        m_app.acceptManager(this);
        try {
            while (!m_nodeMain.isStarted()) {
                Thread.sleep(250);
            }
        } catch (InterruptedException e) {
            return false;
        }

        m_nodeMain.sendGuestScienceState();
        m_nodeMain.publishGuestScienceConfig(m_app);

        return true;
    }

    public void sendData(MessageType type, String topic, byte[] byteData) {
        if (byteData.length > 2048) {
            throw new RuntimeException("Data passed to sendData function is too big to send to " +
                    "ground. Must be 2K.");
        }
        m_nodeMain.sendGuestScienceData(m_app.getFullName(), topic, byteData, type);
    }

    public void sendData(MessageType type, String topic, String dataString) {
        byte[] byteData = dataString.getBytes();
        if (byteData.length > 2048) {
            throw new RuntimeException("Data passed to sendData function is too big to send to " +
                    "ground. Must be 2K.");
        }
        m_nodeMain.sendGuestScienceData(m_app.getFullName(), topic, byteData, type);
    }

    public boolean undocked() {
        return m_nodeMain.undocked();
    }

    public Bitmap getBitmapDockCam() {
      Bitmap ret = m_nodeMain.getBitmapDockCam();
      return ret;
   }

public Mat getMatNavCam() {
      Mat ret = m_nodeMain.getMatNavCam();
      return ret;
   }

   public Mat getMatDockCam() {
      Mat ret = m_nodeMain.getMatDockCam();
      return ret;
   }

       public Kinematics getCurrentKinematics() {
      return m_nodeMain.getCurrentKinematics();
   }

   public ImuResult getImu() {
      ImuResult ret = m_nodeMain.getImu();
      return ret;
   }

     public boolean getOnSimulation() {
      return m_nodeMain.getOnSimulation();
   }

     public Bitmap getBitmapNavCam() {
      return m_nodeMain.getBitmapNavCam();
   }

    public List<Integer> getActiveTargets() {
        synchronized (this.mLock) {
            if (this.isMissionStart) {
                return this.Phases[this.targetPhaseIndex].getTargetActiveList();
            }
             logger.debug("Stemx365RpcApi [GameManager] mission not started. (in getActiveTargets())");
            return new ArrayList<Integer>();
        }
    }

     public JSONObject getRandomPhasesConfig() throws JSONException {
        return m_nodeMain.getRandomPhasesConfig();
    }

    public Time getCurrentTime() {
        return m_nodeMain.getCurrentTime();
    }

     public boolean setSignalState(final Byte signalState, final int times) {
        return m_nodeMain.setSignalState(signalState,times);
     }

    public int getCurrentPhaseNumber() {
        synchronized (this.mLock) {
            if (this.targetPhaseIndex < 0) {
                return 0;
            }
            return this.targetPhaseIndex + 1;
        }
    }

    public boolean targetDeactivation(final int targetId) {
        synchronized (this.mLock) {
            if (!this.isMissionStart) {
                 logger.debug("Stemx365RpcApi [GameManager] mission not started. (in targetDeactivation())");
                return false;
            }
            try {
                this.Phases[this.targetPhaseIndex].setTargetActive(targetId, false);
                this.sendPhaseInfo();
                return true;
            }
            catch (Exception e) {
                 logger.error("Stemx365RpcApi [GameManager] Fail to deactivate the target.", (Throwable)e);
                return false;
            }
        }
    }

      private void sendPhaseInfo() {
         logger.debug("Stemx365RpcApi [Start] sendPhaseInfo");
        if (this.isMissionStart) {
            try {
                final JSONObject data = new JSONObject();
                data.put("phase_number", this.targetPhaseIndex + 1);
                data.put("t_stamp_phase", (Object)this.df.format(new Date(System.currentTimeMillis())));
                data.put("active_time", (Object)this.getActiveTime());
                final List<Integer> l = this.Phases[this.targetPhaseIndex].getTargetActiveList();
                final int[] array = new int[l.size()];
                for (int i = 0; i < array.length; ++i) {
                    array[i] = l.get(i);
                }
                final JSONArray jsonArray = new JSONArray((Object)array);
                data.put("active_targets", (Object)jsonArray);
                this.sendData(MessageType.JSON, "data", data.toString());
                 logger.debug("Stemx365RpcApi sendPhaseInfo: " + data.toString());
            }
            catch (JSONException e) {
                this.sendExceptionMessage("[GameManager] Failed to send a phase info.", (Exception)e);
            }
        }
        else {
             logger.debug("Stemx365RpcApi [GameManager] mission not started. (in sendPhaseInfo())");
        }
         logger.debug("Stemx365RpcApi [Finish] sendPhaseInfo");
    }

       private String getActiveTime() {
        final Time now = this.getCurrentTime();
        final Duration duration = now.subtract(this.tmActiveStart);
        duration.normalize();
        final int min = duration.secs / 60;
        final int sec = duration.secs % 60;
        final int msec = duration.nsecs / 1000000;
        return String.format("%02d:%02d.%03d", min, sec, msec);
    }

 private boolean switchNextPhase() {
         logger.debug("Stemx365RpcApi [Start] switchNextPhase");
        synchronized (this.mLock) {
            final int curentIndex = this.targetPhaseIndex;
            if (this.targetPhaseIndex < 0) {
                this.targetPhaseIndex = 0;
            }
            else if (this.phaseNum == this.targetPhaseIndex + 1) {
                this.isPhaseOver = true;
                 logger.debug("Stemx365RpcApi [switchNextPhase] phase is over.");
            }
            else {
                ++this.targetPhaseIndex;
            }
            if (curentIndex < this.targetPhaseIndex) {
                this.tmActiveStart = this.getCurrentTime();
                this.tmActiveLimit = new Time(this.tmActiveStart.secs, this.tmActiveStart.nsecs);
                final Time tmActiveLimit = this.tmActiveLimit;
                tmActiveLimit.secs += (int)120L;
                final String msg1 = String.format("switching phase from %d to %d.", curentIndex + 1, this.targetPhaseIndex + 1);
                final String msg2 = String.format("phase(%d): %s", this.targetPhaseIndex + 1, this.Phases[this.targetPhaseIndex].toString());
                 logger.debug("Stemx365RpcApi [switchNextPhase] " + msg1);
                 logger.debug("Stemx365RpcApi [switchNextPhase] " + msg2);
                 logger.debug("Stemx365RpcApi [Finish] switchNextPhase");
                return true;
            }
             logger.debug("Stemx365RpcApi [switchNextPhase] Did not switch phases.");
             logger.debug("Stemx365RpcApi [Finish] switchNextPhase");
            return false;
        }
    }
    

    public void startMission() {
        logger.debug("Stemx365RpcApi [GameManager] startMission");
        this.tmMissionStart = this.getCurrentTime();
        this.tmMissionLimit = new Time(this.tmMissionStart.secs, this.tmMissionStart.nsecs);
        final Time tmMissionLimit = this.tmMissionLimit;
        tmMissionLimit.secs += (int)300L;
        this.tmLastPhaseInfoNotice = new Time(this.tmMissionStart.secs, this.tmMissionStart.nsecs);
        this.isMissionStart = true;
        this.targetPhaseIndex = -1;
        this.switchNextPhase();
    }

    private void sendExceptionMessage(final String errmsg, final Exception err) {
         logger.debug("Stemx365RpcApi [GameManager] sendExceptionMessage start.");
        try {
            final JSONObject data = new JSONObject();
            data.put("error", (Object)("[" + err.getClass().getName() + "] " + errmsg));
             logger.error("Stemx365RpcApi [GameManager] " + errmsg, (Throwable)err);
            this.sendData(MessageType.JSON, "data", data.toString());
        }
        catch (JSONException e) {
            this.sendData(MessageType.JSON, "data", e.getClass().getName());
             logger.error("Stemx365RpcApi [GameManager] sendExceptionMessage error.", (Throwable)e);
        }
         logger.debug("Stemx365RpcApi [GameManager] sendExceptionMessage finish.");
    }

    public List<Long> getTimeRemaining() {
        synchronized (this.mLock) {
            final List<Long> list = new ArrayList<Long>();
            list.add(this.getActiveTimeRemain());
            list.add(this.getMissionTimeRemain());
            return list;
        }
    }

      private long getActiveTimeRemain() {
         logger.debug("Stemx365RpcApi [Start] getActiveTimeRemain");
        long ret = 0L;
        if (this.isMissionStart) {
            final Time now = this.getCurrentTime();
            if (this.tmActiveLimit.compareTo(now) > 0) {
                final Duration duration = this.tmActiveLimit.subtract(now);
                duration.normalize();
                ret = duration.totalNsecs() / 1000000L;
            }
        }
        else {
             logger.debug("Stemx365RpcApi [GameManager] mission not started. (in getActiveTimeRemain())");
        }
         logger.debug("Stemx365RpcApi [getActiveTimeRemain] ActiveTimeRemain: " + ret);
         logger.debug("Stemx365RpcApi [Finish] getActiveTimeRemain");
        return ret;
    }

        private long getMissionTimeRemain() {
         logger.debug("Stemx365RpcApi [Start] getMissionTimeRemain");
        long ret = 0L;
        if (this.isMissionStart) {
            final Time now = this.getCurrentTime();
            if (this.tmMissionLimit.compareTo(now) > 0) {
                final Duration duration = this.tmMissionLimit.subtract(now);
                duration.normalize();
                ret = duration.totalNsecs() / 1000000L;
            }
        }
        else {
             logger.debug("Stemx365RpcApi [GameManager] mission not started. (in getMissionTimeRemain())");
        }
         logger.debug("Stemx365RpcApi [getMissionTimeRemain] MissionTimeRemain: " + ret);
         logger.debug("Stemx365RpcApi [Finish] getMissionTimeRemain");
        return ret;
    }
}
