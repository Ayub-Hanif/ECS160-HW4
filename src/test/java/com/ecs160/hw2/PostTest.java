package com.ecs160.hw2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Timestamp;

import org.junit.jupiter.api.Test;

class PostTest {

    @Test
    void test_post_initialization() {
        // time for now is: "2024-12-10 06:26:59"
        Timestamp timestamp = Timestamp.valueOf("2024-12-10 06:26:59");
        Post post = new Post("Test post content", timestamp, 5);

        // assertEquals(-1, post.getPostId());
        assertEquals("Test post content", post.getPostContent());
        assertEquals(5, post.getWordCount());
    }

    @Test
    void test_add_reply_under_post() {
        Timestamp timestamp = Timestamp.valueOf("2024-12-10 06:26:59");
        Post post = new Post("Parent post", timestamp, 3);

        Post reply = new Post("Reply post", timestamp, 2);
        post.addReplyUnderPost(reply);

        assertEquals(1, post.getReplies().size());
        assertEquals("Reply post", post.getReplies().getFirst().getPostContent());
    }
}
