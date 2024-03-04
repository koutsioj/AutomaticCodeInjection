package com.koutsioumaris.outputTesting;

import java.sql.ResultSet;
import java.io.File;
import java.util.ArrayList;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
public class Student  {
    String AM;
    String email;
    int yearOfStudies;
    String fullName;
    boolean postGraduate;

    public Student() {}
    public Student(String AM,String email,int yearOfStudies,String fullName,boolean postGraduate) {
        this.AM = AM;
        this.email = email;
        this.yearOfStudies = yearOfStudies;
        this.fullName = fullName;
        this.postGraduate = postGraduate;
    }

    private static Connection connect() {
        String currentDirectory = System.getProperty("user.dir");
        String dbName = currentDirectory + File.separator + "UnipiDB";
        String connectionString = "jdbc:h2:"+dbName;
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(connectionString);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return connection;
    }

    public static void createTableAndData(){
        try {
            Connection connection = connect();

            String createTableSQL = "CREATE TABLE Student"
                    + "(AM VARCHAR(100) NOT NULL PRIMARY KEY, Email VARCHAR(100), YearOfStudies INTEGER, FullName VARCHAR(100), PostGraduate BOOLEAN)";
            Statement statement = connection.createStatement();
            statement.executeUpdate(createTableSQL);

            statement.close();
            connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static int insertStudent (String AM, String Email, int Year, String FullName, boolean PostGraduate) {
        try {
            Connection connection = connect();
            String insertSQL = "INSERT INTO Student VALUES(?,?,?,?,?)";
            PreparedStatement preparedStatement = connection.prepareStatement(insertSQL);
            preparedStatement.setString(1, AM);
            preparedStatement.setString(2, Email);
            preparedStatement.setInt(3, Year);
            preparedStatement.setString(4, FullName);
            preparedStatement.setBoolean(5, PostGraduate);

            int count = preparedStatement.executeUpdate();
            preparedStatement.close();
            connection.close();

            return count;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public static Student getOneStudent (String AM) {
        try {
            Connection connection = connect();
            String selectSQL = "SELECT * FROM Student WHERE AM = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(selectSQL);

            preparedStatement.setString(1, AM);

            ResultSet resultsFound = preparedStatement.executeQuery();
            Student selectedStudent = null;
            while(resultsFound.next()) {
                selectedStudent = new Student(
                        resultsFound.getString("AM"),
                        resultsFound.getString("email"),
                        resultsFound.getInt("yearOfStudies"),
                        resultsFound.getString("fullName"),
                        resultsFound.getBoolean("postGraduate"));
            }
            preparedStatement.close();
            connection.close();
            return selectedStudent;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public static int deleteStudent (String AM) {
        try {
            Connection connection = connect();
            String deleteSQL = "DELETE FROM Student WHERE AM = ?";
            PreparedStatement preparedStatement = connection.prepareStatement(deleteSQL);
            preparedStatement.setString(1, AM);
            int rowsAffected = preparedStatement.executeUpdate();
            preparedStatement.close();
            connection.close();

            return rowsAffected;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public static List getAllStudents () {
        try {
            Connection connection = connect();
            Statement statement = connection.createStatement();
            String selectSQL = "SELECT * FROM Student";

            ResultSet resultsFound = statement.executeQuery(selectSQL);
            List<Student> list = new ArrayList<>();
            while(resultsFound.next()) {
                Student temp = new Student(
                        resultsFound.getString("AM"),
                        resultsFound.getString("email"),
                        resultsFound.getInt("yearOfStudies"),
                        resultsFound.getString("fullName"),
                        resultsFound.getBoolean("postGraduate"));
                list.add(temp);
            }
            statement.close();
            connection.close();
            return list;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public static int deleteStudents () {
        try {
            Connection connection = connect();
            Statement statement = connection.createStatement();
            String deleteSQL = "DELETE FROM Student";

            int rowsAffected = statement.executeUpdate(deleteSQL);

            statement.close();
            connection.close();

            return rowsAffected;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}