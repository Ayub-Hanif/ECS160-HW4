package com.ecs160.hw2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
//file readers
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
//sql time based and instants
import java.sql.Timestamp;
import java.time.Instant;
//for the list we need
import java.util.ArrayList;
import java.util.List;
//we need it for the 128-bit unique ID
import java.util.UUID;

//for the Json file and its parsers file.
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


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
                    Post post = parse_post(thread_obj.getAsJsonObject("post"), null);

                    if (thread_obj.has("replies")) {
                        parse_replies(thread_obj.getAsJsonArray("replies"), post);
                    }
                    posts_file.add(post);
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
        int postId;
        if (parent_postId == null) {
            //we will be using the Universally Unique Identifier which uses 128-bit value.
            postId = UUID.randomUUID().toString().hashCode();

        } else {postId = (post_content + parent_postId).hashCode();}

        String create_string = post_object.getAsJsonObject("record").get("createdAt").getAsString();
        Timestamp when_created = Timestamp.from(Instant.parse(create_string));

        /*
            \\s+, is the best thing to use.
            Matches any whitespace character, including:
                Space    (  )
                Tabs     (\t)
                Newlines (\n)
             We can count the number of words using this and then find the largest post.
         */
        int word_count = post_content.split("\\s+").length;

        return new Post(postId, post_content, when_created, word_count);
    }

    private void parse_replies(JsonArray replies_array, Post parent_posts) {
        for (JsonElement Element_reply : replies_array) {
            JsonObject reply_obj = Element_reply.getAsJsonObject().getAsJsonObject("post");

            Post reply = parse_post(reply_obj, parent_posts.get_post_Id());

            parent_posts.add_reply_under_post(reply);

            if (Element_reply.getAsJsonObject().has("replies")) {
                parse_replies(Element_reply.getAsJsonObject().getAsJsonArray("replies"), reply);
            }
        }
    }
}

