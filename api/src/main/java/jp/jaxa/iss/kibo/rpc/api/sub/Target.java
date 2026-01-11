/*
 * Decompiled with CFR 0.152.
 */
package jp.jaxa.iss.kibo.rpc.api.sub;

public class Target {
    private int id;
    private boolean active = false;

    public Target(int id, boolean active) {
        this.setId(id);
        this.setActive(active);
    }

    public boolean getActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        sb.append("id: " + this.id + ", ");
        sb.append("active: " + this.active);
        sb.append(" }");
        return sb.toString();
    }
}

