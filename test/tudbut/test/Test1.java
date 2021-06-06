package tudbut.test;

import de.tudbut.tools.Tools;
import tudbut.api.impl.*;
import tudbut.net.pbic2.PBIC2;
import tudbut.net.pbic2.PBIC2AEventHandler;
import tudbut.net.pbic2.PBIC2AListener;
import tudbut.net.pbic2.PBIC2Client;
import tudbut.obj.DoubleTypedObject;
import tudbut.parsing.JSON;
import tudbut.tools.ArrayTools;
import tudbut.tools.Lock;
import tudbut.tools.encryption.RawKey;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.UUID;

public class Test1 {
    
    public static void main(String[] args) throws IOException, RateLimit, JSON.JSONFormatException, InterruptedException {
        
        UUID uuid = UUID.fromString("b8dd8777-a074-4f3d-a5b9-0b19def1b1ac");
        System.out.println("Handshake...");
        TudbuTAPIV2.handshake(uuid);
        System.out.println("Handshake done.");
    
        System.out.println(TudbuTAPIV2.request(uuid, "check", "").t);
        System.out.println(TudbuTAPIV2.request(uuid, "track/login", "TTC TudbuT/ttc:master@v1.9.0a").t);
        //System.out.println(TudbuTAPIV2.request(uuid, "message", "other=65a10b8e-2dc4-44fa-9064-4be955e911b6", "§f§n§lTudbuT2 is real").t);
    
        PBIC2AListener listener = new PBIC2AListener() {
            @Override
            public void onMessage(String s) throws IOException {
                System.out.println(s);
            }
        
            @Override
            public void onError(Throwable throwable) {
            
            }
        };
    
        new PBIC2AEventHandler().start(TudbuTAPIV2.connectGateway(uuid), listener);
    
    }
}
