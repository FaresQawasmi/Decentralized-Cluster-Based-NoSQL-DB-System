package com.example.bootstrapnode.utils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class FileUtil {
    private static final Path USER_FILE_PATH = Paths.get("users.json");

    public static JSONArray readUsersFromFile() {
        try {
            String content = Files.lines(USER_FILE_PATH).collect(Collectors.joining());
            return new JSONArray(content);
        } catch (IOException e) {
            return new JSONArray(); // return empty array if error occurs or file does not exist
        }
    }

    public static void writeUsersToFile(JSONArray users) {
        try {
            Files.write(USER_FILE_PATH, users.toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
