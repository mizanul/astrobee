package  jp.jaxa.iss.kibo.rpc.api;

import ff_msgs.*;
import org.ros.message.Time;
import android.graphics.Bitmap;
import gov.nasa.arc.astrobee.AstrobeeRuntimeException;
import gov.nasa.arc.astrobee.ros.NodeExecutorHolder;
import gov.nasa.arc.astrobee.ros.internal.util.CmdInfo;
import gov.nasa.arc.astrobee.ros.internal.util.CmdType;
import gov.nasa.arc.astrobee.ros.internal.util.Constants;
import gov.nasa.arc.astrobee.android.gs.MessageType;
import gov.nasa.arc.astrobee.ros.internal.util.Stringer;

import gov.nasa.arc.astrobee.Kinematics;
import org.opencv.core.Mat;

import gov.nasa.arc.astrobee.ros.DefaultKinematics;

import jp.jaxa.iss.kibo.rpc.api.types.ImuResult;
import jp.jaxa.iss.kibo.rpc.api.types.MonoImage;
import jp.jaxa.iss.kibo.rpc.api.types.PointCloud;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.ros.message.MessageFactory;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.Node;
import org.ros.node.NodeConfiguration;
import org.ros.node.topic.Publisher;
import org.ros.node.topic.Subscriber;
import org.ros.node.parameter.ParameterTree;
import std_msgs.Header;
import java.net.URI;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import sensor_msgs.Image;
import sensor_msgs.Imu;
import sensor_msgs.PointCloud2;
import rosgraph_msgs.Clock;

import gov.nasa.arc.astrobee.ros.gs.Command;

import gov.nasa.arc.astrobee.android.gs.StartGuestScienceService;

//import org.w3c.dom.Node;


public class GetterNode extends AbstractNodeMain implements MessageListener<CommandStamped> {
    private final Log logger = LogFactory.getLog(GetterNode.class);

    public static GetterNode instance = null;
    private ConnectedNode m_node = null;
    private Publisher<AckStamped> m_ackStampedPublisher = null;
    private Publisher<GuestScienceConfig> m_gsConfigPublisher = null;
    private Publisher<GuestScienceData> m_gsDataPublisher = null;
    private Publisher<GuestScienceState> m_gsStatePublisher = null;
    private Publisher<SignalState> signalStatePublisher;

    private Subscriber<CommandStamped> mCommandSubscriber;

    private StartGuestScienceService m_currentApplication;

    private boolean m_started = false;
    private boolean m_apkIsRunning = false;
    private String m_apkName; // for convenience
    private long SERIAL_NUMBER = 1;

    private NodeConfiguration mNodeConfig;
    private MessageFactory mMessageFactory;

    private CmdInfo mCmdInfo;

    /////////////////////////////////////////////////

    private Subscriber<EkfState> ekfSubscriber;
    private Subscriber<Imu> imuSubscriber;
    private Subscriber<Image> navCamSubscriber;
    private Subscriber<Image> dockCamSubscriber;
    private Subscriber<PointCloud2> hazCamSubscriber;
    private Subscriber<PointCloud2> perchCamSubscriber;
    private Subscriber<DockState> dockStateSubscriber;
    private static final URI ROS_MASTER_URI = URI.create("http://llp:11311");
    private static final String EMULATOR_ROS_HOSTNAME = "hlp";
    static final String NODE_NAME = "GetterNode";
    static final String EKF_TOPIC = "/gnc/ekf";
    static final String EKF_NOISE_TOPIC = "/gnc/ekf_noise";
    static final String IMU_TOPIC = "/hw/imu";
    static final String NAV_CAM_TOPIC = "/hw/cam_nav";
    static final String DOCK_CAM_TOPIC = "/hw/cam_dock";
    static final String HAZ_CAM_TOPIC = "/hw/depth_haz/points";
    static final String PERCH_CAM_TOPIC = "/hw/depth_perch/points";
    static final String DOCK_STATE_TOPIC = "/beh/dock/state";
    private DefaultKinematics m_kinematics = new DefaultKinematics();
    private ImuResult imu_pub;
    private MonoImage monoImageNavCam;
    private MonoImage monoImageDockCam;
    private PointCloud point_cloud_haz_cam;
    private PointCloud point_cloud_perch_cam;
    private boolean undocked = false;
    private boolean on_simulation;
   private Clock clock;
   private final Object clock_lock;
    private final Object m_kinematics_lock = new Object();
    private final Object imu_pub_lock = new Object();
    private final Object mono_image_nav_cam_lock = new Object();
    private final Object mono_image_dock_cam_lock = new Object();
    private final Object point_cloud_haz_cam_lock = new Object();
    private final Object point_cloud_perch_cam_lock = new Object();
    private final Object Undock_lock = new Object();
   private final Object phases_info_lock;
   private final Object signal_state_lock;

