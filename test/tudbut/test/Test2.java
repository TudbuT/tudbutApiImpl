package tudbut.test;

import de.tudbut.api.TTCVersion;
import de.tudbut.api.TudbuTAPI;
import de.tudbut.api.User;
import de.tudbut.async.TaskQueue;

/**
 * @author TudbuT
 * @since 04 Jun 2022
 */

public class Test2 {
    public static void main(String[] args) throws InterruptedException {
        TudbuTAPI.getUser("TudbuT")
                 .<User>compose((resp, res, rej) -> {
                     System.out.println("UUID");
                     resp.retrieveUUID().ok().await();
                     res.call(resp);
                     System.out.println("A");
                 }).then(System.out::println).err(Throwable::printStackTrace).ok();
        TudbuTAPI.getUser("TudbuT")
                 .<User>compose((resp, res, rej) -> {
                     System.out.println("UUID");
                     resp.retrieveUUID().ok().await();
                     res.call(resp);
                     System.out.println("B");
                 }).then(System.out::println).err(Throwable::printStackTrace).ok();
        TudbuTAPI.getUser("TudbuT")
                 .<User>compose((resp, res, rej) -> {
                     System.out.println("UUID");
                     resp.retrieveUUID().ok().await();
                     res.call(resp);
                     System.out.println("C");
                 }).then(System.out::println).then(user -> System.out.println(user.getVersion().isOlderThan(new TTCVersion("", "", "v0.0.0a")))).err(Throwable::printStackTrace).ok();
        TaskQueue.main.finish();
        System.exit(0);
    }
}
