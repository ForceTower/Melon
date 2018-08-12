package com.forcetower.sagres.database.model;

import com.google.gson.annotations.SerializedName;

public class Linker {
    @SerializedName(value = "$link")
    private Link link;

    public Linker(Link link) {
        this.link = link;
    }

    public String getLink() {
        return link.getHref();
    }

    public void setLink(Link link) {
        this.link = link;
    }

    private static class Link {
        private String href;

        public Link(String href) {
            this.href = href;
        }

        public String getHref() {
            return href;
        }

        public void setHref(String href) {
            this.href = href;
        }
    }
}
