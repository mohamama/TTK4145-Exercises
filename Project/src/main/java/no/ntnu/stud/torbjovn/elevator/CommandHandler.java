package no.ntnu.stud.torbjovn.elevator;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.MessageHandler;

import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Class to handle received messages and further dispatch the necessary commands.
 *
 * Created by tovine on 3/23/16.
 */
class CommandHandler implements MessageHandler {
    // Cost is converted to milliseconds
    public static final int COST_NOT_HERE = 10, // Extra cost added for the elevators other than the one where the button was pressed
            COST_EACH_FLOOR = 100,
            COST_MOVING = 100,
            JOB_TIMEOUT = 10000; // If a job isn't marked as completed before this period expires, the next available elevator will take it.

    // Identifiers for different message types
    public static final String MESSAGE_TYPE_NEW_REQUEST = "request",
            MESSAGE_TYPE_JOB_TAKEN = "take",
            MESSAGE_TYPE_JOB_COMPLETE = "completed",
    // Identifiers for the different message properties
            PROPERTY_MESSAGE_TYPE = "type",
            PROPERTY_REQUEST_FLOOR = "target", // A number representing the target floor - negative sign is down, no sign is up
            PROPERTY_SOURCE_NODE = "source",
            NODE_ID = Settings.getSetting("ip_address"); // TODO: use only part of the IP?

    // TODO: figure out the best way to associate the Elevator instance (pass as parameter to constructor, or make Elevator class/methods static?)
    private Elevator thisElevator;

    //TODO: use a map of jobs, mirroring all active jobs in the system

    ScheduledThreadPoolExecutor waitingJobs = new ScheduledThreadPoolExecutor(1);

    /*
     * Algorithm outline:
     * ------------------
     *  1. When the user presses a 'call' button, a command is broadcast to the elevator cluster
     *  2. The elevators then (upon receiving the message) calculate a "cost" for them to respond, based on the following:
     *    - 1 point for every floor between the caller and the elevator's current position
     *    - If the elevator is moving in the direction the caller wants to go, add 1 extra point to allow an idle elevator to take priority (TODO: or not?)
     *    - If the elevator is moving in the other direction it is assumed "busy"
     *    - Also add a random delay (< 1 point) to avoid two elevators responding at exactly the same time
     *    TODO: more conditions?
     *  3. After estimating the cost, a delay period is calculated based on the number of points (multiplying by a number of milliseconds - TODO: tune it)
     *    3.1: The delay period starts running only when the elevator is idle (or moving in the right direction for the job)
     *  4. Once the delay period expires, the elevator takes the job (light on and start moving) and broadcasts a message notifying the others that it's been handled
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
                markRequestTaken(clientMessage.getIntProperty(PROPERTY_REQUEST_FLOOR));
                break;
            case MESSAGE_TYPE_JOB_COMPLETE:
                removeRequest(clientMessage.getIntProperty(PROPERTY_REQUEST_FLOOR));
                break;
            default:
                System.out.println("Got a message of unknown type, content follows:");
                System.out.println("Got message: " + clientMessage.toString());
        }
        try {
            clientMessage.acknowledge();
        } catch (ActiveMQException e) {
            System.out.println("An error occurred while acknowledging message:");
            e.printStackTrace();
        }
    }

    private void markRequestTaken(int target) {
        // TODO: Add 10 seconds to the dispatch timeout for the job
        // Step 1 - retrieve the job matching the target

        // Step 2 - add JOB_TIMEOUT to the execution timer

        // Step 3 - save the job and reschedule timer if needed
    }

    private void removeRequest(int target) {
        // TODO: remove the pending request
    }

    private void processNewRequest(int target, String source) {
        // TODO: calculate cost and add request to timeout queue
        long timeNow = System.currentTimeMillis();
        int cost = 0;
        if (!NODE_ID.equalsIgnoreCase(source)) cost += COST_NOT_HERE;
        // TODO: only include the following line if the elevator is at a location where it will make sense
        if (thisElevator.isMoving()) cost += COST_MOVING;
        cost += Math.abs(target - thisElevator.getCurrentFloor()) * COST_EACH_FLOOR;

    }

    // TODO: this doesn't really belong in this class
    /**
     * Function to broadcast a new command to the elevator cluster
     * @param targetFloor
     * @param direction
     */
    private void sendRequest(int targetFloor, int direction) {
        if (direction == Elevator.DIR_UP || direction == Elevator.DIR_DOWN) {
            if (targetFloor > Elevator.NUM_FLOORS || targetFloor < 1) return;
            Message request = Networking.createMessage()
                    .putStringProperty(PROPERTY_MESSAGE_TYPE, MESSAGE_TYPE_NEW_REQUEST)
                    .putIntProperty(PROPERTY_REQUEST_FLOOR, targetFloor * direction)
                    .putStringProperty(PROPERTY_SOURCE_NODE, NODE_ID);
            Networking.sendMessage(request);
        }
    }


}
