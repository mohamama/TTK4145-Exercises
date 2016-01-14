Exercise 1 : Hello World
========================

This exercise does not require that you use the machines at the real-time lab. However, the C code uses POSIX, so you'll need a POSIX-compliant OS, like linux or OSX.

[Go](http://golang.org) has an [interactive "Tour"](http://tour.golang.org/list) you can take. Go's syntax is a bit different, so it may be worth skimming through, or at least using as a quick reference.



1: Thinking about elevators
---------------------------

Not for handing in, just for thinking about. Talk to other groups, assistants, or even people who have taken the course in previous years.

Brainstorm some techniques you could use to prevent Sverre from being hopelessly stranded, waiting for an elevator that will never arrive. Think about the  [worst-case](http://xkcd.com/748/) behaviour of the system.
 - What if the software controlling one of the elvators suddeny crashes?
 - What if it doesn't crash, but hangs?
 - What if a message between machines is lost?
 - What if the network cable is suddenly disconnected? Then re-connected?
 - What if a user of the system is being a troll?
 - What if the elevator car never arrives at its destination?

 
2: Set up source control and build tools
----------------------------------------

A version control system is a tool helps a group of people work on the same files in a systematic and safe manner, allowing multiple users to make changes to the same file and merge the changes later. It is also possible to create diverging branches so that multiple independent areas of development can happen in parallel, then have these merged together safely at a later time. Version control systems also keep track of all previous versions of files, so that you can revert any or all changes made since a given date.

Using a version control system is not mandatory, but is inarguably better than hand-made "versioned" folders, and arguably better than cloud syncing software like Dropbox, Google Drive or others. Cloud syncing software is a great tool, but (usually) don't allow people to work on the same files at the same time without overwriting eachothers work (with VCS you copy your own version, make changes, and merge your version back with an automated tool), and it (again, usually) doesn't keep a full version history that lets you see what the entire folder looked like at a particular time in the past (ever tried getting a previous version of a *folder* in Google Drive? You simply can't).  
One solution is to have 

You will need some place to store your files, either a decentralized repository online (eg [GitHub](https://github.com/) or [Bitbucket](https://bitbucket.org/) (which also has free private repos)) or centralized on a common group directory at an external server (do not use the machines on the lab for storage, because you never know who might delete it). You can apply for a group directory [here](http://www.stud.ntnu.no/kundesenter/).

 - You can find more info on using the version control console commands here: [Git](http://git-scm.com/), [SVN](http://svnbook.org/), [Mercurial](http://mercurial.selenic.com/)
 - If you prefer graphical interfaces, there are several available. Git has a [list of GUI clients](https://git-scm.com/downloads/guis) on their website, Subversion clients even have their own [Wikipedia page](https://en.wikipedia.org/wiki/Comparison_of_Subversion_clients). [UnGit](https://github.com/FredrikNoren/ungit) is a simple visual interface for git that runs in your browser. [The video](http://youtu.be/hkBVAi3oKvo) is worth looking at even if you don't plan to use UnGit, since it's a nice explanation of how version control works.
 - If you plan to learn Git from the command line, you can try [this guided tour](https://try.github.io/), and maybe the [github repository that's actually a game](https://github.com/git-game/git-game).

You will probably also want to set up your preferred workflow for coding. Browse through the available text editors and IDEs, and find something that is comfortable to work with. If there is a tool you need that isn't there, you should have the necessary user priveleges to install it yourself.
When you start working on the project, you may also want to set up a build system, either with a makefile or just a shell script. Since the need for this is both language-dependent and IDE-dependent, we won't go into any details here. Just remember to *not upload login details or other sensitive information to repositories on the web, even private ones!*


3: Reasons for concurrency and parallelism
------------------------------------------

(Remember to use all the resources at your disposal. Asking the internet isn't a form of "cheating", it's a way of learning.)

 - What is concurrency? What is parallelism? What's the difference?
 - Why have machines become increasingly multicore in the past decade?
 - What kinds of problems motivates the need for concurrent execution? (Or phrased differently: What problems do concurrency help in solving?)
 - Does creating concurrent programs make the programmer's life easier? Harder? Maybe both? (Come back to this after you have worked on part 4 of this exercise)

<!-- -->
 - What are the differences between processes, threads, green threads, and coroutines?
 - Which one of these do `pthread_create()` (C/POSIX), `threading.Thread()` (Python), `go` (Go) create?
 - How does pythons Global Interpreter Lock (GIL) influence the way a python Thread behaves?
 - With this in mind: What is the workaround for the GIL (Hint: it's another module)?
 - What does `func GOMAXPROCS(n int) int` change? 



4: Finally some code
--------------------

Implement this in C, Python and Go:  
(Look at the "helloworld" examples in this directory (top of this page) for how to create threads.)


    global shared int i = 0

    main:
        spawn thread_1
        spawn thread_2
        join all threads (wait for them to finish)
        print i

    thread_1:
        do 1_000_000 times:
            i++
    thread_2:
        do 1_000_000 times:
            i--
            
Obviously, `0 + 1_000_000 - 1_000_000 == 0`.  
What happens, and why?


5: One I Prepared Earlier
-------------------------

You will fix the above problem in Exercise 2, so you should save your code for later. If you have started using version control, maybe use this opportunity and start putting it to use.


