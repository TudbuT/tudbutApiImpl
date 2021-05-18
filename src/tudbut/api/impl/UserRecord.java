package tudbut.api.impl;

import tudbut.parsing.TCN;
import tudbut.tools.encryption.RawKey;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

public class UserRecord {
    
    public TCN tcn;
    public String name = "";
    public long lastNameUpdate = 0;
    public UUID uuid;
    public boolean registered = false;
    public String hashedPassword = "";
    public long lastLogin = new Date().getTime();
    public long playTime = 0;
    public long lastPlayRequest = 0;
    public boolean deactivate = false;
    public boolean singleDeactivate = false;
    public RawKey key = new RawKey("");
    public boolean handshakeSucceeded = false;
    public int req = 0;
    public String version = "";
    public String ip = "";
    
    UserRecord(UUID uuid, TCN tcn) {
        synchronized (UserRecord.class) {
            
            try {
                lastNameUpdate = tcn.getLong("lastNameUpdate");
                registered = tcn.getBoolean("registered");
                hashedPassword = tcn.getString("password");
                lastLogin = tcn.getLong("lastLogin");
                name = tcn.getString("name");
                playTime = tcn.getLong("playTime");
                lastPlayRequest = tcn.getLong("lastPlayRequest");
                deactivate = tcn.getBoolean("deactivate");
                singleDeactivate = tcn.getBoolean("singleDeactivate");
                key = new RawKey(tcn.getString("key"));
                handshakeSucceeded = tcn.getBoolean("handshakeSucceeded");
                version = tcn.getString("version");
                req = tcn.getInteger("req");
                ip = tcn.getString("ip");
            }
            catch (Exception ignored) { }
            
            if (name == null || name.isEmpty()) {
                name = "FETCH_ERROR_" + uuid;
            }
            
            this.uuid = uuid;
            this.tcn = tcn;
            
            dispose();
        }
    }
    
    public RawKey generateKey() {
        key = new RawKey();
        req = 0;
        dispose();
        return key;
    }
    
    public void dispose() {
        set("lastNameUpdate", lastNameUpdate);
        set("registered", registered);
        set("password", hashedPassword);
        set("lastLogin", lastLogin);
        set("name", name);
        set("playTime", playTime);
        set("lastPlayRequest", lastPlayRequest);
        set("deactivate", deactivate);
        set("singleDeactivate", singleDeactivate);
        set("key", key.toString());
        set("handshakeSucceeded", handshakeSucceeded);
        set("version", version);
        set("req", req);
        set("ip", ip);
    }
    
    private void set(String k, Object v) {
        if(!Objects.equals(tcn.get(k), v)) {
            tcn.set(k, v);
        }
    }
    
    public TCN get() {
        TCN tcn = TCN.readMap(this.tcn.toMap());
        tcn.set("key", "REDACTED");
        tcn.set("req", "REDACTED");
        tcn.set("ip", "REDACTED_FOR_PRIVACY");
        return tcn;
    }
}
