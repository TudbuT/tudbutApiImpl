package tudbut.api.impl;

import tudbut.parsing.TCN;
import tudbut.tools.Lock;

import java.io.IOException;
import java.util.Date;
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
    
    /**
     * Creates a UserRecord with given information
     *
     * @param uuid The UUID of the player, can be null
     * @param tcn The TCN of the record
     */
    UserRecord(UUID uuid, TCN tcn) {
        
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
        } catch (Exception ignored) { }
        this.uuid = uuid;
        this.tcn = tcn;
        
        dispose();
    }
    
    /**
     * Completes the information, if possible
     *
     * @throws RateLimit If the API threw a RateLimit
     * @throws IOException If the API is unreachable
     */
    public void complete() throws RateLimit, IOException {
        Lock lock = TudbuTAPI.getRateLimitLock();
        if(uuid == null) {
            lock.waitHere();
            uuid = TudbuTAPI.getUUID(name);
        }
        if(uuid != null && name == null) {
            lock.waitHere();
            name = TudbuTAPI.getName(uuid);
        }
    }
    
    /**
     * Stores changed information back to the TCN, DOES NOT CHANGE THE VALUES IN THE DATABASE, ONLY ADJUSTS THE TCN
     */
    public void dispose() {
        tcn.set("lastNameUpdate", lastNameUpdate);
        tcn.set("registered", registered);
        tcn.set("password", hashedPassword);
        tcn.set("lastLogin", lastLogin);
        tcn.set("name", name);
        tcn.set("playTime", playTime);
        tcn.set("lastPlayRequest", lastPlayRequest);
        tcn.set("deactivate", deactivate);
        tcn.set("singleDeactivate", singleDeactivate);
    }
}
