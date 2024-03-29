package com.koutsioumaris.service;

import com.koutsioumaris.annotations.*;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

public class InjectionService {

    private final Class<?> c;
    private final Method[] methods;
    private final Field[] fields;
    private final Database dbAnnotation;
    private final Table tableAnnotation;
    private final ArrayList<DBField> fieldsAnnotations;
    private final String primaryKey;
    private final HashSet<String> importsSet = new HashSet<>();


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

    private LinkedHashMap<String,String> getMethodParameters(Method method) {

        return Arrays.stream(method.getParameters())
                .collect(Collectors.toMap(
                        parameter -> { //for each parameter get "paramAnnotation.name()" or "parameter.getName()" as Key
                            Param paramAnnotation = parameter.getDeclaredAnnotation(Param.class);
                            return (paramAnnotation != null) ? paramAnnotation.name() : parameter.getName(); //returns value of "name" in "@Param" if it exists else "arg0" etc
                        }, // Key mapper
                        parameter -> parameter.getType().getSimpleName(), // value mapper. value : e.g "String"
                        (existing, replacement) -> existing, // merge function, keeps existing value
                        LinkedHashMap::new // supplier for the map, returns LinkedHashMap instead of the default HashMap so that the parameters are ordered by insertion
                ));
    }

    public void createOutputClass() {
        StringBuilder builder = new StringBuilder();

        buildClass(builder); //build class
        buildFields(builder);
        buildConstructor(builder);

        buildDbConnection(builder);
        buildDbTable(builder);

        buildMethods(builder);

        importsSet.forEach(element -> builder.insert(0, element+"\n"));
        builder.append("\n}");

        createFile(builder.toString());
    }

    public void createFile(String builder) {
        try {
            FileWriter outputFile = new FileWriter("output.java");
            outputFile.write(builder);
            outputFile.close();
            System.out.println("File created successfully");
        }catch (IOException e) {
            System.out.println("Unable to create file");
            e.printStackTrace();
        }
    }

