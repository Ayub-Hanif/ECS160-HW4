package com.ecs160.hw4;

/**
 * A simple Visitor implementation that counts the total number of posts visited.
 * Each time visit(Post post) is invoked the post count is incremented.
 */

public class CountingVisitor implements SocialVisitor {
    private int total_post_count = 0;

    @Override
    public void visit(Post post) {
        total_post_count++;
    }

    public int getCount() {
        return total_post_count;
    }
    
}
