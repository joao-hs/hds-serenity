package pt.ulisboa.tecnico.hdsledger.service.models;

import java.util.concurrent.atomic.AtomicInteger;

public class ProgressIndicator {
    private AtomicInteger timeout = new AtomicInteger(100); // ms
    private Boolean isMakingProgress = Boolean.FALSE;
    private final Object lock = new Object();


    public ProgressIndicator() {}

    public void registerProgress() {
        synchronized (lock) {
            isMakingProgress = true;
        }
    }

    public void resetProgress() {
        synchronized (lock) {
            isMakingProgress = false;
        }
    }

    public boolean isFrozen() {
        synchronized (lock) {
            if (!isMakingProgress) {
                timeout.set(timeout.get() * 2);
                return true;
            } else {
                isMakingProgress = false;
                return false;
            }
        }
    }

    public int getTimeout() {
        return timeout.get();
    }
}
