from threading import Thread

i = 0

def plusThread():
    global i
    for _ in range(1000000):
        i+=1

def minusThread():
    global i
    for _ in range(1000000):
        i-=1

# Potentially useful thing:
#   In Python you "import" a global variable, instead of "export"ing it when you declare it
#   (This is probably an effort to make you feel bad about typing the word "global")


def main():
    global i
    threadP = Thread(target = plusThread, args = (),)
    threadM = Thread(target = minusThread, args = (),)
    threadP.start()
    threadM.start()
    threadP.join()
    threadM.join()

    print("Value of i after execution: ", i)


main()
