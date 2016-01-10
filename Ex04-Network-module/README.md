Exercise 4 : From Prototype to Production
=========================================

1. Don't overengineer.
2. Always design properly.
3. Minor detail change will ruin your perfect model.
4. Always prototype first.
5. You will only know what you didn't know on the second iteration.
6. There is only budget/time for the first version.
7. The prototype will become production.


Exercise
--------

###Create a network module that you can use in the Elevator Project

Take your thoughts from [the beginning of Exercise 1](/Ex02-Intro#1-thinking-about-elevators), and reevaluate them in the light of what you have learned about network programming (and - if applicable - concurrency control).

Here are some things you may want to consider:
 - Will you use TCP, UDP, both, or something completely different?
 - In the case of a master-slave configuration: Do you have only one program, or two?
   - Is a slave becoming a master a part of the network module?
 - If you are using TCP: How do you know who connects to who?
   - Do you need an initialization phase to set up all the connections?
 - Will you be using blocking sockets & many threads, or nonblocking sockets and [`select()`](http://en.wikipedia.org/wiki/Select_%28Unix%29)?
 - Do you want to build the necessary reliability into the module, or handle that at a higher level?
 - How will you pack and unpack (serialize) data?
   - Do you use structs, classes, tuples, lists, ...?
   - JSON, XML, or just plain strings?
   - Is serialization a part of the network module?
   
By the end of this exercise, you should be able to send some data structure (struct, record, etc) from one machine to another. How you acheive this (in terms of network topology, protocol, serialization) does not matter. The key here is *abstraction*.  

This module should *simplify* the interface between machines: Creating and handling sockets in all routines that need to communicate with the outside world is possible, but unwieldy and unmaintainable. We want to encapsulate all the necessary functionality in a single module, so we have a single decoupled component where we can say "This module sends our data over the network". This will almost always be preferable, but above all else: *Think about what best suits your particular design*.
 
Pencil and paper is encouraged! Drawing a diagram/graph of the message pathways between nodes (elevators) will aid in visualizing complexity. Drawing the order of messages through time will let you more easily see what happens when communication fails.


Running from another computer
-----------------------------

You can log in to another computer and run your program remotely. Be nice the people sitting at that computer, and try to avoid using the same ports as them.

 - Logging in:
   - `ssh username@#.#.#.#` where #.#.#.# is the remote IP
 - Copying files between machines:
   - `scp source destination`, with optional flag `-r` for recursive copy (folders)
   - Examples:
     - Copying files *to* remote: `scp -r fileOrFolderAtThisMachine username@#.#.#.#:fileOrFolderAtOtherMachine`
     - Copying files *from* remote: `scp -r username@#.#.#.#:fileOrFolderAtOtherMachine fileOrFolderAtThisMachine`
    

Extracurricular
---------------

[The Night Watch](https://web.archive.org/web/20140214100538/http://research.microsoft.com/en-us/people/mickens/thenightwatch.pdf)
*"Systems people discover bugs by waking up and discovering that their first-born children are missing and "ETIMEDOUT" has been written in blood on the wall."*

[The case of the 500-mile email](http://www.ibiblio.org/harris/500milemail.html)
*"We can't send mail farther than 500 miles from here," he repeated. "A little bit more, actually. Call it 520 miles. But no farther."*

[21 Nested Callbacks](http://blog.michellebu.com/2013/03/21-nested-callbacks/)
*"I gathered from these exchanges that programmers have a perpetual competition to see who can claim the most things as 'simple.'"*