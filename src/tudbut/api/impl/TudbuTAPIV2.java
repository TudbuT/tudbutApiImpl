package tudbut.api.impl;

import de.tudbut.timer.AsyncTask;
import de.tudbut.tools.Hasher;
import sun.awt.windows.ThemeReader;
import tudbut.io.TypedInputStream;
import tudbut.io.TypedOutputStream;
import tudbut.net.http.*;
import tudbut.net.pbic2.*;
import tudbut.obj.DoubleTypedObject;
import tudbut.parsing.JSON;
import tudbut.parsing.TCN;
import tudbut.parsing.TCNArray;
import tudbut.tools.encryption.KeyStream;
import tudbut.tools.encryption.RawKey;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static tudbut.api.impl.TudbuTAPI.rateLimitLock;

public class TudbuTAPIV2 {
    public static final String host = TudbuTAPI.host;
    public static final int port = TudbuTAPI.port;
    
    public static DoubleTypedObject<Integer, RawKey> getReq(UUID uuid) {
        return req.get(uuid).clone();
    }
    
    private static final Map<UUID, DoubleTypedObject<Integer, RawKey>> req = new HashMap<>();

    public static RawKey handshake(UUID uuid) throws IOException, RateLimit {
        HTTPRequest request;
        RawKey key = new RawKey();
        ParsedHTTPValue p;
    
        String s = "";
        while (!s.equals("OK.S")) {
            rateLimitLock.waitHere();
            
            // Get key
            request = new HTTPRequest(HTTPRequestType.POST, host, port, "/api/v2/handshake/login?uuid=" + uuid);
            key = new RawKey((p = request.send().parse()).getBodyRaw());
            checkRateLimit(p, null);
    
            // Check key
            request = new HTTPRequest(HTTPRequestType.POST, host, port, "/api/v2/handshake/check?uuid=" + uuid, HTTPContentType.TXT, key.encryptString("OK.C"));
            s = key.decryptString((p = request.send().parse()).getBodyRaw());
            checkRateLimit(p, null);
        }
        req.put(uuid, new DoubleTypedObject<>(0, key));
        while(!s.equals("OK")) {
            s = request(uuid, "check", "").t;
            rateLimitLock.waitHere();
        }
    
        // Done!
        return key;
    }
    
    static void checkRateLimit(ParsedHTTPValue p, UUID uuid) throws RateLimit {
        rateLimitLock.lock((int) (150 + rateLimitLock.timeLeft()));
        if(p.getStatusCodeAsEnum() == HTTPResponseCode.TooManyRequests) {
            try {
                if(uuid != null) {
                    req.get(uuid).o--;
                }
                TCN rateLimit = JSON.read(p.getBodyRaw());
                doRateLimit(rateLimit.getSub("rateLimit"));
            }
            catch (JSON.JSONFormatException e) {
                e.printStackTrace();
            }
        }
        if(p.getStatusCodeAsEnum() == HTTPResponseCode.PayloadTooLarge) {
            try {
                if(uuid != null) {
                    req.get(uuid).o--;
                }
                TCN exception = JSON.read(p.getBodyRaw());
                doConsequences(exception);
            }
            catch (JSON.JSONFormatException e) {
                e.printStackTrace();
            }
        }
    }
    
    private static void doRateLimit(TCN rateLimit) throws RateLimit {
        rateLimitLock.lock(rateLimit.getInteger("time"));
        throw new RateLimit(rateLimit);
    }
    
    private static void doConsequences(TCN payloadTooLargeException) throws RateLimit {
        RateLimit toThrow = null;
        
        TCNArray array = payloadTooLargeException.getArray("consequences");
        for (Object o : array) {
            if(!(o instanceof TCN))
                continue;
            TCN tcn = (TCN) o;
            if(tcn.getString("type").equals("RateLimit")) {
                try {
                    doRateLimit(tcn.getSub("rateLimit"));
                } catch (RateLimit rateLimit) {
                    toThrow = rateLimit;
                }
            }
        }
        
        if(toThrow != null)
            throw toThrow;
    }
    
    public static HTTPHeader nextAuthorizationHeader(UUID uuid) {
        RawKey key = req.get(uuid).t;
        return new HTTPHeader("Authorization", Hasher.sha256hex(key.encryptString(req.get(uuid).o++ + key.toHashString())));
    }
    
    public static DoubleTypedObject<Boolean, String> request(UUID uuid, String path, String body) throws IOException, RateLimit {
        RawKey key = req.get(uuid).t;
        HTTPRequest request = new HTTPRequest(HTTPRequestType.POST, host, port, "/api/v2/" + path + "?uuid=" + uuid, HTTPContentType.TXT, key.encryptString(body), nextAuthorizationHeader(uuid));
        ParsedHTTPValue p = request.send().parse();
        checkRateLimit(p, uuid);
        String s = p.getBodyRaw();
        return new DoubleTypedObject<>(p.getStatusCodeAsEnum() == HTTPResponseCode.OK, p.getStatusCodeAsEnum() != HTTPResponseCode.OK ? s : key.decryptString(s));
    }
    
    public static AsyncTask<DoubleTypedObject<Boolean, String>> requestAsync(UUID uuid, String path, String body) {
        return new AsyncTask<>(() -> request(uuid, path, body));
    }
    
    public static PBIC2 connectGateway(UUID uuid) throws IOException {
        KeyStream key = new KeyStream(req.get(uuid).t);
        HTTPRequest request = new HTTPRequest(HTTPRequestType.POST, host, port, "/api/v2/gateway?uuid=" + uuid, HTTPContentType.ANY, "", nextAuthorizationHeader(uuid));
        return new PBIC2() {
            private final PBIC2 c = new PBIC2Client(request, i -> {
                i = key.decrypt(i);
                System.out.write(i);
                return i;
            }, i -> {
                i = key.encrypt(i);
                return i;
            });
            
            @Override
            public String readMessage() throws IOException {
                String m = c.readMessage();
                System.out.println();
                try {
                    if(JSON.read(m).getString("id").equals("restart")) {
                        throw new Restart();
                    }
                }
                catch (JSON.JSONFormatException e) {
                }
                return m;
            }
    
            @Override
            public String writeMessage(String s) throws IOException {
                return c.writeMessage(s);
            }
    
            @Override
            public String writeMessageWithResponse(String s) throws IOException {
                writeMessage(s);
                return readMessage();
            }
    
            @Override
            public Socket getSocket() {
                return c.getSocket();
            }
    
            @Override
            public TypedInputStream getInput() {
                return c.getInput();
            }
    
            @Override
            public TypedOutputStream getOutput() {
                return c.getOutput();
            }
        };
    }
}
