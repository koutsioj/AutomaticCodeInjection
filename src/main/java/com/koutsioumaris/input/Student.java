package com.koutsioumaris.input;

import com.koutsioumaris.annotations.*;

import java.util.List;

@Database(name="UnipiDB", dbType ="DERBY")
@Table(name="Student")
public class Student {
    @PrimaryKey
    @Field(name="AM",type="Text")
    String AM;
    @Field(name="Email",type="Text")
    String email;
    @Field(name="Year",type="Integer")
    int yearOfStudies;
    @Field(name="FullName",type="Text")
    String fullName;
    @Field(name="PostGraduate",type="Boolean")
    boolean postGraduate;

    //Για τη μέθοδο αυτή μπορείτε να δοκιμάστε να επιστρέφετε List<Student>
    @DBMethod(type="SelectAll")
    public List<String> getAllStudents(){
        return null;
    }
    //Ο επιστρεφόμενος ακέραιος υποδηλώνει τον αριθμό των εγγραφών που διαγράφηκαν
    @DBMethod(type="DeleteOne")
    public int deleteStudents(String AM){
        return 0;
    }
}
