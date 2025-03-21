package com.ecs160.hw4;

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
        Analyzer analyzer = new Analyzer(post_list);
        System.out.println("Total posts: " + analyzer.count_total_posts());
        System.out.println("Average number of replies: " + analyzer.calc_avg_replies(config.isWeighted()));
        System.out.println("Average duration between replies: " + analyzer.get_format_duration());

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
