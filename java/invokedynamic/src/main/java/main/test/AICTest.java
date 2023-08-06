package main.test;

import main.view.OnClickListener;
import main.view.View;

public class AICTest {
    public AICTest() {
        View view = new View();
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                System.out.println("onClick call, view hashcode = " + view.hashCode());
            }
        });
    }
}
