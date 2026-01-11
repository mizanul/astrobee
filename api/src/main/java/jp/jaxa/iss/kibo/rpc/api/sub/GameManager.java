/*
 * Decompiled with CFR 0.152.
 */
package jp.jaxa.iss.kibo.rpc.api.sub;

import android.util.Log;
import gov.nasa.arc.astrobee.android.gs.MessageType;
import gov.nasa.arc.astrobee.android.gs.StartGuestScienceService;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import jp.jaxa.iss.kibo.rpc.api.GetterNode;
import jp.jaxa.iss.kibo.rpc.api.sub.Phase;
import jp.jaxa.iss.kibo.rpc.api.sub.PhaseConfig;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ros.message.Duration;
import org.ros.message.Time;

public class GameManager
extends Thread {
    private static final long MISSION_TIME_LIMIT = 300L;
    private static final long ACTIVE_TIME_LIMIT = 120L;
    private static final int PHASE_INFO_CYCLE = 30;
    private final int WAIT_NODE_ONSTART_RETRY = 3;
    private final int WAIT_NODE_ONSTART_SLEEP_MS = 1000;
    private static GameManager instance = null;
    private final DateFormat df = new SimpleDateFormat("yyyyMMdd hhmmssSSS");
    private StartGuestScienceService gsService;
    private GetterNode getterNode;
    private PhaseConfig phaseConfig;
    private int phaseNum = 0;
    private Phase[] Phases;
    private boolean isLoop = true;
    private boolean isPhaseConfigLoaded = false;
    private boolean isMissionStart = false;
    private boolean isPhaseOver = false;
    private int targetPhaseIndex = -1;
    private Time tmMissionStart = null;
    private Time tmMissionLimit = null;
    private Time tmActiveStart = null;
    private Time tmActiveLimit = null;
    private Time tmLastPhaseInfoNotice = null;
    private final Object mLock = new Object();

    private GameManager(StartGuestScienceService startGuestScienceService) {
        Log.v((String)"KiboRpcApi", (String)"[GameManager] initialization started.");
        try {
            this.gsService = startGuestScienceService;
            this.getterNode = GetterNode.getInstance();
            int i = 1;
            while (!this.getterNode.isNodeStarted()) {
                Thread.sleep(1000L);
                Log.i((String)"KiboRpcApi", (String)("[GameManager] Wait for node started.. Retry " + i));
                if (3 <= i) {
                    Log.e((String)"KiboRpcApi", (String)"[GameManager] Wait for node started... Retry over!");
                    throw new Exception("[GameManager] Wait for node started... Retry over!");
                }
                ++i;
            }
            String configDir = this.gsService.getGuestScienceDataBasePath();
            this.phaseConfig = new PhaseConfig(this.getterNode, configDir);
            this.phaseNum = this.phaseConfig.getPhaseNum();
            this.Phases = this.phaseConfig.getPhases();
            this.isPhaseConfigLoaded = true;
            Log.v((String)"KiboRpcApi", (String)"[GameManager] initialization succeeded.");
        }
        catch (Exception e) {
            this.sendFailedLoadPhaseConfig(e);
            Log.e((String)"KiboRpcApi", (String)"[GameManager] initialization failed.");
        }
    }

    public static GameManager getInstance(StartGuestScienceService startGuestScienceService) {
        if (instance == null) {
            instance = new GameManager(startGuestScienceService);
        }
        return instance;
    }

    @Override
    public void run() {
        while (this.isLoop) {
            if (this.isPhaseConfigLoaded && this.isMissionStart) {
                Time now = this.getCurrentTime();
                if (this.tmActiveLimit == null) continue;
                if (!this.isPhaseOver && this.tmActiveLimit.compareTo(now) < 0) {
                    if (this.switchNextPhase()) {
                        this.sendPhaseInfo();
                    }
                } else if (!this.isPhaseOver && this.Phases[this.targetPhaseIndex].getTargetActiveList().size() <= 0) {
                    if (this.switchNextPhase()) {
                        this.sendPhaseInfo();
                    }
                } else if (this.tmLastPhaseInfoNotice != null) {
                    Duration duration = now.subtract(this.tmLastPhaseInfoNotice);
                    duration.normalize();
                    if (duration.secs >= 30) {
                        this.periodicPhaseInfoNotification();
                        this.tmLastPhaseInfoNotice.secs = now.secs;
                        this.tmLastPhaseInfoNotice.nsecs = now.nsecs;
                    }
                }
            }
            Thread.yield();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public int getCurrentPhaseNumber() {
        Object object = this.mLock;
        synchronized (object) {
            if (this.targetPhaseIndex < 0) {
                return 0;
            }
            return this.targetPhaseIndex + 1;
        }
    }

    public String getCorrectReport() {
        return this.phaseConfig.getCorrectReport();
    }

    public void startMission() {
        Log.v((String)"KiboRpcApi", (String)"[GameManager] startMission");
        this.tmMissionStart = this.getCurrentTime();
        this.tmMissionLimit = new Time(this.tmMissionStart.secs, this.tmMissionStart.nsecs);
        this.tmMissionLimit.secs = (int)((long)this.tmMissionLimit.secs + 300L);
        this.tmLastPhaseInfoNotice = new Time(this.tmMissionStart.secs, this.tmMissionStart.nsecs);
        this.isMissionStart = true;
        this.targetPhaseIndex = -1;
        this.switchNextPhase();
    }

    @Deprecated
    public void stopMission() {
        Log.v((String)"KiboRpcApi", (String)"[GameManager] stopMission");
        this.isMissionStart = false;
        this.targetPhaseIndex = -1;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public List<Integer> getActiveTargets() {
        Object object = this.mLock;
        synchronized (object) {
            if (this.isMissionStart) {
                return this.Phases[this.targetPhaseIndex].getTargetActiveList();
            }
            Log.w((String)"KiboRpcApi", (String)"[GameManager] mission not started. (in getActiveTargets())");
            return new ArrayList<Integer>();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean targetDeactivation(int targetId) {
        Object object = this.mLock;
        synchronized (object) {
            if (!this.isMissionStart) {
                Log.w((String)"KiboRpcApi", (String)"[GameManager] mission not started. (in targetDeactivation())");
                return false;
            }
            try {
                this.Phases[this.targetPhaseIndex].setTargetActive(targetId, false);
                this.sendPhaseInfo();
                return true;
            }
            catch (Exception e) {
                Log.e((String)"KiboRpcApi", (String)"[GameManager] Fail to deactivate the target.", (Throwable)e);
                return false;
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public List<Long> getTimeRemaining() {
        Object object = this.mLock;
        synchronized (object) {
            ArrayList<Long> list = new ArrayList<Long>();
            list.add(this.getActiveTimeRemain());
            list.add(this.getMissionTimeRemain());
            return list;
        }
    }

    private long getActiveTimeRemain() {
        Log.v((String)"KiboRpcApi", (String)"[Start] getActiveTimeRemain");
        long ret = 0L;
        if (this.isMissionStart) {
            Time now = this.getCurrentTime();
            if (this.tmActiveLimit.compareTo(now) > 0) {
                Duration duration = this.tmActiveLimit.subtract(now);
                duration.normalize();
                ret = duration.totalNsecs() / 1000000L;
            }
        } else {
            Log.w((String)"KiboRpcApi", (String)"[GameManager] mission not started. (in getActiveTimeRemain())");
        }
        Log.v((String)"KiboRpcApi", (String)("[getActiveTimeRemain] ActiveTimeRemain: " + ret));
        Log.v((String)"KiboRpcApi", (String)"[Finish] getActiveTimeRemain");
        return ret;
    }

    private long getMissionTimeRemain() {
        Log.v((String)"KiboRpcApi", (String)"[Start] getMissionTimeRemain");
        long ret = 0L;
        if (this.isMissionStart) {
            Time now = this.getCurrentTime();
            if (this.tmMissionLimit.compareTo(now) > 0) {
                Duration duration = this.tmMissionLimit.subtract(now);
                duration.normalize();
                ret = duration.totalNsecs() / 1000000L;
            }
        } else {
            Log.w((String)"KiboRpcApi", (String)"[GameManager] mission not started. (in getMissionTimeRemain())");
        }
        Log.v((String)"KiboRpcApi", (String)("[getMissionTimeRemain] MissionTimeRemain: " + ret));
        Log.v((String)"KiboRpcApi", (String)"[Finish] getMissionTimeRemain");
        return ret;
    }

    private Time getCurrentTime() {
        return this.getterNode.getCurrentTime();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private boolean switchNextPhase() {
        Log.v((String)"KiboRpcApi", (String)"[Start] switchNextPhase");
        Object object = this.mLock;
        synchronized (object) {
            int curentIndex = this.targetPhaseIndex;
            if (this.targetPhaseIndex < 0) {
                this.targetPhaseIndex = 0;
            } else if (this.phaseNum == this.targetPhaseIndex + 1) {
                this.isPhaseOver = true;
                Log.v((String)"KiboRpcApi", (String)"[switchNextPhase] phase is over.");
            } else {
                ++this.targetPhaseIndex;
            }
            if (curentIndex < this.targetPhaseIndex) {
                this.tmActiveStart = this.getCurrentTime();
                this.tmActiveLimit = new Time(this.tmActiveStart.secs, this.tmActiveStart.nsecs);
                this.tmActiveLimit.secs = (int)((long)this.tmActiveLimit.secs + 120L);
                String msg1 = String.format("switching phase from %d to %d.", curentIndex + 1, this.targetPhaseIndex + 1);
                String msg2 = String.format("phase(%d): %s", this.targetPhaseIndex + 1, this.Phases[this.targetPhaseIndex].toString());
                Log.v((String)"KiboRpcApi", (String)("[switchNextPhase] " + msg1));
                Log.v((String)"KiboRpcApi", (String)("[switchNextPhase] " + msg2));
                Log.v((String)"KiboRpcApi", (String)"[Finish] switchNextPhase");
                return true;
            }
            Log.v((String)"KiboRpcApi", (String)"[switchNextPhase] Did not switch phases.");
            Log.v((String)"KiboRpcApi", (String)"[Finish] switchNextPhase");
            return false;
        }
    }

    private void sendExceptionMessage(String errmsg, Exception err) {
        Log.v((String)"KiboRpcApi", (String)"[GameManager] sendExceptionMessage start.");
        try {
            JSONObject data = new JSONObject();
            data.put("error", (Object)("[" + err.getClass().getName() + "] " + errmsg));
            Log.e((String)"KiboRpcApi", (String)("[GameManager] " + errmsg), (Throwable)err);
            this.gsService.sendData(MessageType.JSON, "data", data.toString());
        }
        catch (JSONException e) {
            this.gsService.sendData(MessageType.JSON, "data", ((Object)((Object)e)).getClass().getName());
            Log.e((String)"KiboRpcApi", (String)"[GameManager] sendExceptionMessage error.", (Throwable)e);
        }
        Log.v((String)"KiboRpcApi", (String)"[GameManager] sendExceptionMessage finish.");
    }

    private void sendFailedLoadPhaseConfig(Exception err) {
        Log.v((String)"KiboRpcApi", (String)"[GameManager] sendFailedLoadPhaseConfig start.");
        try {
            JSONObject data = new JSONObject();
            data.put("failed", (Object)"Failed to load phase config");
            Log.e((String)"GameManager", (String)"Failed to load phase config", (Throwable)err);
            this.gsService.sendData(MessageType.JSON, "data", data.toString());
        }
        catch (JSONException e) {
            this.sendExceptionMessage("[GameManager] 'Failed to load phase config' send error.", (Exception)((Object)e));
        }
        Log.v((String)"KiboRpcApi", (String)"[GameManager] sendFailedLoadPhaseConfig finish.");
    }

    private void sendPhaseInfo() {
        Log.v((String)"KiboRpcApi", (String)"[Start] sendPhaseInfo");
        if (this.isMissionStart) {
            try {
                JSONObject data = new JSONObject();
                data.put("phase_number", this.targetPhaseIndex + 1);
                data.put("t_stamp_phase", (Object)this.df.format(new Date(System.currentTimeMillis())));
                data.put("active_time", (Object)this.getActiveTime());
                List<Integer> l = this.Phases[this.targetPhaseIndex].getTargetActiveList();
                int[] array = new int[l.size()];
                for (int i = 0; i < array.length; ++i) {
                    array[i] = l.get(i);
                }
                JSONArray jsonArray = new JSONArray((Object)array);
                data.put("active_targets", (Object)jsonArray);
                this.gsService.sendData(MessageType.JSON, "data", data.toString());
                Log.v((String)"KiboRpcApi", (String)("sendPhaseInfo: " + data.toString()));
            }
            catch (JSONException e) {
                this.sendExceptionMessage("[GameManager] Failed to send a phase info.", (Exception)((Object)e));
            }
        } else {
            Log.w((String)"KiboRpcApi", (String)"[GameManager] mission not started. (in sendPhaseInfo())");
        }
        Log.v((String)"KiboRpcApi", (String)"[Finish] sendPhaseInfo");
    }

    private void periodicPhaseInfoNotification() {
        Log.v((String)"KiboRpcApi", (String)"[Start] periodicPhaseInfoNotification");
        if (this.isMissionStart) {
            try {
                JSONObject data = new JSONObject();
                data.put("periodic_ph_no", this.targetPhaseIndex + 1);
                data.put("periodic_t_stamp", (Object)this.df.format(new Date(System.currentTimeMillis())));
                data.put("periodic_act_time", (Object)this.getActiveTime());
                List<Integer> l = this.Phases[this.targetPhaseIndex].getTargetActiveList();
                int[] array = new int[l.size()];
                for (int i = 0; i < array.length; ++i) {
                    array[i] = l.get(i);
                }
                JSONArray jsonArray = new JSONArray((Object)array);
                data.put("periodic_act_trgts", (Object)jsonArray);
                this.gsService.sendData(MessageType.JSON, "data", data.toString());
                Log.v((String)"KiboRpcApi", (String)("sendPhaseInfo: " + data.toString()));
            }
            catch (JSONException e) {
                this.sendExceptionMessage("[GameManager] Failed to send a phase info.", (Exception)((Object)e));
            }
        } else {
            Log.w((String)"KiboRpcApi", (String)"[GameManager] mission not started. (in periodicPhaseInfoNotification())");
        }
        Log.v((String)"KiboRpcApi", (String)"[Finish] periodicPhaseInfoNotification");
    }

    private String getActiveTime() {
        Time now = this.getCurrentTime();
        Duration duration = now.subtract(this.tmActiveStart);
        duration.normalize();
        int min = duration.secs / 60;
        int sec = duration.secs % 60;
        int msec = duration.nsecs / 1000000;
        return String.format("%02d:%02d.%03d", min, sec, msec);
    }
}

