package cn.refinex.rag.splitter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("MarkdownHeaderTextSplitter")
class MarkdownHeaderTextSplitterTest {

    @Test
    @DisplayName("should aggregate chunks with header metadata and parent relation")
    void shouldAggregateChunksWithHeaderMetadataAndParentRelation() {
        MarkdownHeaderTextSplitter splitter = new MarkdownHeaderTextSplitter(headers(), false, false, true);
        Document source = new Document("""
                # Root
                Intro
                ## Child
                Child body
                ### Grand
                Grand body
                """, Map.of("source", "unit-test"));

        List<Document> documents = splitter.apply(List.of(source));

        assertEquals(3, documents.size());
        Map<String, Object> rootMetadata = documents.get(0).getMetadata();
        Map<String, Object> childMetadata = documents.get(1).getMetadata();
        Map<String, Object> grandMetadata = documents.get(2).getMetadata();
        assertAll("header metadata",
                () -> assertEquals("# Root\nIntro", documents.get(0).getText()),
                () -> assertEquals("## Child\nChild body", documents.get(1).getText()),
                () -> assertEquals("### Grand\nGrand body", documents.get(2).getText()),
                () -> assertEquals("Root", rootMetadata.get("h1")),
                () -> assertEquals("Child", childMetadata.get("h2")),
                () -> assertEquals("Grand", grandMetadata.get("h3")),
                () -> assertEquals(rootMetadata.get("chunkId"), childMetadata.get("parentChunkId")),
                () -> assertEquals(childMetadata.get("chunkId"), grandMetadata.get("parentChunkId")),
                () -> assertNotNull(rootMetadata.get("chunkId")));
    }

    @Test
    @DisplayName("should ignore markdown headers inside fenced code block")
    void shouldIgnoreMarkdownHeadersInsideFencedCodeBlock() {
        MarkdownHeaderTextSplitter splitter = new MarkdownHeaderTextSplitter(headers(), false, true, false);
        Document source = new Document("""
                # Root
                ```java
                ## not a heading
                ```
                After fence
                """, Map.of("source", "unit-test"));

        List<Document> documents = splitter.apply(List.of(source));

        assertEquals(1, documents.size());
        assertAll("code fence content",
                () -> assertEquals("```java\n## not a heading\n```\nAfter fence", documents.getFirst().getText()),
                () -> assertEquals("Root", documents.getFirst().getMetadata().get("h1")),
                () -> assertFalse(documents.getFirst().getMetadata().containsKey("h2")));
    }

    @Test
    @DisplayName("should return empty list when split text is null")
    void shouldReturnEmptyListWhenSplitTextIsNull() {
        MarkdownHeaderTextSplitter splitter = new MarkdownHeaderTextSplitter(headers(), false, true, false);

        List<String> chunks = splitter.splitText(null);

        assertEquals(List.of(), chunks);
    }

    @Test
    @DisplayName("should split with empty metadata when base metadata is null")
    void shouldSplitWithEmptyMetadataWhenBaseMetadataIsNull() throws Exception {
        MarkdownHeaderTextSplitter splitter = new MarkdownHeaderTextSplitter(headers(), false, true, false);
        Method splitWithMetadata = MarkdownHeaderTextSplitter.class.getDeclaredMethod(
                "splitWithMetadata",
                String.class,
                Map.class);
        splitWithMetadata.setAccessible(true);

        assertDoesNotThrow(() -> splitWithMetadata.invoke(splitter, "# Root\nBody", null));
    }

    private static Map<String, String> headers() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("#", "h1");
        headers.put("##", "h2");
        headers.put("###", "h3");
        return headers;
    }
}
