package gov.nasa.arc.astrobee.android.gs;
import java.io.File;

import gov.nasa.arc.astrobee.android.gs.MessageType;
import android.graphics.Bitmap;
import org.opencv.core.Mat;
import gov.nasa.arc.astrobee.Kinematics;

import org.ros.message.Time;

import jp.jaxa.iss.kibo.rpc.api.types.ImuResult;
import jp.jaxa.iss.kibo.rpc.api.types.MonoImage;
import jp.jaxa.iss.kibo.rpc.api.types.PointCloud;

import gov.nasa.arc.astrobee.ros.gs.ApkInfoXmlParser;
import gov.nasa.arc.astrobee.ros.gs.ApkInfo;
import gov.nasa.arc.astrobee.ros.gs.Command;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * This is what they will end up implementing
 */
public abstract class StartGuestScienceService {

    private JavaGuestScienceManager m_manager;

    private List<Command> m_commands = new ArrayList<>();

    private String m_fullName;
    private String m_shortName;
    private boolean m_primary;

    private String mDataBasePath = "";

    public StartGuestScienceService() {
        String currentDirectory = System.getProperty("user.dir");
        System.out.println("Current Working Directory: " + currentDirectory);
        //String xmlFilePath = currentDirectory + File.separator + "/src/main/resources/commands.xml";
        String xmlFilePath = "/home/simulator/scripts/commands.xml";
        ApkInfo apkInfo = ApkInfoXmlParser.parseFile(xmlFilePath);
        m_fullName = apkInfo.getFullName();
        m_shortName = apkInfo.getShortName();
        m_primary = apkInfo.isPrimary();
        m_commands = apkInfo.getCommands();
        System.out.println("m_fullName: " + m_fullName);
        System.out.println("m_shortName: " + m_shortName);
        System.out.println("m_primary: " + m_primary);
        System.out.println("m_commands: " + m_commands);
    }
    
    public void terminate() {
    	int timeout = 30;
    	m_manager.m_nodeMain.shutdown();
    	for(int i = 0; i<timeout; i++) {
    		if (!m_manager.m_nodeMain.isStarted()){
    			break;
    		}
    	}
    	System.exit(0);
    }

    public abstract void onGuestScienceStart();

    public abstract void onGuestScienceStop();

    public abstract void onGuestScienceCustomCmd(String command);

    public String getFullName() {
        return m_fullName;
    }

    public String getShortName() {
        return m_shortName;
    }

    public boolean isPrimary() {
        return m_primary;
    }

    public void sendData(MessageType type, String topic, String data) {
        m_manager.sendData(type, topic, data);
    }

    public void sendData(MessageType type, String topic, byte[] data) {
        m_manager.sendData(type, topic, data);
    }

    public void sendStarted(String topic) {
        sendData(MessageType.JSON, topic, "{\"Summary\": \"Started\"}");
    }

    public void sendStopped(String topic) {
        sendData(MessageType.JSON, topic, "{\"Summary\": \"Stopped\"}");
    }

    public void sendReceivedCustomCommand(String topic) {
        sendData(MessageType.JSON, topic, "{\"Summary\": \"Received Custom Commands\"}");
    }

    public List<Command> getCommands() {
        return m_commands;
    }

    void acceptManager(JavaGuestScienceManager manager) {
        m_manager = manager;
    }

   

    public Bitmap getBitmapDockCam() {
      Bitmap ret = m_manager.getBitmapDockCam();
      return ret;
   }

    public Mat getMatNavCam() {
      Mat ret = m_manager.getMatNavCam();
      return ret;
   }

   public Mat getMatDockCam() {
      Mat ret = m_manager.getMatDockCam();
      return ret;
   }

    public Kinematics getCurrentKinematics() {
      return m_manager.getCurrentKinematics();
   }

   public ImuResult getImu() {
      ImuResult ret = m_manager.getImu();
      return ret;
   }

public boolean getOnSimulation() {
      return m_manager.getOnSimulation();
   }

     public Bitmap getBitmapNavCam() {
      return m_manager.getBitmapNavCam();
   }

    public boolean undocked() {
        return m_manager.undocked();
    }

     public List<Integer> getActiveTargets() {
       return m_manager.getActiveTargets();
    }
    public JSONObject getRandomPhasesConfig() throws JSONException {
        return m_manager.getRandomPhasesConfig();
    }

     public Boolean isStarted() {
        return true;
    }

    public Time getCurrentTime() {
        return m_manager.getCurrentTime();
    }

    public boolean setSignalState(final Byte signalState, final int times) {
        return m_manager.setSignalState(signalState,times);
     }

    public int getCurrentPhaseNumber() {
            return m_manager.getCurrentPhaseNumber();
    }

    public boolean targetDeactivation(final int targetId) {
       return m_manager.targetDeactivation(targetId);
    }

    public List<Long> getTimeRemaining() {
        return getTimeRemaining();
    }

    public void startMission(){
        m_manager.startMission();
    }

    public String getGuestScienceDataBasePath() {
        return mDataBasePath;
    }
}


