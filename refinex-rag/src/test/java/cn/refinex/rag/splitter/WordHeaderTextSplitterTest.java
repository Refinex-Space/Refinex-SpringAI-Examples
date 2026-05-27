package cn.refinex.rag.splitter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("WordHeaderTextSplitter")
class WordHeaderTextSplitterTest {

    @Test
    @DisplayName("should fallback to plain text when word input stream is absent")
    void shouldFallbackToPlainTextWhenWordInputStreamIsAbsent() {
        WordHeaderTextSplitter splitter = new WordHeaderTextSplitter(List.of(1, 2, 3), false, false, false, 10, 0);
        Document source = new Document("纯文本内容", Map.of("source", "plain"));

        List<Document> documents = splitter.apply(List.of(source));

        assertEquals(1, documents.size());
        assertEquals("纯文本内容", documents.getFirst().getText());
        assertEquals("plain", documents.getFirst().getMetadata().get("source"));
    }

    @Test
    @DisplayName("should detect heading level by text pattern")
    void shouldDetectHeadingLevelByTextPattern() throws Exception {
        WordHeaderTextSplitter splitter = new WordHeaderTextSplitter(List.of(1, 2, 3, 4), false, false, false, 10, 0);
        Method method = WordHeaderTextSplitter.class.getDeclaredMethod("detectHeadingByTextPattern", String.class);
        method.setAccessible(true);

        assertEquals(1, method.invoke(splitter, "第一章 总则"));
        assertEquals(2, method.invoke(splitter, "一、适用范围"));
        assertEquals(3, method.invoke(splitter, "1. 目标"));
        assertEquals(4, method.invoke(splitter, "1.1 范围"));
    }

    @Test
    @DisplayName("should throw word splitting exception when stream is invalid")
    void shouldThrowWordSplittingExceptionWhenStreamIsInvalid() {
        WordHeaderTextSplitter splitter = new WordHeaderTextSplitter(List.of(1), false, false, false, 10, 0);
        Document source = new Document("", Map.of("wordInputStream", new byte[] {1, 2, 3}));

        assertThrows(WordHeaderTextSplitter.WordDocumentSplitException.class, () -> splitter.apply(List.of(source)));
    }
}
