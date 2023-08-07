package main;

import java.lang.reflect.Proxy;

public class Main {
    public static void main(String[] args) {
        IConsumer realConsumer = new Consumer();

        IConsumer proxyConsumer = (IConsumer) Proxy.newProxyInstance(
                Consumer.class.getClassLoader(),
                Consumer.class.getInterfaces(),
                new ConsumerProxy(realConsumer)
        );
        proxyConsumer.buy();
    }
}
