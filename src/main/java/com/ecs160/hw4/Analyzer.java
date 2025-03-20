package com.ecs160.hw4;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

    public Analyzer(List<Post> posts) {
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

    public double calc_avg_duration() {
        if (this.count_total_posts() == 0) {
            return 0;
        }
        long total_duration = 0;
        if (posts.isEmpty()) {
            return 0;
        } else if (posts.size() == 1) {
            if (posts.getFirst().get_post_replies().isEmpty()) {
                return 0;
            }
        }
        int num_intervals = 0;
        for (Post p : posts) {
            List<Post> replies_and_post = p.get_post_replies();
            replies_and_post.addFirst(p);
            replies_and_post.sort(new SortByTimestamp());
            if (p.hashCode() != replies_and_post.getFirst().hashCode()) {
                continue;
            }
            for (int i = 0; i < replies_and_post.size() - 1; i++) {
                Instant currentInstant = replies_and_post.get(i + 1).get_creation_time().toInstant();
                Instant previousInstant = replies_and_post.get(i).get_creation_time().toInstant();
                total_duration += ChronoUnit.SECONDS.between(previousInstant, currentInstant);
                num_intervals += 1;
            }
            replies_and_post.removeFirst();
        }
        return (double) total_duration / num_intervals;
    }

    // Return a human-readable string of HH:MM:SS
    public String get_format_duration() {
        double avg_duration = calc_avg_duration();
        Duration duration = Duration.ofSeconds((long) avg_duration);
        return LocalTime.ofSecondOfDay(duration.getSeconds()).toString();
    }
}
