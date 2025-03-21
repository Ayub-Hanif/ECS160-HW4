package com.ecs160.hw4;

import java.sql.Timestamp;
import java.util.List;

/**
 * Also mentioned in the POST.java on top of it same thing because this is used there aswell.
 * 
 * We use the Composite pattern. It lets us treat leaf nodes as single
 * posts and composite nodes as threads with replies via one interface.
 * In HW1, we already had a Post that contained a list of replies which
 * themselves are
 * Posts, which is very similar to the Composite pattern.
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
