package berlin.yuna.repos;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.Runtime.getRuntime;
import static java.util.stream.IntStream.range;

public class BusyCPU {

    private static final List<BusyThread> threads = new CopyOnWriteArrayList<>();

    public static void main(final String[] args) throws InterruptedException {
        start();
        Thread.sleep(120000);
        stop();
    }

    public static void start() {
        start(-1, 2, -1);
    }

    public static void start(final double load, final int threadCount, final long duration) {
        threads.clear();
        range(0, getRuntime().availableProcessors() * (threadCount < 0 ? 1 : threadCount))
                .mapToObj(index -> new BusyThread(index, load, duration))
                .forEach(threads::add);
        System.out.println("Threads [" + threads.size() + "] load [" + (int) (threads.iterator().next().load() * 100) + "%]");
        threads.forEach(Thread::start);
    }

    public static void stop() {
        threads.forEach(Thread::interrupt);
        threads.clear();
        System.out.println("Threads [" + threads.size() + "]");
    }

    private static class BusyThread extends Thread {
        private final double load;
        private final long duration;

        /**
         * Constructor which creates the thread
         *
         * @param id       id of this thread
         * @param load     Load % that this thread should generate [default = 0.9]
         * @param duration Duration that this thread should generate the load for [-1 = endless]
         */
        public BusyThread(final int id, final double load, final long duration) {
            super(BusyThread.class.getSimpleName() + "_" + id);
            this.load = load > 1 || load < 0 ? 0.9 : load;
            this.duration = duration;
        }

        /**
         * Generates the load when run
         */
        @Override
        public void run() {
            final long startTime = System.currentTimeMillis();
            try {
                // Loop for the given duration
                while (!interrupted() && (duration < 0 || System.currentTimeMillis() - startTime < duration)) {
                    // Every 100ms, sleep for the percentage of unladen time
                    if (System.currentTimeMillis() % 100 == 0) {
                        Thread.sleep((long) Math.floor((1 - load) * 100));
                    }
                }
                System.out.println("Thread [" + this.getName() + "/" + this.getId() + "] finished");
            } catch (InterruptedException e) {
                System.out.println("Thread [" + this.getName() + "/" + this.getId() + "] stopped");
            }
        }

        public double load() {
            return load;
        }

        public long duration() {
            return duration;
        }
    }
}
