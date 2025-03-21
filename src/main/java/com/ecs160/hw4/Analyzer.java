package com.ecs160.hw4;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/**
 * This class for performing various social-media analytics
 * (post counts, average replies, and average reply duration) on a list
 * of Post objects. It applies different Visitors (CountingVisitor,
 * ReplyVisitor, AverageDurationVisitor) to traverse the Composite structure of
 * posts. Each Visitor focuses on a particular statistic keeping the logic
 * separate from the Post class itself.
 */

class SortByTimestamp implements Comparator<SocialComposite> {
    public int compare(SocialComposite o1, SocialComposite o2) {
        return o1.get_creation_time().compareTo(o2.get_creation_time());
    }
}

class SortByWordCount implements Comparator<SocialComposite> {
    public int compare(SocialComposite o1, SocialComposite o2) {
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
        CountingVisitor countingVisitor = new CountingVisitor();
        for (SocialComposite c : posts) {
            countingVisitor.visit((Post) c);
        }
        return countingVisitor.getCount();
    }

    public double calc_avg_replies(boolean weighted) {
        ReplyVisitor replyVisitor = new ReplyVisitor(weighted);
        for (SocialComposite c : posts) {
            replyVisitor.visit((Post) c);
        }
        return replyVisitor.getAverageReplies();
    }

    public double calc_avg_duration() {

        AverageDurationVisitor avg_duration_visitor = new AverageDurationVisitor();
        for (SocialComposite p : posts) {
            List<SocialComposite> replies_and_post = p.get_post_replies();
            replies_and_post.addFirst(p);
            replies_and_post.sort(new SortByTimestamp());

            for (int i = 0; i < replies_and_post.size() - 1; i++) {
                avg_duration_visitor.visit(replies_and_post.get(i), replies_and_post.get(i + 1));
            }

            replies_and_post.removeFirst();
        }
        return avg_duration_visitor.getAverageDuration();
    }

    // Return a human-readable string of HH:MM:SS
    public String get_format_duration() {
        double avg_duration = calc_avg_duration();
        Duration duration = Duration.ofSeconds((long) avg_duration);
        return LocalTime.ofSecondOfDay(duration.getSeconds()).toString();
    }

}
