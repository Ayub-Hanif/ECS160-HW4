package com.ecs160.hw4;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

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

    // Pass-through methods
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
        // Just forward to the original
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

    // Convenience methods for replies
    public void setReplyHashtag(int idx) {
        HashtagDecorator child = (HashtagDecorator) tagged_replies.get(idx);
        child.setHashtag();
    }

    public String getReplyHashtag(int idx) {
        return ((HashtagDecorator) tagged_replies.get(idx)).getHashtag();
    }
}