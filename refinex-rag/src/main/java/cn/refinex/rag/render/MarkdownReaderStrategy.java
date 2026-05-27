package cn.refinex.rag.render;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Markdown 文件读取策略
 *
 * @author refinex
 */
@Component
public class MarkdownReaderStrategy implements DocumentReaderStrategy{

    /**
     * 判断是否支持读取该文件
     *
     * @param file 文件
     * @return 是否支持
     */
    @Override
    public boolean supports(File file) {
        return file.getName().toLowerCase().endsWith(".md");
    }

    /**
     * 读取文件
     *
     * @param file 文件
     * @return 文件内容
     * @throws IOException 文件读取异常
     */
    @Override
    public List<Document> read(File file) throws IOException {
        // 读取配置
        MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                // 水平线分割生成新文档
                .withHorizontalRuleCreateDocument(true)
                // 不包含代码块
                .withIncludeCodeBlock(false)
                // 不包含引用
                .withIncludeBlockquote(false)
                // 添加文件名元数据
                .withAdditionalMetadata("filename", file.getName())
                .build();

        // 读取文件资源
        Resource resource = new FileSystemResource(file);
        // 读取 Markdown 文件
        return new MarkdownDocumentReader(resource, config).get();
    }
}
