package main;

import main.compile.CompileAnnotation;

public class HelloWorld {

    public static void main(@CompileAnnotation("this is the value") String[] args) {
        System.out.println("Hello World");
    }
}
