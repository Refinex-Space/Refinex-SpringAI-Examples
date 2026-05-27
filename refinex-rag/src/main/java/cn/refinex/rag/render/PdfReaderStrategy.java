package cn.refinex.rag.render;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * PDF 文件读取策略
 * <p>
 * 两种方案：
 * 1. PagePdfDocumentReader：按页切分
 * 2. ParagraphPdfDocumentReader：按语意段落切分
 * <p>
 * 建议优先考虑使用 ParagraphPdfDocumentReader，因为它能够更好地保留信息的完整性。
 * 段落通常是一个完整的意思表达，这对于 LLM 理解上下文非常有帮助。但是它非常依赖 PDF
 * 本身的质量——如果 PDF 是扫描件或者没有良好的内部结构标记，效果可能不理想甚至回退到按行读取。
 *
 * @author refinex
 */
@Component
public class PdfReaderStrategy implements DocumentReaderStrategy {

    /**
     * 是否支持该文件类型
     *
     * @param file 文件
     * @return true 表示支持，false 表示不支持
     */
    @Override
    public boolean supports(File file) {
        return file.getName().toLowerCase().endsWith(".pdf");
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
        // 读取配置
        PdfDocumentReaderConfig config = PdfDocumentReaderConfig.builder()
                // 忽略顶部 50 个单位的页眉
                .withPageTopMargin(50)
                // 忽略底部 50 个单位的页脚
                .withPageBottomMargin(50)
                // 每一页作为一个 Document
                .withPageExtractedTextFormatter(new ExtractedTextFormatter.Builder()
                        // 每页再额外删除前 0 行
                        .withNumberOfTopTextLinesToDelete(0)
                        .build())
                .build();

        // 读取文件资源
        Resource resource = new FileSystemResource(file);
        // 读取 PDF 文件
        return new PagePdfDocumentReader(resource, config).get();
    }
}
