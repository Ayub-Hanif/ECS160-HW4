package com.ecs160.hw4;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * A Visitor class that calculates the average duration in seconds
 * between pairs of consecutive posts. visit(Post post) method,
 * this visitor is designed to handle two posts at a time—such as a parent post
 * and its immediate reply by computing the time difference between them.
 */


public class AverageDurationVisitor {
    private int total_duration = 0;
    private int total_posts = 0;

    public void visit(SocialComposite prevPost, SocialComposite nextPost) {
        Instant prev_instant = prevPost.get_creation_time().toInstant();
        Instant next_instant = nextPost.get_creation_time().toInstant();
        total_duration += ChronoUnit.SECONDS.between(prev_instant, next_instant);
        total_posts++;
    }

    public double getAverageDuration() {
        if (total_posts == 0 || total_duration == 0) {
            return 0;
        }
        return (double) total_duration / total_posts;
    }
}
