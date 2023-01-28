import java.awt.desktop.SystemEventListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.PriorityQueue;


public class primes {
    static int limit = (int) Math.pow(10, 2);
    static boolean[] primesBoolean = new boolean[limit+1];//starts as all false, false values indicate primes/potential primes
    static PrimesList primesList = new PrimesList();
    static primesThread[] threadArray = new primesThread[8];//consider changing to list, maybe move this into its own class that can

    public static void main(String[] args) throws InterruptedException {
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadArray.length; i++) {
            threadArray[i] = new primesThread();
        }

        for (primesThread thread : threadArray) {
            thread.start();
        }

        for (Thread thread : threadArray) {
            thread.join();
        }
        System.out.println("TEST back in main");

        int[] topPrimes = primesList.getTopTen();

        long executionTime = System.currentTimeMillis() - startTime;

        StringBuilder stringOut = new StringBuilder();
        stringOut.append(executionTime).append(" ").append(primesList.PQ.size()).append(" ").append(primesList.sum);
        stringOut.append("\n");
        for (int topPrime : topPrimes) {
            stringOut.append(topPrime).append(" ");
        }
        File myFile = new File("primes.txt");
        try {
            if (myFile.createNewFile()) {
                FileWriter myWriter = new FileWriter("primes.txt");
                myWriter.write(stringOut.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.print(stringOut);
    }

    static class PrimesList {
        long sum;
        PriorityQueue<Integer> PQ;
        int booleanIdx;

        PrimesList() {
            sum = 0;
            PQ = new PriorityQueue<>(11, Comparator.reverseOrder());
            booleanIdx = 2;
        }

        public void addPrime (Integer primeToAdd) {
            synchronized(this) {
                this.sum += (long) primeToAdd;
                PQ.add(primeToAdd);
            }
        }
        public int getAndIncrement () {

            synchronized (this) {
                return booleanIdx++;
                /*int temp = booleanIdx;
                for (int i = booleanIdx + 1; i < primesBoolean.length; i++) {
                    //if (!primesBoolean[i]) {
                        booleanIdx = i;
                        break;
                    //}
                    //if (i + 2 >= primesBoolean.length) {//TODO might remove this
                      //  temp = primesBoolean.length + 1;
                       // break;
                    //}
                }
                System.out.println("TEST getAndIncrement booleanIdx: " + booleanIdx + " temp: " + temp);
                return temp;*/
            }
        }
        public int[] getTopTen () {//TODO consider replacing with getting a sub-array of the last 10 and removing the reverse comparable
            int[] retArray = new int[10];
            for (int i = 0; i < 10; i++) {
                if (PQ.peek() != null) {
                    retArray[9 - i] = PQ.poll();
                }
            }
            return retArray;
        }
    }

    public static class primesThread extends Thread {
            boolean activeFlag = false;
            int minValueChecked;
            int maxValueChecked;

            @Override
            public void run() {
            do {
                int value = primesList.getAndIncrement();//returns index of next unmarked boolean and moves up
                if (value >= primesBoolean.length) {
                    System.out.println("TEST " + this.getId() + " has value " + value + " over " + primesBoolean.length);
                    break;
                }
                System.out.println("TEST " + this.getId() + " has value " + value);
                int minOfMaxChecking = primesBoolean.length;
                do {// if value is not less than minimum of the max value checked in every active thread, wait
                    for (primesThread thread : threadArray) {
                        if (thread.getActive() && thread.getMax() < minOfMaxChecking) {
                            minOfMaxChecking = thread.getMax();
                        }
                    }
                } while (value >= minOfMaxChecking);
                if (primesBoolean[value]) {// if primesBoolean[value] = *false, this index has been marked as composite*, continue, get new value;
                    System.out.println("TEST continue on " + value);
                    continue;
                }

                activeFlag = true;
                minValueChecked = value;
                maxValueChecked = value;

                System.out.println("Thread " + this.getId() + " adding prime " + value);
                primesList.addPrime(value);

                while (value < primesBoolean.length) {//continue for every multiple of the prime below 10^8
                    primesBoolean[value] = true;//composite value, multiple of minValueChecked, a prime number
                    maxValueChecked = value;//update max value checked so next thread can see if its value has been checked for being composite
                    value += minValueChecked;//minValueChecked is the prime number, starting point
                }
                minValueChecked = 0;
                maxValueChecked = primesBoolean.length+1;
                activeFlag = false;
            } while (minValueChecked < limit);
            System.out.println(this.getId() + " breaks loop");
            //
        }
            public boolean getActive () {
            return activeFlag;
        }

            public int getMax () {
            return maxValueChecked;
        }
    }
}


//PrimesList class
    //sum
    //PriorityQueue with comparator so that biggest elements are at the head, maybe start with 2 already within?
    //put counter class in here, instead of incrementing I should update it to be next unmarked boolean

    /*
        Requirements
    - Find all primes between 1 and 10^8
    - Evenly spread work over 8 threads
    - Output: primes.txt file
        "<exec time> <number of primes> <sum of all primes>
        <lowest to the highest list of top ten primes>
        "
    - Submit gitHub link with source code, Read Me file
        - might need to specify where to output file as a runtime argument?
    - Use synchronized block for incrementing through list
    - 1 and 0 are not primes

        Outline
    1. shared counter that tells what value we're testing
        shared - no thread should test same thing and each should greedily grab as fast as they can
    2. Some structure that is quickly iterable and will hold the prime values (starting with 2)
        should be able to start from front or back (top ten primes)
        should be able to get length (number of primes)
    3. Shared sum counter (sum of all primes), maybe as part of the #2 structure?

    Steps
    - Start tracking execution time?
    - Start each thread
        - thread takes number from counter and increments
        - if number % any number from PrimesFound, stop and get new number
        - after running through every previous prime, add to PrimesFound and sum of primes, repeat
    - Once counter == 10^8, stop giving out new numbers (maybe exit()/end() when counter >= 10^8?)
    - wait() until every thread returns
    - calc execution time?
    - create file and write results to it in format (maybe using buffered string?)

    Other solutions to look at
        Sieve of Eratosthenes
        Sieve of Sundaram
        Sieve of Atkin
            These above three all involve going through a boolean array of numbers
            and flipping between composite or prime
            If I implement one of them, I would instead have to do sum math, etc.
            with the indexes of each
    */
//