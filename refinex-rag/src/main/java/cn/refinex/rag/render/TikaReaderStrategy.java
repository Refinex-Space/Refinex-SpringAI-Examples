package cn.refinex.rag.render;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Tika 文档读取策略, 这里用来处理 doc, docx 文件
 *
 * @author refinex
 */
@Component
public class TikaReaderStrategy implements DocumentReaderStrategy{

    /**
     * 是否支持该文件类型
     *
     * @param file 文件
     * @return true 表示支持，false 表示不支持
     */
    @Override
    public boolean supports(File file) {
        String fileName = file.getName().toLowerCase();
        return fileName.endsWith(".doc") || fileName.endsWith(".docx");
    }

    /**
     * 读取文件
     *
     * @param file 文件
     * @return 文件内容
     * @throws IOException 如果读取文件失败
     */
    @Override
    public List<Document> read(File file) throws IOException {
        Resource resource = new FileSystemResource(file);
        return new TikaDocumentReader(resource).get();
    }
}
