package models;

public class CourseUpdateRequest {
    public String title;
    public int    totalCapacity;
    public int    availableSlots;
    public String endDate;

    public static CourseUpdateRequest create(String title, int totalCapacity,
                                             int availableSlots, String endDate) {
        CourseUpdateRequest r  = new CourseUpdateRequest();
        r.title          = title;
        r.totalCapacity  = totalCapacity;
        r.availableSlots = availableSlots;
        r.endDate        = endDate;
        return r;
    }
}
