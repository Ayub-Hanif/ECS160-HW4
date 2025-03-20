package com.ecs160.hw2;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.UUID;
import java.sql.Timestamp;
import java.time.Instant;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class JsonParserFile {
    public List<Post> json_parser(String filePath) {
        List<Post> posts_file = new ArrayList<>();

        InputStream input_file = null;
        try {
            File file = new File(filePath);
            if (file.exists()) {
                input_file = new FileInputStream(file);
            } else {
                input_file = getClass().getClassLoader().getResourceAsStream(filePath);
                if (input_file == null) {
                    System.err.println("File not found: " + filePath);
                }
            }

            Reader file_reader = new InputStreamReader(input_file);
            JsonElement parse_element = JsonParser.parseReader(file_reader);

            JsonObject json_object = parse_element.getAsJsonObject();
            JsonArray get_array = json_object.get("feed").getAsJsonArray();

            for (JsonElement object_file : get_array) {
                if (object_file.getAsJsonObject().has("thread")) {
                    JsonObject thread_obj = object_file.getAsJsonObject().getAsJsonObject("thread");
                    JsonObject topPostObj = thread_obj.getAsJsonObject("post");

                    Post topPost = parse_post(topPostObj, null);

                    // only parse immediate replies, we skip "replies-of-replies"
                    if (thread_obj.has("replies")) {
                        JsonArray repliesArr = thread_obj.getAsJsonArray("replies");
                        for (JsonElement replyElem : repliesArr) {
                            JsonObject replyPostObj = replyElem.getAsJsonObject().getAsJsonObject("post");
                            Post reply = parse_post(replyPostObj, topPost.get_post_Id());
                            topPost.add_reply_under_post(reply);
                            // IGNORE deeper replies (do not call parse_replies again)
                        }
                    }
                    posts_file.add(topPost);
                }
            }
        } catch (FileNotFoundException exception) {
            System.err.println("File not found: " + filePath);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return posts_file;
    }

    private Post parse_post(JsonObject post_object, Integer parent_postId) {
        String post_content = post_object.getAsJsonObject("record").get("text").getAsString();

        // Just create a "unique" ID for top-level. For a reply, incorporate the parent's ID
        int postId;
        if (parent_postId == null) {
            // random
            postId = UUID.randomUUID().toString().hashCode();
        } else {
            postId = (post_content + parent_postId).hashCode();
        }

        String create_string = post_object.getAsJsonObject("record").get("createdAt").getAsString();
        Timestamp when_created = Timestamp.from(Instant.parse(create_string));

        int word_count = post_content.split("\\s+").length;

        return new Post(postId, post_content, when_created, word_count);
    }
}
