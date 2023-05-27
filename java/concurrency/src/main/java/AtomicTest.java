import java.util.concurrent.atomic.AtomicInteger;

public class AtomicTest {
    public static AtomicTest object = new AtomicTest();

    public static void main(String[] args) {
        object.testAtomicInteger();
    }

    private void testAtomicInteger() {
        AtomicInteger ai = new AtomicInteger();
        ai.set(10); // 这一步不保证thread-safe，因为它仅仅是给一个volatile的变量赋值
        System.out.println(ai.incrementAndGet()); // 这一步是thread-safe的，因此它底层是CAS
    }
}
