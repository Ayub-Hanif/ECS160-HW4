package com.ecs160.hw4;

import java.sql.Timestamp;
import java.util.List;

public class HashtagDecorator implements SocialComposite {
    private final SocialComposite original_post;  // The original Post/Thread
    private String hashtag;

    public HashtagDecorator(SocialComposite original_post, String hashtag) {
        this.original_post = original_post;
        this.hashtag = hashtag;
    }

    // We wrapped original_post object:

    @Override
    public int get_post_Id() {
        return original_post.get_post_Id();
    }
    
    @Override
    public int get_word_count() {
        return original_post.get_word_count();
    }

    @Override
    public String get_post_content() {
        return original_post.get_post_content();
    }

    @Override
    public Timestamp get_creation_time() {
        return original_post.get_creation_time();
    }

    @Override
    public List<SocialComposite> get_post_replies() {
        return original_post.get_post_replies();
    }

    @Override
    public void add_reply_under_post(SocialComposite child) {
        original_post.add_reply_under_post(child);
    }

    @Override
    public void accept(SocialVisitor visitor) {
        // we just forward to the original_post:
        original_post.accept(visitor);
    }

    public String getHashtag() {
        return hashtag;
    }

    public void setHashtag(String newHashTag) {
        this.hashtag = newHashTag;
    }
}

