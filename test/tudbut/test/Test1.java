package tudbut.test;

import de.tudbut.tools.Tools;
import tudbut.api.impl.RateLimit;
import tudbut.api.impl.Restart;
import tudbut.api.impl.TudbuTAPI;
import tudbut.api.impl.TudbuTAPIV2;
import tudbut.net.pbic2.PBIC2;
import tudbut.obj.DoubleTypedObject;
import tudbut.parsing.JSON;
import tudbut.tools.Lock;
import tudbut.tools.encryption.RawKey;

import java.io.IOException;
import java.util.UUID;

public class Test1 {
    
    public static void main(String[] args) throws IOException, RateLimit, JSON.JSONFormatException, InterruptedException {
        UUID uuid = UUID.fromString("b8dd8777-a074-4f3d-a5b9-0b19def1b1ac");
    
        System.out.println("Handshake...");
        TudbuTAPIV2.handshake(uuid);
        System.out.println("Handshake done.");
    
        System.out.println(TudbuTAPIV2.request(uuid, "check", "").t);
        System.out.println(TudbuTAPIV2.request(uuid, "track/login", "TTC TudbuT/ttc:master@v1.9.0a").t);
        PBIC2 pbic2 = TudbuTAPIV2.connectGateway(uuid);
        
        while(true) {
            try {
                System.out.println(JSON.writeReadable(JSON.read(pbic2.readMessage())));
            } catch (Restart restart) {
                System.out.println("RESTART");
                Thread.sleep(15000);
                
                System.out.println("Handshake...");
                TudbuTAPIV2.handshake(uuid);
                System.out.println("Handshake done.");
    
                System.out.println(TudbuTAPIV2.request(uuid, "check", "").t);
                System.out.println(TudbuTAPIV2.request(uuid, "track/login", "TTC TudbuT/ttc:master@v1.9.0a").t);
    
                pbic2 = TudbuTAPIV2.connectGateway(uuid);
            }
        }
    
    }
}
