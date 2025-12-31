package jp.jaxa.iss.kibo.rpc.sampleapk;

import java.io.File;
import jp.jaxa.iss.kibo.rpc.api.sub.JavaGuestScienceManager;
//import jp.jaxa.iss.kibo.rpc.api.*;

public class StemxAstrobeeMain {

    public static void main(String[] args) throws InterruptedException {
    	JavaGuestScienceManager manager = new JavaGuestScienceManager();

        //String xmlFilePath = System.getProperty("user.dir") + File.separator + "/src/main/resources/commands.xml";
        String xmlFilePath = "/home/simulator/scripts/commands.xml";
        YourService app = new YourService();
        Thread.sleep(2000);
        manager.acceptApplication(app);
    }
}