   private java.lang.String phases_info_json_string;

   public GetterNode() {
        this.phases_info_json_string = null;
        this.phases_info_lock = new Object();
        this.on_simulation = false;
        this.clock_lock = new Object();
        this.signal_state_lock = new Object();
    }

        public static GetterNode getInstance() {
        if (instance == null) {
            instance = new GetterNode();
        }
        return instance;
    }

    @Override
    public synchronized void onStart(final ConnectedNode connectedNode) {

       System.out.print(">>>>>>>>>>>>>>>>>NODE MAIN STRAT  >>>>>>>>>>>>");
        m_node = connectedNode;

        mNodeConfig = NodeConfiguration.newPrivate();
        mMessageFactory = mNodeConfig.getTopicMessageFactory();

        m_gsConfigPublisher = connectedNode.newPublisher(
                Constants.TOPIC_GUEST_SCIENCE_MANAGER_CONFIG, GuestScienceConfig._TYPE);
        m_ackStampedPublisher = connectedNode.newPublisher(
                Constants.TOPIC_GUEST_SCIENCE_MANAGER_ACK, AckStamped._TYPE);
        m_gsDataPublisher = connectedNode.newPublisher(
                Constants.TOPIC_GUEST_SCIENCE_DATA, GuestScienceData._TYPE);
        m_gsStatePublisher = connectedNode.newPublisher(
                Constants.TOPIC_GUEST_SCIENCE_MANAGER_STATE,
                GuestScienceState._TYPE);

        // State and config file topics are latched
        m_gsConfigPublisher.setLatchMode(true);
        m_gsStatePublisher.setLatchMode(true);

        mCommandSubscriber = connectedNode.newSubscriber(
                Constants.TOPIC_MANAGEMENT_EXEC_COMMAND, CommandStamped._TYPE);
        mCommandSubscriber.addMessageListener(this);

        mCmdInfo = new CmdInfo();

        m_started = true;


        try {
         ParameterTree params = connectedNode.getParameterTree();
         this.on_simulation = params.getBoolean("/krpc/on_simulation", false);
         if (this.on_simulation) {
            this.ekfSubscriber = connectedNode.newSubscriber("/gnc/ekf_noise", "ff_msgs/EkfState");
         } else {
            this.ekfSubscriber = connectedNode.newSubscriber("/gnc/ekf", "ff_msgs/EkfState");
         }

         this.imuSubscriber = connectedNode.newSubscriber("/hw/imu", "sensor_msgs/Imu");
         this.navCamSubscriber = connectedNode.newSubscriber("/hw/cam_nav", "sensor_msgs/Image");
         this.dockCamSubscriber = connectedNode.newSubscriber("/hw/cam_dock", "sensor_msgs/Image");
         this.hazCamSubscriber = connectedNode.newSubscriber("/hw/depth_haz/points", "sensor_msgs/PointCloud2");
         this.perchCamSubscriber = connectedNode.newSubscriber("/hw/depth_perch/points", "sensor_msgs/PointCloud2");
         this.dockStateSubscriber = connectedNode.newSubscriber("/beh/dock/state", "ff_msgs/DockState");
         this.ekfSubscriber.addMessageListener(new MessageListener<EkfState>() {
            public void onNewMessage(EkfState ekfState) {
               synchronized(GetterNode.this.m_kinematics_lock) {
                  GetterNode.this.m_kinematics = new DefaultKinematics(ekfState);
               }

               GetterNode.instance = GetterNode.this;
            }
         });
         this.imuSubscriber.addMessageListener(new MessageListener<Imu>() {
            public void onNewMessage(Imu imu) {
               synchronized(GetterNode.this.imu_pub_lock) {
                  GetterNode.this.imu_pub = new ImuResult(imu);
               }

               GetterNode.instance = GetterNode.this;
            }
         }, 10);
         this.navCamSubscriber.addMessageListener(new MessageListener<Image>() {
            public void onNewMessage(Image message) {
               synchronized(GetterNode.this.mono_image_nav_cam_lock) {
                  GetterNode.this.monoImageNavCam = new MonoImage(message);
               }

               GetterNode.instance = GetterNode.this;
            }
         }, 1);
         this.dockCamSubscriber.addMessageListener(new MessageListener<Image>() {
            public void onNewMessage(Image message) {
               synchronized(GetterNode.this.mono_image_dock_cam_lock) {
                  GetterNode.this.monoImageDockCam = new MonoImage(message);
               }

               GetterNode.instance = GetterNode.this;
            }
         }, 1);
         this.hazCamSubscriber.addMessageListener(new MessageListener<PointCloud2>() {
            public void onNewMessage(PointCloud2 message) {
               synchronized(GetterNode.this.point_cloud_haz_cam_lock) {
                  GetterNode.this.point_cloud_haz_cam = new PointCloud(message);
               }

               GetterNode.instance = GetterNode.this;
            }
         }, 1);
         this.perchCamSubscriber.addMessageListener(new MessageListener<PointCloud2>() {
            public void onNewMessage(PointCloud2 message) {
               synchronized(GetterNode.this.point_cloud_perch_cam_lock) {
                  GetterNode.this.point_cloud_perch_cam = new PointCloud(message);
               }

               GetterNode.instance = GetterNode.this;
            }
         }, 1);
         this.dockStateSubscriber.addMessageListener(new MessageListener<DockState>() {
            public void onNewMessage(DockState message) {
               if (message.getState() == -4) {
                  synchronized(GetterNode.this.Undock_lock) {
                      System.out.println(">>>>>>>>>>>>>>>>> undocked >>>>>>>>>>>>");
                      System.out.println(">>>>>>>>>>>>>>>>> undocked >>>>>>>>>>>>");
                      System.out.println(">>>>>>>>>>>>>>>>> undocked >>>>>>>>>>>>");
                     GetterNode.this.undocked = true;
                  }
               }

               GetterNode.instance = GetterNode.this;
            }
         }, 1);
         instance = this;
         System.out.print(">>>>>>>>>>>>>>>>>NODE MAIN END  >>>>>>>>>>>>");
         this.logger.debug("Stemx365RpcApi GetterNode initialization succeeded.");
      } catch (Exception var3) {
         this.logger.error("Stemx365RpcApi GetterNode initialization failed, msg:");
      }
    }

