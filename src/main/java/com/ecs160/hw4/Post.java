package com.ecs160.hw4;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;


/**
 * We using the Composite pattern. it lets us treat “leaf” nodes as (single posts) and “composite” nodes as (threads with replies) via one interface.
 * We already had a Post that contain a list of replies (which themselves are Posts). Which is halfway to Composite!
 * Each Post is both: A “Leaf” if it has no replies. A “Composite” if it has one or more replies.
 */

public class Post implements SocialComposite {
    private final int post_Id;
    private final String post_content;
    private final Timestamp creation_time;
    private final int word_count;
    private final List<SocialComposite> post_replies = new ArrayList<>();;

    public Post(int post_Id, String post_content, Timestamp creation_time, int word_count) {
        this.post_Id = post_Id;
        this.post_content = post_content;
        this.creation_time = creation_time;
        this.word_count = word_count;
    }

    @Override
    public int get_post_Id() {
        return post_Id;
    }

    @Override
    public String get_post_content() {
        return post_content;
    }

    @Override
    public Timestamp get_creation_time() {
        return creation_time;
    }

    public int get_word_count() {
        return word_count;
    }

    @Override
    public List<SocialComposite> get_post_replies() {
        return post_replies;
    }

    @Override
    public void add_reply_under_post(SocialComposite reply) {
        this.post_replies.add(reply);
    }

    @Override
    public void accept(SocialVisitor visitor) {
        visitor.visit(this);
        for (SocialComposite c : post_replies) {
            c.accept(visitor);
        }
    }
}