package no.ntnu.stud.torbjovn.elevator;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.MessageHandler;

/**
 * Class to handle received messages and further dispatch the necessary commands.
 *
 * Created by tovine on 3/23/16.
 */
class CommandHandler implements MessageHandler {
    // Cost is converted to milliseconds
    public static final int COST_NOT_HERE = 2, // Extra cost added for the elevators other than the one where the button was pressed
            COST_EACH_FLOOR = 5,
            COST_MOVING = 2,
            MILLIS_PER_COST = 100, // The factor to multiply the cost by to get the delay (ms)
            JOB_TIMEOUT = 15000; // If a job isn't marked as completed before this period expires, the next available elevator will take it.

    // Identifiers for different message types
    public static final String MESSAGE_TYPE_NEW_REQUEST = "request",
            MESSAGE_TYPE_JOB_TAKEN = "take",
            MESSAGE_TYPE_JOB_COMPLETE = "completed",
    // Identifiers for the different message properties
            PROPERTY_MESSAGE_TYPE = "type",
            PROPERTY_REQUEST_FLOOR = "target", // A number representing the target floor - negative sign is down, no sign is up
            PROPERTY_SOURCE_NODE = "source",
            NODE_ID = Settings.getSetting("ip_address");

    private Elevator thisElevator = Main.getElevator();

    /*
     * Algorithm outline:
     * ------------------
     *  1. When the user presses a 'call' button, a command is broadcast to the elevator cluster
     *  2. The elevators then (upon receiving the message) calculate a "cost" for them to respond, based on the following:
     *    - 1 point for every floor between the caller and the elevator's current position
     *    - If the elevator is moving in the direction the caller wants to go, add 1 extra point to allow an idle elevator to take priority
     *    - If the elevator is moving in the other direction it is assumed "busy"
     *    - Give the elevator where the button was pressed an advantage to somewhat mitigate race conditions
     *  3. After estimating the cost, a delay period is calculated based on the number of points (multiplying by a number of milliseconds - this can be tuned)
     *    3.1: The delay period starts running only when the elevator is idle
     *  4. Once the delay period expires, the elevator takes the job and broadcasts a message notifying the others that it's been handled
     *    4.1: All other elevators abort the job upon receiving this broadcast (and store the ID of the elevator handling it)
     *    4.2: If that elevator goes offline, the job is redistributed among the remaining ones in the same manner as before
     *  5. Finally once the job is done, the elevator who completed it broadcasts to the others that the order is processed and can be safely deleted from the system
     */

    /**
     * Handler that fires when a new message is received from the broadcast cluster
     * @param clientMessage
     */
    public void onMessage(ClientMessage clientMessage) {
        switch (clientMessage.getStringProperty(PROPERTY_MESSAGE_TYPE)) {
            case MESSAGE_TYPE_NEW_REQUEST:
                processNewRequest(clientMessage.getIntProperty(PROPERTY_REQUEST_FLOOR), clientMessage.getStringProperty(PROPERTY_SOURCE_NODE));
                break;
            case MESSAGE_TYPE_JOB_TAKEN:
                if (NODE_ID.equalsIgnoreCase(clientMessage.getStringProperty(PROPERTY_SOURCE_NODE)))
                    System.out.println("Request was sent by me, ignoring...");
                else
                    markRequestTaken(clientMessage.getIntProperty(PROPERTY_REQUEST_FLOOR));
                break;
            case MESSAGE_TYPE_JOB_COMPLETE:
                removeRequest(clientMessage.getIntProperty(PROPERTY_REQUEST_FLOOR));
                break;
            default:
                System.out.println("Got a message of unknown type, content follows:\n" + clientMessage.toString());
        }
        System.out.println("Got message from " + clientMessage.getStringProperty(PROPERTY_SOURCE_NODE) + ", type: " + clientMessage.getStringProperty(PROPERTY_MESSAGE_TYPE) + ", target floor: " + clientMessage.getIntProperty(PROPERTY_REQUEST_FLOOR));
        try {
            clientMessage.acknowledge();
        } catch (ActiveMQException e) {
            System.out.println("An error occurred while acknowledging message:");
            e.printStackTrace();
        }
    }

    private void markRequestTaken(int target) {
        // Step 1 - retrieve the job matching the target
        Long timeout = CommandDispatcher.getActiveJobs().get(target);
        if (timeout == null) {
            // Job doesn't exist in the queue, so it should be added - initialize variable to avoid any issues
            timeout = 0L;
        }
        // Step 2 - add JOB_TIMEOUT to the execution timer
        timeout += JOB_TIMEOUT;
        // Step 3 - save the job and reschedule timer if needed
        CommandDispatcher.addRequestToQueue(target, timeout);
    }

