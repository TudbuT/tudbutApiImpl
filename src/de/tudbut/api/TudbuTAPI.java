package de.tudbut.api;

import de.tudbut.async.ComposeCallback;
import de.tudbut.async.Task;
import tudbut.net.http.HTTPRequest;
import tudbut.net.http.HTTPRequestType;
import tudbut.net.http.ParsedHTTPValue;
import tudbut.parsing.JSON;
import tudbut.parsing.TCN;

import java.util.UUID;

import static tudbut.api.impl.TudbuTAPIV2.checkRateLimit;

/**
 * @author TudbuT
 * @since 03 Jun 2022
 */

public class TudbuTAPI {
    public static String HOST = "https://api.tudbut.de";
    public static int    PORT =  83;
    static final ComposeCallback<ParsedHTTPValue, UUID> parseUUID = (resp, res, rej) -> res.call(UUID.fromString(resp.getBody()));
    static final ComposeCallback<ParsedHTTPValue, TCN> parseTCN = (resp, res, rej) -> {
        try {
            res.call(TCN.read(resp.getBody()));
        } catch (Exception e) {
            rej.call(e);
        }
    };
    
    public static Task<User[]> getAllUsers() {
        return get("getUsers", "")
                .<String[]>compose((resp, res, rej) -> res.call(resp.getBody().split("\n")))
                .compose((resp, res, rej) -> {
                    User[] users = new User[resp.length];
                    for (int i = 0 ; i < users.length ; i++) {
                        users[i] = new User(new TCN(), UUID.fromString(resp[i]));
                    }
                    res.call(users);
                });
    }
    
    public static Task<String[]> getAllUsernames() {
        return get("getUsersByName", "")
                .compose((resp, res, rej) -> res.call(resp.getBody().split("\n")));
    }
    
    public static Task<User> getUser(UUID uuid) {
        return get("getUserRecord", "uuid=" + uuid)
                .compose((result, res, rej) -> {
                    try {
                        res.call(new User(JSON.read(result.getBody()), uuid));
                    }
                    catch (JSON.JSONFormatException e) {
                        rej.call(e);
                    }
                });
    }
    
    public static Task<User> getUser(String name) {
        return get("getUserRecordByName", "name=" + name)
                .compose(parseTCN)
                .compose((resp, res, rej) -> res.call(new User(resp, null)));
    }
    
    public static Task<UUID> getUUID(String name) {
        return get("getUUID", "name=" + name)
                .compose(parseUUID);
    }
    
    public static Task<Long> getOverallPlaytime() {
        return get("getOverallPlaytime", "")
                .compose((result, res, rej) -> {
                    res.call(Long.parseLong(result.getBody()));
                });
    }
    
    public static Task<ParsedHTTPValue> get(String method, String params) {
        return new Task<>((res, rej) -> {
            try {
                HTTPRequest request = new HTTPRequest(HTTPRequestType.GET, HOST, PORT, "/api/" + method + "?" + params);
                ParsedHTTPValue response = request.send().parse();
                checkRateLimit(response, null);
                res.call(response);
            } catch (Exception e) {
                rej.call(e);
            }
        });
    }
}
