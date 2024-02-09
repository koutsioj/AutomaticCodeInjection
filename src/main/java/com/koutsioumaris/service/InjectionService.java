package com.koutsioumaris.service;

import com.koutsioumaris.DB.DBActions;
import com.koutsioumaris.annotations.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;


public class InjectionService {

    private Database dbAnnotation = null;
    private Table tableAnnotation = null;
    private ArrayList<DBField> fieldsAnnotation = new ArrayList<>();
    private PrimaryKey primaryKeyAnnotation = null;
    private Method[] methods = null;
    private Field[] fields = null;
    private Class c = null;

    public InjectionService(Class<?>  c) {
        this.dbAnnotation = getDatabaseAnnotation(c);
        this.tableAnnotation = getTableAnnotation(c);
        this.fieldsAnnotation = getDBFieldAnnotation(c);
        this.primaryKeyAnnotation = getPrimaryKeyAnnotation(c);
        this.methods = getMethods(c);
        this.fields = getFields(c);
        this.c = c;
    }

    private Database getDatabaseAnnotation(Class<?> c) {
        return c.getDeclaredAnnotation(Database.class);
    }

    private Table getTableAnnotation(Class<?> c) {
        return c.getDeclaredAnnotation(Table.class);
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

    private Field[] getFields(Class<?> c) {
        return c.getDeclaredFields();
    }

    private Method[] getMethods(Class<?> c) {
         return c.getDeclaredMethods();
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
