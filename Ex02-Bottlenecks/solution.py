import threading
i = 0
lock= threading.Lock()

def plusThread():
    global i
    for _ in range(1000000):
        lock.acquire()
	i+=1
	lock.release()

def minusThread():
    global i
    for _ in range(1000000):
	lock.acquire()
        i-=1
	lock.release()

# Potentially useful thing:
#   In Python you "import" a global variable, instead of "export"ing it when you declare it
#   (This is probably an effort to make you feel bad about typing the word "global")


def main():
    global i
    threadP = threading.Thread(target = plusThread, args = (),)
    threadM = threading.Thread(target = minusThread, args = (),)
    threadP.start()
    threadM.start()
    threadP.join()
    threadM.join()

    print("Value of i after execution: ", i)


main()
