package cn.refinex.rag.controller;

import cn.refinex.rag.cleaner.DocumentCleaner;
import cn.refinex.rag.render.DocumentReaderStrategySelector;
import cn.refinex.rag.splitter.OverlapParagraphTextSplitter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * RAG 阅读器控制器
 *
 * @author refinex
 */
@Slf4j
@RestController
@RequestMapping("/rag")
public class RagReaderController {

    private final DocumentReaderStrategySelector selector;

    @Autowired
    public RagReaderController(DocumentReaderStrategySelector selector) {
        this.selector = selector;
    }

    /**
     * 读取文件
     */
    @GetMapping("/read")
    public List<Document> readDocument(@RequestParam("path") String path) {
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("文件不存在或不是有效文件: " + path);
        }

        List<Document> documents;
        try {
            // 1. 加载文档
            documents = selector.read(file);

            // 2. 文本清洗
            documents = DocumentCleaner.cleanDocuments(documents);

            // 3. 文档分片
            // documents = split(documents);
            OverlapParagraphTextSplitter splitter = new OverlapParagraphTextSplitter(
                    // 每块最大字符数
                    400,
                    // 块之间重叠 100 字符
                    100
            );
            documents = splitter.apply(documents);
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + e.getMessage(), e);
        }

        return documents;
    }

    /**
     * 文档分片
     *
     * @param documents 待分片的文档列表
     * @return 分片后的文档列表
     */
    public List<Document> split(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return Collections.emptyList();
        }

        TokenTextSplitter splitter = new TokenTextSplitter(
                // chunkSize：每个文本块的目标大小（以 token 为单位，默认值 800）
                600,
                // minChunkSizeChars：每个文本块的最小字符数（以字符为单位，默认值 350）
                300,
                // minChunkLengthToEmbed：只有长度超过此值的块才会被发送给向量模型，用于过滤无意义的空行或超短片段
                5,
                // maxNumChunks：单个文档允许拆分出的最大块数（默认值 10000），防止文档过大时生成过多分块
                10000,
                // keepSeparator：是否在块中保留分隔符（如换行符），默认值 true
                true
        );
        return splitter.apply(documents);
    }
}
