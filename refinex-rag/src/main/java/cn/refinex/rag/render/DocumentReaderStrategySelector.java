package cn.refinex.rag.render;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 文档读取策略选择器
 *
 * @author refinex
 */
@Component
public class DocumentReaderStrategySelector {

    /**
     * 文档读取策略列表
     */
    private final List<DocumentReaderStrategy> strategies;

    /**
     * 构造函数
     *
     * @param strategies 文档读取策略列表
     */
    public DocumentReaderStrategySelector(List<DocumentReaderStrategy> strategies) {
        this.strategies = strategies;
    }

    /**
     * 读取文档
     *
     * @param file 文档文件
     * @return 文档列表
     * @throws IOException 如果读取文档时发生I/O错误
     */
    public List<Document> read(File file) throws IOException {
        for (DocumentReaderStrategy strategy : strategies) {
            if (strategy.supports(file)) {
                return strategy.read(file);
            }
        }

        throw new IllegalArgumentException("Unsupported file type: " + file);
    }
}
