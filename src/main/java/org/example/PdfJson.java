package org.example;

import java.util.ArrayList;
import java.util.List;

class PdfJson {
    List<Page> pages = new ArrayList<>();

    static class Page {
        int pageNumber;
        List<String> lines = new ArrayList<>();

        Page(int pageNumber) {
            this.pageNumber = pageNumber;
        }
    }
}