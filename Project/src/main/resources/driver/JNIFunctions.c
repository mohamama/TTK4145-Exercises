//#pragma once
#include "no_ntnu_stud_torbjovn_elevator_Elevator.h"

#include "elev.h"

/* NOTE: the floor is returned as 1-indexed (0 is invalid), to reflect the "natural" floor numbering
 * Class:       no_ntnu_stud_torbjovn_elevator_ELevator
 * Method:      getCurrentFloor
 * Signature:   ()I
 */
JNIEXPORT jint JNICALL Java_no_ntnu_stud_torbjovn_elevator_Elevator_getCurrentFloor
  (JNIEnv * env, jobject obj) {
        return elev_get_floor_sensor_signal() + 1;
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
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_no_ntnu_stud_torbjovn_elevator_Elevator_hw_1init
  (JNIEnv * env, jobject obj, jint mode) {
        return elev_init(mode);
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
 * Method:    elev_set_floor_indicator
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_no_ntnu_stud_torbjovn_elevator_Elevator_elev_1set_1floor_1indicator
  (JNIEnv * env, jobject obj, jint floor) {
        elev_set_floor_indicator(floor - 1); // Convert from 1-indexed to 0-indexed
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