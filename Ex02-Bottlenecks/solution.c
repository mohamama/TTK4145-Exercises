#include <pthread.h>
#include <stdio.h>

int i = 0;
pthread_mutex_t lock;

// Note the return type: void*
void* incrementerFunction(){
	for (int j = 0;j < 1000000;j++) {
		pthread_mutex_lock(&lock);
		i++;
		pthread_mutex_unlock(&lock);
	}
	return NULL;
}

void* decrementerFunction(){
	for (int j = 0;j < 1000000;j++) {
		pthread_mutex_lock(&lock);
		i--;
		pthread_mutex_unlock(&lock);
	}
	return NULL;
}

int main(){
    if(pthread_mutex_init(&lock, NULL) !=0){
	    printf("\n mutex init failed \n");
	    return 1;
    }
    pthread_t plusThread, minusThread;
    pthread_create(&plusThread, NULL, incrementerFunction, NULL);
    pthread_create(&minusThread, NULL, decrementerFunction, NULL);
    // Arguments to a thread would be passed here ---------^
    
    pthread_join(plusThread, NULL);
    pthread_join(minusThread, NULL);
    printf("Final value of i: %d\n", i);
    
    pthread_mutex_destroy(&lock);
    return 0;
    
}