    @Override
    public void onNewMessage(CommandStamped cmd) {
        // Command syntax checked in executive
        if (cmd.getCmdName().equals(CommandConstants.CMD_NAME_CUSTOM_GUEST_SCIENCE)) {
            handleGuestScienceCustomCommand(cmd);

        } else if (cmd.getCmdName().equals(CommandConstants.CMD_NAME_STOP_GUEST_SCIENCE)) {
            handleGuestScienceStopCommand(cmd);

        } else if (cmd.getCmdName().equals(CommandConstants.CMD_NAME_START_GUEST_SCIENCE)) {
            handleGuestScienceStartCommand(cmd);

        } else {
            String msg = "Command " + cmd.getCmdName() + " is not a guest science command.";
            sendAck(cmd.getCmdId(), AckCompletedStatus.EXEC_FAILED, msg);
        }
    }

    public void shutdown()  {
    	this.logger.debug("Attempting to shutdown node");
    	NodeExecutorHolder.getExecutor().getScheduledExecutorService().submit(new Runnable() {
            public void run() {
                NodeExecutorHolder.getExecutor().shutdownNodeMain(GetterNode.this);
            }
        });
    }

    public void handleGuestScienceStartCommand(CommandStamped cmd) {
        if(!validateApkName(cmd)) {
            return;
        }
        String msg;
        if(m_apkIsRunning) {
            msg =  "Apk " + m_apkName + " is already running.";
            sendAck(cmd.getCmdId(), AckCompletedStatus.EXEC_FAILED, msg);
            return;
        }

        m_apkIsRunning = true;


        mCmdInfo.setCmd(cmd.getCmdId(), cmd.getCmdOrigin(), m_apkName, CmdType.START);

        ackGuestScienceStart(true, m_apkName, "");
        m_currentApplication.onGuestScienceStart();
    }

