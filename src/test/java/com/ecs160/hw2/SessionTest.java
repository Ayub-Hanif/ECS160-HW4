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
        Post post = new Post("Test post content", timestamp, 5);
        s.add(post);
        try {
            s.persistAll();
        } catch (IllegalArgumentException | IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Post toLoadPost = new Post(1);
        Post loadedPost = (Post) s.load(toLoadPost);

        assertEquals(1, loadedPost.getPostId());
        assertEquals("Test post content", loadedPost.getPostContent());
        assertEquals(5, loadedPost.getWordCount());
    }

    @Test
    void multiple_posts() {
        Session s = new Session();
        Post post1 = new Post("Test post content 1", Timestamp.valueOf("2024-12-10 06:26:59"), 5);
        Post post2 = new Post("other test heh content hehaef test", Timestamp.valueOf("2024-12-10 06:26:59"), 6);
        s.add(post1);
        s.add(post2);
        try {
            s.persistAll();
        } catch (IllegalArgumentException | IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        Post toLoadPost1 = new Post(1);
        Post loadedPost1 = (Post) s.load(toLoadPost1);
        assertEquals(1, loadedPost1.getPostId());
        assertEquals("Test post content 1", loadedPost1.getPostContent());
        assertEquals(5, loadedPost1.getWordCount());

        Post toLoadPost2 = new Post(2);
        Post loadedPost2 = (Post) s.load(toLoadPost2);
        assertEquals(2, loadedPost2.getPostId());
        assertEquals("other test heh content hehaef test", loadedPost2.getPostContent());
        assertEquals(6, loadedPost2.getWordCount());
    }

    @Test
    void multiple_posts_with_list() {
        Session s = new Session();
        Post post1 = new Post("Test post content 1", Timestamp.valueOf("2024-12-10 06:26:59"), 5);
        Post post2 = new Post("other test heh content hehaef test", Timestamp.valueOf("2024-12-10 06:26:59"), 6);
        Post post3 = new Post("Test post content 3", Timestamp.valueOf("2024-12-10 06:26:59"), 7);
        Post post4 = new Post("Test post content 4", Timestamp.valueOf("2024-12-10 06:26:59"), 8);

        post1.addReplyUnderPost(post2);
        post1.addReplyUnderPost(post3);

        s.add(post1);
        // s.add(post2);
        // s.add(post3);
        s.add(post4);
        try {
            s.persistAll();
        } catch (IllegalArgumentException | IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // System.out.println("LOADING post 1");
        Post toLoadPost1 = new Post(1);
        Post loadedPost1 = (Post) s.load(toLoadPost1);
        assertEquals(1, loadedPost1.getPostId());
        assertEquals("Test post content 1", loadedPost1.getPostContent());
        assertEquals(5, loadedPost1.getWordCount());
        assertEquals(2, loadedPost1.getReplies().size());
        assertEquals(2, loadedPost1.getReplies().get(0).getPostId());
        assertEquals("other test heh content hehaef test", loadedPost1.getReplies().get(0).getPostContent());
        assertEquals(6, loadedPost1.getReplies().get(0).getWordCount());
        assertEquals(3, loadedPost1.getReplies().get(1).getPostId());

        // System.out.println("LOADING post 2");
        Post toLoadPost2 = new Post(2);
        Post loadedPost2 = (Post) s.load(toLoadPost2);
        assertEquals(2, loadedPost2.getPostId());
        assertEquals("other test heh content hehaef test", loadedPost2.getPostContent());
        assertEquals(6, loadedPost2.getWordCount());
        assertEquals(0, loadedPost2.getReplies().size());
        // assertEquals(4, loadedPost2.get_post_replies().get(0).get_post_Id());
        // assertEquals("Test post content 4",
        // loadedPost2.get_post_replies().get(0).get_post_content());

        Post toLoadPost3 = new Post(3);
        Post loadedPost3 = (Post) s.load(toLoadPost3);
        assertEquals(3, loadedPost3.getPostId());
        assertEquals("Test post content 3", loadedPost3.getPostContent());
        assertEquals(7, loadedPost3.getWordCount());
        assertEquals(0, loadedPost3.getReplies().size());

        Post toLoadPost4 = new Post(4);
        Post loadedPost4 = (Post) s.load(toLoadPost4);
        assertEquals(4, loadedPost4.getPostId());
        assertEquals("Test post content 4", loadedPost4.getPostContent());
        assertEquals(8, loadedPost4.getWordCount());
        assertEquals(0, loadedPost4.getReplies().size());
    }

    @Test
    void multiple_posts_with_list_2() {
        Session s = new Session();
        Post post1 = new Post("Test post content 1", Timestamp.valueOf("2024-12-10 06:26:59"), 5);
        Post post2 = new Post("other test heh content hehaef test", Timestamp.valueOf("2024-12-10 06:26:59"), 6);
        Post post3 = new Post("Test post content 3", Timestamp.valueOf("2024-12-10 06:26:59"), 7);
        Post post4 = new Post("Test post content 4", Timestamp.valueOf("2024-12-10 06:26:59"), 8);

        post1.addReplyUnderPost(post2);
        post1.addReplyUnderPost(post3);
        post1.addReplyUnderPost(post4);

        post2.addReplyUnderPost(post4);
        post2.addReplyUnderPost(post3);

        s.add(post1);
        s.add(post2);
        s.add(post3);
        s.add(post4);
        try {
            s.persistAll();
        } catch (IllegalArgumentException | IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // System.out.println("LOADING post 1");
        Post toLoadPost1 = new Post(1);
        Post loadedPost1 = (Post) s.load(toLoadPost1);
        assertEquals(1, loadedPost1.getPostId());
        assertEquals("Test post content 1", loadedPost1.getPostContent());
        assertEquals(5, loadedPost1.getWordCount());
        assertEquals(3, loadedPost1.getReplies().size());
        assertEquals(2, loadedPost1.getReplies().get(0).getPostId());
        assertEquals("other test heh content hehaef test", loadedPost1.getReplies().get(0).getPostContent());
        assertEquals(6, loadedPost1.getReplies().get(0).getWordCount());
        assertEquals(4, loadedPost1.getReplies().get(1).getPostId());
        assertEquals(3, loadedPost1.getReplies().get(2).getPostId());

        // System.out.println("LOADING post 2");
        Post toLoadPost2 = new Post(2);
        Post loadedPost2 = (Post) s.load(toLoadPost2);
        assertEquals(2, loadedPost2.getPostId());
        assertEquals("other test heh content hehaef test", loadedPost2.getPostContent());
        assertEquals(6, loadedPost2.getWordCount());
        assertEquals(2, loadedPost2.getReplies().size());
        assertEquals(3, loadedPost2.getReplies().get(0).getPostId());
        assertEquals("Test post content 4", loadedPost2.getReplies().get(0).getPostContent());
        assertEquals(4, loadedPost2.getReplies().get(1).getPostId());
        assertEquals("Test post content 3", loadedPost2.getReplies().get(1).getPostContent());
        assertEquals(7, loadedPost2.getReplies().get(1).getWordCount());
        Post toLoadPost3 = new Post(4);
        Post loadedPost3 = (Post) s.load(toLoadPost3);
        assertEquals(4, loadedPost3.getPostId());
        assertEquals("Test post content 3", loadedPost3.getPostContent());
        assertEquals(7, loadedPost3.getWordCount());
        assertEquals(0, loadedPost3.getReplies().size());

        Post toLoadPost4 = new Post(3);
        Post loadedPost4 = (Post) s.load(toLoadPost4);
        assertEquals(3, loadedPost4.getPostId());
        assertEquals("Test post content 4", loadedPost4.getPostContent());
        assertEquals(8, loadedPost4.getWordCount());
        assertEquals(0, loadedPost4.getReplies().size());
    }
}
