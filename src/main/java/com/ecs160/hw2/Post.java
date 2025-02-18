package com.ecs160.hw2;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.ecs160.persistence.Session.Persistable;
import com.ecs160.persistence.Session.PersistableField;
import com.ecs160.persistence.Session.PersistableId;
import com.ecs160.persistence.Session.PersistableListField;

@Persistable
public class Post {
    @PersistableId
    private final int post_Id;

    @PersistableField
    private final String post_content;

    @PersistableField
    private final int word_count;

    @PersistableListField(className = "Post")
    private final List<Post> post_replies;

    public Post(int post_Id, String post_content, Timestamp creation_time, int word_count) {
        this.post_Id = post_Id;
        this.post_content = post_content;
        this.word_count = word_count;
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
}