    private void removeRequest(int target) {
        int button, floor;
        CommandDispatcher.cancelRequest(target);
        if (target > 0)
            button = Elevator.BUTTON_TYPE_CALL_UP;
        else
            button = Elevator.BUTTON_TYPE_CALL_DOWN;
        // Convert from 1- to 0-indexed for the low-level functions
        floor = Math.abs(target) - 1;
        thisElevator.elev_set_button_lamp(button, floor, 0);
    }

    private void processNewRequest(int target, String source) {
        if (!thisElevator.isMoving() && Math.abs(target) == thisElevator.getCurrentFloor()){
            // We're already here, cancel this request
            signalJobCompleted(target);
            thisElevator.asyncGoToFloor(target);
            return;
        }
        long cost = calculateDelay(target, source);
        CommandDispatcher.addRequestToQueue(target, cost);
        int button, floor;
        if (target > 0)
            button = Elevator.BUTTON_TYPE_CALL_UP;
        else
            button = Elevator.BUTTON_TYPE_CALL_DOWN;
        // Convert from 1- to 0-indexed for the low-level functions
        floor = Math.abs(target) - 1;
        thisElevator.elev_set_button_lamp(button, floor, 1);
    }

    private long calculateDelay(int target, String source) {
        long cost = 0;
        if (!NODE_ID.equalsIgnoreCase(source)) cost += COST_NOT_HERE;
        if (thisElevator.isMoving()) cost += COST_MOVING;
        cost += Math.abs(Math.abs(target) - thisElevator.getCurrentFloor()) * COST_EACH_FLOOR;
        return cost * MILLIS_PER_COST;
    }

    /**
     * Function to broadcast a new command to the elevator cluster
     * @param targetFloor - the target floor to go to (1-indexed), negative sign means "down"
     */
    public static void sendRequest(int targetFloor) {
        if (Math.abs(targetFloor) > Elevator.NUM_FLOORS || targetFloor == 0) return;
        try {
            Message request = Networking.createMessage()
                    .putStringProperty(PROPERTY_MESSAGE_TYPE, MESSAGE_TYPE_NEW_REQUEST)
                    .putIntProperty(PROPERTY_REQUEST_FLOOR, targetFloor)
                    .putStringProperty(PROPERTY_SOURCE_NODE, NODE_ID);
            Networking.sendMessage(request);
        } catch (NullPointerException npe) {
            System.out.println("Network is not yet ready, please wait...");
        }
    }

    public static void signalTakeJob(int targetFloor) {
        if (Math.abs(targetFloor) > Elevator.NUM_FLOORS || targetFloor == 0) return;
        try {
            Message notification = Networking.createMessage()
                    .putStringProperty(PROPERTY_MESSAGE_TYPE, MESSAGE_TYPE_JOB_TAKEN)
                    .putStringProperty(PROPERTY_SOURCE_NODE, NODE_ID)
                    .putIntProperty(PROPERTY_REQUEST_FLOOR, targetFloor);
            Networking.sendMessage(notification);
        } catch (NullPointerException npe) {
            System.out.println("Network is not yet ready, please wait...");
        }
    }

    public static void signalJobCompleted(int targetFloor) {
        if (Math.abs(targetFloor) > Elevator.NUM_FLOORS || targetFloor == 0) return;
        try {
            Message notification = Networking.createMessage()
                    .putStringProperty(PROPERTY_MESSAGE_TYPE, MESSAGE_TYPE_JOB_COMPLETE)
                    .putStringProperty(PROPERTY_SOURCE_NODE, NODE_ID)
                    .putIntProperty(PROPERTY_REQUEST_FLOOR, targetFloor);
            Networking.sendMessage(notification);

            // If at the top or bottom, there's only one way to go - make double-sure the job is marked done
            if (Math.abs(targetFloor) == 1 || Math.abs(targetFloor) == Elevator.NUM_FLOORS) {
                Message notification2 = Networking.createMessage()
                        .putStringProperty(PROPERTY_MESSAGE_TYPE, MESSAGE_TYPE_JOB_COMPLETE)
                        .putStringProperty(PROPERTY_SOURCE_NODE, NODE_ID)
                        .putIntProperty(PROPERTY_REQUEST_FLOOR, -(targetFloor));
                Networking.sendMessage(notification2);
            }
        } catch (NullPointerException npe) {
            System.out.println("Network is not yet ready, please wait...");
        }
    }

}
