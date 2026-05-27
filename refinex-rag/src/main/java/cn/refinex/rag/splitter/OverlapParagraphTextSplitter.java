package cn.refinex.rag.splitter;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 自定义分片器：支持 chunkSize、overlap，并按段落拆分
 *
 * @author refinex
 */
public class OverlapParagraphTextSplitter extends TextSplitter {

    /**
     * 每块最大字符数
     */
    private final int chunkSize;

    /**
     * 相邻块之间重叠字符数
     */
    private final int overlap;

    /**
     * 构造函数
     *
     * @param chunkSize 每块最大字符数
     * @param overlap   相邻块之间重叠字符数
     */
    public OverlapParagraphTextSplitter(int chunkSize, int overlap) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize 必须大于 0");
        }
        if (overlap < 0) {
            throw new IllegalArgumentException("overlap 不能为负数");
        }
        if (overlap >= chunkSize) {
            throw new IllegalArgumentException("overlap 不能大于等于 chunkSize");
        }
        this.chunkSize = chunkSize;
        this.overlap = overlap;
    }

    /**
     * 按照段落拆分文本
     *
     * @param text 待拆分的文本
     * @return 拆分后的文本块列表
     */
    @Override
    protected List<String> splitText(String text) {
        if (StringUtils.isBlank(text)) {
            return Collections.emptyList();
        }

        // 按照段落拆分得到所有段落
        String[] paragraphs = text.split("\\n+");
        List<String> allChunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();

        // 依次处理每个段落
        for (String paragraph : paragraphs) {
            if (StringUtils.isBlank(paragraph)) {
                continue;
            }

            int start = 0;
            while (start < paragraph.length()) {
                // 计算剩余空间：每个块的最大字符数减去当前块的长度
                int remainingSpace = chunkSize - currentChunk.length();
                // 计算结束位置：剩余空间 和 段落长度 的最小值
                int end = Math.min(start + remainingSpace, paragraph.length());

                // 如果当前块不为空，则添加一个换行符
                if (!currentChunk.isEmpty()) {
                    currentChunk.append("\n");
                }
                // 添加当前段落的子串到当前块中
                currentChunk.append(paragraph, start, end);

                // 如果当前块已满，保存并生成新块
                if (currentChunk.length() >= chunkSize) {
                    // 保存当前块
                    allChunks.add(currentChunk.toString());

                    // 计算重叠
                    String overlapText = "";
                    if (overlap > 0) {
                        // 计算重叠部分：当前块长度 减去 重叠字符数
                        int overlapStart = Math.max(0, currentChunk.length() - overlap);
                        // 获取重叠部分
                        overlapText = currentChunk.substring(overlapStart);
                    }

                    // 生成新块
                    currentChunk = new StringBuilder();
                    if (!overlapText.isEmpty()) {
                        // 将重叠部分添加到新块中
                        currentChunk.append(overlapText);
                    }
                }

                // 更新起始位置
                start = end;
            }
        }

        // 如果当前块不为空，则保存当前块
        if (!currentChunk.isEmpty()) {
            allChunks.add(currentChunk.toString());
        }

        return allChunks;
    }

    /**
     * 批量拆分: 对文档列表进行分块处理
     *
     * @param documents 待处理的文档列表
     * @return 分块处理后的文档列表
     */
    @Override
    public List<Document> apply(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return Collections.emptyList();
        }

        List<Document> result = new ArrayList<>();
        for (Document document : documents) {
            List<String> chunks = splitText(document.getText());
            for (String chunk : chunks) {
                result.add(new Document(chunk));
            }
        }
        return result;
    }
}
