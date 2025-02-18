package com.ecs160.hw2;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.ecs160.persistence.PersistenceAnnotations.Persistable;
import com.ecs160.persistence.PersistenceAnnotations.PersistableField;
import com.ecs160.persistence.PersistenceAnnotations.PersistableId;
import com.ecs160.persistence.PersistenceAnnotations.PersistableListField;

@Persistable
public class Post {
    @PersistableId
    private final Integer post_Id;

    @PersistableField
    private final String post_content;

    @PersistableField
    private final Integer word_count;

    @PersistableListField(className = "com.ecs160.hw2.Post")
    private final List<Post> post_replies;

    public Post(String post_content, Timestamp creation_time, int word_count) {
        this.post_Id = -1; // placeholder
        this.post_content = post_content;
        this.word_count = word_count;
        this.post_replies = new ArrayList<>();
    }

    public Post() {
        this.post_Id = -1;
        this.post_content = "";
        this.word_count = -1;
        this.post_replies = new ArrayList<>();
    }

    public Post(int id) {
        this.post_Id = id;
        this.post_content = "";
        this.word_count = -1;
        this.post_replies = new ArrayList<>();
    }

    public int get_post_Id() {
        return post_Id;
    }

    public String get_post_content() {
        return post_content;
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

    public String toString() {
        String ret = "===\n" + "Post ID: " + post_Id + " | " + "Word Count: " + word_count + " | " + "Num Replies: "
                + post_replies.size();
        ret += "\n" + "Content: " + post_content + "\n===";
        return ret;
        // return "Post ID: " + post_Id + " | " +
        // "Content: " + post_content + " | " +
        // "Word Count: " + word_count + " | " +
        // "Num Replies: " + post_replies.size();

    }
}
