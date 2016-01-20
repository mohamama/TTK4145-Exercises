#include <pthread.h>
#include <stdio.h>

int i = 0;

// Note the return type: void*
void* incrementerFunction(){
	for (int j = 0;j < 1000000;j++) {
		i++;
	}
	return NULL;
}

void* decrementerFunction(){
	for (int j = 0;j < 1000000;j++) {
		i--;
	}
	return NULL;
}

int main(){
    pthread_t plusThread, minusThread;
    pthread_create(&plusThread, NULL, incrementerFunction, NULL);
    pthread_create(&minusThread, NULL, decrementerFunction, NULL);
    // Arguments to a thread would be passed here ---------^
    
    pthread_join(plusThread, NULL);
    pthread_join(minusThread, NULL);
    printf("Final value of i: %d\n", i);
    return 0;
    
}
