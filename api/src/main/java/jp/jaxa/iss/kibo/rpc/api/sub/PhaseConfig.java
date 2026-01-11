/*
 * Decompiled with CFR 0.152.
 */
package jp.jaxa.iss.kibo.rpc.api.sub;

import android.util.Log;
import java.io.IOException;
import jp.jaxa.iss.kibo.rpc.api.GetterNode;
import jp.jaxa.iss.kibo.rpc.api.sub.Phase;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PhaseConfig {
    private static final String PHASE_CONFIG_FILE = "/incoming/random_phase_config.json";
    private static final int MAX_PHASE_NUM = 20;
    private final int retryTimes = 3;
    private final int retrySleepTimeMs = 1000;
    private int phaseNum = 0;
    private GetterNode getterNode;
    private Phase[] Phases = new Phase[20];
    private String correctReport = null;

    public PhaseConfig(GetterNode getterNode, String configDir) throws IOException, JSONException, InterruptedException {
        Log.v((String)"KiboRpcApi", (String)"[PhaseConfig] Start.");
        this.getterNode = getterNode;
        boolean isSimulation = this.getterNode.getOnSimulation();
        Log.v((String)"KiboRpcApi", (String)("[PhaseConfig] isSimulation: " + isSimulation));
        Log.v((String)"KiboRpcApi", (String)("[PhaseConfig] configDir: " + configDir));
        this.loadRandomPhaseConfig(isSimulation, configDir);
        Log.i((String)"KiboRpcApi", (String)("[PhaseConfig] created Phase is " + this.phaseNum));
        Log.v((String)"KiboRpcApi", (String)"[PhaseConfig] Finish.");
    }

    public int getPhaseNum() {
        return this.phaseNum;
    }

    public Phase[] getPhases() {
        return this.Phases;
    }

    public String getCorrectReport() {
        return this.correctReport;
    }

    private void loadRandomPhaseConfig(boolean isSimulation, String configDir) throws IOException, JSONException, InterruptedException {
        JSONObject jsonObj = null;
        if (isSimulation) {
            jsonObj = this.getterNode.getRandomPhasesConfig();
            for (int i = 1; i <= this.retryTimes && jsonObj.length() == 0; ++i) {
                this.getClass();
                Thread.sleep(1000L);
                Log.i((String)"KiboRpcApi", (String)("[PhaseConfig] loadRandomPhaseConfig Retry " + i));
                jsonObj = this.getterNode.getRandomPhasesConfig();
            }
        } else {
            String filePathName = configDir + PHASE_CONFIG_FILE;
            String sentence = this.readFile(filePathName);
            jsonObj = new JSONObject(sentence);
        }
        Log.i((String)"KiboRpcApi", (String)("[PhaseConfig] loadRandomPhaseConfig " + jsonObj));
        this.parseRandomPhasesConfig(jsonObj);
    }

    private void parseRandomPhasesConfig(JSONObject jsonObj) throws JSONException {
        JSONArray items = jsonObj.getJSONArray("phases_info");
        int itemsLength = items.length();
        if (itemsLength > 0) {
            int itemIdx = 0;
            for (int i = 0; i < 20; ++i) {
                JSONArray jsonPhaseConfig = items.getJSONArray(itemIdx);
                this.Phases[i] = new Phase(jsonPhaseConfig);
                this.phaseNum = i + 1;
                if (++itemIdx < itemsLength) continue;
                itemIdx = 0;
            }
        }
        for (int k = 0; k < this.phaseNum; ++k) {
            String msg = String.format("Phases(%d): %s", k + 1, this.Phases[k].toString());
            Log.v((String)"KiboRpcApi", (String)("[PhaseConfig] " + msg));
        }
        if (!this.getterNode.getOnSimulation()) {
            this.correctReport = jsonObj.getString("qr_info");
            Log.v((String)"KiboRpcApi", (String)("[PhaseConfig] parseRandomPhasesConfig qr_info: " + this.correctReport));
        }
    }

    /*
     * Exception decompiling
     */
    private String readFile(String file) throws IOException {
        /*
         * This method has failed to decompile.  When submitting a bug report, please provide this stack trace, and (if you hold appropriate legal rights) the relevant class file.
         * 
         * org.benf.cfr.reader.util.ConfusedCFRException: Started 4 blocks at once
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.getStartingBlocks(Op04StructuredStatement.java:412)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op04StructuredStatement.buildNestedBlocks(Op04StructuredStatement.java:487)
         *     at org.benf.cfr.reader.bytecode.analysis.opgraph.Op03SimpleStatement.createInitialStructuredBlock(Op03SimpleStatement.java:736)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisInner(CodeAnalyser.java:850)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysisOrWrapFail(CodeAnalyser.java:278)
         *     at org.benf.cfr.reader.bytecode.CodeAnalyser.getAnalysis(CodeAnalyser.java:201)
         *     at org.benf.cfr.reader.entities.attributes.AttributeCode.analyse(AttributeCode.java:94)
         *     at org.benf.cfr.reader.entities.Method.analyse(Method.java:531)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseMid(ClassFile.java:1055)
         *     at org.benf.cfr.reader.entities.ClassFile.analyseTop(ClassFile.java:942)
         *     at org.benf.cfr.reader.Driver.doJarVersionTypes(Driver.java:257)
         *     at org.benf.cfr.reader.Driver.doJar(Driver.java:139)
         *     at org.benf.cfr.reader.CfrDriverImpl.analyse(CfrDriverImpl.java:76)
         *     at org.benf.cfr.reader.Main.main(Main.java:54)
         */
        throw new IllegalStateException("Decompilation failed");
    }
}

