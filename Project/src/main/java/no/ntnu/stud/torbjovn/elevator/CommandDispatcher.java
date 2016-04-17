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
    private static Map.Entry<Integer, Long> currentJob = null;

    public static boolean jobExists(int target) {
        return activeJobs.containsKey(target);
    }

    public static Map<Integer, Long> getActiveJobs() {
        return activeJobs;
    }

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
     * Process the job that is currently first in line if it's time
     */
    private synchronized void processNextJob() {
        if (currentJob != null && System.currentTimeMillis() >= nextWakeup && !thisElevator.isBusy()) {
            System.out.println("earliestJob - target: " + currentJob.getKey() + ", value: " + currentJob.getValue());
            dispatchJob(currentJob.getKey());
        }
    }

    /**
     * Iterate over the pending jobs and get the one with the lowest delay in the queue
     * @return a key-value pair with the key being the target floor and the value being the calculated timeout
     *        -> If no jobs with valid timeout exist, return null
     */
    private Map.Entry<Integer, Long> getFirstJob() {
        Set<Map.Entry<Integer, Long>> jobSet = activeJobs.entrySet();
        Map.Entry<Integer, Long> earliestJob = null;
        // Sort the pending jobs by delay period (cost) and return the earliest (if existent)
        for (Map.Entry<Integer, Long> job : jobSet) {
            if (job.getValue() == null) {
                System.out.println("WARN: a job in the activeJobs set has a null value - this should never happen. Key: " + job.getKey());
                continue;
            }
            System.out.println("Job - target: " + job.getKey() + ", delay/cost: " + job.getValue());
            if (earliestJob == null || earliestJob.getValue() > job.getValue()) {
                earliestJob = job;
            }
        }
        return earliestJob;
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
            currentJob = getFirstJob();
            if (currentJob != null)
                sleepTimeout = currentJob.getValue();
            else
                sleepTimeout = 0;
            // Extra safeguard against negative timeouts
            if (sleepTimeout < 0)
                sleepTimeout = 1;
            nextWakeup = System.currentTimeMillis() + sleepTimeout;
            synchronized (waitLock) {
                try {
                    System.out.println("Waiting for new notifications to process");
                    waitLock.wait(sleepTimeout); // Initially (and when jobSchedule is empty) sleepTimeout is 0, so it will wait until notified
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                if (currentJob.getKey() == getFirstJob().getKey())
                    processNextJob();
            } catch (NullPointerException npe) {
                System.out.println("The scheduled job is no longer available");
            }
        }
    }
}
