/*
 * Decompiled with CFR 0.152.
 */
package jp.jaxa.iss.kibo.rpc.api;

import android.util.Log;
import ff_msgs.SignalState;
import java.net.URI;
import org.ros.android.NodeMainExecutorService;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.NodeConfiguration;
import org.ros.node.NodeMain;
import org.ros.node.topic.Publisher;

public class SetterNode
extends AbstractNodeMain {
    public static SetterNode instance = null;
    private NodeMainExecutorService nodeMainExecutorService;
    private Publisher<SignalState> signalStatePublisher;
    private static final URI ROS_MASTER_URI = URI.create("http://llp:11311");
    private static final String EMULATOR_ROS_HOSTNAME = "hlp";
    static final int PUB_SLEEP_TIME_MS = 10;
    static final String NODE_NAME = "SetterNode";
    static final String TOPIC_SIGNALS = "signals";
    private final Object signal_state_lock = new Object();

    private SetterNode() {
        this.nodeMainExecutorService = new NodeMainExecutorService();
        NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic((String)EMULATOR_ROS_HOSTNAME);
        nodeConfiguration.setMasterUri(ROS_MASTER_URI);
        this.nodeMainExecutorService.execute((NodeMain)this, nodeConfiguration);
    }

    public GraphName getDefaultNodeName() {
        return GraphName.of((String)NODE_NAME);
    }

    public void onStart(ConnectedNode connectedNode) {
        Log.d((String)"KiboRpcApi", (String)"SetterNode initialization started.");
        try {
            this.signalStatePublisher = connectedNode.newPublisher(TOPIC_SIGNALS, "ff_msgs/SignalState");
            this.signalStatePublisher.setLatchMode(true);
            instance = this;
            Log.d((String)"KiboRpcApi", (String)"SetterNode initialization succeeded.");
        }
        catch (Exception e) {
            Log.e((String)"KiboRpcApi", (String)"SetterNode initialization failed, msg:", (Throwable)e);
        }
    }

    public static SetterNode getInstance() {
        if (instance == null) {
            instance = new SetterNode();
        }
        return instance;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public boolean setSignalState(Byte signalState, int times) {
        Object object = this.signal_state_lock;
        synchronized (object) {
            if (times <= 0) {
                Log.w((String)"KiboRpcApi", (String)("SetterNode setSignalState : Specified times <" + times + "> is not correct."));
                return false;
            }
            switch (signalState) {
                case 0: {
                    Log.i((String)"KiboRpcApi", (String)"SetterNode setSignalState status : VIDEO_ON");
                    break;
                }
                case 1: {
                    Log.i((String)"KiboRpcApi", (String)"SetterNode setSignalState status : VIDEO_OFF");
                    break;
                }
                case 3: {
                    Log.i((String)"KiboRpcApi", (String)"SetterNode setSignalState status : SUCCESS");
                    break;
                }
                case 4: {
                    Log.i((String)"KiboRpcApi", (String)"SetterNode setSignalState status : ENTER_HATCHWAY");
                    break;
                }
                case 5: {
                    Log.i((String)"KiboRpcApi", (String)"SetterNode setSignalState status : UNDOCK");
                    break;
                }
                case 6: {
                    Log.i((String)"KiboRpcApi", (String)"SetterNode setSignalState status : UNPERCH");
                    break;
                }
                case 7: {
                    Log.i((String)"KiboRpcApi", (String)"SetterNode setSignalState status : MOTION_IMPAIRED");
                    break;
                }
                case 8: {
                    Log.i((String)"KiboRpcApi", (String)"SetterNode setSignalState status : THRUST_FORWARD");
                    break;
                }
                case 9: {
                    Log.i((String)"KiboRpcApi", (String)"SetterNode setSignalState status : THRUST_AFT");
                    break;
                }
                case 10: {
                    Log.i((String)"KiboRpcApi", (String)"SetterNode setSignalState status : TURN_RIGHT");
                    break;
                }
                case 11: {
                    Log.i((String)"KiboRpcApi", (String)"SetterNode setSignalState status : TURN_LEFT");
                    break;
                }
                case 12: {
                    Log.i((String)"KiboRpcApi", (String)"SetterNode setSignalState status : TURN_UP");
                    break;
                }
                case 13: {
                    Log.i((String)"KiboRpcApi", (String)"SetterNode setSignalState status : TURN_DOWN");
                    break;
                }
                case 14: {
                    Log.i((String)"KiboRpcApi", (String)"SetterNode setSignalState status : CLEAR");
                    break;
                }
                case 15: {
                    Log.i((String)"KiboRpcApi", (String)"SetterNode setSignalState status : SLEEP");
                    break;
                }
                case 16: {
                    Log.i((String)"KiboRpcApi", (String)"SetterNode setSignalState status : WAKE");
                    break;
                }
                case 17: {
                    Log.i((String)"KiboRpcApi", (String)"SetterNode setSignalState status : STOP_ALL_LIGHTS");
                    break;
                }
                case 18: {
                    Log.i((String)"KiboRpcApi", (String)"SetterNode setSignalState status : CHARGING");
                    break;
                }
                default: {
                    Log.w((String)"KiboRpcApi", (String)"SetterNode setSignalState : Specified state is unknown.");
                    return false;
                }
            }
            Log.i((String)"KiboRpcApi", (String)("SetterNode setSignalState times  : " + times));
            SignalState message = (SignalState)this.signalStatePublisher.newMessage();
            message.setState(signalState.byteValue());
            try {
                for (int i = 0; i < times; ++i) {
                    this.signalStatePublisher.publish(message);
                    Log.i((String)"KiboRpcApi", (String)("SetterNode setSignalState publish " + (i + 1) + " th"));
                    Thread.sleep(10L);
                }
            }
            catch (InterruptedException e) {
                Log.e((String)"KiboRpcApi", (String)"SetterNode setSignalState All publishes may not have been possible.", (Throwable)e);
                return false;
            }
            return true;
        }
    }
}

