package utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.params.provider.Arguments;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class TestDataLoader {

    /**
     * Loads course test data from src/test/resources/testdata/courses.json
     * Appends a timestamp to each courseCode to ensure uniqueness per run.
     */
    public static Stream<Arguments> loadCourseData() {
        long timestamp = java.time.Instant.now().toEpochMilli();

        try (InputStream input = TestDataLoader.class
                .getClassLoader()
                .getResourceAsStream("testdata/courseList.json")) {

            if (input == null)
                throw new RuntimeException("testdata/courseList.json not found in resources");

            JsonArray courses = JsonParser
                    .parseReader(new InputStreamReader(input))
                    .getAsJsonArray();

            List<Arguments> args = new ArrayList<>();

            for (int i = 0; i < courses.size(); i++) {
                JsonObject course = courses.get(i).getAsJsonObject();

                args.add(Arguments.of(
                        course.get("title").getAsString(),
                        course.get("courseCode").getAsString() + "_" + timestamp,
                        course.get("category").getAsString(),
                        course.get("totalCapacity").getAsInt(),
                        course.get("startDate").getAsString(),
                        course.get("endDate").getAsString()
                ));
            }

            return args.stream();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load course test data: " + e.getMessage(), e);
        }
    }
}
