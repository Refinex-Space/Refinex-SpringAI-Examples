package cn.refinex.rag.render;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.JsonReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Json 文件读取策略
 * <p>
 * 注意：可处理 JSON 数组或对象，但不支持 JSON 嵌套字段，比较适合简单的 JSON 文件。如果文件本身比较复杂，还是建议使用 Jackson、FastJSON 等专业的 JSON 工具来手动处理
 *
 * @author refinex
 */
@Component
public class JsonReaderStrategy implements DocumentReaderStrategy {

    /**
     * 判断是否支持读取该文件
     *
     * @param file 文件
     * @return 是否支持
     */
    @Override
    public boolean supports(File file) {
        return file.getName().toLowerCase().endsWith(".json");
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
        Resource resource = new FileSystemResource(file);
        // 假设目标提取 Json 的两个字段: description 和 content
        JsonReader jsonReader = new JsonReader(resource, "description", "content");
        return jsonReader.get();
    }
}
