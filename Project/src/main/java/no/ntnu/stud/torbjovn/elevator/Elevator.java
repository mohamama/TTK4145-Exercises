package no.ntnu.stud.torbjovn.elevator;

import cz.adamh.utils.NativeUtils;

import java.io.IOException;

/**
 * Created by marje on 22.03.2016.
 */
public class Elevator {
    // TODO: do something to persist the current status in case of restart
    public static final int NUM_FLOORS = 4;
    public static final int DIR_UP = 1,
                            DIR_DOWN = -1,
                            DIR_STOP = 0;
    private int direction = 0;
    private boolean doorOpen; // TODO: implement handling of this - should be true when stopping at a floor (for a given amount of time), and when doorObstructed is true
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
        if (!hw_init()) {
            System.out.println("Failed to initialize elevator hardware!");
            // TODO: enter "simulation mode" (for testing network handling etc.) if initialization failed
            Runtime.getRuntime().exit(1);
        }
        System.out.println("HW initialization done");
        int currFloor = getCurrentFloor();
        if (currFloor == 0) {
            System.out.println("Elevator is currently between floors, trying to find out where");
            findMyLocation();
        } else
            System.out.println("Elevator is currently at floor " + currFloor);
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

    public void goToFloor(int target) {
        if (target < 1 || target > NUM_FLOORS) {
            System.out.println("Invalid floor specified (" + target + "), ignoring");
            return; // Invalid value, do nothing
        }
        int lastFloor = getCurrentFloor();
        // If elevator is between floors (we don't know its current location), go down to the nearest one
        if (lastFloor == 0) {
            findMyLocation();
            lastFloor = getCurrentFloor();
        }

        while (doorOpen) { // Don't move until the door is closed
            try { Thread.sleep(10); } catch (InterruptedException ignored) {} // Sleep to save CPU resources
        }
        
        int difference = target - lastFloor;

        if (difference == 0) return;
        else if (difference > 0) {
            // Target is higher than the current floor
            setDirection(DIR_UP);
        } else {
            // Target is lower than the current floor
            setDirection(DIR_DOWN);
        }
        // TODO: make this asynchronous?
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
        }
        setDirection(DIR_STOP);
        System.out.println("Arrived at target floor");
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

    private native boolean hw_init();

    private native void elev_set_motor_direction(int dirn);
    private native void elev_set_button_lamp(int button, int floor, int value);
    /**
     * @param floor - 1-indexed, to match the "intuitive" understanding of a floor
     */
    private native void elev_set_floor_indicator(int floor);

    private native void elev_set_door_open_lamp(int value);
    private native void elev_set_stop_lamp(int value);

    private class InputListener extends Thread {
        // TODO: implement a driver for this (abstract it away and make asynchronous events for button presses)
        public native boolean doorObstructed();
        public static final int NUM_BUTTONS = 3;
        @Override
        public void run() {
            super.run();
            while(true) {
                /*
                 TODO: Iterate through all buttons on all floors and check the state
                 If a button is pressed (and it wasn't before), fire an event. For now, just mark it with a //TODO
                 or something - implementation will be done later.

                 Different actions should be performed, depending on which button was pressed:
                     - Call buttons (outside the elevator - BUTTON_UP/DOWN_n in elev.c) should submit a command to the network
                     - Command buttons (inside) can directly call the goToFloor() function with the target floor as argument
                     - Stop button should stopElevator()

                 You need to keep track of the last state of all buttons (you only want to fire the event handler once
                 for each time the button is pressed)
                  */
            }
        }

        private native int elev_get_button_signal(int button, int floor);
        private native int elev_get_stop_signal();
    }
}
