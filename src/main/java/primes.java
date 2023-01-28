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

        int numberPrimes = primesList.PQ.size();
        int[] topPrimes = primesList.getTopTen();

        long executionTime = System.currentTimeMillis() - startTime;

        StringBuilder stringOut = new StringBuilder();
        stringOut.append(executionTime).append(" ").append(numberPrimes).append(" ").append(primesList.sum);
        stringOut.append("\n");
        for (Object topPrime : topPrimes) {
            stringOut.append(topPrime).append(" ");
        }
        File myFile = new File("primes.txt");
        try {
            myFile.createNewFile();
            FileWriter myWriter = new FileWriter("primes.txt");
            myWriter.write(stringOut.toString());
            myWriter.close();

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

        public boolean addPrime (Integer primeToAdd) {
                if (primesBoolean[primeToAdd]) {
                    return false;
                }
                for (primes.primesThread primesThread : threadArray) {
                    if (primeToAdd % primesThread.minValueChecked == 0 && primeToAdd != primesThread.minValueChecked) {
                        return false;
                    }
                }
                synchronized (this) {
                    this.sum += (long) primeToAdd;
                    PQ.add(primeToAdd);
                }
                return true;
        }
        public int getAndIncrement () {

            synchronized (this) {
                for (int i = booleanIdx; i < primesBoolean.length; i++) {
                    if (!primesBoolean[i]) {
                        boolean flag = false;
                        for (primes.primesThread primesThread : threadArray) {
                            if (i < primesThread.getMin()) {
                                flag = true;
                                break;
                            }
                        }
                        if (flag) continue;
                        //above every min here
                        flag = true;
                        for (primes.primesThread primesThread : threadArray) {
                            if (i < primesThread.getMax()) {
                                flag = false;
                                break;
                            }
                        }
                        if (flag) continue;
                        //below at least one maximum
                        booleanIdx = i+1;
                        return i;
                    }
                }
            }
            return primesBoolean.length+1;
        }
        public int[] getTopTen () {
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
            int minValueChecked = primesList.booleanIdx-1;
            int maxValueChecked = primesBoolean.length;

            @Override
            public void run() {
            do {
                int value = primesList.getAndIncrement();//returns index of next unmarked boolean and moves up
                synchronized (this) {
                    activeFlag = true;
                    minValueChecked = value;
                    maxValueChecked = value;
                }
                if (value >= primesBoolean.length) {
                    break;
                }
                try {
                    sleep(0, 1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (!primesList.addPrime(value))
                    break;

                while (value < primesBoolean.length) {//continue for every multiple of the prime below 10^8
                    primesBoolean[value] = true;//composite value, multiple of minValueChecked, a prime number
                    //maxValueChecked = value;//update max value checked so next thread can see if its value has been checked for being composite
                    value += minValueChecked;//minValueChecked is the prime number, starting point
                }
                minValueChecked = primesList.booleanIdx-1;
                maxValueChecked = primesBoolean.length+1;
                activeFlag = false;
            } while (minValueChecked < limit);
            //System.out.println(this.getId() + " breaks loop");
            /*while (true) {//break and stop running once every thread is no longer active again
                boolean x = false;
                for (primes.primesThread primesThread : threadArray) {
                    if (primesThread.activeFlag) {
                        x = true;
                    }
                }
                if (x) continue;
                break;
            }*/
        }
            public boolean getActive () {
            return activeFlag;
        }
        public int getMin () {
            return minValueChecked;
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