    public void handleGuestScienceStopCommand(CommandStamped cmd) {
        if(!validateApkName(cmd)) {
            return;
        }
        String msg;
        if(!m_apkIsRunning) {
            msg =  "Apk " + m_apkName + " is already stopped.";
            sendAck(cmd.getCmdId(), AckCompletedStatus.EXEC_FAILED, msg);
            return;
        }

        m_apkIsRunning = false;

        mCmdInfo.setCmd(cmd.getCmdId(), cmd.getCmdOrigin(), m_apkName, CmdType.STOP);

        ackGuestScienceStop(true, m_apkName, "");
        m_currentApplication.onGuestScienceStop();
    }

    public void handleGuestScienceCustomCommand(CommandStamped cmd) {
        if(!validateApkName(cmd)) {
            return;
        }
        String command = cmd.getArgs().get(1).getS();

        sendAck(cmd.getCmdId());
        m_currentApplication.onGuestScienceCustomCmd(command);
    }

    @Override
    public synchronized void onShutdown(org.ros.node.Node node) {
        m_node = null;
        m_gsConfigPublisher = null;
        m_ackStampedPublisher = null;
        m_gsDataPublisher = null;
        m_gsStatePublisher = null;
    }

    @Override
    public void onShutdownComplete(Node node) {
    	m_started = false;
    }

    synchronized MessageFactory getTopicMessageFactory() {
        if (m_node == null)
            throw new AstrobeeRuntimeException("Node is not ready or died");
        return m_node.getTopicMessageFactory();
    }

    public void sendAck(String cmdId) {
        sendAck(cmdId, AckCompletedStatus.OK, "", AckStatus.COMPLETED);
    }

    public void sendAck(String cmdId, byte completedStatus, String message) {
        if(completedStatus == AckCompletedStatus.EXEC_FAILED || completedStatus == AckCompletedStatus.BAD_SYNTAX) {
            logger.error(message);
        }
        sendAck(cmdId, completedStatus, message, AckStatus.COMPLETED);
    }

    // does this need to be synchronized???
    synchronized public void sendAck(String cmdId, byte completedStatus, String message,
                                     byte status) {
        if (m_ackStampedPublisher == null)
            throw new AstrobeeRuntimeException("Node not ready or dead");

        AckStamped ack = m_ackStampedPublisher.newMessage();
        Header hdr = mMessageFactory.newFromType(Header._TYPE);
        hdr.setStamp(m_node.getCurrentTime());
        ack.setHeader(hdr);
        ack.setCmdId(cmdId);
        ack.setMessage(message);

        AckCompletedStatus ackCS = mMessageFactory.newFromType(AckCompletedStatus._TYPE);
        ackCS.setStatus(completedStatus);
        ack.setCompletedStatus(ackCS);

        AckStatus ackStatus = mMessageFactory.newFromType(AckStatus._TYPE);
        ackStatus.setStatus(status);
        ack.setStatus(ackStatus);

        m_ackStampedPublisher.publish(ack);
    }

    public void ackGuestScienceStart(boolean started, String apkName, String errMsg) {
        if (started) {
            m_apkIsRunning = true;
            sendGuestScienceState();
            sendAck(mCmdInfo.mId);
        } else {
            sendAck(mCmdInfo.mId, AckCompletedStatus.EXEC_FAILED, errMsg);
        }
        mCmdInfo.resetCmd();
    }

