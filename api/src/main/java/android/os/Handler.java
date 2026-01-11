package android.os;
public class Handler {
    public void post(Runnable r) { new Thread(r).start(); }
}
