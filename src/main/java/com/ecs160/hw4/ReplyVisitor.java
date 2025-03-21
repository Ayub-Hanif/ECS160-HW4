package com.ecs160.hw4;

/**
 * This class implements the SocialVisitor interface and is used to visit
 * posts and calculate the average number of replies for each post.
 * 
 * The visitor takes in a boolean weighted which determines if the average
 * is weighted by the word count of the replies.
 */

public class ReplyVisitor implements SocialVisitor {
    private int totalTopLevelPosts = 0;
    private int totalImmediateReplies = 0;
    private double totalWeightedReplies = 0;

    private boolean weighted;
    private int longestPostWordCount = -1;

    public ReplyVisitor(boolean weighted) {
        this.weighted = weighted;
    }

    @Override
    public void visit(Post post) {
        totalTopLevelPosts++;
        int directReplyCount = post.get_post_replies().size();
        totalImmediateReplies += directReplyCount;

        if (weighted) {
            if (longestPostWordCount == -1) {
                longestPostWordCount = Math.max(1, post.get_word_count());
            }
            double weightedSum = 0;
            for (SocialComposite child : post.get_post_replies()) {
                Post replyPost = (Post) child;
                weightedSum += 1 + ((double) replyPost.get_word_count() / longestPostWordCount);
            }
            totalWeightedReplies += weightedSum;
        }
    }

    public double getAverageReplies() {
        if (totalTopLevelPosts == 0) {
            return 0;
        }
        if (weighted) {
            return totalWeightedReplies / totalTopLevelPosts;
        }
        return (double) totalImmediateReplies / totalTopLevelPosts;
    }
}