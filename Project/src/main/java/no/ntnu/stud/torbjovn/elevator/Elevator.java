package no.ntnu.stud.torbjovn.elevator;

/**
 * Created by marje on 22.03.2016.
 */
public class Elevator {
    private int currentFloor = 0;
    private int direction = 0; // negative numbers =down; positive numbers = up; zero= stationary;

    static {
        System.loadLibrary("elev");
    }

    public int getCurrentFloor(){
        return currentFloor;
    }

    public int getDirection() {
        return direction;
    }

    private native void elev_init();
    private native void elev_set_motor_direction(int dirn);
    private native void elev_set_button_lamp(int button, int floor, int value);
    private native void elev_set_floor_indicator(int floor);
    private native void elev_set_door_open_lamp(int value);
    private native void elev_set_stop_lamp(int value);

    private native int elev_get_button_signal(int button, int floor);
    private native int elev_get_floor_sensor_signal();
    private native int elev_get_stop_signal();
    private native int elev_get_obstruction_signal();

}
