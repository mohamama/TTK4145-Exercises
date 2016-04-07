//#pragma once
#include "no_ntnu_stud_torbjovn_elevator_Elevator.h"

#include "elev.h"

JNIEXPORT jint JNICALL Java_no_ntnu_stud_torbjovn_elevator_Elevator_getCurrentFloor
  (JNIEnv * env, jobject obj) {
        return elev_get_floor_sensor_signal();
  }

/*
 * Class:     no_ntnu_stud_torbjovn_elevator_Elevator
 * Method:    doorObstructed
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_no_ntnu_stud_torbjovn_elevator_Elevator_doorObstructed
  (JNIEnv * env, jobject obj) {
        return (elev_get_obstruction_signal() != 0);
  }

/*
 * Class:     no_ntnu_stud_torbjovn_elevator_Elevator
 * Method:    hw_init
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_no_ntnu_stud_torbjovn_elevator_Elevator_hw_1init
  (JNIEnv * env, jobject obj) {
        return elev_init();
  }

/*
 * Class:     no_ntnu_stud_torbjovn_elevator_Elevator
 * Method:    elev_set_motor_direction
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_no_ntnu_stud_torbjovn_elevator_Elevator_elev_1set_1motor_1direction
  (JNIEnv * env, jobject obj, jint direction) {
        elev_set_motor_direction(direction);
  }

/*
 * Class:     no_ntnu_stud_torbjovn_elevator_Elevator
 * Method:    elev_set_button_lamp
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_no_ntnu_stud_torbjovn_elevator_Elevator_elev_1set_1button_1lamp
  (JNIEnv * env, jobject obj, jint button, jint floor, jint value) {
        elev_set_button_lamp(button, floor, value);
  }

/*
 * Class:     no_ntnu_stud_torbjovn_elevator_Elevator
 * Method:    elev_set_door_open_lamp
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_no_ntnu_stud_torbjovn_elevator_Elevator_elev_1set_1door_1open_1lamp
  (JNIEnv * env, jobject obj, jint value) {
        elev_set_door_open_lamp(value);
  }

/*
 * Class:     no_ntnu_stud_torbjovn_elevator_Elevator
 * Method:    elev_set_stop_lamp
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_no_ntnu_stud_torbjovn_elevator_Elevator_elev_1set_1stop_1lamp
  (JNIEnv * env, jobject obj, jint value) {
        elev_set_stop_lamp(value);
  }

/*
 * Class:     no_ntnu_stud_torbjovn_elevator_Elevator
 * Method:    elev_get_button_signal
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_no_ntnu_stud_torbjovn_elevator_Elevator_elev_1get_1button_1signal
  (JNIEnv * env, jobject obj, jint button, jint floor) {
        return elev_get_button_signal(button, floor);
  }

/*
 * Class:     no_ntnu_stud_torbjovn_elevator_Elevator
 * Method:    elev_get_stop_signal
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_no_ntnu_stud_torbjovn_elevator_Elevator_elev_1get_1stop_1signal
  (JNIEnv * env, jobject obj) {
        return elev_get_stop_signal();
  }