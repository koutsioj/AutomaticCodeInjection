package com.koutsioumaris.service;

import com.koutsioumaris.annotations.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class InjectionService {

    private final Class<?> c;
    private final Method[] methods;
    private final Field[] fields;
    private final Database dbAnnotation;
    private final Table tableAnnotation;
    private final ArrayList<DBField> fieldsAnnotations;
    private final String primaryKey;


    public InjectionService(Class<?>  c) {
        this.c = c;
        this.methods = getMethods(c);
        this.fields = getFields(c);
        this.dbAnnotation = getDatabaseAnnotation(c);
        this.tableAnnotation = getTableAnnotation(c);
        this.fieldsAnnotations = getDBFieldAnnotation();
        this.primaryKey = getPrimaryKey();
    }

    private Database getDatabaseAnnotation(Class<?> c) {
        return c.getDeclaredAnnotation(Database.class);
    }

    private Table getTableAnnotation(Class<?> c) {
        return c.getDeclaredAnnotation(Table.class);
    }

    private String getPrimaryKey() {
        for (Field field: fields) {
            PrimaryKey primaryKey = field.getAnnotation(PrimaryKey.class);
            if (primaryKey != null) {
                return field.getAnnotation(DBField.class).name(); //get the name value of the DBfield that is the primary key
            }
        }
        return null;
    }

    private ArrayList<DBField> getDBFieldAnnotation() {
        ArrayList<DBField> fieldAnnotations = new ArrayList<>();

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

    private Map<String,String> getMethodParameters(Method method) {

        //List implementation
        /*
        return Arrays.stream(method.getParameters())
                .map(parameter -> {
                    String parameterType = parameter.getType().getSimpleName(); //e.g. String. We assume only primitive types are used.
                    String parameterName = null;

                    Param paramAnnotation = parameter.getDeclaredAnnotation(Param.class);
                    if(paramAnnotation != null) {
                        parameterName = paramAnnotation.name(); //returns value of "name" in "@Param" (e.g. AM)
                    }
                    else { //if parameter in not declared with @Param
                        parameterName = parameter.getName(); //returns name of parameter like "arg0" , "arg1" etc.
                    }
                    // builder.append(parameterType).append(" ").append(parameterName).append(" ");
                    return parameterType+" "+parameterName; //e.g. "String email"
                }).toList();
        */

        //map implementation
        return Arrays.stream(method.getParameters())
                .collect(Collectors.toMap(
                        parameter -> { //for each parameter get "paramAnnotation.name()" or "parameter.getName()" as Key
                            Param paramAnnotation = parameter.getDeclaredAnnotation(Param.class);
                            return (paramAnnotation != null) ? paramAnnotation.name() : parameter.getName(); //returns value of "name" in "@Param" or "arg0" etc
                        }, // Key mapper
                        parameter -> parameter.getType().getSimpleName() // Value mapper. value : e.g "String"
                ));
    }

    public void createOutputClass() {
        StringBuilder builder = new StringBuilder();

        buildClass(builder); //build class
        buildFields(builder);

        buildDbConnection(builder);
        buildDbTable(builder);

        buildMethodDefinitions(builder);
        //build methods-----------------------



        //inside method--------------------------

        System.out.println(builder);
    }

    private void buildDbConnection(StringBuilder builder) {
        builder.append("private static Connection connect() {\n"); //build method definition

        if (dbAnnotation == null || tableAnnotation == null) { //annotation missing
            throw new IllegalArgumentException("Missing class annotation 'Database' or 'Table'");
        }
        String name = dbAnnotation.name();
        String dbType = dbAnnotation.dbType();

        builder.append("\tString dbName = \"").append(name).append("\";\n");

        //build connection string depending on dbType
        if (dbType.equalsIgnoreCase("sqlite")) {
            builder.append("\tString connectionString = \"jdbc:sqlite:\"+dbName+\".db\"").append(";\n"); //SQLite
        }
        else if (dbType.equalsIgnoreCase("derby")) {
            builder.append("\tString connectionString = \"jdbc:derby:+dbName+;create=true\"").append(";\n"); //derby
        }
        else if (dbType.equalsIgnoreCase("h2")) {
            builder.append("\tString connectionString = \"jdbc:h2:mem:+dbName\"").append(";\n"); //assuming the connection is to an in-memory db
        } else {
            throw new IllegalArgumentException("Accepted values for 'dbType' annotation are : 'SQLITE', 'DERBY', 'H2'");
        }

        //connect to db
        builder.append("""
                \tConnection connection = null;
                \ttry {
                \t\tconnection = DriverManager.getConnection(connectionString);
                \t} catch (SQLException e) {
                \t\tthrow new RuntimeException(e);
                \t}
                \treturn connection;
                }
                """);
        builder.append("\n");
    }

    private void buildDbTable(StringBuilder builder) {

        StringBuilder tableBuilder = new StringBuilder();
        String className = c.getSimpleName();
     //   String primaryKey = getPrimaryKey();

        builder.append("""
                public static void createTableAndData(){
                \ttry {
                \t\tConnection connection = connect();
                """);
        builder.append("\n\t\tString createTableSQL = \"CREATE TABLE IF NOT EXISTS ").append(className).append("\"\n\t\t + ");

        tableBuilder.append("\"(");
        for (int i=0 ; i<fieldsAnnotations.size() ; i++) {

            if (i!=0) { //not the first field
                tableBuilder.append(", ");
            }

            tableBuilder.append(fieldsAnnotations.get(i).name()).append(" ").append(fieldsAnnotations.get(i).type().toUpperCase());
            if (primaryKey.equals(fieldsAnnotations.get(i).name())) { //if the name value of DBField is the primary key
                tableBuilder.append(" NOT NULL PRIMARY KEY");
            }
        }
        tableBuilder.append(")\";\n");

        tableBuilder = new StringBuilder(tableBuilder.toString().replace("TEXT", "VARCHAR(100)"));
        tableBuilder.append("""
                \t\tStatement statement = connection.createStatement();
                \t\tstatement.executeUpdate(createTableSQL);

                \t\tstatement.close();
                \t\tconnection.close();
                \t} catch (SQLException e) {
                \t\tthrow new RuntimeException(e);
                \t}
                 }""");
        tableBuilder.append("\n\n");
        builder.append(tableBuilder);
    }

    private void buildClass(StringBuilder builder) {
        String classModifiers = Modifier.toString(c.getModifiers()); //for "public abstract class" e.g. it gets "public abstract"
        String className = c.getSimpleName(); //class name (e.g. "Student")
        String superclassSimpleName = c.getSuperclass().getSimpleName(); //"Object" if it does not extend a class
        String superclassName = c.getSuperclass().getName(); //"java.lang.Object" if it does not extend a class

        ArrayList<String> classInterfaces = new ArrayList<>(); //ArrayList with all the interfaces the class implements
        Arrays.stream(c.getInterfaces()).forEach(i -> classInterfaces.add(i.getSimpleName()));

        //start building
        builder.append(classModifiers).append(" class ").append(className).append(" ");
        if (!superclassName.equalsIgnoreCase("java.lang.Object")) { //so it does extend a class other than Java's "Object"
            builder.append("extends ").append(superclassSimpleName).append(" ");
        }

        for (int i=0 ; i<classInterfaces.size() ; i++) {
            if (i==0) { //first interface
                builder.append("Implements ").append(classInterfaces.get(i)).append(" ");
            }
            else { //other interfaces
                builder.append(", ").append(classInterfaces.get(i));
            }
        }

        builder.append(" {\n");
    }

    private void buildFields(StringBuilder builder) {
        Arrays.stream(fields).forEach(field -> {
            String fieldModifier = Modifier.toString(field.getModifiers()); //e.g "private"
            String fieldType = field.getType().getSimpleName(); //e.g. "String"
            String fieldName = field.getName(); //e.g. "AM"

            builder.append(fieldModifier).append(" ").append(fieldType).append(" ").append(fieldName).append(";");
            builder.append("\n");
        });
        builder.append("\n");
    }

    private void buildMethodDefinitions(StringBuilder builder) {
        Arrays.stream(methods).forEach(method -> {

            DBMethod dbMethodAnnotation = method.getDeclaredAnnotation(DBMethod.class);
            if (dbMethodAnnotation == null) { //no DBMethod annotation found. We ignore this method.
                return; //it works like "continue;" in loops. moves to the next element in the stream
            }

            //create method definition ------------>
            String methodModifier = Modifier.toString(method.getModifiers()); //e.g. public
            String methodType = method.getGenericReturnType().toString(); //e.g. int or java.util.List<java.lang.String>
            String methodName = method.getName(); //e.g. getAllStudents

            Map<String,String> parametersMap = getMethodParameters(method);

            builder.append(methodModifier).append(" ").append(methodType).append(" ").append(methodName).append(" (");

            int parameterCount = 0;
            for (Map.Entry<String,String> parameter: parametersMap.entrySet()) {
                parameterCount ++;
                builder.append(parameter.getValue()).append(" ").append(parameter.getKey());
                if (parameterCount < parametersMap.size()) {
                    builder.append(", ");
                }
            }

//            Iterator<String> iterator = parametersMap.iterator(); //create list iterator
//            parametersMap.forEach(parameter -> {
//                builder.append(parameter);
//
//                iterator.next(); //move to the next element
//                if (iterator.hasNext()) { //if this is not the last parameter
//                    builder.append(", ");
//                }
//            });

            builder.append(") {\n");
            //<-------------- create method definition

            //build method execution
            if (dbMethodAnnotation.type().equalsIgnoreCase("SelectAll")) {
                //buildSelectAll(...)
            } else if (dbMethodAnnotation.type().equalsIgnoreCase("DeleteOne")) {
                buildDeleteOne(builder, method);
            }
        });
    }

    private void buildDeleteOne(StringBuilder builder, Method method) {
        //we assume the method only contains one parameter

        Map<String,String> methodParameters = getMethodParameters(method);
        String parameterName = "";
        String parameterType = "";
        for (Map.Entry<String,String> parameter: methodParameters.entrySet()) {
            parameterName = parameter.getKey(); //name of the parameter
            parameterType = parameter.getValue(); //type of the parameter
        }

        builder.append("""
                \ttry {
                \t\tConnection connection = connect();
                \t\tStatement statement = connection.createStatement();
                \t\tString deleteSQL = "DELETE FROM\s""").append(tableAnnotation.name()).append(" WHERE ").append(parameterName).append(" = ?\";\n");
        builder.append("\t\tPreparedStatement preparedStatement = connection.prepareStatement(deleteSQL);\n");

        if (parameterType.equalsIgnoreCase("String")) {
            builder.append("\t\tpreparedStatement.setString(1, ").append(parameterName).append(");");
        } else if (parameterType.equalsIgnoreCase("int")) {
            builder.append("\t\tpreparedStatement.setInt(1, ").append(parameterName).append(");");
        } else if (parameterType.equalsIgnoreCase("boolean")) {
            builder.append("\t\tpreparedStatement.setBoolean(1, ").append(parameterName).append(");");
        }
        builder.append("\n");
        builder.append("\t\tint rowsAffected = preparedStatement.executeUpdate(deleteSQL);\n");
        builder.append("""
                \t\tstatement.close();
                \t\tconnection.close();

                \t\treturn rowsAffected;
                \t} catch (SQLException e) {
                \t\tthrow new RuntimeException(e);
                \t}
                """);

        builder.append(" }\n");
    }

}
