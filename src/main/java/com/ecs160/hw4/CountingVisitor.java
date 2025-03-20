package com.ecs160.hw4;

public class CountingVisitor implements SocialVisitor {
    private int total_post_count = 0;

    public void visit(Post post) {
        total_post_count++;
    }

    public int getCount() {
        return total_post_count;
    }
    
}
