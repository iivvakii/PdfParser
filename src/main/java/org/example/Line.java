package org.example;

import java.util.List;

class Line {
    double y;
    List<TextChunk> chunks;

    Line(double y, List<TextChunk> chunks) {
        this.y = y;
        this.chunks = chunks;
    }
}