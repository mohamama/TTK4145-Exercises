sharedlib : io.o elev.o
	gcc --shared -Wl,-soname,elevator.so -lcomedi -lm -o elevator.so io.o elev.o 

io.o : io.h io.c channels.h
	gcc -std=c11 -fPIC -g -c -Wall io.c

elev.o : elev.c elev.h io.c io.h channels.h no_ntnu_stud_torbjovn_elevator_Elevator.h 
	gcc -std=c11 -I"/usr/lib/jvm/default-java/include/" -fPIC -g -c -Wall elev.c

clean :
	rm -f *.o *.so
