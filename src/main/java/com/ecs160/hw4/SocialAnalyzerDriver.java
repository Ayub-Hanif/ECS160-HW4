package com.ecs160.hw4;

import java.util.Comparator;
import java.util.List;

public class SocialAnalyzerDriver {
    public static void main(String[] args) {

        SingleConfig config = SingleConfig.getInstance();
        config.setWeighted(false);
        config.setJsonFilePath("input.json");

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--weighted") && i + 1 < args.length && args[i + 1].equals("true")) {
                config.setWeighted(true);
            }
            if (args[i].equals("--file") && i + 1 < args.length) {
                config.setJsonFilePath(args[i + 1]);
            }
        }

        Database data_base = new Database();
        init_db(data_base, config.getJsonFilePath());

        List<Post> post_list = data_base.get_posts_db();
        post_list.sort(Comparator.comparing(Post::get_like_count).reversed());

        Analyzer analyzer = new Analyzer(post_list);
        System.out.println("\n--------------------------------- Stats --------------------------------\n");
        System.out.println("Total posts: " + analyzer.count_total_posts());
        System.out.println("Average number of replies: " + analyzer.calc_avg_replies(config.isWeighted()));
        System.out.println("Average duration between replies: " + analyzer.get_format_duration());
        System.out.println("--------------------------------- top 10 posts --------------------------------\n");

        // Process top 10 posts
        List<Post> topPosts = post_list.subList(0, Math.min(10, post_list.size()));
        int z = 1;
        for (Post post : topPosts) {
            if(z != 1) {
                System.out.println("\n--------------------------------- next post --------------------------------\n");
            }
            HashtagDecorator hashtagDecorator = new HashtagDecorator(post);
            hashtagDecorator.setHashtag();

            System.out.println( "POST #"+ (z++) + ": "+ hashtagDecorator.get_post_content() + " " + hashtagDecorator.getHashtag() + "\n");

             List<SocialComposite> replies = hashtagDecorator.get_post_replies();
            for (int i = 0; i < Math.min(2, replies.size()); i++) {
                hashtagDecorator.setReplyHashtag(i);

                String childContent = replies.get(i).get_post_content();
                String childHashtag = hashtagDecorator.getReplyHashtag(i);

                System.out.println("Reply #"+ (i+1) +"--> "+ childContent + " " + childHashtag);
            }
            
        }
        System.out.println("--------------------------------- END!!! --------------------------------\n");
    }

    private static void init_db(Database data_base, String filePath) {
        data_base.free_table();
        try {
            JsonParserFile parser = new JsonParserFile();
            List<Post> posts_from_input = parser.json_parser(filePath);

            for (Post post : posts_from_input) {
                data_base.insert_post(post, null);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            data_base.close();
        }
    }
}
