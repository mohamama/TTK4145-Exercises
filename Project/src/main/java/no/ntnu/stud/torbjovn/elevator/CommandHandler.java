package no.ntnu.stud.torbjovn.elevator;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.MessageHandler;

/**
 * Class to handle received messages and further dispatch the necessary commands.
 *
 * Created by tovine on 3/23/16.
 */
class CommandHandler implements MessageHandler {
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

    public void onMessage(ClientMessage clientMessage) {
        System.out.println("Got message: " + clientMessage.toString());
        System.out.println("Message content: " + clientMessage.getStringProperty(Networking.MESSAGE_FIELD_TEXT));
        try {
            clientMessage.acknowledge();
        } catch (ActiveMQException e) {
            System.out.println("An error occurred while acknowledging message:");
            e.printStackTrace();
        }
    }
}
