
public class ThreadLocalTest {
    private static final ThreadLocal<Integer> threadLocal =
            ThreadLocal.withInitial(() -> {
                return 0; // 初始值设为0
            });

    public static void main(String[] args) {
        Runnable task = () -> {
            int value = threadLocal.get();
            System.out.println(Thread.currentThread().getName() + " -> " + value);
            threadLocal.set(value + 1);
            value = threadLocal.get();
            System.out.println(Thread.currentThread().getName() + " -> " + value);
        };

        Thread thread1 = new Thread(task, "Thread 1");
        Thread thread2 = new Thread(task, "Thread 2");
        Thread thread3 = new Thread(task, "Thread 3");

        thread1.start();
        thread2.start();
        thread3.start();
    }
}
