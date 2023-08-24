#include "jni_HelloWorldJNI.h"
#include<iostream>

JNIEXPORT void JNICALL Java_jni_HelloWorldJNI_sayHello
  (JNIEnv *, jobject) {
std::cout << "Hello World JNI" << std::endl;
};