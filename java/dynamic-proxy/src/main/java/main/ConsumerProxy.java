package main;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class ConsumerProxy implements InvocationHandler {
    private IConsumer consumer;

    public ConsumerProxy() {}

    //构造方法传入真正被代理的对象
    public ConsumerProxy(IConsumer consumer){
        this.consumer = consumer;
    }

    /*
     * @param proxy 执行newInstance时生成的代理对象
     * @param method 被代理对象的方法
     * @param 被代理对象方法执行的参数
     * */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        before();
        Object returnValue = method.invoke(consumer, args);
        after();
        return returnValue;
    }

    private void before(){
        System.out.println("select before buying");
    }

    private void after(){
        System.out.println("sale return after buying");
    }
}
