package com.koutsioumaris.service;

import com.koutsioumaris.DB.DBActions;
import com.koutsioumaris.annotations.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import javax.tools.JavaFileObject;


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

    public String createOutputClass() {
        StringBuilder builder = new StringBuilder();

        String classModifiers = Modifier.toString(c.getModifiers()); //for "public abstract" class e.g. it gets "public abstract"
        String className = c.getSimpleName(); //class name (e.g. "Student")
        String superclassSimpleName = c.getSuperclass().getSimpleName(); //"Object" if it does not extend a class
        String superclassName = c.getSuperclass().getName(); //"java.lang.Object" if it does not extend a class

        builder.append(classModifiers+" "+className+" ");
        if (!superclassName.equalsIgnoreCase("java.lang.Object")) { //so it does extend a class other than Object
            builder.append("extends "+superclassSimpleName+ " ");
        }

        ArrayList<String> classInterfaces = new ArrayList<>();
        Arrays.stream(c.getInterfaces()).forEach(i -> {
            classInterfaces.add(i.getSimpleName());
        });
        for (int i=0 ; i<classInterfaces.size() ; i++) {
            if (i==0) { //first interface
                builder.append("Implements "+classInterfaces.get(i)+" ");
            }
            else { //other interfaces
                builder.append(", "+classInterfaces.get(i));
            }
        }

        builder.append(" {");
        System.out.println(builder);
        return null;
    }

    public void reflectionTests() {
        //class
        String classModifiers = Modifier.toString(c.getModifiers()); //for "public abstract" class e.g. it gets "public abstract"
        String className = c.getSimpleName(); //class name (e.g. "Student")
        String superclassName = c.getSuperclass().getSimpleName(); //"Object" if it does not extend a class
        Arrays.stream(c.getInterfaces()).forEach(x -> {
            System.out.println("1 "+x.getSimpleName()); //e.g. Readable
        });

        //fields
        Arrays.stream(fields).forEach(field -> {
            String fieldModifier = Modifier.toString(field.getModifiers()); //e.g "private"
            String fieldType = field.getType().getSimpleName(); //e.g. "String"
            String fieldName = field.getName(); //e.g. "AM"
        });

        //methods
        Arrays.stream(methods).forEach(method -> {
            System.out.println(Modifier.toString(method.getModifiers())); //e.g. public
            System.out.println(method.getGenericReturnType()); //e.g. int or java.util.List<java.lang.String>
            //System.out.println(method.getReturnType().getSimpleName()); //e.g. int or List
            System.out.println(method.getName()); //e.g. getAllStudents


            Arrays.stream(method.getParameters()).forEach(parameter -> {
                System.out.println(parameter.getType().getSimpleName()); //e.g. String. We assume only primitive types are used.

                Param paramAnnotation = parameter.getDeclaredAnnotation(Param.class);
                if(paramAnnotation != null) {
                    System.out.println(paramAnnotation.name()); //returns value of "name" in "@Param" (e.g. AM)
                }
                else { //if parameter in not declared with @Param
                    System.out.println("2 : " + parameter.getName()); //returns name of parameter like "arg0" , "arg1" etc.
                }
            });
            System.out.println();
        });


    }
}
