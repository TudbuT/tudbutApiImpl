package tudbut.api.impl;

import de.tudbut.timer.AsyncTask;
import de.tudbut.tools.Hasher;
import de.tudbut.tools.Tools;
import tudbut.net.pbic2.PBIC2;
import tudbut.obj.TLMap;
import tudbut.parsing.JSON;
import tudbut.parsing.TCN;
import tudbut.tools.ArrayTools;
import tudbut.tools.Lock;

import java.io.IOException;
import java.util.UUID;

@Deprecated
public class TudbuTAPIGateway {
    final PBIC2 pbic2;
    
    public TudbuTAPIGateway(PBIC2 pbic2) {
        this.pbic2 = pbic2;
    }
    
    final Lock rateLimitLock = new Lock();
    final TLMap<UUID, PBIC2> gateway = new TLMap<>();
    
    public void awaitRateLimitEnd() {
        rateLimitLock.waitHere();
    }
    
    public long getOverallPlaytime() throws RateLimit, IOException {
        return Long.parseLong(get("getOverallPlaytime", ""));
    }
    
    public UserRecord getUserRecord(UUID uuid) throws RateLimit, IOException {
        try {
            return new UserRecord(uuid, TCN.read(get("getUserRecord", "uuid=" + uuid.toString())));
        } catch (TCN.TCNException var2) {
            return null;
        }
    }
    
    public UserRecord getUserRecordByName(String name) throws RateLimit, IOException {
        try {
            return new UserRecord(null, TCN.read(get("getUserRecordByName", "name=" + name)));
        } catch (TCN.TCNException var2) {
            return null;
        }
    }
    
    public UUID getUUID(String name) {
        try {
            return UUID.fromString(get("getUUID", "name=" + name));
        } catch (Exception var2) {
            return null;
        }
    }
    
    public String getName(UUID uuid) throws RateLimit, IOException {
        String s = get("getName", "uuid=" + uuid);
        if (s.startsWith("FETCH_ERROR") || s.equals("")) {
            getRateLimitLock().waitHere();
            s = get("getName", "uuid=" + uuid);
        }
        
        return s;
    }
    
    public AsyncTask<UserRecord[]> getUserRecords() {
        AsyncTask<UserRecord[]> task = new AsyncTask<>(() -> {
            Lock rateLimitLock = getRateLimitLock();
            UUID[] users = getUsers();
            UserRecord[] records = new UserRecord[users.length];
            
            for(int i = 0; i < records.length; ++i) {
                rateLimitLock.waitHere();
                records[i] = getUserRecord(users[i]);
            }
            
            return records;
        });
        task.catchExceptions(Throwable::printStackTrace);
        return task;
    }
    
    public UUID[] getUsers() throws RateLimit, IOException {
        return ArrayTools.getFromArray(get("getUsers", "").split("\n"), UUID::fromString);
    }
    
    public String[] getUsersByName() throws RateLimit, IOException {
        return get("getUsersByName", "").split("\n");
    }
    
    public int getUserAmount() throws RateLimit, IOException {
        return Integer.parseInt(get("getUserAmount", ""));
    }
    
    public String getHashedPassword(UUID uuid) throws RateLimit, IOException {
        return get("getHashedPassword", "uuid=" + uuid.toString());
    }
    
    public String setPassword(UUID uuid, String key, String password) throws RateLimit, IOException {
        return get("setPassword", "uuid=" + uuid.toString() + "&key=" + key + "&password=" + Hasher.sha512hex(Hasher.sha256hex(password)));
    }
    
    public Lock getRateLimitLock() {
        return rateLimitLock;
    }
    
    public String get(String method, String params) throws RateLimit, IOException {
        TCN tcn = new TCN();
        TCN query = TCN.readMap(Tools.stringToMap(params.replaceAll("=", ":").replaceAll("&", ";")));
        tcn.set("id", "api");
        tcn.set("path", "/api/" + method);
        tcn.set("query", query);
        while(true) {
            rateLimitLock.waitHere();
            rateLimitLock.lock(20);
            TCN rec = new TCN();
            try {
                rec = JSON.read(pbic2.writeMessageWithResponse(JSON.write(tcn)));
            }
            catch (JSON.JSONFormatException ignored) {
            }

            if ("response".equals(rec.getString("id"))) {
                return rec.getString("data");
            }
            if ("rateLimit".equals(tcn.getString("id"))) {
                TCN rl = rec.getSub("rateLimit");
                rateLimitLock.lock((int) (rl.getLong("expiresAt") - System.currentTimeMillis()));
                throw new RateLimit(rl);
            }
        }
    }
}
