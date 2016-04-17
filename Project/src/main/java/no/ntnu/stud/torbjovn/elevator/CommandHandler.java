package no.ntnu.stud.torbjovn.elevator;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.Message;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.MessageHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to handle received messages and further dispatch the necessary commands.
 *
 * Created by tovine on 3/23/16.
 */
class CommandHandler implements MessageHandler {
    // Cost is converted to milliseconds
    public static final int COST_NOT_HERE = 1, // Extra cost added for the elevators other than the one where the button was pressed
            COST_EACH_FLOOR = 3,
            COST_MOVING = 2,
            MILLIS_PER_COST = 500, // The factor to multiply the cost by to get the delay (ms)
            JOB_TIMEOUT = 20000; // If a job isn't marked as completed before this period expires, the next available elevator will take it.

    // Identifiers for different message types
    public static final String MESSAGE_TYPE_NEW_REQUEST = "request",
            MESSAGE_TYPE_JOB_TAKEN = "take",
            MESSAGE_TYPE_JOB_COMPLETE = "completed",
    // Identifiers for the different message properties
            PROPERTY_MESSAGE_TYPE = "type",
            PROPERTY_REQUEST_FLOOR = "target", // A number representing the target floor - negative sign is down, no sign is up
            PROPERTY_SOURCE_NODE = "source",
            NODE_ID = Settings.getSetting("ip_address"); // TODO: use only part of the IP?

    private Elevator thisElevator = Main.getElevator();

    //TODO: use a map of jobs, mirroring all active jobs in the system
    private static Map<Integer, Long> activeJobs = new HashMap<>(Elevator.NUM_FLOORS * 2);

    public static boolean jobExists(int target) {
        return activeJobs.containsKey(target);
    }

    // TODO: borrow timeout implementation from alarm, or delay scheduling of new jobs until the current one is completed
//    private ScheduledThreadPoolExecutor waitingJobs = new ScheduledThreadPoolExecutor(1);
//    private ScheduledFuture<?> currentJob = null; private int currentJobFloor = 0;

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
                if (NODE_ID.equalsIgnoreCase(clientMessage.getStringProperty(PROPERTY_SOURCE_NODE))) break;
                markRequestTaken(clientMessage.getIntProperty(PROPERTY_REQUEST_FLOOR));
                break;
            case MESSAGE_TYPE_JOB_COMPLETE:
                removeRequest(clientMessage.getIntProperty(PROPERTY_REQUEST_FLOOR));
                break;
            default:
                System.out.println("Got a message of unknown type, content follows:");
                System.out.println("Got message: " + clientMessage.toString());
        }
        System.out.println("Got message from " + clientMessage.getStringProperty(PROPERTY_SOURCE_NODE) + ", type: " + clientMessage.getStringProperty(PROPERTY_MESSAGE_TYPE) + ", target floor: " + clientMessage.getIntProperty(PROPERTY_REQUEST_FLOOR));
        try {
            clientMessage.acknowledge();
        } catch (ActiveMQException e) {
            System.out.println("An error occurred while acknowledging message:");
            e.printStackTrace();
        }
    }

