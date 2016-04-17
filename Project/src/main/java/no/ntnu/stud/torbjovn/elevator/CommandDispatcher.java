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
    private static final long SLEEP_TIMEOUT = 1000;
    private static Map<Integer, Long> activeJobs = new HashMap<>(Elevator.NUM_FLOORS * 2);

    private static Elevator thisElevator = Main.getElevator();

    public static void addRequestToQueue(int target, long delay) {
        System.out.println("New request received, target: " + target + ", delay: " + delay);
        // Start timer if not running, update remaining time if the new request wants an earlier action
        // Step 2: Update the timer
        activeJobs.put(target, delay);
        System.out.println("Request successfully added to queue, waking dispatcher thread to schedule next wakeup");
        synchronized (waitLock) {
            // Wake up the sleeping thread to adjust the sleeping period
            waitLock.notifyAll();
        }
    }

    // TODO: is this not working?
    public static void cancelRequest(int target) {
        activeJobs.remove(target);
        synchronized (waitLock) {
            waitLock.notifyAll();
        }
    }

    public static void recalculateJobCosts() {
        int currentFloor = thisElevator.getCurrentFloor();
        if (currentFloor == 0) // Invalid floor
            return;
        for (Map.Entry<Integer, Long> job : activeJobs.entrySet()) {
            // Job cost == -1 means that this is a cabin job, that should be processed immediately
            if (job.getValue() == -1 || job.getValue() == CommandHandler.JOB_TIMEOUT) continue;
            job.setValue(recalculateCost(job.getKey(), currentFloor));
        }
        synchronized (waitLock) {
            // Wake up the sleeping thread to adjust the sleeping period
            waitLock.notifyAll();
        }
    }

    /**
     * Function to recalculate cost based on the new position
     * @param target
     * @param currentPos
     * @return - the new cost for the job
     */
    public static long recalculateCost(int target, int currentPos) {
        long cost = Math.abs(Math.abs(target) - currentPos) * CommandHandler.COST_EACH_FLOOR;
        // TODO: add more cost factors if needed
        return cost * CommandHandler.MILLIS_PER_COST;
    }

    /**
     * Process the job that is currently first in line, and calculates the delay for the next
     */
    private synchronized void processNextJob() {
//        System.out.println("processNextJob called");
        Set<Map.Entry<Integer, Long>> jobSet = activeJobs.entrySet();
        Map.Entry<Integer, Long> earliestJob = null, nextJob = null;
        // Sort the pending jobs by delay period (cost) and return the two earliest (if existent)
        for (Map.Entry<Integer, Long> job : jobSet) {
            if (job.getValue() == null) {
                System.out.println("WARN: a job in the activeJobs set has a null value - this should never happen. Key: " + job.getKey());
                continue;
            }
            System.out.println("Job - target: " + job.getKey() + ", delay/cost: " + job.getValue());
            if (earliestJob == null || earliestJob.getValue() > job.getValue()) {
                if (earliestJob != null) { // Bump the job that was earliest before down to 2nd place
                    nextJob = earliestJob;
                }
                earliestJob = job;
            }
        }

        if (earliestJob != null && System.currentTimeMillis() >= nextWakeup && !thisElevator.isBusy()) {
            System.out.println("earliestJob - target: " + earliestJob.getKey() + ", value: " + earliestJob.getValue());
            dispatchJob(earliestJob.getKey());
        } else // We woke up too soon - go back to sleep
            nextJob = earliestJob;

        // Step 2: calculate the next wait interval, verify that it's > 0, update sleepTimeout and return to main loop
        if (nextJob == null) {
//            sleepTimeout = 0; // If no more jobs are pending, go to sleep until a new task is added
            sleepTimeout = SLEEP_TIMEOUT;
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
            while(thisElevator.isBusy()) {
                try {
                    sleep(10);
                } catch (InterruptedException ignored) {}
            }
            processNextJob();
            synchronized (waitLock) {
                try {
//                    System.out.println("Waiting for new notifications to process");
                    waitLock.wait(sleepTimeout); // Initially (and when jobSchedule is empty) sleepTimeout is 0, so it will wait until notified
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
//        System.out.println("NotificationDispatcher thread shut down");
    }
}
