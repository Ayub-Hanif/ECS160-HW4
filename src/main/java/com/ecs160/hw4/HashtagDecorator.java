package com.ecs160.hw4;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * The Decorator pattern is used here to add hashtags to posts and their replies
 * without modifying or subclassing the original Post class. This class wraps
 * an existing SocialComposite the original_post and provides a hashtag field
 * along with its own list of decorated replies. By overriding methods like
 * get_post_replies() we ensure that any child posts are also decorated
 * allowing each post and reply to have its own hashtag if desired.
 */

public class HashtagDecorator implements SocialComposite {
    private final SocialComposite original_post; // The original Post/Thread
    private String hashtag;
    private final List<SocialComposite> tagged_replies = new ArrayList<>();


    public HashtagDecorator(SocialComposite original_post) {
        this.original_post = original_post;
        for (SocialComposite child : original_post.get_post_replies()) {
            if (!(child instanceof HashtagDecorator)) {
                tagged_replies.add(new HashtagDecorator(child));
            } else {
                tagged_replies.add(child);
            }
        }
    }

    // We wrapped original_post object:

    @Override
    public List<SocialComposite> get_post_replies() {
        return tagged_replies;
    }

    @Override
    public void add_reply_under_post(SocialComposite child) {
        tagged_replies.add(new HashtagDecorator(child));
    }

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
    public void accept(SocialVisitor visitor) {
        original_post.accept(visitor);
    }

    // Hashtag logic
    public String getHashtag() {
        return hashtag;
    }

    public void setHashtag() {
        if (this.hashtag == null) {
            LlamaInstance llamaInstance = new LlamaInstance();
            this.hashtag = llamaInstance.generateHashtag(this.get_post_content());
        }
    }

    public void setReplyHashtag(int idx) {
        HashtagDecorator child = (HashtagDecorator) tagged_replies.get(idx);
        child.setHashtag();
    }

    public String getReplyHashtag(int idx) {
        return ((HashtagDecorator) tagged_replies.get(idx)).getHashtag();
    }
}