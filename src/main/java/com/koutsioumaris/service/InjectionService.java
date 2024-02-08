package com.koutsioumaris.service;

import com.koutsioumaris.DB.DBActions;
import com.koutsioumaris.annotations.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;


public class InjectionService {

    Database dbAnnotation = null;
    Table tableAnnotation = null;
    ArrayList<DBField> fieldsAnnotation = new ArrayList<>();
    PrimaryKey primaryKeyAnnotation = null;
    Method[] methods = null;

    public InjectionService(Class<?>  c) {
        dbAnnotation = this.getDatabaseAnnotation(c);
        tableAnnotation = this.getTableAnnotation(c);
        fieldsAnnotation = this.getDBFieldAnnotation(c);
        primaryKeyAnnotation = this.getPrimaryKeyAnnotation(c);
        methods = this.getMethods(c);
    }

    private Database getDatabaseAnnotation(Class<?> c) {
        return c.getAnnotation(Database.class);
    }

    private Table getTableAnnotation(Class<?> c) {
        return c.getAnnotation(Table.class);
    }

    private PrimaryKey getPrimaryKeyAnnotation(Class<?> c) {
        return c.getDeclaredAnnotation(PrimaryKey.class);
    }

    private ArrayList<DBField> getDBFieldAnnotation(Class<?> c) {
        ArrayList<DBField> fieldAnnotations = new ArrayList<>();
        Field[] fields = c.getDeclaredFields();

        Arrays.stream(fields).forEach(field -> {
            DBField dbField = field.getAnnotation(DBField.class);
            if (dbField != null) {
                fieldAnnotations.add(dbField);
            }
        });

        return fieldAnnotations;
    }

    private Method[] getMethods(Class<?> c) {
         return c.getMethods();
    }

    public void executeDBMethod() {
        Arrays.stream(methods).forEach(method -> { //for each method
            DBMethod dbMethodAnnotation = method.getDeclaredAnnotation(DBMethod.class);
            if ((dbMethodAnnotation != null) && (dbMethodAnnotation.type().equalsIgnoreCase("SelectAll"))) {
                ArrayList<String> resultList = DBActions.selectAll(dbAnnotation, tableAnnotation, fieldsAnnotation);
                System.out.println("got resultList : "+resultList);
            }
        });
        //Parameter[] parameters = method.getParameters();
        //Arrays.stream(parameters).forEach(parameter -> DBMethodImpl.implementation(parameter.getName()));
    }
}
