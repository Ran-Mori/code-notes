package main.test;

import main.view.View;

public class LambdaTest {
    public LambdaTest() {
        View view = new View();
        view.setOnClickListener(v -> System.out.println("onClick call, view hashcode = " + v.hashCode()));
    }
}
