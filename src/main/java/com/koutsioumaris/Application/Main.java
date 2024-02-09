package com.koutsioumaris.Application;
import com.koutsioumaris.input.Student;
import com.koutsioumaris.service.InjectionService;

public class Main {

    public static void main(String[] args) {
        InjectionService injectionService = new InjectionService(Student.class);
        injectionService.createOutputClass();
    }
}