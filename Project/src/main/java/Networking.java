import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;

/**
 * Created by tovine on 3/22/16.
 */
public class Networking {
    private static EmbeddedActiveMQ embedded = new EmbeddedActiveMQ();

    private static void startServer() throws Exception {
        System.out.println("Attempting to start EmbeddedActiveMQ server...");
        try {
            embedded.start();
        } catch (Exception e) {
            System.out.println("Failed to start EmbeddedActiveMQ server, stack trace follows:");
            e.printStackTrace();
            throw e;
        }
    }

    public static void main(String[] args) {
        try {
            startServer();
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }
}