//    private synchronized void updateSchedule() {
//        if (activeJobs.isEmpty()) {
//            System.out.println("No valid job found");
//            return;
//        }
//        Long minimum = null;
//        Integer target = null;
//        // We can do it this way because the size of the set is relatively limited - ( (NUM_FLOORS - 1) * 2)
//        for (Map.Entry<Integer, Long> job: activeJobs.entrySet()) {
//            Long timeout = job.getValue();
//            if (timeout == null) {
//                timeout = calculateDelay(job.getKey(), "");
//            }
//            if (minimum == null || timeout < minimum) {
//                minimum = timeout;
//                target = job.getKey();
//            }
//        }
//        if (minimum == null || (currentJob != null && minimum > currentJob.getDelay(TimeUnit.MILLISECONDS))) {
////            minimum = calculateDelay(target, ""); // TODO: calculate cost for all pending requests in the system and take the lowest?
//            return;
//        }
//        currentJob = waitingJobs.schedule(new ElevatorTask(target), minimum, TimeUnit.MILLISECONDS);
//        activeJobs.remove(target);
//        System.out.println("Submitted new request for execution in " + minimum + "ms, target: " + target);
//    }

    private void markRequestTaken(int target) {
        // Step 1 - retrieve the job matching the target
        Long timeout = activeJobs.get(target);
        if (timeout == null) {
            // Job doesn't exist in the queue - add it
//            processNewRequest(target, "");
//            timeout = activeJobs.get(target);
            timeout = 0L;
        }
        // Step 2 - add JOB_TIMEOUT to the execution timer
        int waitingJobs = activeJobs.size();
        if (waitingJobs == 0) waitingJobs = 1;
        timeout += JOB_TIMEOUT * waitingJobs;
        // Step 3 - save the job and reschedule timer if needed
//        activeJobs.put(target, timeout);
//        updateSchedule();
        CommandDispatcher.addRequestToQueue(target, timeout);
    }

    private void removeRequest(int target) {
        int button, floor;
//        if (currentJob != null && currentJobFloor != target)
//            currentJob.cancel(false);
//        activeJobs.remove(target);
        CommandDispatcher.cancelRequest(target);
        if (target > 0)
            button = Elevator.BUTTON_TYPE_CALL_UP;
        else
            button = Elevator.BUTTON_TYPE_CALL_DOWN;
        floor = Math.abs(target) - 1; // Convert from 1- to 0-indexed
        thisElevator.elev_set_button_lamp(button, floor, 0);
//        updateSchedule();
    }

    private void processNewRequest(int target, String source) {
        if (!thisElevator.isMoving() && Math.abs(target) == thisElevator.getCurrentFloor()){
            // We're already here, cancel this request
            signalJobCompleted(target);
            return;
        }
        long cost = calculateDelay(target, source);
        int button, floor;
        // TODO: only include the following line if the elevator is at a location where it will make sense
//        if (currentJob == null) cost = calculateDelay(target, source);
//        activeJobs.put(target, cost);
        CommandDispatcher.addRequestToQueue(target, cost);
        if (target > 0)
            button = Elevator.BUTTON_TYPE_CALL_UP;
        else
            button = Elevator.BUTTON_TYPE_CALL_DOWN;
        floor = Math.abs(target) - 1; // Convert from 1- to 0-indexed
        thisElevator.elev_set_button_lamp(button, floor, 1);
//        updateSchedule();
    }

    private long calculateDelay(int target, String source) {
        long cost = 0;
        if (!NODE_ID.equalsIgnoreCase(source)) cost += COST_NOT_HERE;
        if (thisElevator.isMoving()) cost += COST_MOVING;
        cost += Math.abs(Math.abs(target) - thisElevator.getCurrentFloor()) * COST_EACH_FLOOR;
        return cost * MILLIS_PER_COST;
    }

    // TODO: this doesn't really belong in this class
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
        // If at the top or bottom, there's only one call button - make double-sure we get the right one
        if (targetFloor == -1) targetFloor = 1;
        else if (targetFloor == Elevator.NUM_FLOORS) targetFloor = -Elevator.NUM_FLOORS;
        try {
            Message notification = Networking.createMessage()
                    .putStringProperty(PROPERTY_MESSAGE_TYPE, MESSAGE_TYPE_JOB_COMPLETE)
                    .putStringProperty(PROPERTY_SOURCE_NODE, NODE_ID)
                    .putIntProperty(PROPERTY_REQUEST_FLOOR, targetFloor);
            Networking.sendMessage(notification);
        } catch (NullPointerException npe) {
            System.out.println("Network is not yet ready, please wait...");
        }
    }

//    public class ElevatorTask implements Runnable {
//        int target;
//
//        public ElevatorTask(int targetFloor) {
//            target = targetFloor;
//        }
//
//        @Override
//        public void run() {
//            signalTakeJob(target);
//            currentJobFloor = target;
//            thisElevator.goToFloor(Math.abs(target));
//            signalJobCompleted(target);
//            updateSchedule();
//            currentJob = null;
//            currentJobFloor = 0;
//        }
//    }
}
