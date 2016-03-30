package no.ntnu.stud.torbjovn.elevator;

/**
 * Created by marje on 22.03.2016.
 */
public class Elevator {
    private int currentFloor = 0;
    private int direction = 0; // negative numbers =down; positive numbers = up; zero= stationary;

    public int getCurrentFloor(){
        return currentFloor;
    }

    public int getDirection() {
        return direction;
    }
}
