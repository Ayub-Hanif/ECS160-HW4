package com.ecs160.hw2;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.ecs160.persistence.PersistenceAnnotations.LazyLoad;
import com.ecs160.persistence.PersistenceAnnotations.Persistable;
import com.ecs160.persistence.PersistenceAnnotations.PersistableField;
import com.ecs160.persistence.PersistenceAnnotations.PersistableId;
import com.ecs160.persistence.PersistenceAnnotations.PersistableListField;

@Persistable
public class Post {

    @PersistableId
    private Integer postId;

    @PersistableField
    private String postContent;

    @PersistableField
    private Long creationTimeMillis;

    @PersistableField
    private Integer wordCount;

    @PersistableListField(className = "com.ecs160.hw2.Post")
    @LazyLoad
    private List<Post> replies;

    public Post() {
        this.postId = -1;
        this.postContent = "";
        this.creationTimeMillis = 0L;
        this.wordCount = 0;
        this.replies = new ArrayList<>();
    }
    public Post(int id) {
        this.postId = id;
        this.postContent = "";
        this.creationTimeMillis = 0L;
        this.wordCount = 0;
        this.replies = new ArrayList<>();
    }

    public Post(String content, Timestamp creationTime, int wCount) {
        this.postId = -1;
        this.postContent = content;
        this.creationTimeMillis = (creationTime == null) ? 0L : creationTime.getTime();
        this.wordCount = wCount;
        this.replies = new ArrayList<>();
    }
    public Post(int postId, String content, Timestamp creationTime, int wCount) {
        this.postId = postId;
        this.postContent = content;
        this.creationTimeMillis = (creationTime == null) ? 0L : creationTime.getTime();
        this.wordCount = wCount;
        this.replies = new ArrayList<>();
    }
    public int get_post_Id() {
        return (postId == null) ? -1 : postId;
    }

    public String get_post_content() {
        return postContent;
    }

    public Timestamp get_creation_time() {
        return new Timestamp(creationTimeMillis);
    }

    public int get_word_count() {
        return (wordCount == null) ? 0 : wordCount;
    }

    public List<Post> get_post_replies() {
        return replies;
    }

    public void add_reply_under_post(Post reply) {
        this.replies.add(reply);
    }

    @Override
    public String toString() {
        Timestamp t = get_creation_time();
        String dateStr = (t == null) ? "null" : t.toString();
        return "===\n" +
            "Post ID: " + postId + "\n" +
            "Content: " + postContent + "\n" +
            "Created: " + dateStr + "\n" +
            "Word Count: " + wordCount + "\n" +
            "Num Replies: " + replies.size() + "\n" +
            "===";
    }
}
