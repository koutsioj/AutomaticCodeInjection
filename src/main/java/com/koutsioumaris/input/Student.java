package com.koutsioumaris.input;

import com.koutsioumaris.annotations.*;

import java.util.List;

//@Database(name="UnipiDB", dbType ="sqlite")
//@Database(name="UnipiDB", dbType ="derby")
@Database(name="UnipiDB", dbType ="h2")
@Table(name="Student")
public class Student {
    @PrimaryKey
    @DBField(name="AM",type="Text")
    String AM;
    @DBField(name="Email",type="Text")
    String email;
    @DBField(name="YearOfStudies",type="Integer")
    int yearOfStudies;
    @DBField(name="FullName",type="Text")
    String fullName;
    @DBField(name="PostGraduate",type="Boolean")
    boolean postGraduate;

    @NoArgConstructor //not necessary
    public Student() {
    }

    @FullArgConstructor //necessary for "select" methods
    public Student(String AM, String email,int yearOfStudies,String fullName,boolean postGraduate) {
    }

    @DBMethod(type="InsertOne")
    public static int insertStudent(@Param(name="AM") String AM,@Param(name="Email") String email,@Param(name="Year") int yearOfStudies,
                                      @Param(name="FullName") String fullName,@Param(name="PostGraduate") boolean postGraduate){
        return 0;
    }

    //Για τη μέθοδο αυτή μπορείτε να δοκιμάστε να επιστρέφετε List<Student>
    @DBMethod(type="SelectAll")
    public static List<Student> getAllStudents(){
        return null;
    }

    //Επιστρέφουμε τον μοναδικό μαθητή με το συγκεκριμένο ΑΦΜ
    @DBMethod(type="SelectOne")
    public static Student getOneStudent(@Param(name="AM") String AM){
        return null;
    }

    //Ο επιστρεφόμενος ακέραιος υποδηλώνει τον αριθμό των εγγραφών που διαγράφηκαν
    @DBMethod(type="DeleteOne")
    public static int deleteStudent(@Param(name="AM") String AM){
        return 0;
    }

    @DBMethod(type="DeleteAll")
    public static int deleteStudents(){
        return 0;
    }

    //This method will not be added to the output class because it doesn't contain the @DBMethod annotation
    public static int test(String AM,@Param(name="Test") int test){
        return 0;
    }

}
