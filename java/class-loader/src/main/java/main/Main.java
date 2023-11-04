package main;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello World");
        int intValue = 1;
        int[] intArray = new int[2];
        Integer interValue = 10;
        Integer[] interArray = new Integer[2];

        System.out.println("int ClassLoader is " + int.class.getCanonicalName());
        System.out.println("intArray ClassLoader is " + intArray.getClass().getClassLoader());
        System.out.println("interValue ClassLoader is " + interValue.getClass().getClassLoader());
        System.out.println("interArray ClassLoader is " + interArray.getClass().getClassLoader());
        System.out.println("Object ClassLoader is " + Object.class.getClassLoader());
        System.out.println("Main ClassLoader is " + Main.class.getClassLoader());
        System.out.println("_________________________");

        ClassLoader cl = Main.class.getClassLoader();
        while (cl != null) {
            System.out.println(cl);
            cl = cl.getParent();
        }
    }
}
