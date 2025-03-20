package com.ecs160.hw2;

import redis.clients.jedis.Jedis;

import java.sql.Timestamp;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis-based Database class without any username/password fields.
 */
public class Database {
    private Jedis jedis;   // our Redis client

    public Database() {
        connection();
        create_db_table();
    }

    // In Redis, "connection" is just creating a new Jedis instance.
    public void connection() {
        try {
            // By default, connect to localhost:6379 (adjust if needed).
            jedis = new Jedis("localhost", 6379);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to connect to Redis");
        }
    }

    // No actual table creation needed for Redis, so either do nothing or log it.
    public void create_db_table() {
    }

    private boolean post_table_exists(int input_post_Id) {
        return jedis.exists("post:" + input_post_Id);
    }

    public void free_table() {
        jedis.flushAll();
    }


    public void insert_post(Post post, Integer parent_post_Id) {
        // If the post already exists, skip
        if (post_table_exists(post.get_post_Id())) {
            return;
        }

        // Store this post in a hash "post:<id>"
        String key = "post:" + post.get_post_Id();
        Map<String, String> fields = new HashMap<>();
        fields.put("post_content", post.get_post_content());

        // store creation time as a string (millis) to parse later
        fields.put("creation_time", String.valueOf(post.get_creation_time().getTime()));
        fields.put("word_count", String.valueOf(post.get_word_count()));

        // If parent is null, store "NULL" or something
        fields.put("parent_post_id", parent_post_Id == null ? "NULL" : String.valueOf(parent_post_Id));
        jedis.hset(key, fields);

        // If parent is null, this is top-level. Add its ID to "topLevelPosts"
        if (parent_post_Id == null) {
            jedis.sadd("topLevelPosts", String.valueOf(post.get_post_Id()));
        } else {
            // Add this post's ID to the parent's set of replies
            jedis.sadd("post:" + parent_post_Id + ":replies", String.valueOf(post.get_post_Id()));
        }

        // Only store immediate replies (one level)
        for (Post reply : post.get_post_replies()) {
            insert_post(reply, post.get_post_Id());
        }
    }


    public List<Post> get_posts_db() {
        List<Post> post_list = new ArrayList<>();
        Set<String> topIds = jedis.smembers("topLevelPosts");
        if (topIds == null) {
            return post_list;
        }
        for (String topIdStr : topIds) {
            int topId = Integer.parseInt(topIdStr);
            Post topPost = fetchPost(topId);
            if (topPost == null) continue;

            // fetch immediate children
            Set<String> childIds = jedis.smembers("post:" + topId + ":replies");
            if (childIds != null) {
                for (String childIdStr : childIds) {
                    int childId = Integer.parseInt(childIdStr);
                    Post childPost = fetchPost(childId);
                    if (childPost != null) {
                        topPost.add_reply_under_post(childPost);
                    }
                }
            }
            post_list.add(topPost);
        }
        return post_list;
    }

    private Post fetchPost(int postId) {
        String key = "post:" + postId;
        if (!jedis.exists(key)) return null;

        Map<String,String> fields = jedis.hgetAll(key);
        if (fields == null || fields.isEmpty()) return null;

        String content = fields.getOrDefault("post_content", "");
        String timeStr = fields.getOrDefault("creation_time", "0");
        String wordCountStr = fields.getOrDefault("word_count", "0");

        long timeMillis = Long.parseLong(timeStr);
        Timestamp creationTime = new Timestamp(timeMillis);

        int wc = Integer.parseInt(wordCountStr);

        return new Post(postId, content, creationTime, wc);
    }

    public void close() {
        if (jedis != null) {
            jedis.close();
        }
    }
}