    private void buildDbConnection(StringBuilder builder) {
        //add necessary imports
        importsSet.add("import java.sql.Connection;");
        importsSet.add("import java.sql.DriverManager;");
        importsSet.add("import java.sql.SQLException;");
        importsSet.add("import java.sql.ResultSet;");
        importsSet.add("import java.util.ArrayList;");
        importsSet.add("import java.util.List;");
        builder.append("private static Connection connect() {\n"); //build method definition

        if (dbAnnotation == null || tableAnnotation == null) { //annotation missing
            throw new IllegalArgumentException("Missing class annotation 'Database' or 'Table'");
        }
        String name = dbAnnotation.name();
        String dbType = dbAnnotation.dbType();

        //build connection string depending on dbType
        if (dbType.equalsIgnoreCase("sqlite")) {
            builder.append("\tString dbName = \"").append(name).append("\";\n");
            builder.append("\tString connectionString = \"jdbc:sqlite:\"+dbName+\".db\"").append(";\n"); //SQLite
        }
        else if (dbType.equalsIgnoreCase("derby")) {
            builder.append("\tString dbName = \"").append(name).append("\";\n");
            builder.append("\tString connectionString = \"jdbc:derby:\"+dbName+\";create=true\"").append(";\n"); //derby
        }
        else if (dbType.equalsIgnoreCase("h2")) {
            importsSet.add("import java.io.File;"); //for File.separator
            builder.append("\tString currentDirectory = System.getProperty(\"user.dir\");\n");
            builder.append("\tString dbName = currentDirectory + File.separator + \"").append(name).append("\";\n");
            builder.append("\tString connectionString = \"jdbc:h2:\"+dbName").append(";\n"); //assuming the connection is to an in-memory db
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
        importsSet.add("import java.sql.Statement;"); //add necessary import

        StringBuilder tableBuilder = new StringBuilder();
        String className = c.getSimpleName();

        builder.append("""
                public static void createTableAndData(){
                \ttry {
                \t\tConnection connection = connect();
                """);
        builder.append("\n\t\tString createTableSQL = \"CREATE TABLE ").append(className).append("\"\n\t\t + ");

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

    //creates an all arg constructor
    private void buildConstructor(StringBuilder builder) {

        //get all declared constructors of the class
        Constructor<?>[] constructors = c.getDeclaredConstructors();

        //only keep the constructors that are annotated with the relevant annotations
        //(we assume each of these annotations are used only up to 1 time)
        List<Constructor<?>> annotatedConstructors = Arrays.stream(constructors)
                .filter(constructor -> (constructor.getDeclaredAnnotation(FullArgConstructor.class) != null) ||
                        constructor.getDeclaredAnnotation(NoArgConstructor.class) != null)
                .toList();

        if (annotatedConstructors.isEmpty()) { //if there is no annotated constructor end method execution
            return;
        }

        for (Constructor<?> constructor: annotatedConstructors) {
            String constructorModifier = Modifier.toString(constructor.getModifiers()); //e.g public
            String constructorName = c.getSimpleName();

            if (constructor.getDeclaredAnnotation(NoArgConstructor.class) != null) {
                builder.append(constructorModifier).append(" ").append(constructorName).append("() {}\n"); //create no arg constructor
            } else { //create full arg constructor
                ArrayList<String> paramTypes = new ArrayList<>();
                ArrayList<String> paramNames = new ArrayList<>();

                //get the field names and types to later insert them in the all arg constructor
                for (Field field: fields) {
                    paramTypes.add(field.getType().getSimpleName());
                    paramNames.add(field.getName());
                }

                //create the declaration of the all arg constructor
                builder.append(constructorModifier).append(" ").append(constructorName).append("(");
                for (int i = 0; i < paramTypes.size(); i++) {
                    builder.append(paramTypes.get(i)).append(" ").append(paramNames.get(i));
                    if (i!= paramTypes.size()-1) {
                        builder.append(",");
                    }
                }
                builder.append(") {\n");

                //create the internal code of the constructor
                for (String paramName: paramNames) {
                    builder.append("\tthis.").append(paramName).append(" = ").append(paramName).append(";\n");
                }
                builder.append("}\n\n");
            }
        }
    }


        private void buildMethods(StringBuilder builder) {
        Arrays.stream(methods).forEach(method -> {

            DBMethod dbMethodAnnotation = method.getDeclaredAnnotation(DBMethod.class);
            if (dbMethodAnnotation == null) { //no DBMethod annotation found. We ignore this method.
                return; //it works like "continue;" in loops. moves to the next element in the stream
            }

            //create method definition ------------>
            String methodModifier = Modifier.toString(method.getModifiers()); //e.g. public
            String methodType = method.getReturnType().getSimpleName(); //e.g. "int" or "List"
            String methodName = method.getName(); //e.g. getAllStudents

            LinkedHashMap<String, String> methodParameters = getMethodParameters(method);

            builder.append(methodModifier).append(" ").append(methodType).append(" ").append(methodName).append(" (");

            int parameterCount = 0;
            for (Map.Entry<String, String> parameter : methodParameters.entrySet()) {
                parameterCount++;
                builder.append(parameter.getValue()).append(" ").append(parameter.getKey());
                if (parameterCount < methodParameters.size()) {
                    builder.append(", ");
                }
            }

            builder.append(") {\n");
            //<-------------- create method definition

            //build method execution
            String dbMethodType = dbMethodAnnotation.type();

            if (dbMethodType.equalsIgnoreCase("InsertOne")) {
                buildInsertOne(builder, methodParameters);
            } else if (dbMethodType.equalsIgnoreCase("SelectAll")) {
                buildSelectAll(builder);
            } else if(dbMethodType.equalsIgnoreCase("SelectOne")) {
                buildSelectOne(builder, methodParameters);
            } else if (dbMethodType.equalsIgnoreCase("DeleteOne")) {
                buildDeleteOne(builder, methodParameters);
            } else if (dbMethodType.equalsIgnoreCase("DeleteAll")) {
                buildDeleteAll(builder);
            }
        });
    }

    private void buildInsertOne(StringBuilder builder, LinkedHashMap<String, String> methodParameters) {
        importsSet.add("import java.sql.PreparedStatement;");

        builder.append("""
                \ttry {
                \t\tConnection connection = connect();
                \t\tString insertSQL = "INSERT INTO\s""")
                .append(tableAnnotation.name()).append(" VALUES(");
        for (int i = 0; i < methodParameters.size() ; i++) { //add a "?" for each parameter
            builder.append("?");
            if (i != methodParameters.size()-1) {
                builder.append(",");
            }
        }
        builder.append(")\";\n");
        builder.append("\t\tPreparedStatement preparedStatement = connection.prepareStatement(insertSQL);\n");

        int parameterCounter = 0; //the number of values to be added to the preparedStatement
        for (Map.Entry<String,String> parameter: methodParameters.entrySet()) { //for each parameter add that param in the preparedStatement
            String parameterName = parameter.getKey(); //name of the parameter
            String parameterType = parameter.getValue(); //type of the parameter
            parameterCounter++;

            if (parameterType.equalsIgnoreCase("String")) {
                builder.append("\t\tpreparedStatement.setString(");
            } else if (parameterType.equalsIgnoreCase("int")) {
                builder.append("\t\tpreparedStatement.setInt(");
            } else if (parameterType.equalsIgnoreCase("boolean")) {
                builder.append("\t\tpreparedStatement.setBoolean(");
            }
            builder.append(parameterCounter).append(", ").append(parameterName).append(");\n");
        }

        builder.append("\n");
        builder.append("\t\tint count = preparedStatement.executeUpdate();\n");
        builder.append("""
                \t\tpreparedStatement.close();
                \t\tconnection.close();

                \t\treturn count;
                \t} catch (SQLException e) {
                \t\tthrow new RuntimeException(e);
                \t}
                """);

        builder.append(" }\n");
    }

    private void buildDeleteOne(StringBuilder builder, LinkedHashMap<String, String> methodParameters) {
        //we assume the method only contains one parameter

        importsSet.add("import java.sql.PreparedStatement;");

        String parameterName = "";
        String parameterType = "";
        for (Map.Entry<String,String> parameter: methodParameters.entrySet()) {
            parameterName = parameter.getKey(); //name of the parameter
            parameterType = parameter.getValue(); //type of the parameter
        }

        builder.append("""
                \ttry {
                \t\tConnection connection = connect();
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
        builder.append("\t\tint rowsAffected = preparedStatement.executeUpdate();\n");
        builder.append("""
                \t\tpreparedStatement.close();
                \t\tconnection.close();

                \t\treturn rowsAffected;
                \t} catch (SQLException e) {
                \t\tthrow new RuntimeException(e);
                \t}
                """);

        builder.append(" }\n");
    }

    private void buildDeleteAll(StringBuilder builder) {
        //we assume the method contains no parameter

        importsSet.add("import java.sql.Statement;");

        builder.append("""
                \ttry {
                \t\tConnection connection = connect();
                \t\tStatement statement = connection.createStatement();
                \t\tString deleteSQL = "DELETE FROM\s""").append(tableAnnotation.name()).append("\";\n");
        builder.append("\n");
        builder.append("""
                \t\tint rowsAffected = statement.executeUpdate(deleteSQL);

                \t\tstatement.close();
                \t\tconnection.close();

                \t\t return rowsAffected;
                \t} catch (SQLException e) {
                \t\tthrow new RuntimeException(e);
                \t}
                """);

        builder.append(" }\n");
    }

    //build Select ALL Students
    private void buildSelectAll(StringBuilder builder) {
        //Here we implement the select All function so there are no parameters
        importsSet.add("import java.sql.Statement;");
        builder.append("""
                \ttry {
                \t\tConnection connection = connect();
                \t\tStatement statement = connection.createStatement();
                \t\tString selectSQL = "SELECT * FROM\s""").append(tableAnnotation.name()).append("\";\n");
        builder.append("\n\t\tResultSet resultsFound = statement.executeQuery(selectSQL);");
        builder.append("\n\t\tList<");
        builder.append(c.getSimpleName());
        builder.append("""
            > list = new ArrayList<>();
            \t\twhile(resultsFound.next()) {
            \t\t\t""");
        builder.append(c.getSimpleName());
        builder.append("""
                \stemp = new\s""").append(c.getSimpleName()).append("(");

        Arrays.stream(fields).forEach(field -> {
            String fieldType = field.getType().toString();

            builder.append("\n\t\t\t\tresultsFound");
            if (fieldType.endsWith("String")) {
                builder.append(".getString(\"");
            } else if (fieldType.equalsIgnoreCase("int")) {
                builder.append(".getInt(\"");
            } else if (fieldType.equalsIgnoreCase("boolean")) {
                builder.append(".getBoolean(\"");
            }
            builder.append(field.getName()).append("\")");
            if(field == (Arrays.stream(fields).reduce((first,second) -> second)).orElse(null)) {
                builder.append(");");
                builder.append("\n\t\t\tlist.add(temp);");
            }
            else {
                builder.append((","));
            }
        });

        builder.append("""
                \n\t\t}
                \t\tstatement.close();
                \t\tconnection.close();
                \t\treturn list;
                \t} catch (SQLException e) {
                \t\tthrow new RuntimeException(e);
                \t}
                """);

        builder.append(" }\n");
    }

    //build Select One Student()
    private void buildSelectOne(StringBuilder builder, LinkedHashMap<String, String> methodParameters) {
        //we assume the method only contains one parameter
        importsSet.add("import java.sql.Statement;");
        builder.append("""
                \ttry {
                \t\tConnection connection = connect();
                \t\tString selectSQL = "SELECT * FROM\s""").append(tableAnnotation.name()).append(" WHERE ");
                int parameterCounter = 0; //the number of values to be added to the preparedStatement
                for (Map.Entry<String,String> parameter: methodParameters.entrySet()) { //for each parameter add that param in the preparedStatement
                    String parameterName = parameter.getKey(); //name of the parameter
                    String parameterType = parameter.getValue(); //type of the parameter

                    builder.append(parameterName).append(" = ?\";\n");

                    parameterCounter++;

                    builder.append("\t\tPreparedStatement preparedStatement = connection.prepareStatement(selectSQL);\n");

                    if (parameterType.equalsIgnoreCase("String")) {
                        builder.append("\n\t\tpreparedStatement.setString(");
                    } else if (parameterType.equalsIgnoreCase("int")) {
                        builder.append("\n\t\tpreparedStatement.setInt(");
                    } else if (parameterType.equalsIgnoreCase("boolean")) {
                        builder.append("\n\t\tpreparedStatement.setBoolean(");
                    }
                    builder.append(parameterCounter).append(", ").append(parameterName).append(");\n");
                }
        builder.append("\n");
        builder.append("\t\tResultSet resultsFound = preparedStatement.executeQuery();");
        builder.append("""
            \n\t\tStudent selectedStudent = null;
            \t\twhile(resultsFound.next()) {
            \t\t\t""");
        builder.append("\sselectedStudent = new\s").append(c.getSimpleName()).append("(");

        Arrays.stream(fields).forEach(field -> {
            String fieldType = field.getType().toString();
            builder.append("\n\t\t\tresultsFound");
            if (fieldType.endsWith("String")) {
                builder.append(".getString(\"");
            } else if (fieldType.equalsIgnoreCase("int")) {
                builder.append(".getInt(\"");
            } else if (fieldType.equalsIgnoreCase("boolean")) {
                builder.append(".getBoolean(\"");
            }
            builder.append(field.getName()).append("\")");
            if(field == (Arrays.stream(fields).reduce((first,second) -> second)).orElse(null)) {
                builder.append(");");

            }
            else {
                builder.append((","));
            }
        });

        builder.append("""
                \n\t\t}
                \t\tpreparedStatement.close();
                \t\tconnection.close();
                \t\treturn selectedStudent;
                \t} catch (SQLException e) {
                \t\tthrow new RuntimeException(e);
                \t}
                """);

        builder.append(" }\n");
    }
}
