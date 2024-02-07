package com.koutsioumaris.main;

import com.koutsioumaris.DB.DBActions;
import com.koutsioumaris.annotations.*;

import com.koutsioumaris.input.Student;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {
    static Database dbAnnotation = null;
    static Table tableAnnotation = null;
    static ArrayList<DBField> fieldsAnnotation = new ArrayList<>();
    static PrimaryKey primaryKeyAnnotation = null;
    public static void main(String[] args) {
        Class<?> c = Student.class;
        Annotation[] annotations = c.getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof Database) {
                dbAnnotation = (Database) annotation;
                //System.out.println("got dbAnnotation : "+ dbAnnotation);
            }
            if (annotation instanceof Table) {
                tableAnnotation = (Table) annotation;
                //System.out.println("got tableAnnotation : "+ tableAnnotation);
            }
        }

        Field[] fields = c.getDeclaredFields();
        Arrays.stream(fields).forEach(field -> {
            Arrays.stream(field.getAnnotations()).forEach(annotation -> {
                if (annotation instanceof DBField) {
                    fieldsAnnotation.add((DBField) annotation);
                    //System.out.println("got fieldsAnnotation : "+ fieldsAnnotation);

                }
                else if (annotation instanceof PrimaryKey) {
                    primaryKeyAnnotation = (PrimaryKey) annotation;
                    //System.out.println("got primaryKeyAnnotation : "+ primaryKeyAnnotation);

                }
            });
        });
        
        Method[] methods = c.getMethods();
        Arrays.stream(methods).forEach(method -> { //for each method
            Arrays.stream(method.getAnnotations()).forEach(annotation -> { //for each method's annotation
                if (annotation instanceof DBMethod DBMethodAnn) {
                    if (DBMethodAnn.type().equalsIgnoreCase("SelectAll")) {
                        ArrayList<String> resultList = DBActions.selectAll(dbAnnotation, tableAnnotation, fieldsAnnotation);
                        System.out.println("got resultList : "+resultList);
                    }
                }

            });
        });
        //Parameter[] parameters = method.getParameters();
            //Arrays.stream(parameters).forEach(parameter -> DBMethodImpl.implementation(parameter.getName()));

    }
}