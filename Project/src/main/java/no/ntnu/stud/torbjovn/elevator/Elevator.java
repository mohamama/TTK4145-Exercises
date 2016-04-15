package no.ntnu.stud.torbjovn.elevator;

import cz.adamh.utils.NativeUtils;

import java.io.IOException;

/**
 * Created by marje on 22.03.2016.
 */
public class Elevator {
    private InputListener inputListenerThread = new InputListener();
    // TODO: do something to persist the current status in case of restart?
    public static final int NUM_FLOORS = 4,
            DIR_UP = 1,
            DIR_DOWN = -1,
            DIR_STOP = 0,
            NUM_BUTTONS = 3,
            BUTTON_TYPE_CALL_UP = 0,
            BUTTON_TYPE_CALL_DOWN = 1,
            BUTTON_TYPE_COMMAND = 2;

    // A list of commands added by button presses from within the elevator cabin - these should have priority OVER commands received from network
    private static boolean[] internalCommands = new boolean[NUM_FLOORS];

    private static final int ET_Comedi = 0,
            ET_Simulation = 1;

    private int direction = 0;
    private boolean doorOpen, // TODO: implement handling of this - should be true when stopping at a floor (for a given amount of time), and when doorObstructed is true
                    busy;
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
            // TODO: enter "simulation mode" (for testing network handling etc.) if initialization failed
        }
        System.out.println("HW initialization done");
        int currFloor = getCurrentFloor();
        if (currFloor == 0) {
            System.out.println("Elevator is currently between floors, trying to find out where");
            findMyLocation();
        } else
            System.out.println("Elevator is currently at floor " + currFloor);

        // Start listening for button events
        inputListenerThread.start();
    }

    public void stopElevator() {
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

    // TODO: this function should be made asynchronous (by using a background thread)
    public void goToFloor(int target) {
        // TODO: waiting or return with error if busy?
        while (doorOpen || busy) { // Don't move until the door is closed
            try { Thread.sleep(10); } catch (InterruptedException ignored) {} // Sleep to save CPU resources
        }
        busy = true;
        if (target < 1 || target > NUM_FLOORS) {
            System.out.println("Invalid floor specified (" + target + "), ignoring");
            return; // Invalid value, do nothing
        }
        // TODO: background thread code should start here
        int lastFloor = getCurrentFloor();
        // If elevator is between floors (we don't know its current location), go down to the nearest one
        if (lastFloor == 0) {
            findMyLocation();
            lastFloor = getCurrentFloor();
        }

        int difference = target - lastFloor;

        if (difference == 0) return;
        else if (difference > 0) {
            // Target is higher than the current floor
            direction = DIR_UP;
        } else {
            // Target is lower than the current floor
            direction = DIR_DOWN;
        }
        setDirection(direction);

        outerLoop:
        while(lastFloor != target) {
            while (getCurrentFloor() == 0 || getCurrentFloor() == lastFloor) {
                // Wait until reaching the next floor
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {}
            }
            lastFloor = getCurrentFloor();
            elev_set_floor_indicator(lastFloor);
            System.out.println("Current floor: " + lastFloor);
            if (lastFloor >= NUM_FLOORS || lastFloor <= 1) break; // Stop if we have reached top or bottom for some reason
            // If request exists here, stop (and resume afterwards)
            if (handleRequestIfExists(lastFloor, direction)) {
                // TODO: stop, and add another request to the original target (with the same priority as an internal command)
                break outerLoop;
            }
        }
        setDirection(DIR_STOP);
        System.out.println("Arrived at target floor");
        internalCommands[lastFloor] = false;
        busy = false;
    }

    /**
     * Function to check if a matching request exists (and notify the others that we took it if applicable)
     * @param floor - target floor
     * @param direction - requested direction
     * @return whether or not the job exists (and was handled)
     */
    private boolean handleRequestIfExists(int floor, int direction) {
        if (floor >= NUM_FLOORS || floor < 0 || Math.abs(direction) > 1)
            return false;

        if ( internalCommands[floor])
            return true;

        // 1-indexed with the sign indicating the direction
        int targetWithDirection = (floor + 1) * direction;
        if (CommandHandler.jobExists(targetWithDirection)) {
            CommandHandler.jobCompleted(targetWithDirection); // Notify the others that we intend to stop here
            return true;
        }

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
        // TODO: implement asyncGoToFloor
        if (!asyncGoToFloor(target))
            return;
        elev_set_button_lamp(BUTTON_TYPE_COMMAND,target,1);
        internalCommands[target] = true;
    }

    private class InputListener extends Thread {
        /*
            This is a polling-based input driver to generate "interrupt events when buttons are pressed"
            Different actions should be performed, depending on which button was pressed:
             - Call buttons (outside the elevator - BUTTON_UP/DOWN_n in elev.c) should submit a command to the network
             - Command buttons (inside) can directly call the goToFloor() function with the target floor as argument
             - Stop button should stopElevator()
        */

        // Variables for storing the previous status of buttons
        private int [][] buttonStatus = new int[NUM_FLOORS][NUM_BUTTONS];
        private int stopButtonStatus = 0;
        private boolean obstruction_LastValue;

        @Override
        public void run() {
            super.run(); // TODO: include this or take out?
            int tempInt;
            boolean tempBool;

            while(true) {
                for (int floor = 0; floor < NUM_FLOORS; floor++) {
                    for (int button = 0; button < NUM_BUTTONS; button++) {
                        tempInt = elev_get_button_signal(button, floor);
                        if (buttonStatus[floor][button] != tempInt) {
                            if (tempInt == 1) {
                                // Button was pressed - signal the appropriate handler - TODO
                                switch (button) {
                                    case BUTTON_TYPE_CALL_UP:
                                        if (floor == (NUM_FLOORS -1)) {
                                            System.out.println("This is the top floor - there is no button to call up...");
                                        } else {
                                            CommandHandler.sendRequest(floor + 1);
                                            System.out.println("Request to go up from floor " + (floor+ 1) + " sent");
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

                // TODO: does this function really need a callback, or should it be checked on demand?
                tempBool = doorObstructed();
                if (obstruction_LastValue != tempBool) {
                    // TODO: callback?
                    obstruction_LastValue = doorObstructed();
                }

                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {}
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
