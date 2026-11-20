package app.mindpause.android.core;

public enum AccessState {
    READY, WAITING, OFF;
    public static AccessState resolve(boolean liveConnection,boolean managerEnabled,boolean settingsEnabled) {
        if(liveConnection)return READY;
        return managerEnabled || settingsEnabled?WAITING:OFF;
    }
}
