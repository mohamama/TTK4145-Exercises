package no.ntnu.stud.torbjovn.elevator;

import cz.adamh.utils.NativeUtils;

import java.io.IOException;

/**
 * Created by marje on 22.03.2016.
 */
public class Elevator {
    private int currentFloor = 0;
    private int direction = 0; // negative numbers =down; positive numbers = up; zero= stationary;

    static {
        try {
            NativeUtils.loadLibraryFromJar("/driver/libelevator.so");
        } catch (IOException e) {
            System.out.println("ERROR: unable to read library file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Elevator() {
        int stop = elev_get_stop_signal();
        System.out.println("Got a result - stop=" + stop);
        hw_init();
    }

    public native int getCurrentFloor();
//    {
//        return currentFloor;
//    }

    public boolean isMoving() {
        return (direction != 0);
    }
    public int getDirection() {
        return direction;
    }

    public native boolean doorObstructed();

    private native boolean hw_init();
    private native void elev_set_motor_direction(int dirn);
    private native void elev_set_button_lamp(int button, int floor, int value);
    // TODO: include this in a native function
//    private native void elev_set_floor_indicator(int floor);
    private native void elev_set_door_open_lamp(int value);
    private native void elev_set_stop_lamp(int value);

    private native int elev_get_button_signal(int button, int floor);
//    private native int elev_get_floor_sensor_signal();
    private native int elev_get_stop_signal();
//    private native int elev_get_obstruction_signal();

}
