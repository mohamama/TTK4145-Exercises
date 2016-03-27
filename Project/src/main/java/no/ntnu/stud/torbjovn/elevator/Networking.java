package no.ntnu.stud.torbjovn.elevator;

import org.apache.activemq.artemis.api.core.*;
import org.apache.activemq.artemis.api.core.client.*;
import org.apache.activemq.artemis.core.config.FileDeploymentManager;
import org.apache.activemq.artemis.core.config.impl.FileConfiguration;
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
    public static final String DESTINATION_ADDRESS = "elevatorQueue",
                            MESSAGE_FIELD_TEXT = "messageText",
                            MESSAGING_USER = "elevator",
                            MESSAGING_PASS = "superSecret",
                            CONNECTOR_NAME = "netty-connector";
    // UDP discovery settings
    private static final String GROUP_BROADCAST_ADDRESS = "224.0.0.1";
    private static final int GROUP_BROADCAST_PORT = 9876;
    // Networking objects
    private static String queueName;
    private static EmbeddedActiveMQ embeddedServer = new EmbeddedActiveMQ();
    private static ClientSession artemisSession = null;
    private static ClientConsumer messageConsumer = null;
    private static ClientProducer messageProducer = null;

    private static void startServer() throws Exception {
        System.out.println("Attempting to start EmbeddedActiveMQ server...");
        try {
            // Manually read the config from the broker.xml, as we have to set some options programmatically
            FileDeploymentManager deploymentManager = new FileDeploymentManager("broker.xml");
            FileConfiguration config = new FileConfiguration();
            deploymentManager.addDeployable(config);
            deploymentManager.readConfiguration();
            try {
                // Load the ip address to be used from config file, to be able to deploy the same JAR to different nodes
                config.getConnectorConfigurations().get(CONNECTOR_NAME).getParams().put(TransportConstants.HOST_PROP_NAME, Settings.getSetting("ip_address"));
            } catch (NullPointerException npe) {
                System.out.println("Error: ip_address property not set in config file, falling back to 'localhost'");
            }
            embeddedServer.setConfiguration(config);
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
                artemisSession.deleteQueue(queueName); // Remove the subscription to the address
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
//            DiscoveryGroupConfiguration discoveryGroupConfiguration = new DiscoveryGroupConfiguration();
//            UDPBroadcastEndpointFactory udpBroadcastEndpointFactory = new UDPBroadcastEndpointFactory();
//            udpBroadcastEndpointFactory.setGroupAddress(GROUP_BROADCAST_ADDRESS).setGroupPort(GROUP_BROADCAST_PORT);
//            discoveryGroupConfiguration.setBroadcastEndpointFactory(new UDPBroadcastEndpointFactory().setGroupAddress(GROUP_BROADCAST_ADDRESS).setGroupPort(GROUP_BROADCAST_PORT));
            ClientSessionFactory nettyFactory = ActiveMQClient.createServerLocatorWithHA(
                    new DiscoveryGroupConfiguration().setBroadcastEndpointFactory(
                            new UDPBroadcastEndpointFactory().setGroupAddress(GROUP_BROADCAST_ADDRESS).setGroupPort(GROUP_BROADCAST_PORT)
                    )
            )
//                    ActiveMQClient.createServerLocator(true, new TransportConfiguration(NettyConnectorFactory.class.getName(), connectionParams))
                    .createSessionFactory();
            // TODO: uncomment once user is created in artemis broker configuration
            // Create a session with the supplied user name and password, the rest is left to the default values when calling createSession() without parameters
//            artemisSession = nettyFactory.createSession(MESSAGING_USER, MESSAGING_PASS, false, true, true,
//                    nettyFactory.getServerLocator().isPreAcknowledge(), nettyFactory.getServerLocator().getAckBatchSize());
            artemisSession = nettyFactory.createSession();
//            System.out.println("Artemis sessionFactory ID: " +artemisSession.getSessionFactory().getConnection().getID().toString()); // -862590771
            queueName = DESTINATION_ADDRESS + artemisSession.getSessionFactory().getConnection().getID().toString();
            artemisSession.createQueue(DESTINATION_ADDRESS, queueName, true); // (String address, String queue, boolean durable) // TODO: does this need to be durable?
            messageConsumer = artemisSession.createConsumer(queueName);
            messageProducer = artemisSession.createProducer(DESTINATION_ADDRESS);
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
            int count = 0;
            while (!messageProducer.isClosed()) {
                sendMessage(createMessage("Hello world " + count));
                count++;
                Thread.sleep(2000);
            }
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }
}
