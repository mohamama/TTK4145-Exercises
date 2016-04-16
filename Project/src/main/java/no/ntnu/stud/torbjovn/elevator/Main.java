package no.ntnu.stud.torbjovn.elevator;

/**
 * Created by tovine on 4/11/16.
 */
public class Main {
    private static Elevator elevator = new Elevator();
    private static CommandDispatcher dispatcherThread = new CommandDispatcher();

    public static Elevator getElevator() {
        return elevator;
    }

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    Networking.shutdown();
                    elevator.stopElevator();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        dispatcherThread.start();
        try {
            Networking.init();
        } catch (Exception e) {
            System.out.println("Exception when trying to initialize Networking module: " + e.getMessage());
            elevator.stopElevator();
            System.exit(1);
        }
    }
}
