package tudbut.api.impl;

import de.tudbut.timer.AsyncTask;
import de.tudbut.tools.Hasher;
import tudbut.net.http.HTTPRequest;
import tudbut.net.http.HTTPRequestType;
import tudbut.net.http.HTTPUtils;
import tudbut.net.http.ParsedHTTPValue;
import tudbut.net.pbic2.PBIC2;
import tudbut.obj.TLMap;
import tudbut.parsing.TCN;
import tudbut.parsing.TCN.TCNException;
import tudbut.tools.ArrayTools;
import tudbut.tools.Lock;

import java.io.IOException;
import java.util.UUID;

import static tudbut.api.impl.TudbuTAPIV2.checkRateLimit;

@SuppressWarnings("unused")
public class TudbuTAPI {
    public static final String host = "localhost";
    public static final int port = 8080;
    
    static final Lock rateLimitLock = new Lock();
    static final TLMap<UUID, PBIC2> gateway = new TLMap<>();

    public static void awaitRateLimitEnd() {
        rateLimitLock.waitHere();
    }

    public static long getOverallPlaytime() throws RateLimit, IOException {
        return Long.parseLong(get("getOverallPlaytime", ""));
    }

    public static UserRecord getUserRecord(UUID uuid) throws RateLimit, IOException {
        try {
            return new UserRecord(uuid, TCN.read(get("getUserRecord", "uuid=" + uuid.toString())));
        } catch (TCNException var2) {
            return null;
        }
    }

    public static UserRecord getUserRecordByName(String name) throws RateLimit, IOException {
        try {
            return new UserRecord(null, TCN.read(get("getUserRecordByName", "name=" + name)));
        } catch (TCNException var2) {
            return null;
        }
    }

    public static UUID getUUID(String name) {
        try {
            return UUID.fromString(get("getUUID", "name=" + name));
        } catch (Exception var2) {
            return null;
        }
    }

    public static String getName(UUID uuid) throws RateLimit, IOException {
        String s = get("getName", "uuid=" + uuid);
        if (s.startsWith("FETCH_ERROR") || s.equals("")) {
            getRateLimitLock().waitHere();
            s = get("getName", "uuid=" + uuid);
        }

        return s;
    }

    public static AsyncTask<UserRecord[]> getUserRecords() {
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

    public static UUID[] getUsers() throws RateLimit, IOException {
        return ArrayTools.getFromArray(get("getUsers", "").split("\n"), UUID::fromString);
    }

    public static String[] getUsersByName() throws RateLimit, IOException {
        return get("getUsersByName", "").split("\n");
    }

    public static int getUserAmount() throws RateLimit, IOException {
        return Integer.parseInt(get("getUserAmount", ""));
    }

    public static String getHashedPassword(UUID uuid) throws RateLimit, IOException {
        return get("getHashedPassword", "uuid=" + uuid.toString());
    }

    public static String setPassword(UUID uuid, String key, String password) throws RateLimit, IOException {
        return get("setPassword", "uuid=" + uuid.toString() + "&key=" + key + "&password=" + Hasher.sha512hex(Hasher.sha256hex(password)));
    }
    
    public static UserRecord adminGetUserRecord(UUID uuid, String key) throws RateLimit, IOException {
        try {
            return new UserRecord(uuid, TCN.read(get("admin/getUserRecordExact", "uuid=" + uuid.toString() + "&key=" + HTTPUtils.encodeUTF8(key))));
        } catch (TCNException var2) {
            return null;
        }
    }
    
    public static UserRecord adminGetUserRecordByName(String name, String key) throws RateLimit, IOException {
        try {
            return new UserRecord(null, TCN.read(get("admin/getUserRecordByNameExact", "name=" + name + "&key=" + HTTPUtils.encodeUTF8(key))));
        } catch (TCNException var2) {
            return null;
        }
    }

    public static Lock getRateLimitLock() {
        return rateLimitLock;
    }

    public static String get(String method, String params) throws RateLimit, IOException {
        HTTPRequest request = new HTTPRequest(HTTPRequestType.GET, host, port, "/api/" + method + "?" + params);
        ParsedHTTPValue response = request.send().parse();
        checkRateLimit(response, null);
        return response.getBody();
    }
}
