package com.ecs160.hw2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

class JsonParserFileTest {

    @Test
    void test_json_parser_file() {
        JsonParserFile parser = new JsonParserFile();
        List<Post> posts = parser.json_parser("test_input.json");

        // checks the number of posts parsed right
        assertNotNull(posts);
        // 1 posts for now
        assertEquals(1, posts.size());

        // checks the content of the post
        Post firstPost = posts.getFirst();
        assertEquals("Which feeds includes posts marked with #atproto?", firstPost.get_post_content());
        assertEquals(0, firstPost.get_post_replies().size());
    }
}
