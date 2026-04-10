package models;

public class Course {
    public String title;
    public String instructor;
    public String courseCode;
    public String category;
    public int    totalCapacity;
    public String startDate;
    public String endDate;

    public static Course create(String title, String instructor,
                                String courseCode, String category,
                                int totalCapacity, String startDate,
                                String endDate) {
        Course c        = new Course();
        c.title         = title;
        c.instructor    = instructor;
        c.courseCode    = courseCode;
        c.category      = category;
        c.totalCapacity = totalCapacity;
        c.startDate     = startDate;
        c.endDate       = endDate;
        return c;
    }
}
