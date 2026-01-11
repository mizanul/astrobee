/*
 * Decompiled with CFR 0.152.
 */
package jp.jaxa.iss.kibo.rpc.api.sub;

import android.util.Log;
import java.util.ArrayList;
import java.util.List;
import jp.jaxa.iss.kibo.rpc.api.sub.Target;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Phase {
    private static final int MAX_TARGET_NUM = 6;
    private int targetNum = 0;
    private Target[] Targets = new Target[6];

    public Phase(JSONArray jsonPhaseConfig) throws JSONException {
        this.parseRandomPhaseConfig(jsonPhaseConfig);
    }

    private void parseRandomPhaseConfig(JSONArray jsonPhaseConfig) throws JSONException {
        for (int i = 0; i < 6 && i < jsonPhaseConfig.length(); ++i) {
            JSONObject jsonTargetConfig = jsonPhaseConfig.getJSONObject(i);
            int targetId = jsonTargetConfig.getInt("target_id");
            boolean active = jsonTargetConfig.getBoolean("active");
            this.Targets[i] = new Target(targetId, active);
            this.targetNum = i + 1;
        }
        Log.v((String)"Phase", (String)("[parseRandomPhaseConfig] created targets is " + this.targetNum));
    }

    public List<Integer> getTargetActiveList() {
        ArrayList<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < this.targetNum; ++i) {
            if (!this.Targets[i].getActive()) continue;
            list.add(this.Targets[i].getId());
        }
        return list;
    }

    public void setTargetActive(int targetId, boolean active) {
        for (int i = 0; i < this.targetNum; ++i) {
            if (targetId != this.Targets[i].getId()) continue;
            this.Targets[i].setActive(active);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < this.targetNum; ++i) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(this.Targets[i].toString());
        }
        sb.append("]");
        return sb.toString();
    }
}

