package cn.refinex.rag.render;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 文本读取策略
 *
 * @author refinex
 */
@Component
public class TextReaderStrategy implements DocumentReaderStrategy {

    /**
     * 是否支持该文件类型
     *
     * @param file 文件
     * @return true 表示支持，false 表示不支持
     */
    @Override
    public boolean supports(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".txt") || fileName.endsWith(".tex") || fileName.endsWith(".text");
    }

    /**
     * 读取文件
     *
     * @param file 文件
     * @return 文档列表
     * @throws IOException 如果读取文件失败
     */
    @Override
    public List<Document> read(File file) throws IOException {
        Resource resource = new FileSystemResource(file);
        return new TextReader(resource).get();
    }
}
