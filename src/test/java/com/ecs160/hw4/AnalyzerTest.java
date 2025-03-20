package com.ecs160.hw4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class AnalyzerTest {
    static Post createPostWithString(String timestampString, int word_count) {

        return new Post(0, "", Timestamp.from(Instant.parse(timestampString)), word_count);
    }

    static Post createPostWithTimestamp(int timestampSeconds, int word_count) {
        return new Post(0, "", Timestamp.from(Instant.ofEpochSecond(timestampSeconds)), word_count);
    }

    static void assertBetween(double actual, double lower, double upper) {
        assertTrue(actual <= upper);
        assertTrue(actual >= lower);
    }

    static void assertWithinEpsilon(double actual, double expected, double epsilon) {
        assertBetween(actual, expected - epsilon, expected + epsilon);
    }

    @Test
    void testNoPosts() {
        List<Post> posts = new ArrayList<>();
        Analyzer analyzer = new Analyzer(posts);
        assertEquals(0, analyzer.calc_avg_duration());
        assertEquals(0, analyzer.calc_avg_replies(false));
        assertEquals(0, analyzer.calc_avg_replies(true));
        assertEquals(0, analyzer.count_total_posts());
    }

    @Test
    void testSinglePost() {
        Post p = createPostWithString("2024-12-10T06:26:59.479Z", 100);
        List<Post> posts = new ArrayList<>();
        posts.add(p);
        Analyzer analyzer = new Analyzer(posts);
        assertEquals(0, analyzer.calc_avg_duration());
        assertEquals(0, analyzer.calc_avg_replies(false));
        assertEquals(0, analyzer.calc_avg_replies(true));
        assertEquals(1, analyzer.count_total_posts());
    }

    @Test
    void testSinglePostWithOneReply() {
        int count_1 = 200;
        Post post = createPostWithString("2023-11-10T06:26:59.479Z", count_1 * 2);
        Post reply = createPostWithString("2024-12-10T06:26:59.479Z", count_1);
        post.add_reply_under_post(reply);
        List<Post> posts = new ArrayList<>();
        posts.add(post);
        Analyzer analyzer = new Analyzer(posts);
        assertEquals(1, analyzer.count_total_posts());
        assertEquals(34214400, analyzer.calc_avg_duration());
        assertEquals(1, analyzer.calc_avg_replies(false));
        // weight of the one reply is = 1 + (200 / 400) = 2.
        // this is only post, so the average weight is 2 / 1 post = 2.
        assertEquals(1.5, analyzer.calc_avg_replies(true));
    }

    @Test
    void testMultiplePosts() {

        // Post 1 (3 replies)
        Post post1 = createPostWithTimestamp(1830603795, 291);
        Post reply11 = createPostWithTimestamp(1907742645, 482);
        Post reply12 = createPostWithTimestamp(1958223010, 123);
        Post reply13 = createPostWithTimestamp(2072632530, 789);

        // Post 2 (2 replies)
        Post post2 = createPostWithTimestamp(1751909739, 202);
        Post reply21 = createPostWithTimestamp(1861355420, 345);
        Post reply22 = createPostWithTimestamp(1912956055, 678);

        // Post 3 (1 reply)
        Post post3 = createPostWithTimestamp(1792403867, 0);
        Post reply31 = createPostWithTimestamp(1974795950, 111);

        post1.add_reply_under_post(reply11);
        post1.add_reply_under_post(reply12);
        post1.add_reply_under_post(reply13);
        post2.add_reply_under_post(reply21);
        post2.add_reply_under_post(reply22);
        post3.add_reply_under_post(reply31);

        List<Post> posts = new ArrayList<>();
        posts.add(post1);
        posts.add(post2);
        posts.add(post3);
        Analyzer analyzer = new Analyzer(posts);
        double epsilon = 0.001;
        assertEquals(3, analyzer.count_total_posts());
        assertWithinEpsilon(analyzer.calc_avg_duration(), 97577855.66666670, epsilon);
        assertWithinEpsilon(analyzer.calc_avg_replies(false), (3 + 2 + 1.) / 3, epsilon);
        assertWithinEpsilon(analyzer.calc_avg_replies(true), 4.89576174112257, epsilon);
    }

}
