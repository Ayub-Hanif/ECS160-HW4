package com.ecs160.hw2;

import redis.clients.jedis.Jedis;

import java.sql.Timestamp;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * Redis-based replacement for the old PostgreSQL Database class.
 */
public class Database {
    private final String sql_name;      // these fields are now just placeholders
    private final String sql_user;      // since we no longer need them for Redis
    private final String sql_password;
    
    private Jedis jedis;   // our Redis client

    public Database(String sql_name, String sql_user, String sql_password) {
        this.sql_name = sql_name;
        this.sql_user = sql_user;
        this.sql_password = sql_password;
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
        // no-op for Redis
        // System.out.println("No table creation needed in Redis.");
    }

    public String get_db_name() {
        return sql_name;
    }

    public String get_db_user() {
        return sql_user;
    }

    public String get_db_password() {
        return sql_password;
    }

    // Check if a post with this ID already exists in Redis
    private boolean post_table_exists(int input_post_Id) {
        // We will store each post in a Redis hash named "post:<ID>"
        return jedis.exists("post:" + input_post_Id);
    }

    // Clear out all Redis data for a clean start (like "DELETE FROM posts;")
    public void free_table() {
        jedis.flushAll();
    }

    /**
     * Insert a post into Redis, along with its immediate replies.
     * Because we are ignoring "replies-of-replies," we only store one level deep.
     */
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
        // If parent is null, we store "NULL" or something
        fields.put("parent_post_id", parent_post_Id == null ? "NULL" : String.valueOf(parent_post_Id));
        jedis.hset(key, fields);

        // If parent is null, then this is top-level. We'll add its ID to a set of top-level posts
        if (parent_post_Id == null) {
            jedis.sadd("topLevelPosts", String.valueOf(post.get_post_Id()));
        } else {
            // Add this post's ID to the parent's set of replies
            jedis.sadd("post:" + parent_post_Id + ":replies", String.valueOf(post.get_post_Id()));
        }

        // In the old version, we recursively inserted each reply's replies. Now ignoring 
        // deeper replies-of-replies, so only store the immediate children:
        for (Post reply : post.get_post_replies()) {
            // Insert each immediate reply (but do not recurse further).
            // That means reply-of-reply is not stored.
            insert_post(reply, post.get_post_Id());
        }
    }

    /**
     * Retrieve top-level posts + their immediate replies from Redis.
     */
    public List<Post> get_posts_db() {
        List<Post> post_list = new ArrayList<>();
        // get all the top-level IDs
        Set<String> topIds = jedis.smembers("topLevelPosts");
        if (topIds == null) {
            return post_list;
        }
        for (String topIdStr : topIds) {
            int topId = Integer.parseInt(topIdStr);
            Post topPost = fetchPost(topId);
            if (topPost == null) continue;

            // Now fetch immediate children
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

    /**
     * Helper to fetch a single Post from Redis by ID (ignoring any deeper replies).
     */
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

        // Build a Post object. 
        return new Post(postId, content, creationTime, wc);
    }

    public void close() {
        if (jedis != null) {
            jedis.close();
        }
    }
}
