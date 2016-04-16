package no.ntnu.stud.torbjovn.elevator;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Timeout event dispatcher
 * Created by tovine on 4/14/16.
 * Concept and parts of implementation borrowed from a class I wrote for my employer, LuxSave AS. No confidential details are included
 */
public class CommandDispatcher extends Thread {
    private static final Object waitLock = new Object();
    private static long sleepTimeout, nextWakeup;
    private static Map<Integer, Long> activeJobs = new HashMap<>(Elevator.NUM_FLOORS * 2);

    private Elevator thisElevator = Main.getElevator();

    public static void addRequestToQueue(int target, long delay) {
        System.out.println("New request received, target: " + target + ", delay: " + delay);
        // Start timer if not running, update remaining time if the new request wants an earlier action
        // Step 2: Update the timer
        activeJobs.put(target, delay);
        System.out.println("Request successfully added to queue, waking dispatcher thread to schedule next wakeup");
        synchronized (waitLock) {
            // Wake up the sleeping thread to adjust the sleeping period
            waitLock.notify();
        }
    }

    public static void cancelRequest(int target) {
        activeJobs.remove(target);
    }

    /**
     * Process the job that is currently first in line, and calculates the delay for the next
     */
    private void processNextJob() {
        Set<Map.Entry<Integer, Long>> jobSet = activeJobs.entrySet();
        Map.Entry<Integer, Long> earliestJob = null, nextJob = null;
        // Sort the pending jobs by delay period (cost) and return the two earliest (if existent)
        for (Map.Entry<Integer, Long> job : jobSet) {
            if (job.getValue() == null) {
                System.out.println("WARN: a job in the activeJobs set has a null value - this should never happen. Key: " + job.getKey());
                continue;
            }
            if (earliestJob == null || earliestJob.getValue() > job.getValue()) {
                if (earliestJob != null) { // Bump the job that was earliest before down to 2nd place
                    nextJob = earliestJob;
                }
                earliestJob = job;
            }
        }

        if (earliestJob != null && System.currentTimeMillis() >= nextWakeup) {
            dispatchJob(earliestJob.getKey());
        } else // We woke up too soon - go back to sleep
            nextJob = earliestJob;

        // Step 2: calculate the next wait interval, verify that it's > 0, update sleepTimeout and return to main loop
        if (nextJob == null) {
            sleepTimeout = 0; // If no more jobs are pending, go to sleep until a new task is added
            nextWakeup = 0;
        } else {
            sleepTimeout = nextJob.getValue();
            nextWakeup = System.currentTimeMillis() + sleepTimeout;
            System.out.println("Next wakeup scheduled in " + sleepTimeout + "ms");
            if (sleepTimeout < 1) sleepTimeout = 1; // Safeguard against negative durations and accidentally waiting for a new notification when there are more left to process
        }
    }

    private void dispatchJob(int target) {
        CommandHandler.signalTakeJob(target);
        thisElevator.asyncGoToFloor(Math.abs(target));
        activeJobs.remove(target);
    }

    public void run() {
        System.out.println("CommandDispatcher thread started");
        while(true) {
            processNextJob();
            synchronized (waitLock) {
                try {
                    System.out.println("Waiting for new notifications to process");
                    waitLock.wait(sleepTimeout); // Initially (and when jobSchedule is empty) sleepTimeout is 0, so it will wait until notified
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
//        System.out.println("NotificationDispatcher thread shut down");
    }
}
