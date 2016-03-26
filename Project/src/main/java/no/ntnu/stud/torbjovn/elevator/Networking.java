package no.ntnu.stud.torbjovn.elevator;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.*;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;

import java.util.HashMap;
import java.util.Map;

/**
 * Class implementing the networking and message passing between the elevators in the cluster.
 *
 * Created by tovine on 3/22/16.
 */
class Networking {
    public static final String QUEUE_NAME = "elevatorQueue",
                            MESSAGE_FIELD_TEXT = "messageText",
                            MESSAGING_USER = "elevator",
                            MESSAGING_PASS = "superSecret";
    private static EmbeddedActiveMQ embeddedServer = new EmbeddedActiveMQ();
    private static ClientSession artemisSession = null;
    private static ClientConsumer messageConsumer = null;
    private static ClientProducer messageProducer = null;

    private static void startServer() throws Exception {
        System.out.println("Attempting to start EmbeddedActiveMQ server...");
        try {
            embeddedServer.start();
        } catch (Exception e) {
            System.out.println("Failed to start EmbeddedActiveMQ server, stack trace follows:");
            e.printStackTrace();
            throw e;
        }
    }

    private static void stopServer() throws Exception {
        System.out.println("Attempting to stop EmbeddedActiveMQ server...");
        try {
            if (messageProducer != null) messageProducer.close();
            if (messageConsumer != null) messageConsumer.close();
            if (artemisSession != null) {
                artemisSession.close();
                artemisSession.getSessionFactory().close();
                artemisSession.getSessionFactory().getServerLocator().close();
            }
            embeddedServer.stop();
            System.out.println("Stopping server");
        } catch (Exception e) {
            System.out.println("Failed to stop EmbeddedActiveMQ server, stack trace follows:");
            e.printStackTrace();
            throw e;
        }
    }

    private static void startClient() throws Exception {
        try {
            Map<String, Object> connectionParams = new HashMap<String, Object>();
            connectionParams.put(TransportConstants.PORT_PROP_NAME, 61617);
            connectionParams.put(TransportConstants.HOST_PROP_NAME, "localhost");

            // FIXME: create user/pass combination and use it
            ClientSessionFactory nettyFactory =  ActiveMQClient.createServerLocator(true, new TransportConfiguration(NettyConnectorFactory.class.getName(), connectionParams))
                    .createSessionFactory();
            // TODO: uncomment once user is created in artemis broker configuration
            // Create a session with the supplied user name and password, the rest is left to the default values when calling createSession() without parameters
//            artemisSession = nettyFactory.createSession(MESSAGING_USER, MESSAGING_PASS, false, true, true,
//                    nettyFactory.getServerLocator().isPreAcknowledge(), nettyFactory.getServerLocator().getAckBatchSize());
            artemisSession = nettyFactory.createSession();
            if (!artemisSession.queueQuery(SimpleString.toSimpleString(QUEUE_NAME)).isExists())
                artemisSession.createQueue(QUEUE_NAME, QUEUE_NAME, true); // (String address, String queue, boolean durable) // TODO: does this need to be durable?
            messageConsumer = artemisSession.createConsumer(QUEUE_NAME);
            messageProducer = artemisSession.createProducer(QUEUE_NAME);
            messageConsumer.setMessageHandler(new CommandHandler());
            artemisSession.start();
        } catch (Exception e) {
            System.out.println("Failed to start a client connection, stack trace:");
            e.printStackTrace();
            throw e;
        }
    }

    public static boolean sendMessage(Message msg) {
        try {
            messageProducer.send(msg);
        } catch (ActiveMQException e) {
            System.out.println("Failed to send message:");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static Message createMessage(String msgString) {
        return artemisSession.createMessage(false).putStringProperty(MESSAGE_FIELD_TEXT, msgString);
    }

    public static void main(String[] args) {
        try {
            startServer();
            startClient();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    System.out.println("Shutting down");
                    try {
                        stopServer();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            sendMessage(createMessage("Hello world!"));
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }
}
