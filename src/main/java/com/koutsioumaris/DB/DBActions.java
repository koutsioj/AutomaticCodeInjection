package com.koutsioumaris.DB;

import com.koutsioumaris.annotations.DBField;
import com.koutsioumaris.annotations.Database;
import com.koutsioumaris.annotations.Table;

import java.sql.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBActions {
    public static Connection connect(Database dbAnnotation) {

        String name = dbAnnotation.name();
        String dbType = dbAnnotation.dbType();

        String connectionString;
        if (dbType.equalsIgnoreCase("sqlite")) {
            connectionString = "jdbc:sqlite:"+name+".db"; //SQLite
        }
        else if (dbType.equalsIgnoreCase("derby")) {
            connectionString = "jdbc:derby:"+name+";create=true";
        }
        else if (dbType.equalsIgnoreCase("h2")) {
            connectionString = "jdbc:h2:mem:"+name; //assuming the connection is to an in-memory db
        }
        else {
            throw new IllegalArgumentException("Accepted values for 'dbType' annotation are : 'SQLITE', 'DERBY', 'H2'");
        }

        Connection connection = null;
        try {
            connection = DriverManager.getConnection(connectionString);
        } catch (SQLException ex) {
            Logger.getLogger(DBActions.class.getName()).log(Level.SEVERE, null, ex);
        }
        return connection;
    }

    public static ArrayList<String> selectAll(Database dbAnnotation, Table tableAnnotation, ArrayList<DBField> fieldAnnotations){
        try {
            Connection connection = connect(dbAnnotation);
            Statement statement = connection.createStatement();
            String selectSQL = "select * from "+tableAnnotation.name();
            ResultSet resultSet = statement.executeQuery(selectSQL);

            ArrayList<String> printList = getResults(resultSet, fieldAnnotations);

            System.out.println("\nDone!");

            statement.close();
            connection.close();

            return printList;
        } catch (SQLException ex) {
            Logger.getLogger(DBActions.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private static ArrayList<String> getResults(ResultSet resultSet, ArrayList<DBField> fieldAnnotations) {
        ArrayList<String> printList = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
//        try {
//            while (resultSet.next()) {
//                Arrays.stream(fieldAnnotations).forEach(fieldAnnotation -> {
//                    String fieldType = fieldAnnotation.type();
//                    String fieldName = fieldAnnotation.name();
//                    try {
//                        if (fieldType.equalsIgnoreCase("text")) {
//                            builder.append(resultSet.getString(fieldName));
//                        } else if (fieldType.equalsIgnoreCase("integer")) {
//                            builder.append(resultSet.getInt(fieldName));
//                        } else if (fieldType.equalsIgnoreCase("boolean")) {
//                            builder.append(resultSet.getBoolean(fieldName));
//                        }
//                    } catch (SQLException e) {
//                        throw new RuntimeException(e);
//                    }
//                    builder.append(",");
//                });
//                builder.deleteCharAt(builder.length() - 1); // to delete the appended ","
//                printList.add(builder.toString());
//            }
//        } catch (SQLException e) {
//            throw new RuntimeException(e);
//        }
//        return printList;

        String result = "";
        try {
            while (resultSet.next()) { //for every entry
                result = fieldAnnotations.stream()
                        .map(fieldAnnotation -> { //for every fieldAnnotation

                            String fieldType = fieldAnnotation.type();
                            String fieldName = fieldAnnotation.name();
                            try {
                                if (fieldType.equalsIgnoreCase("text")) {
                                    return resultSet.getString(fieldName);
                                } else if (fieldType.equalsIgnoreCase("integer")) {
                                    return resultSet.getInt(fieldName);
                                } else if (fieldType.equalsIgnoreCase("boolean")) {
                                    return resultSet.getBoolean(fieldName);
                                }
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                            return "";
                        }).toList().toString();
                printList.add(result);
            }

        }catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return printList;
    }
}
