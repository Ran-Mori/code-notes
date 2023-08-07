package main;

public class Consumer implements IConsumer{
    @Override
    public void buy() {
        System.out.println("buy something");
    }
}
