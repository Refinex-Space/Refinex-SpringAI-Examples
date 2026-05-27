package cn.refinex.rag.cleaner;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.ai.document.Document;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 *
 *
 * @author refinex
 */
public class DocumentCleaner {

    private DocumentCleaner() {
    }

    /**
     * 清理文档
     *
     * @param documents 待清理的文档列表
     * @return 清理后的文档列表
     */
    public static List<Document> cleanDocuments(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return documents;
        }

        return documents.stream()
                .map(doc -> {
                    if (doc == null || doc.getText() == null) {
                        return doc;
                    }

                    // 提取文本
                    String text = doc.getText();

                    // 1. 去除多余空白字符：空格、制表符、换行等
                    text = text.replaceAll("\\s+", " ").trim();
                    // 2. 去掉无意义的乱码或特殊符号
                    text = text.replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}\\n]", "");
                    // 3. 可选：统一大小写
                    // text = text.toLowerCase();
                    // 4. 按换行拆分段落，去除重复段落
                    String[] paragraphs = text.split("\\n+");
                    Set<String> seen = new LinkedHashSet<>();
                    for (String para : paragraphs) {
                        String trimmed = para.trim();
                        if (!trimmed.isEmpty()) {
                            seen.add(trimmed);
                        }
                    }
                    text = String.join("\n", seen);
                    return new Document(text);
                })
                .toList();
    }
}