    public void ackGuestScienceStop(boolean stopped, String apkName, String errMsg) {
        if (stopped) {
            m_apkIsRunning = false;
            sendGuestScienceState();
            sendAck(mCmdInfo.mId);
        } else {
            sendAck(mCmdInfo.mId, AckCompletedStatus.EXEC_FAILED, errMsg);
        }
        mCmdInfo.resetCmd();
    }

    public void sendGuestScienceState() {
        GuestScienceState mState = m_gsStatePublisher.newMessage();
        boolean[] runningApks = {m_apkIsRunning};
        mState.setRunningApks(runningApks);
        Header hdr = mMessageFactory.newFromType(Header._TYPE);
        hdr.setStamp(mNodeConfig.getTimeProvider().getCurrentTime());
        mState.setHeader(hdr);
        mState.setSerial(SERIAL_NUMBER);
        m_gsStatePublisher.publish(mState);
    }

    public synchronized void publishGuestScienceConfig(StartGuestScienceService app) {
        if (m_gsConfigPublisher == null)
            throw new AstrobeeRuntimeException("Node not ready or dead");

        m_currentApplication = app;
        m_apkName = app.getFullName();
        GuestScienceApk apk = mMessageFactory.newFromType(GuestScienceApk._TYPE);
        apk.setApkName(app.getFullName());
        apk.setShortName(app.getShortName());
        apk.setPrimary(app.isPrimary());

        List<GuestScienceCommand> cmds = new ArrayList<>();

        if (app.getCommands() != null) {
            for (Command gsCmd : app.getCommands()) {
                GuestScienceCommand cmd = mMessageFactory.newFromType(GuestScienceCommand._TYPE);
                cmd.setName(gsCmd.getName());
                cmd.setCommand(gsCmd.getSyntax());
                cmds.add(cmd);
            }
        }
        apk.setCommands(cmds);

        GuestScienceConfig mConfig = m_gsConfigPublisher.newMessage();
        try {
            mConfig.getHeader().setStamp(m_node.getCurrentTime());
        } catch (NullPointerException e) {
            mConfig.getHeader().setStamp(new org.ros.message.Time());
        }
        mConfig.setSerial(SERIAL_NUMBER);
        List<GuestScienceApk> apks = new ArrayList<>();
        apks.add(apk);
        mConfig.setApks(apks);

        logger.debug("Publishing " + Stringer.toString(mConfig));
        m_gsConfigPublisher.publish(mConfig);

    }

    /* Call this to send data from apk to ground */
    public void sendGuestScienceData(String apkFullName, String topic, byte[] data, MessageType dataType) {
        if (topic.length() > 32) {
            logger.error("The topic string in the guest science message is too " +
                    "big to send to the ground so the message will not be sent. Length must " +
                    " be no more than 32 characters not " + topic.length() + ".");
            return;
        }

        if (data.length > 2048) {
            logger.error("The data in the guest science message is too big to send " +
                    "to the ground so the message will not be sent. Length of data must be no" +
                    " more than 2048 bytes not " + data.length + ".");
            return;
        }

        GuestScienceData dataMsg = mMessageFactory.newFromType(GuestScienceData._TYPE);
        Header hdr = mMessageFactory.newFromType(Header._TYPE);

        hdr.setStamp(mNodeConfig.getTimeProvider().getCurrentTime());
        dataMsg.setHeader(hdr);

        dataMsg.setApkName(apkFullName);

        if (dataType == MessageType.STRING) {
            dataMsg.setDataType(GuestScienceData.STRING);
        } else if (dataType == MessageType.JSON) {
            dataMsg.setDataType(GuestScienceData.JSON);
        } else if (dataType == MessageType.BINARY) {
            dataMsg.setDataType(GuestScienceData.BINARY);
        } else {
            logger.error("Message type in guest science message is unknown so the message " +
                    "will not be sent to the ground.");
            return;
        }

        dataMsg.setTopic(topic);

        ChannelBuffer dataBuff = ChannelBuffers.wrappedBuffer(ByteOrder.LITTLE_ENDIAN, data);
        dataMsg.setData(dataBuff);
        m_gsDataPublisher.publish(dataMsg);
    }

