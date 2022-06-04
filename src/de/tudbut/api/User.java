package de.tudbut.api;

import de.tudbut.async.ComposeCallback;
import de.tudbut.async.Task;
import tudbut.net.http.ParsedHTTPValue;
import tudbut.parsing.JSON;
import tudbut.parsing.TCN;

import java.util.Date;
import java.util.UUID;

import static de.tudbut.api.TudbuTAPI.*;

/**
 * @author TudbuT
 * @since 03 Jun 2022
 */

public class User {
    
    
    public TCN object;
    private UUID uuid;
    
    public User(TCN object, UUID uuid) {
        this.object = object;
        this.uuid = uuid;
    }
    
    public String getName() {
        return object.getString("name");
    }
    
    public Date getLastLogin() {
        return new Date(object.getLong("lastLogin"));
    }
    
    public Date getLastPlay() {
        return new Date(object.getLong("lastPlayRequest"));
    }
    
    public int getPlaytimeSeconds() {
        return object.getInteger("playTime");
    }
    
    public boolean canRun() {
        return !object.getBoolean("deactivate") && !object.getBoolean("singleDeactivate");
    }
    
    public boolean didHandshakeSucceed() {
        return object.getBoolean("handshakeSucceeded");
    }
    
    public boolean isPremium() {
        return object.getBoolean("registered");
    }
    
    public String getPremiumPasswordHash() {
        return isPremium() ? object.getString("password") : null;
    }
    
    public TTCVersion getVersion() {
        return new TTCVersion(object.getString("version"));
    }
    
    public boolean isIncomplete() {
        return object.getBoolean("incomplete") || object.getBoolean("removalQueued");
    }
    
    public boolean isRetrieved() {
        return object.map.size() != 0;
    }
    
    public Task<User> retrieveSelf() {
        return getUUID()
                .<ParsedHTTPValue>compose((resp, res, rej) -> get("getUserRecord", "uuid=" + resp).then(res).err(rej).ok())
                .compose(parseTCN)
                .compose((resp, res, rej) -> {
                    this.object = resp;
                    res.call(this);
                });
    }
    
    public Task<UUID> getUUID() {
        if (uuid == null) {
            return retrieveUUID();
        }
        return new Task<>((res, rej) -> res.call(uuid));
    }
    
    public Task<UUID> retrieveUUID() {
        return get("getUUID", "name=" + getName())
                .<UUID>compose((resp, res, rej) -> {
                    res.call(UUID.fromString(resp.getBody()));
                })
                .then(uuid -> this.uuid = uuid);
    }
    
    @Override
    public String toString() {
        return object + "\n" +
               "uuid: " + uuid;
    }
}
