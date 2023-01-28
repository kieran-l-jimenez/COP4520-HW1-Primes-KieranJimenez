import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.PriorityQueue;


public class primes {
    static int limit = (int) Math.pow(10, 8);
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
            int minValueChecked = primesList.booleanIdx-1;
            int maxValueChecked = primesBoolean.length;

            @Override
            public void run() {
            do {
                int value = primesList.getAndIncrement();//returns index of next unmarked boolean and moves up
                synchronized (this) {
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
                    value += minValueChecked;//minValueChecked is the prime number, starting point
                }
                minValueChecked = primesList.booleanIdx-1;
                maxValueChecked = primesBoolean.length;
            } while (true);
        }

        public int getMin () {
            return minValueChecked;
        }
            public int getMax () {
            return maxValueChecked;
        }
    }
}