    public boolean validateApkName(CommandStamped cmd) {
        String incomingApkName = cmd.getArgs().get(0).getS();
        if(m_apkName == null || !m_apkName.equals(incomingApkName)) {
            String msg = "Unknown apk " + incomingApkName + ".";
            sendAck(cmd.getCmdId(), AckCompletedStatus.EXEC_FAILED, msg);
            logger.error(msg);
            return false;
        }
        return true;
    }

    public boolean isStarted() {
        return m_started;
    }

    public Kinematics getCurrentKinematics() {
      synchronized(this.m_kinematics_lock) {
         return this.m_kinematics;
      }
   }

   public ImuResult getImu() {
      synchronized(this.imu_pub_lock) {
         ImuResult var10000;
         try {
            var10000 = this.imu_pub;
         } catch (Exception var4) {
            this.logger.error("Stemx365RpcApi getImu failed, msg:", var4);
            return null;
         }

         return var10000;
      }
   }


   public Bitmap getBitmapNavCam() {
      synchronized(this.mono_image_nav_cam_lock) {
         Bitmap var10000;
         try {
            var10000 = this.monoImageNavCam.getBitmap();
         } catch (Exception var4) {
            this.logger.error("Stemx365RpcApi getBitmapNavCam failed, msg:", var4);
            return null;
         }

         return var10000;
      }
   }

   public Bitmap getBitmapDockCam() {
      synchronized(this.mono_image_dock_cam_lock) {
         Bitmap var10000;
         try {
            var10000 = this.monoImageDockCam.getBitmap();
         } catch (Exception var4) {
            this.logger.error("Stemx365RpcApi getBitmapDockCam failed, msg:", var4);
            return null;
         }

         return var10000;
      }
   }
 public Mat getMatNavCam() {
    synchronized(this.mono_image_nav_cam_lock) {
         Mat var10000;
         try {
            System.out.println("==========getMatNavCam-1===========");
            var10000 = this.monoImageNavCam.getMat();
            System.out.println(var10000);
            System.out.println("==========getMatNavCam-2===========");
         } catch (Exception var4) {
            this.logger.error("Stemx365RpcApi getMatNavCam failed, msg:", var4);
            return null;
         }
         System.out.println("==========getMatNavCam-12===========");
         System.out.println(var10000);
         return var10000;
      }
   }

   public Mat getMatDockCam() {
      synchronized(this.mono_image_dock_cam_lock) {
         Mat var10000;
         try {
            var10000 = this.monoImageDockCam.getMat();
         } catch (Exception var4) {
            this.logger.error("Stemx365RpcApi getMatDockCam failed, msg:", var4);
            return null;
         }

         return var10000;
      }
   }

      public PointCloud getPointCloudHazCam() {
      synchronized(this.point_cloud_haz_cam_lock) {
         PointCloud var10000;
         try {
            var10000 = this.point_cloud_haz_cam.clone();
         } catch (Exception var4) {
            this.logger.error("Stemx365RpcApi getPointCloudHazCam failed, msg:", var4);
            return null;
         }

         return var10000;
      }
   }

   public PointCloud getPointCloudPerchCam() {
      synchronized(this.point_cloud_perch_cam_lock) {
         PointCloud var10000;
         try {
            var10000 = this.point_cloud_perch_cam.clone();
         } catch (Exception var4) {
            this.logger.error("Stemx365RpcApi getPointCloudPerchCam failed, msg:", var4);
            return null;
         }

         return var10000;
      }
   }
   public boolean undocked() {
      return this.undocked;
   }

   public boolean getOnSimulation() {
      //System.out.println("getOnSimulation >>>>222-B>>>>>>");
      //System.out.println(this.on_simulation);
      return this.on_simulation;
   }


