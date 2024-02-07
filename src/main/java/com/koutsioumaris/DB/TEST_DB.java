package com.koutsioumaris.DB;

import com.koutsioumaris.main.Main;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;


public class TEST_DB {
    public static void main(String[] args) {
        createTableAndData();
        insertTestData();
    }


    public static void createTableAndData(){
        try {
            Connection connection = connect();

            String createTableSQL = "CREATE TABLE IF NOT EXISTS Student"
                    + "(AM VARCHAR(100) NOT NULL PRIMARY KEY,"
                    + "Email VARCHAR(100),"
                    + "Year INTEGER,"
                    + "FullName VARCHAR(100),"
                    + "PostGraduate BOOLEAN)";
            Statement statement = connection.createStatement();
            statement.executeUpdate(createTableSQL);

            statement.close();
            connection.close();
        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    public static boolean insertTestData(){

        try {
            Connection connection = connect();
            String insertSQL = "INSERT INTO Student VALUES(?,?,?,?,?)";
            PreparedStatement preparedStatement = connection.prepareStatement(insertSQL);

            preparedStatement.setString(1, "AM123456");
            preparedStatement.setString(2, "koutsioj2@gmail.com");
            preparedStatement.setInt(3, 2025);
            preparedStatement.setString(4, "Giannis Kouts");
            preparedStatement.setBoolean(5, false);


            int count = preparedStatement.executeUpdate();
            if(count>0){
                System.out.println(count+" record updated");
            }
            preparedStatement.close();
            connection.close();

        } catch (SQLException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        return true;
    }

    public static Connection connect() {
        String connectionString = "jdbc:sqlite:UnipiDB.db";

        Connection connection = null;
        try {
            connection = DriverManager.getConnection(connectionString);
        } catch (SQLException ex) {
            Logger.getLogger(DBActions.class.getName()).log(Level.SEVERE, null, ex);
        }
        return connection;
    }
}
