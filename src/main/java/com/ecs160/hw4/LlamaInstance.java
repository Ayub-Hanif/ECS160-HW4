package com.ecs160.hw4;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.response.OllamaResult;
import io.github.ollama4j.utils.OptionsBuilder;

/**
 * This class uses the Ollama API to generate a hashtag for a given social media
 * post.
 * It uses the generate method to generate a hashtag for a given post.
 */

public class LlamaInstance {
    private static final String OLLAMA_API_URL = "http://localhost:11434/";

    public String generateHashtag(String content) {
        try {
            return generateHashtagWithOllama(content);
        } catch (Exception e) {
            e.printStackTrace();
            return "#bskypost";
        }
    }

    private String generateHashtagWithOllama(String content) {
        try {
            String prompt = "Return only one concise hashtag (just the hashtag) for this social media post: " + content;
            OllamaAPI ollamaAPI = new OllamaAPI(OLLAMA_API_URL);
            OllamaResult result = ollamaAPI.generate("llama3.2", prompt,
                    false,
                    new OptionsBuilder().build());
            String output = result.getResponse().trim();
            Pattern pattern = Pattern.compile("#\\S+");
            Matcher matcher = pattern.matcher(output);
            if (matcher.find()) {
                return matcher.group();
            }
            return "#bskypost";
        } catch (Exception e) {
            e.printStackTrace();
            return "#bskypost";
        }
    }

}
