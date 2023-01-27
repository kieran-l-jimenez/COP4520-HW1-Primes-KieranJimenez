import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.PriorityQueue;


public class primes {
    static int limit = (int) Math.pow(10, 8);
    static boolean[] primesBoolean = new boolean[limit+1];//starts as all false, false values indicate primes/potential primes
    static PrimesList primesList = new PrimesList();
    static primesThread[] threadArray = new primesThread[8];//consider changing to list, maybe move this into its own class that can

    public static void main(String[] args) throws InterruptedException {
            //hold each threads max value checked and active flags if I can't get threads to read each other correctly
        long startTime = System.currentTimeMillis();
        //loop that creates *creates* each thread
        //OPTION B : new Runnable () {
        //each thread has an active flag and a min and max value checked variable and functions to return those values
        // TODO change it so it doesn't check for multiples of invalid numbers, ie composite ones
        // value = getAndIncrement counter (next unmarked boolean index)
        // if value is not less than minimum of the max value checked every active thread, yield
        // if primesBoolean[value] = *false, this index has been marked as composite*, continue, get new value;
        // update three thread variables
        // add value to PQ and sum
        // while value < primesBoolean.length
        // value+= minValueChecked (note, this is the starting point)
        // primesBoolean[value] = false
        // update max value checked
        // loop back to getAndIncrement counter
        // NOTE does this approach mean I'll have to pass through the entire array again
        // }

        for (int i = 0; i < threadArray.length; i++) {
            threadArray[i] = new primesThread();
        }

        //loop that *starts* each thread
        for (primesThread thread : threadArray) {
            thread.start();
        }
        //loop that *joins* each thread from main thread, won't continue until all threads done
        for (Thread thread : threadArray) {
            thread.join();
        }
        // loop through priority queue, grab top ten values
        int[] topPrimes = primesList.getTopTen();
        // sum should have been updated each time a prime was added to the PQ
        //subtraction to find execution time
        long executionTime = System.currentTimeMillis() - startTime;
        // create file and write results
        //"<exec time> <number of primes> <sum of all primes>
        // <lowest to the highest list of top ten primes>"
        StringBuilder stringOut = new StringBuilder();
        stringOut.append(executionTime).append(" ").append(primesList.PQ.size()).append(" ").append(primesList.sum);
        stringOut.append("\n");
        for (int topPrime : topPrimes) {
            stringOut.append(topPrime);
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
    }

    static class PrimesList {
        int sum;
        PriorityQueue<Integer> PQ;
        int booleanIdx;

        PrimesList() {
            sum = 0;
            PQ = new PriorityQueue<>();//TODO add custom comparator that arranges high to low? or maybe change to List, order it, and access going up the tail?
            booleanIdx = 2;
        }

        public void addPrime (Integer primeToAdd) {
            synchronized(this) {
                this.sum += primeToAdd;
                PQ.add(primeToAdd);
            }
        }
        public int getAndIncrement () {
            synchronized (this) {
                int temp = booleanIdx;
                for (int i = booleanIdx; i < primesBoolean.length; i++) {
                    if (primesBoolean[i]) {
                        booleanIdx = i;
                        break;
                    }
                }
                return temp;
            }
        }
        public int[] getTopTen () {
            int[] retArray = new int[10];
            for (int i = 0; i < 10; i++) {
                retArray[9-i] = PQ.poll();
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
                int minOfMaxChecking = primesBoolean.length;
                do {// if value is not less than minimum of the max value checked in every active thread, wait
                    for (primesThread thread : threadArray) {
                        if (thread.getActive() && thread.getMax() < minOfMaxChecking) {
                            minOfMaxChecking = thread.getMax();
                        }
                    }
                } while (value >= minOfMaxChecking);
                if (primesBoolean[value]) {// if primesBoolean[value] = *false, this index has been marked as composite*, continue, get new value;
                    continue;
                }
                // update three thread variables
                activeFlag = true;
                minValueChecked = value;
                maxValueChecked = value;
                // add value to PQ and sum
                primesList.addPrime(value);
                // while value < primesBoolean.length
                while (value < primesBoolean.length) {//continue for every multiple of the prime below 10^8
                    value += minValueChecked;//minValueChecked is the prime number, starting point
                    primesBoolean[value] = true;//composite value, multiple of minValueChecked, a prime number
                    maxValueChecked = value;//update max value checked so next thread can see if its value has been checked for being composite
                }
            } while (minValueChecked < limit);
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