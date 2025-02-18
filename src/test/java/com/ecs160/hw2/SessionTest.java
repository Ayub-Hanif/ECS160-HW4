package com.ecs160.hw2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Timestamp;

import org.junit.jupiter.api.Test;

import com.ecs160.persistence.Session;

public class SessionTest {
   @Test 
   void basic_test() {
        Session s = new Session();
        Timestamp timestamp = Timestamp.valueOf("2024-12-10 06:26:59");
        Post post = new Post(1, "Test post content", timestamp, 5);
        s.add(post);
        try {
            s.persistAll();
        } catch (IllegalArgumentException | IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        timestamp = Timestamp.valueOf("2024-01-10 06:36:59");
        Post toLoadPost = new Post(1, "", timestamp, 0);
        Post loadedPost = (Post)s.load(toLoadPost);

        assertEquals(1, loadedPost.get_post_Id());
        assertEquals("Test post content", loadedPost.get_post_content());
        assertEquals(5, loadedPost.get_word_count());
   }
}