    public JSONObject getRandomPhasesConfig() throws JSONException {
        if (this.phases_info_json_string == null) {
            return new JSONObject();
        }
        synchronized (this.phases_info_lock) {
            return new JSONObject(this.phases_info_json_string);
        }
    }

   public Time getCurrentTime() {
        if (this.on_simulation) {
            if (this.clock == null) {
                return new Time();
            }
            synchronized (this.clock_lock) {
                return new Time(this.clock.getClock());
            }
        }
        return Time.fromMillis(System.currentTimeMillis());
    }

    public boolean setSignalState(final Byte signalState, final int times) {
        synchronized (this.signal_state_lock) {
            if (times <= 0) {
                 logger.debug("Stemx365RpcApi SetterNode setSignalState : Specified times <" + times + "> is not correct.");
                return false;
            }
            switch (signalState) {
                case 0: {
                    logger.info("Stemx365RpcApi SetterNode setSignalState status : VIDEO_ON");
                    break;
                }
                case 1: {
                    logger.info("Stemx365RpcApi SetterNode setSignalState status : VIDEO_OFF");
                    break;
                }
                case 3: {
                    logger.info("Stemx365RpcApi SetterNode setSignalState status : SUCCESS");
                    break;
                }
                case 4: {
                    logger.info("Stemx365RpcApi SetterNode setSignalState status : ENTER_HATCHWAY");
                    break;
                }
                case 5: {
                    logger.info("Stemx365RpcApi SetterNode setSignalState status : UNDOCK");
                    break;
                }
                case 6: {
                    logger.info("Stemx365RpcApi SetterNode setSignalState status : UNPERCH");
                    break;
                }
                case 7: {
                    logger.info("Stemx365RpcApi SetterNode setSignalState status : MOTION_IMPAIRED");
                    break;
                }
                case 8: {
                    logger.info("Stemx365RpcApi SetterNode setSignalState status : THRUST_FORWARD");
                    break;
                }
                case 9: {
                    logger.info("Stemx365RpcApi SetterNode setSignalState status : THRUST_AFT");
                    break;
                }
                case 10: {
                    logger.info("Stemx365RpcApi SetterNode setSignalState status : TURN_RIGHT");
                    break;
                }
                case 11: {
                    logger.info("Stemx365RpcApi SetterNode setSignalState status : TURN_LEFT");
                    break;
                }
                case 12: {
                    logger.info("Stemx365RpcApi SetterNode setSignalState status : TURN_UP");
                    break;
                }
                case 13: {
                    logger.info("Stemx365RpcApi SetterNode setSignalState status : TURN_DOWN");
                    break;
                }
                case 14: {
                    logger.info("Stemx365RpcApi SetterNode setSignalState status : CLEAR");
                    break;
                }
                case 15: {
                    logger.info("Stemx365RpcApi SetterNode setSignalState status : SLEEP");
                    break;
                }
                case 16: {
                    logger.info("Stemx365RpcApi SetterNode setSignalState status : WAKE");
                    break;
                }
                case 17: {
                    logger.info("Stemx365RpcApi SetterNode setSignalState status : STOP_ALL_LIGHTS");
                    break;
                }
                case 18: {
                    logger.info("Stemx365RpcApi SetterNode setSignalState status : CHARGING");
                    break;
                }
                default: {
                     logger.debug("Stemx365RpcApi SetterNode setSignalState : Specified state is unknown.");
                    return false;
                }
            }
            logger.info("Stemx365RpcApi SetterNode setSignalState times  : " + times);
            final SignalState message = (SignalState)this.signalStatePublisher.newMessage();
            message.setState((byte)signalState);
            try {
                for (int i = 0; i < times; ++i) {
                    this.signalStatePublisher.publish(message);
                    logger.info("Stemx365RpcApi SetterNode setSignalState publish " + (i + 1) + " th");
                    Thread.sleep(10L);
                }
            }
            catch (InterruptedException e) {
                 logger.error("KiboRpcApi SetterNode setSignalState All publishes may not have been possible.", (Throwable)e);
                return false;
            }
            return true;
        }
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("gs_manager_stub");
    }

    public boolean isNodeStarted() {
        return this.m_started;
    }

}
