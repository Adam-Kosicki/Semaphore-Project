import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.LinkedList;


public class dmv {

    //How many customers to simulate
    public static final int NUM_CUSTOMERS = 20;

    //Semaphores
    static Semaphore infoDeskAvailable = new Semaphore(0, true);
    static Semaphore customerEntersWaitroom = new Semaphore(0, true);
    static Semaphore customerEntersAgentline = new Semaphore(4, true);
    static Semaphore finishedInfoDesk = new Semaphore(0,true);
    static Semaphore[] numberCalled = new Semaphore[NUM_CUSTOMERS];
    static Semaphore walkingToAgentLine = new Semaphore(0,true);
    static Semaphore agentAvailable = new Semaphore(0,true);
    static Semaphore agentAsks = new Semaphore(0,true);
    static Semaphore customerFinished = new Semaphore(0,true);
    static Semaphore giveCustomerLicense = new Semaphore(0,true);
    static Semaphore callingCustomerFromAgentLine = new Semaphore(1,true);

    //Global variables used among the threads
    static int customerRequestID = 0;
    static int customerRequestNumber = 0;
    static int agentRequestID = 0;
    static Queue<Customer> waitRoom = new LinkedList<Customer>();
    static Queue<Customer> agentLine = new LinkedList<Customer>();



    public static void main(String[] args) {

        //Initialize all threads
        Thread infoDesk = new Thread(new InfoDesk());
        Thread announcer = new Thread(new Announcer());
        Thread agent0 = new Thread(new Agent(0));
        Thread agent1 = new Thread(new Agent(1));

        Thread customers[] = new Thread[NUM_CUSTOMERS];
        for (int i = 0; i < NUM_CUSTOMERS; i++)
            customers[i] = new Thread(new Customer(i));

        for (int i = 0; i < NUM_CUSTOMERS; i++)
            numberCalled[i] = new Semaphore(0, true);

        //Start all threads
        infoDesk.start();
        announcer.start();
        agent0.start();
        agent1.start();
        for (int i = 0; i < NUM_CUSTOMERS; i++)
            customers[i].start();

        // Wait for all customers to join
        for(int i=0; i < NUM_CUSTOMERS; i++){
            try{
                customers[i].join();
                System.out.println("Joined customer " + i);
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
        System.out.println("Done");
        System.exit(0);

    }

    public static class InfoDesk implements Runnable {
        int number = 1;
        @Override
        public void run() {
            try {
                System.out.println("Info desk created");
                while (true) {

                    //Accept next customer
                    infoDeskAvailable.release();

                    //End transaction, give customer number
                    customerRequestNumber = number;
                    number++;
                    finishedInfoDesk.acquire();

                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

        public static class Customer implements Runnable {
            // ID of the customer
            int id;
            int num;
            int agID;

            public Customer(int id) {
                this.id = id;
            }

            @Override
            public void run() {
                try {

                    System.out.println("Customer " + id + " created, enters DMV");

                    //Wait for Information Desk Worker to be available
                    infoDeskAvailable.acquire();

                    //Customer gets assigned number
                    num = customerRequestNumber;
                    //System.out.println("Customer " + id + " has number " + num);
                    System.out.println("Customer " + id + " gets number " + num + ", enters waiting room");

                    //Customer adding themselves to wait room
                    waitRoom.add(this);
                    finishedInfoDesk.release();
                    customerEntersWaitroom.release();

                    //customer waits for number to be called
                    numberCalled[num-1].acquire();
                    agentLine.add(this);
                    customerEntersAgentline.acquire();

                    //Agent line
                    System.out.println("Customer " + id + " joins agent line");
                    walkingToAgentLine.release();
                    customerEntersAgentline.release();

                    //Enters agent line
                    customerRequestID = id;
                    agentAvailable.release();

                    //Agent asks customer for photo and eye exam
                    agentAsks.acquire();
                    agID = agentRequestID;

                    //Customer is done
                    customerFinished.release();
                    giveCustomerLicense.acquire();
                    System.out.println("Customer " + id + " gets license and departs");

                } catch (InterruptedException e){
                    e.printStackTrace();
                }
            }
        }

    public static class Announcer implements Runnable {
        private int currentNum;
        @Override
        public void run() {
            currentNum = 1;
            try {
                System.out.println("Announcer created");
                while (true) {

                    //Announcer makes sure agent line is 4 max
                    if (agentLine.size() < 4) {
                        customerEntersWaitroom.acquire();
                        System.out.println("Announcer calls " + currentNum);
                        numberCalled[currentNum-1].release();
                        currentNum++;
                        walkingToAgentLine.acquire();
                    }


                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public static class Agent implements Runnable{
        //ID of the Agent
        int id;
        //ID of customer being served
        int custID;

        public Agent(int id) {
            this.id = id;
        }

        @Override
        public void run(){
            try{
                System.out.println("Agent " + id + " created");
                while (true){

                    //Agent serves customer
                    agentAvailable.acquire();
                    custID = customerRequestID;
                    callingCustomerFromAgentLine.acquire();
                    Customer c = agentLine.remove();
                    callingCustomerFromAgentLine.release();

                    System.out.println("Agent " + id + " is serving customer " + c.id);

                    //Agent asks customer to take photo and eye exam
                    agentAsks.release();
                    System.out.println("Agent " + id + " asks customer " + c.id + " to take photo and eye exam");
                    agentRequestID = id;

                    System.out.println("Customer " + c.id + " completes photo and eye exam for agent " + id);
                    //Customer is finished
                    customerFinished.acquire();
                    giveCustomerLicense.release();

                }
            }catch (InterruptedException e){
                e.printStackTrace();
            }
        }
    }

}

