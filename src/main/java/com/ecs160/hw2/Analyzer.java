package com.ecs160.hw2;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

class SortByTimestamp implements Comparator<Post> {
    public int compare(Post o1, Post o2) {
        return o1.get_creation_time().compareTo(o2.get_creation_time());
    }
}

class SortByWordCount implements Comparator<Post> {
    public int compare(Post o1, Post o2) {
        return o1.get_word_count() - o2.get_word_count();
    }
}

public class Analyzer {
    private final List<Post> posts;
    private int longest_post_word_count;

    public Analyzer(List<Post> posts)  {
        this.posts = posts;
        this.longest_post_word_count = -1; 
    }

    public int count_total_posts() {
        return posts.size();
    }

    /**
     * Calculates the average number of replies. Weighted or unweighted.
     * Because we ignore deeper replies-of-replies, this is just immediate children.
     */
    public double calc_avg_replies(boolean weighted) {
        if (this.count_total_posts() == 0) {
            return 0;
        }
        if (!weighted) {
            int total_post_replies = 0;
            for (Post p : posts) {
                total_post_replies += p.get_post_replies().size();
            }
            return (double) total_post_replies / this.count_total_posts();
        } else {
            if (this.longest_post_word_count == -1) {
                // find largest by word count
                this.longest_post_word_count = Collections.max(posts, new SortByWordCount()).get_word_count();
            }
            double total_post_weights = 0;
            for (Post p : posts) {
                double current_post_weight = 0;
                for (Post reply : p.get_post_replies()) {
                    current_post_weight += 1 + ((((double) reply.get_word_count()) / this.longest_post_word_count));
                }
                total_post_weights += current_post_weight;
            }
            return total_post_weights / this.count_total_posts();
        }
    }

    /**
     * Calculates the average time difference between consecutive “comments” 
     * (the post and its replies) in seconds. 
     */
    public double calc_avg_duration() {
        if (this.count_total_posts() == 0) {
            return 0;
        }

        long total_duration = 0;
        int num_intervals = 0;
        for (Post p : posts) {
            // immediate replies, plus parent as first
            // ignoring deeper reply-of-reply
            List<Post> allComments = p.get_post_replies();
            // in old code we addFirst(p), but let's do it with a local list:
            // must put p in the front
            // You can just do something simpler, or replicate your old logic:

            // Build a mini-list [p, r1, r2, ...] sorted by timestamp
            // If there's only the parent, there are no intervals.
            // If 1 parent + 2 replies => 2 intervals to measure.
            // etc.
            List<Post> miniList = new ArrayList<>(allComments);
            miniList.add(p);
            miniList.sort(new SortByTimestamp());
            if (!miniList.isEmpty() && miniList.get(0).hashCode() != p.hashCode()) {
                // the parent isn't the earliest, skip 
                continue;
            }
            for (int i = 0; i < miniList.size() - 1; i++) {
                Instant current = miniList.get(i + 1).get_creation_time().toInstant();
                Instant prev    = miniList.get(i).get_creation_time().toInstant();
                total_duration += ChronoUnit.SECONDS.between(prev, current);
                num_intervals++;
            }
        }
        if (num_intervals == 0) return 0;
        return (double) total_duration / num_intervals;
    }

    // Return a human-readable string of HH:MM:SS
    public String get_format_duration() {
        double avg_duration = calc_avg_duration();
        Duration duration = Duration.ofSeconds((long) avg_duration);
        return LocalTime.ofSecondOfDay(duration.getSeconds()).toString();
    }
}
