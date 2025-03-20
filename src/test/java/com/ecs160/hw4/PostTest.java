package com.ecs160.hw4;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Timestamp;

import org.junit.jupiter.api.Test;

class PostTest {

    @Test
    void test_post_initialization() {
        // time for now is: "2024-12-10 06:26:59"
        Timestamp timestamp = Timestamp.valueOf("2024-12-10 06:26:59");
        Post post = new Post(1, "Test post content", timestamp, 5);

        assertEquals(1, post.get_post_Id());
        assertEquals("Test post content", post.get_post_content());
        assertEquals(timestamp, post.get_creation_time());
        assertEquals(5, post.get_word_count());
    }

    @Test
    void test_add_reply_under_post() {
        Timestamp timestamp = Timestamp.valueOf("2024-12-10 06:26:59");
        Post post = new Post(1, "Parent post", timestamp, 3);

        Post reply = new Post(2, "Reply post", timestamp, 2);
        post.add_reply_under_post(reply);

        assertEquals(1, post.get_post_replies().size());
        assertEquals("Reply post", post.get_post_replies().getFirst().get_post_content());
    }
}
