package de.citytwin.catalog;

/**
 * this class present a simple wiki dump article
 *
 * @author Maik Siegmund, FH Erfurt
 */
public class WikiArticle {

    private int id = 0;
    private int revid = 0;
    private String url = "";
    private String title = "";
    private String text = "";

    /**
     * constructor.
     */
    public WikiArticle() {
        id = 0;
        revid = 0;
        url = "";
        title = "";
        text = "";
    }

    /**
     * constructor.
     *
     * @param isCore
     * @param morphem
     * @param term
     * @param ontologies
     */
    public WikiArticle(int id, int revid, final String url, final String title, final String text) {
        this.id = id;
        this.revid = revid;
        this.url = url;
        this.title = title;
        this.text = text;
    }

    public int getId() {
        return id;
    }

    public int getRevid() {
        return revid;
    }

    public String getText() {
        return text;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setRevid(int revid) {
        this.revid = revid;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String toString() {
        return "WikiDumpArticle [id=" + id + ", revid=" + revid + ", url=" + url + ", title=" + title + ", text=" + text + "]";
    }

}
