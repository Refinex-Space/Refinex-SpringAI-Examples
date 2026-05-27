package cn.refinex.rag.splitter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("OverlapParagraphTextSplitter")
class OverlapParagraphTextSplitterTest {

    @Test
    @DisplayName("should split text by chunk size with overlap")
    void shouldSplitTextByChunkSizeWithOverlap() {
        OverlapParagraphTextSplitter splitter = new OverlapParagraphTextSplitter(5, 2);

        List<String> chunks = splitter.splitText("abcdefg");

        assertEquals(List.of("abcde", "de\nfg", "fg"), chunks);
    }

    @Test
    @DisplayName("should preserve paragraph break when merging paragraphs")
    void shouldPreserveParagraphBreakWhenMergingParagraphs() {
        OverlapParagraphTextSplitter splitter = new OverlapParagraphTextSplitter(20, 0);

        List<String> chunks = splitter.splitText("第一段\n\n第二段");

        assertEquals(List.of("第一段\n第二段"), chunks);
    }

    @Test
    @DisplayName("should reject invalid overlap")
    void shouldRejectInvalidOverlap() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new OverlapParagraphTextSplitter(5, 5));

        assertEquals("overlap 不能大于等于 chunkSize", exception.getMessage());
    }

    @Test
    @DisplayName("should split documents and keep chunk text")
    void shouldSplitDocumentsAndKeepChunkText() {
        OverlapParagraphTextSplitter splitter = new OverlapParagraphTextSplitter(4, 1);

        List<Document> documents = splitter.apply(List.of(new Document("abcde")));

        assertEquals(List.of("abcd", "d\ne"), documents.stream().map(Document::getText).toList());
    }
}
