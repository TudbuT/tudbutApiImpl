package tudbut.test;

import de.tudbut.api.TudbuTAPI;
import de.tudbut.api.User;
import tudbut.api.impl.RateLimit;
import tudbut.api.impl.Restart;
import tudbut.api.impl.TudbuTAPIV2;
import tudbut.net.pbic2.PBIC2AEventHandler;
import tudbut.net.pbic2.PBIC2AListener;
import tudbut.parsing.AsyncJSON;
import tudbut.parsing.JSON;
import tudbut.parsing.TCN;

import java.io.IOException;
import java.util.UUID;

public class Test1 {
    
    public static void main(String[] args) throws IOException, RateLimit, JSON.JSONFormatException, InterruptedException {
        
        UUID uuid = UUID.fromString("b8dd8777-a074-4f3d-a5b9-0b19def1b1ac");
        System.out.println("Handshake...");
        TudbuTAPIV2.handshake(uuid);
        System.out.println("Handshake done.");
    
        System.out.println(TudbuTAPIV2.request(uuid, "check", "").t);
        System.out.println(TudbuTAPIV2.request(uuid, "track/login", "de.tudbut.api TudbuT/tudbutApiImpl:master@v0.0.0a").t);
        
        PBIC2AEventHandler handler = new PBIC2AEventHandler();
    
        TCN db = new TCN();
        
        TudbuTAPI
                .getAllUsers()
                .<User[]>compose((resp, res, rej) -> {
                    try {
                        Thread.sleep(500);
                    }
                    catch (InterruptedException e) {
                        rej.call(e);
                    }
                    for (User user : resp) {
                        user = user.retrieveSelf()
                                   .ok()
                                   .await();
                        System.out.println("Retrieved: " +
                                           user.getUUID().ok().await() + ": " +
                                           user.getName());
                        try {
                            Thread.sleep(150);
                        }
                        catch (InterruptedException e) {
                            rej.call(e);
                        }
                    }
                    res.call(resp);
                })
                .then(users -> {
                    for (int i = 0 ; i < users.length ; i++) {
                        db.set(users[i].getUUID().ok().await().toString(), users[i].object);
                    }
                })
                .err(Throwable::printStackTrace)
                .ok()
                .await();
        
        PBIC2AListener[] listener = { null };
        listener[0] = new PBIC2AListener() {
            @Override
            public void onMessage(String s) throws IOException {
                System.out.println(s);
                AsyncJSON.read(s)
                        .<UUID[]>compose((json, res, rej) -> {
                            if (json.getString("id").equals("save")) {
                                Object[] arr = json.getSub("data").getArray("changes").toArray();
                                UUID[] uuids = new UUID[arr.length];
                                for (int i = 0 ; i < arr.length ; i++) {
                                    uuids[i] = UUID.fromString((String) arr[i]);
                                }
                                res.call(uuids);
                            }
                            else
                                res.call(null);
                        })
                        .then(uuids -> {
                            if(uuids == null)
                                return;
                            for (int i = 0 ; i < uuids.length ; i++) {
                                int finalI = i;
                                TudbuTAPI.getUser(uuids[i]).then(user -> {
                                    db.set(uuids[finalI].toString(), user.object);
                                });
                            }
                        })
                        .then(uuids -> AsyncJSON.write(db).then(System.out::println).err(Throwable::printStackTrace).ok())
                        .err(Throwable::printStackTrace)
                        .ok();
                try {
                    if(JSON.read(s).getString("id").equals("restart")) {
                        throw new Restart();
                    }
                }
                catch (JSON.JSONFormatException ignored) {
                }
            }
        
            @Override
            public void onError(Throwable throwable) {
                if(throwable instanceof Restart) {
                    try {
                        System.out.println("Restart");
                        Thread.sleep(10000);
                        handler.start(TudbuTAPIV2.connectGateway(uuid), listener[0]);
                    }
                    catch (IOException | InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        handler.start(TudbuTAPIV2.connectGateway(uuid), listener[0]);
    
    
        Thread.sleep(500);
        System.out.println(TudbuTAPIV2.request(uuid, "message", "other=" + uuid, "§f§n§lTudbuT2 is real").t);
    }
}
