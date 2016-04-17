package no.ntnu.stud.torbjovn.elevator;

import cz.adamh.utils.NativeUtils;

import java.io.IOException;

/**
 * Class to represent and control the physical/simulated elevator
 * Created by marje on 22.03.2016.
 */
public class Elevator {
    private InputListener inputListenerThread = new InputListener();

    public static final int NUM_FLOORS = 4,
            DIR_UP = 1,
            DIR_DOWN = -1,
            DIR_STOP = 0,
            NUM_BUTTONS = 3,
            BUTTON_TYPE_CALL_UP = 0,
            BUTTON_TYPE_CALL_DOWN = 1,
            BUTTON_TYPE_COMMAND = 2,
            // How long to keep the door open after arriving at a floor (in ms)
            WAIT_OPEN_DOOR = 1000;

    // A list of commands added by button presses from within the elevator cabin - these should have priority OVER commands received from network
    private static boolean[] internalCommands = new boolean[NUM_FLOORS];

    private static final int ET_Comedi = 0,
            ET_Simulation = 1;

    private int direction = 0;
    private boolean busy;

    public boolean isBusy() {
        return busy;
    }

    private AsyncWorker elevWorker = new AsyncWorker();
//    private int currentFloor = 0;

    static {
        try {
            NativeUtils.loadLibraryFromJar("/driver/libelevator.so");
        } catch (IOException e) {
            System.out.println("ERROR: unable to read library file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Elevator() {
        if (!hw_init(ET_Comedi)) {
            System.out.println("Failed to initialize elevator hardware!");
            if(!hw_init(ET_Simulation)){
                System.out.println("Failed to initialize elevator simulator!");
                Runtime.getRuntime().exit(1);
            }
            System.out.println("Simulator initialization done");
        } else
            System.out.println("HW initialization done");

        int currFloor = getCurrentFloor();
        if (currFloor == 0) {
            System.out.println("Elevator is currently between floors, trying to find out where");
            findMyLocation();
        } else {
            System.out.println("Elevator is currently at floor " + currFloor);
            elev_set_floor_indicator(currFloor);
        }

        // Start listening for button events
        elevWorker.start();
        inputListenerThread.start();
    }

    public void stopElevator() {
        elevWorker.interrupt();
        setDirection(DIR_STOP);
    }

    private void findMyLocation() {
        if (getCurrentFloor() > 0) return; // We are already at a known floor
        setDirection(DIR_DOWN);
        while(getCurrentFloor() == 0) {
            try { Thread.sleep(10); } catch (InterruptedException ignored) {} // Sleep to save CPU resources
        }
        setDirection(DIR_STOP);
        elev_set_floor_indicator(getCurrentFloor());
        System.out.println("Found floor " + getCurrentFloor());
    }

    public boolean asyncGoToFloor(int target) {
        // TODO: wait or return with error if busy?
//        while (busy) { // Don't move until the door is closed
//            try { Thread.sleep(10); } catch (InterruptedException ignored) {} // Sleep to save CPU resources
//        }
        System.out.println("asyncGoToFloor called with target: " + target);
        if (target < 1 || target > NUM_FLOORS) {
            System.out.println("Invalid floor specified (" + target + "), ignoring");
            return false; // Invalid value, do nothing
        }
        if (busy) {
            internalCommands[target - 1] = true;
            CommandDispatcher.addRequestToQueue(target, -1);
        } else {
            System.out.println("Initiating async goToFloor");
            elevWorker.goToFloor(target);
            System.out.println("asyncGoToFloor returned");
        }
        return true;
    }

    /**
     * Function to check if a matching request exists (and notify the others that we took it if applicable)
     * @param floor - target floor
     * @param direction - requested direction
     * @return whether or not the job exists (and was handled)
     */
    private boolean handleRequestIfExists(int floor, int direction) {
        if (floor >= NUM_FLOORS || floor < 1 || Math.abs(direction) > 1)
            return false;

        if ( internalCommands[floor - 1] ) {
            internalCommands[floor - 1] = false;
            return true;
        }

        // 1-indexed with the sign indicating the direction
        int targetWithDirection = (floor) * direction;
        if (CommandHandler.jobExists(targetWithDirection)) {
            CommandHandler.signalJobCompleted(targetWithDirection); // Notify the others that we intend to stop here
            return true;
        }
        System.out.println("No requests found at floor " + floor + ", direction: " + direction);
        return false;
    }

    private void stopButtonPressed() {
        // TODO: implementation - should stop the elevator (interrupt ongoing goToFloor calls), mark it as busy and set the stop button light
        System.out.println("Stop button press registered");
        elev_set_stop_lamp(1);
        stopElevator();
    }

    /**
     * @return the current floor, with 1 being the lowest and 0 invalid
     */
    public native int getCurrentFloor();

    public boolean isMoving() {
        return (direction != 0);
    }
    public int getDirection() {
        return direction;
    }

    private void setDirection(int direction) {
        this.direction = direction;
        elev_set_motor_direction(direction);
    }

    private void handleFloorCommand(int target){
        if(target >= NUM_FLOORS || target < 0)
            return ;
        int offsetTarget = target + 1;
        if (!asyncGoToFloor(offsetTarget))
            return;
        System.out.println("Received internal command to go to floor " + offsetTarget);
        elev_set_button_lamp(BUTTON_TYPE_COMMAND,target,1);
        internalCommands[target] = true;
    }

    private class AsyncWorker extends Thread {
        private int mTarget, mDirection, lastFloor;
        private boolean running = true;

        @Override
        public void run() {
            super.run();
            while (running) {
                if (mTarget != 0) { // mTarget will be set back to 0 by the goToFloorInternal func before returning
                    goToFloorInternal(mTarget);
                }
                try {
                    sleep(10);
                } catch (InterruptedException ignored) {}
            }
        }

        public void goToFloor(int target) {
            mTarget = target;
        }

        /** Actually do the work
         *
         * @param target - the target floor to stop at
         */
        private synchronized void goToFloorInternal(int target) {
            try {
                System.out.println("goToFloorInternal called with target = " + target);
                busy = true;
                lastFloor = getCurrentFloor();
                // If elevator is between floors (we don't know its current location), go down to the nearest one
                if (lastFloor == 0) {
                    findMyLocation();
                    lastFloor = getCurrentFloor();
                }

                int difference = target - lastFloor;

                if (difference == 0) return;
                else if (difference > 0) {
                    // Target is higher than the current floor
                    mDirection = DIR_UP;
                } else {
                    // Target is lower than the current floor
                    mDirection = DIR_DOWN;
                }
                setDirection(mDirection);

                boolean interrupted = false;

                outerLoop:
                while (lastFloor != target && !interrupted) {
                    while ((getCurrentFloor() == 0 || getCurrentFloor() == lastFloor)) {
                        // Wait until reaching the next floor
                        System.out.print('.');
                        System.out.print(getCurrentFloor());
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException ie) {
                            interrupted = true;
                            break outerLoop;
                        }
                    }
                    lastFloor = getCurrentFloor();
                    elev_set_floor_indicator(lastFloor);
                    System.out.println("Current floor: " + lastFloor);
                    if (lastFloor == target) break outerLoop;
                    // If request exists here, stop (and resume afterwards)
                    if (handleRequestIfExists(lastFloor, mDirection)) {
                        waitAtCurrentFloor(); // Open the door and wait before continuing
                        setDirection(mDirection);
                    }
//                    if (lastFloor >= NUM_FLOORS || lastFloor <= 1) {
//                        System.out.println("Arrived at the top/bottom floor - not possible to go any further");
//                        break; // Stop if we have reached top or bottom for some reason
//                    }
                }
                if (!interrupted) {
                    System.out.println("Arrived at target floor");
                    waitAtCurrentFloor();
                }
            } finally { // The following should ALWAYS happen upon completion
                mTarget = 0;
                busy = false;
                CommandDispatcher.recalculateJobCosts();
            }
        }

        private void waitAtCurrentFloor() {
            System.out.println("waitAtCurrentFloor called");
            setDirection(DIR_STOP);
            markFloorDone(lastFloor);
            elev_set_door_open_lamp(1);
            long waitUntil = System.currentTimeMillis() + WAIT_OPEN_DOOR;
            // Wait for people to get in and out
            while (System.currentTimeMillis() < waitUntil || doorObstructed()) {
                try {
                    sleep(100L);
                } catch (InterruptedException ignored) {}
            }
            elev_set_door_open_lamp(0);
        }

        /**
         * Update internalCommands, notify others and set turn off button lamp
         * @param floor
         */
        private void markFloorDone(int floor) {
            CommandHandler.signalJobCompleted(floor * mDirection);
            internalCommands[floor -1] = false;
            elev_set_button_lamp(BUTTON_TYPE_COMMAND, floor - 1, 0);
        }
    }

    /**
        This is a polling-based input driver to generate "interrupt events when buttons are pressed"
        Different actions should be performed, depending on which button was pressed:
         - Call buttons (outside the elevator - BUTTON_UP/DOWN_n in elev.c) should submit a command to the network
         - Command buttons (inside) can directly call the goToFloor() function with the target floor as argument
         - Stop button should stopElevator()
    */
    private class InputListener extends Thread {

        // Variables for storing the previous status of buttons
        private int [][] buttonStatus = new int[NUM_FLOORS][NUM_BUTTONS];
        private int stopButtonStatus = 0;

        @Override
        public void run() {
            super.run(); // TODO: include this or take out?
            int tempInt;

            while(true) {
                try {
                    for (int floor = 0; floor < NUM_FLOORS; floor++) {
                        for (int button = 0; button < NUM_BUTTONS; button++) {
                            tempInt = elev_get_button_signal(button, floor);
                            if (buttonStatus[floor][button] != tempInt) {
                                if (tempInt == 1) {
                                    // Button was pressed - signal the appropriate handler
                                    switch (button) {
                                        case BUTTON_TYPE_CALL_UP:
                                            if (floor == (NUM_FLOORS - 1)) {
                                                System.out.println("This is the top floor - there is no button to call up...");
                                            } else {
                                                CommandHandler.sendRequest(floor + 1);
                                                System.out.println("Request to go up from floor " + (floor + 1) + " sent");
                                            }
                                            break;
                                        case BUTTON_TYPE_CALL_DOWN:
                                            if (floor == 0) {
                                                System.out.println("This is the bottom floor - there is no button to call down...");
                                            } else {
                                                CommandHandler.sendRequest(-(floor + 1));
                                                System.out.println("Request to go down from floor " + (floor + 1) + " sent");
                                            }
                                            break;
                                        case BUTTON_TYPE_COMMAND:
                                            if (floor != (getCurrentFloor() - 1))
                                                handleFloorCommand(floor);
                                            break;
                                    }
                                }
                                buttonStatus[floor][button] = tempInt;
                            }
                        }
                    }
                    tempInt = elev_get_stop_signal();
                    if (stopButtonStatus != tempInt) {
                        if (tempInt == 1) {
                            stopButtonPressed();
                        }
                        stopButtonStatus = tempInt;
                    }

                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {}
                } catch (Exception e) { // Catch and display any (unhandled) exceptions that occurred within the polling loop to prevent it from stopping, and enable debugging
                    e.printStackTrace();
                }
            }
        }
    }

    /*
     * Native wrapper functions folllow
     */
    private native boolean hw_init(int mode);

    private native void elev_set_motor_direction(int dirn);
    public native void elev_set_button_lamp(int button, int floor, int value);
    /**
     * @param floor - 1-indexed, to match the "intuitive" understanding of a floor
     */
    private native void elev_set_floor_indicator(int floor);

    private native void elev_set_door_open_lamp(int value);
    private native void elev_set_stop_lamp(int value);
    private native int elev_get_button_signal(int button, int floor);
    private native int elev_get_stop_signal();
    public native boolean doorObstructed();
}
