package com.ecs160.hw2;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class Post {
    private int post_Id;
    private String post_content;
    private Timestamp creation_time;
    private int word_count;
    private List<Post> post_replies;

    public Post() {
        this.post_Id = -1;
        this.post_content = "";
        this.creation_time = new Timestamp(0);
        this.word_count = 0;
        this.post_replies = new ArrayList<>();
    }

    public Post(int id) {
        this.post_Id = id;
        this.post_content = "";
        this.creation_time = new Timestamp(0);
        this.word_count = 0;
        this.post_replies = new ArrayList<>();
    }

    public Post(String post_content, Timestamp creation_time, int word_count) {
        this.post_Id = -1;  // default ID -1
        this.post_content = post_content;
        this.creation_time = creation_time;
        this.word_count = word_count;
        this.post_replies = new ArrayList<>();
    }

    public Post(int post_Id, String post_content, Timestamp creation_time, int word_count) {
        this.post_Id = post_Id;
        this.post_content = post_content;
        this.creation_time = creation_time;
        this.word_count = word_count;
        this.post_replies = new ArrayList<>();
    }

    public int get_post_Id() {
        return post_Id;
    }

    public String get_post_content() {
        return post_content;
    }

    public Timestamp get_creation_time() {
        return creation_time;
    }

    public int get_word_count() {
        return word_count;
    }

    public List<Post> get_post_replies() {
        return post_replies;
    }

    public void add_reply_under_post(Post reply) {
        this.post_replies.add(reply);
    }

    @Override
    public String toString() {
        return "===\n" +
               "Post ID: " + post_Id + "\n" +
               "Content: " + post_content + "\n" +
               "Word Count: " + word_count + "\n" +
               "Replies: " + post_replies.size() + "\n" +
               "===";
    }
}
