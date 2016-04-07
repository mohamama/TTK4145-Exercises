sharedlib : JNIFunctions.o io.o elev.o
	gcc --shared -Wl,-soname,libelevator.so -Wl,-export-dynamic -lcomedi -lm -o libelevator.so io.o elev.o JNIFunctions.o

JNIFunctions.o : no_ntnu_stud_torbjovn_elevator_Elevator.h JNIFunctions.c elev.o
	gcc -std=c11 -I"/usr/lib/jvm/default-java/include/" -fPIC -g -c -Wall JNIFunctions.c

io.o : io.h io.c channels.h
	gcc -std=c11 -fPIC -g -c -Wall io.c

elev.o : elev.c elev.h io.c io.h channels.h 
	gcc -std=c11 -fPIC -g -c -Wall elev.c

clean :
	rm -f *.o *.so
