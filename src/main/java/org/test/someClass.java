package org.test;

import org.test.annotations.MyAnnotation;

@MyAnnotation(name = "class",description ="xyz")
public class someClass {
    public void test() {
        System.out.println("Hello world!");
    }
}