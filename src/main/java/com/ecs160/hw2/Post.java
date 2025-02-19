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
    private Integer wordCount;

    @PersistableListField(className = "com.ecs160.hw2.Post")
    @LazyLoad // lazy loading (Session.load)
    private List<Post> replies;

    public Post(String post_content, Timestamp creation_time, int word_count) {
        this.postId = -1; // placeholder
        this.postContent = post_content;
        this.wordCount = word_count;
        this.replies = new ArrayList<>();
    }

    public Post() {
        this.postId = -1;
        this.postContent = "";
        this.wordCount = -1;
        this.replies = new ArrayList<>();
    }

    public Post(int id) {
        this.postId = id;
        this.postContent = "";
        this.wordCount = -1;
        this.replies = new ArrayList<>();
    }

    public int getPostId() {
        // System.out.println("GETTING post id = " + postId);
        // if (postId == -31415 || postId == -1) {
        // throw new RuntimeException("test");
        // }
        return this.postId;
    }

    public String getPostContent() {
        // System.out.println("GETTING post content = " + postContent);
        return postContent;
    }

    public int getWordCount() {
        return wordCount;
    }

    public List<Post> getReplies() {
        return replies;
    }

    public void addReplyUnderPost(Post reply) {
        this.replies.add(reply);
    }

    public String toString() {
        String ret = "===\n" + "Post ID: " + postId + " | " + "Word Count: " + wordCount + " | " + "Num Replies: "
                + replies.size() + "\n" + "Content: " + postContent + "\n===";
        return ret;
    }

}
