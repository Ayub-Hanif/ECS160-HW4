package com.ecs160.hw4;

import java.sql.Timestamp;
import java.util.List;

/**
 * We using the Composite pattern. it lets us treat “leaf” nodes as (single
 * posts) and “composite” nodes as (threads with replies) via one interface.
 * We already had a Post that contain a list of replies (which themselves are
 * Posts). Which is halfway to Composite!
 * Each Post is both: A “Leaf” if it has no replies. A “Composite” if it has one
 * or more replies.
 */
public interface SocialComposite {
    int get_post_Id();

    String get_post_content();

    Timestamp get_creation_time();

    int get_word_count();

    List<SocialComposite> get_post_replies();

    void add_reply_under_post(SocialComposite reply);

    // For the Visitor pattern:
    void accept(SocialVisitor visitor);
}
