package com.koutsioumaris.outputTesting;

import java.util.List;

public class main {
    public static void main(String[] args) {

        Student.createTableAndData();

        Student.insertStudent("AM1000", "email1", 2, "fullname1", false);
        Student.insertStudent("AM2000", "email2", 4, "fullname2", true);

        int i = Student.insertStudent("AM3000", "email3", 5, "fullname3", false);
        System.out.println("\ninsert count: "+i+"\n");

        Student s2 = Student.getOneStudent("AM2000");
        System.out.println("Student:");
        System.out.println("AM = "+s2.AM);
        System.out.println("email = "+s2.email);
        System.out.println("yearOfStudies = "+s2.yearOfStudies);
        System.out.println("fullName = "+s2.fullName);
        System.out.println("postGraduate = "+s2.postGraduate+"\n");

        List<Student> ls = Student.getAllStudents();
        System.out.println("All Student AMs : ");
        for (Student s: ls) {
            System.out.println("AM = "+s.AM);
        }

        int deleteCount1 = Student.deleteStudent("AM1000");
        System.out.println("\ndeleteOne count : "+deleteCount1+"\n");

        int deleteCount2 = Student.deleteStudents();
        System.out.println("deleteAll count : "+deleteCount2);
    }
}
