package cn.refinex.rag.render;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.jsoup.JsoupDocumentReader;
import org.springframework.ai.reader.jsoup.config.JsoupDocumentReaderConfig;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * HTML 文件读取策略
 *
 * @author refinex
 */
@Component
public class HtmlReaderStrategy implements DocumentReaderStrategy {

    /**
     * 判断是否支持读取该文件
     *
     * @param file 文件
     * @return 是否支持
     */
    @Override
    public boolean supports(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".html") || fileName.endsWith(".htm");
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
        JsoupDocumentReaderConfig config = JsoupDocumentReaderConfig.builder()
                // 只提取 p 标签段落
                .selector("p")
                // 设置文件编码
                .charset("UTF-8")
                // 包含超链接
                .includeLinkUrls(true)
                // 提取 meta 标签的元数据
                .metadataTags(List.of("author", "date"))
                // 添加自定义元数据
                .additionalMetadata("filename", file.getName())
                .build();

        // 读取文件资源
        Resource resource = new FileSystemResource(file);
        // 读取 HTML 文件
        return new JsoupDocumentReader(resource, config).get();
    }
}
