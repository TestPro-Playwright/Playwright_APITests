package models;

public class EnrolmentRequest {
    public String username;
    public String courseCode;

    public EnrolmentRequest(String username, String courseCode) {
        this.username   = username;
        this.courseCode = courseCode;
    }
}
