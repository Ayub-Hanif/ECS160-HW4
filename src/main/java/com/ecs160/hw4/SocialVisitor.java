package com.ecs160.hw4;

/**
 * A easy interface for the Visitor pattern defining a single operation
 * that can be performed on a Post object. Concrete Visitor implementations
 * like CountingVisitor, ReplyVisitor. provide specialized behavior
 * when visiting a Post, allowing analytics and operations to be added
 * without modifying the Post class itself.
 */

public interface SocialVisitor {
    void visit(Post post);
}
