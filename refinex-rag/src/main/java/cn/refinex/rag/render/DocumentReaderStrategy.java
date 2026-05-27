package cn.refinex.rag.render;

import org.springframework.ai.document.Document;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 文档读取策略
 *
 * @author refinex
 */
public interface DocumentReaderStrategy {

    /**
     * 是否支持该文件类型
     *
     * @param file 文件
     * @return 是否支持
     */
    boolean supports(File file);

    /**
     * 读取文件
     *
     * @param file 文件
     * @return 文档列表
     * @throws IOException 如果读取文件失败
     */
    List<Document> read(File file) throws IOException;
}
