/*
 * Decompiled with CFR 0.152.
 */
package jp.jaxa.iss.kibo.rpc.api;

import android.util.Log;
import gov.nasa.arc.astrobee.Robot;
import gov.nasa.arc.astrobee.android.gs.MessageType;
import gov.nasa.arc.astrobee.android.gs.StartGuestScienceService;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import jp.jaxa.iss.kibo.rpc.api.GetterNode;
import jp.jaxa.iss.kibo.rpc.api.KiboRpcApi;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.OpenCVLoader;
import org.xbill.DNS.ResolverConfig;

public class KiboRpcService
extends StartGuestScienceService {
    protected KiboRpcApi api = null;
    public Robot robot;
    public GetterNode getterNode;
    private final DateFormat df = new SimpleDateFormat("yyyyMMdd hhmmssSSS");

    public final void onGuestScienceCustomCmd(String command) {
        this.sendReceivedCustomCommand("info");
        try {
            JSONObject jCommand = new JSONObject(command);
            String sCommand = jCommand.getString("name");
            JSONObject jResult = new JSONObject();
            switch (sCommand) {
                case "runPlan1": {
                    try {
                        this.runPlan1();
                    }
                    catch (Exception e) {
                        JSONObject data = new JSONObject();
                        data.put("error", (Object)("Program Down: Exception: " + e.getClass().getName()));
                        this.sendData(MessageType.JSON, "data", data.toString());
                        Log.e((String)"KiboRpcApi", (String)"Program Down: Exception: ", (Throwable)e);
                    }
                    return;
                }
                case "runPlan2": {
                    try {
                        this.runPlan2();
                    }
                    catch (Exception e) {
                        JSONObject data = new JSONObject();
                        data.put("error", (Object)("Program Down: Exception: " + e.getClass().getName()));
                        this.sendData(MessageType.JSON, "data", data.toString());
                        Log.e((String)"KiboRpcApi", (String)"Program Down: Exception: ", (Throwable)e);
                    }
                    return;
                }
                case "runPlan3": {
                    try {
                        this.runPlan3();
                    }
                    catch (Exception e) {
                        JSONObject data = new JSONObject();
                        data.put("error", (Object)("Program Down: Exception: " + e.getClass().getName()));
                        this.sendData(MessageType.JSON, "data", data.toString());
                        Log.e((String)"KiboRpcApi", (String)"Program Down: Exception: ", (Throwable)e);
                    }
                    return;
                }
            }
            this.sendData(MessageType.JSON, "data", "ERROR: Unrecognized command");
            return;
        }
        catch (JSONException e) {
            this.sendData(MessageType.JSON, "data", ((Object)((Object)e)).getClass().getName());
            Log.e((String)"KiboRpcApi", (String)"Program Down: Exception: ", (Throwable)e);
        }
        catch (Exception ex) {
            this.sendData(MessageType.JSON, "data", ex.getClass().getName());
            Log.e((String)"KiboRpcApi", (String)"Program Down: Exception: ", (Throwable)ex);
        }
    }

    public final void onGuestScienceStart() {
        System.setProperty("dns.server", "127.0.0.1");
        System.setProperty("dns.search", "iss");
        ResolverConfig.refresh();
        if (!OpenCVLoader.initDebug()) {
            Log.e((String)"OpenCv", (String)"Unable to load OpenCV");
        } else {
            Log.d((String)"OpenCv", (String)"OpenCV loaded");
        }
        this.api = KiboRpcApi.getInstance(this);
        this.sendStarted("info");
    }

    public final void onGuestScienceStop() {
        this.api.shutdownFactory();
        this.sendStopped("info");
        this.terminate();
    }

    protected void runPlan1() {
    }

    protected void runPlan2() {
    }

    protected void runPlan3() {
    }
}

