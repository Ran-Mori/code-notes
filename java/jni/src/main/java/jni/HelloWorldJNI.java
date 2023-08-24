package jni;

public class HelloWorldJNI {

    static {
        //load native library
        System.loadLibrary("hello_world_jni");
    }

    public static void main(String[] args) {
        new HelloWorldJNI().sayHello();
    }

    private native void sayHello();
}
