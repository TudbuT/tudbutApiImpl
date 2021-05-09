package tudbut.test;

import de.tudbut.timer.AsyncTask;
import tudbut.api.impl.RateLimit;
import tudbut.api.impl.TudbuTAPI;
import tudbut.api.impl.TudbuTAPIV2;
import tudbut.obj.DoubleTypedObject;
import tudbut.tools.encryption.RawKey;

import java.io.IOException;
import java.util.UUID;

public class RateLimitTest {
    
    public static void main(String[] args) throws RateLimit, IOException, InterruptedException {
        UUID uuid = UUID.randomUUID();
        System.out.println("Handshake...");
        RawKey key = TudbuTAPIV2.handshake(uuid);
        System.out.println("Handshake done. Spamming...");
        
        while (true) {
                AsyncTask<DoubleTypedObject<Boolean, String>> task = TudbuTAPIV2.requestAsync(uuid, "track/play", "");
                task.then(o -> {
                    System.out.println(o.t);
                });
                task.catchExceptions(e -> {
                    System.out.println(e.getMessage());
                });
                Thread.sleep(5);
        }
    }
}
