package com.ecs160.hw2;

import java.util.List;

import com.ecs160.persistence.Session;

public class Main {
    public static void main(String[] args) {
        // System.out.println("Hello, World!");
        Session s = new Session();
        // parse the json file
        JsonParserFile parser = new JsonParserFile();
        List<Post> posts = parser.json_parser("input.json");
        // add the posts to the session
        for (Post post : posts) {
            s.add(post);
        }
        // persist the posts
        try {
            s.persistAll();
        } catch (IllegalArgumentException | IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        // prompt the user for the id of the post to load
        System.out.println(posts.size() + " top-level posts parsed.");
        System.out.println("Enter the id of the post to load: ");
        int id = Integer.parseInt(System.console().readLine());
        // load the post
        Post loadedPost = (Post) s.load(new Post(id));

        // print the post
        System.out.println("---POST---");
        System.out.println(loadedPost);
        // print the replies
        // ask the user if they want to load the replies. They can say "yes" by pressing
        System.out.println("Do you want to load the replies? Press Enter to continue.");
        try {
            System.in.read();
        } catch (Exception e) {
        }
        System.out.println("---REPLIES---");
        for (Post reply : loadedPost.get_post_replies()) {
            // indent the replies to distinguish them from the post
            System.out.println(reply);
        }
    }
}
