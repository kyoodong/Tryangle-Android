package com.gomson.tryangle.domain;

public class Image {

    private long id;
    private String url;
    private String author;
    private int compositionProblemCount;
    private int score;

    public Image() {
    }

    public Image(long id, String url, String author, int compositionProblemCount, int score) {
        this.id = id;
        this.url = url;
        this.author = author;
        this.compositionProblemCount = compositionProblemCount;
        this.score = score;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public int getCompositionProblemCount() {
        return compositionProblemCount;
    }

    public void setCompositionProblemCount(int compositionProblemCount) {
        this.compositionProblemCount = compositionProblemCount;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
