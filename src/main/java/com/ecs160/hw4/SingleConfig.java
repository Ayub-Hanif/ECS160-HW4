package com.ecs160.hw4;

/**
 * Singleton class to hold configuration options for the SocialAnalyzer.
 * We want exactly one instance of our configuration class to exist.
 * We make sure it is globally available to other classes such as our Analyzer
 * or your Visitors.
 */
public class SingleConfig {
    private static SingleConfig instance;
    private boolean weighted;
    private String jsonFilePath;

    private SingleConfig() {
    }

    public static SingleConfig getInstance() {
        if (instance == null) {
            synchronized (SingleConfig.class) {
                if (instance == null) {
                    instance = new SingleConfig();
                }
            }
        }
        return instance;
    }

    public boolean isWeighted() {
        return weighted;
    }

    public void setWeighted(boolean weighted) {
        this.weighted = weighted;
    }

    public String getJsonFilePath() {
        return jsonFilePath;
    }

    public void setJsonFilePath(String jsonFilePath) {
        this.jsonFilePath = jsonFilePath;
    }
}
