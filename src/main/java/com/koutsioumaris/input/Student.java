package com.koutsioumaris.input;

import com.koutsioumaris.annotations.*;

import java.util.List;
import java.util.Objects;

@Database(name="UnipiDB", dbType ="sqlite")
@Table(name="Student")
public class Student {
    @PrimaryKey
    @DBField(name="AM",type="Text")
    String AM;
    @DBField(name="Email",type="Text")
    String email;
    @DBField(name="Year",type="Integer")
    int yearOfStudies;
    @DBField(name="FullName",type="Text")
    String fullName;
    @DBField(name="PostGraduate",type="Boolean")
    boolean postGraduate;

    @DBMethod(type="InsertOne")
    public int insertStudent(@Param(name="AM") String AM,@Param(name="Email") String email,@Param(name="Year") int yearOfStudies,
                                      @Param(name="FullName") String fullName,@Param(name="PostGraduate") boolean postGraduate){
        return 0;
    }

    //Για τη μέθοδο αυτή μπορείτε να δοκιμάστε να επιστρέφετε List<Student>
    @DBMethod(type="SelectAll")
    public List<String> getAllStudents(){
        return null;
    }
    //Ο επιστρεφόμενος ακέραιος υποδηλώνει τον αριθμό των εγγραφών που διαγράφηκαν
    @DBMethod(type="DeleteOne")
    public int deleteStudent(@Param(name="AM") String AM){
        return 0;
    }

    @DBMethod(type="DeleteAll")
    public int deleteStudents(){
        return 0;
    }

    //This method will not be added to the output class because it doesn't contain the @DBMethod annotation
    public int test(String AM,@Param(name="Test") int test){
        return 0;
    }

}
