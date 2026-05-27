package cn.refinex.rag.splitter;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TextSplitter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 按段落切分文本并支持固定大小重叠窗口的文本分割器。
 *
 * <p>该分割器优先保留段落边界；当单段或合并后的内容超过 {@code chunkSize}
 * 时，再按字符窗口切分，并把上一块末尾的 {@code overlap} 个字符带入下一块。</p>
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
     * 创建按段落切分的文本分割器。
     *
     * @param chunkSize 每块最大字符数，必须大于 0
     * @param overlap   相邻块之间重叠字符数，必须大于等于 0 且小于 {@code chunkSize}
     * @throws IllegalArgumentException 当 {@code chunkSize} 或 {@code overlap} 不满足约束时抛出
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
     * 按段落拆分文本，必要时对超长段落按固定窗口继续拆分。
     *
     * @param text 待拆分的文本，允许为空
     * @return 拆分后的文本块列表；输入为空白文本时返回空列表
     */
    @Override
    protected List<String> splitText(String text) {
        if (StringUtils.isBlank(text)) {
            return Collections.emptyList();
        }

        String[] paragraphs = text.split("\\n+");
        List<String> allChunks = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            currentChunk = appendParagraph(paragraph, currentChunk, allChunks);
        }

        addRemainingChunk(currentChunk, allChunks);

        return allChunks;
    }

    /**
     * 批量拆分文档文本。
     *
     * @param documents 待处理的文档列表，允许为空
     * @return 分块处理后的文档列表；输入为空时返回空列表
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

    /**
     * 返回单块最大字符数，供子类执行二次切分时复用。
     *
     * @return 单块最大字符数
     */
    protected int getChunkSize() {
        return chunkSize;
    }

    /**
     * 将单个段落追加到当前分块，必要时滚动生成新分块。
     *
     * @param paragraph    当前段落文本，允许为空白
     * @param currentChunk 当前正在构建的分块
     * @param allChunks    已完成的分块列表
     * @return 追加完成后仍在构建的分块
     */
    private StringBuilder appendParagraph(
            String paragraph,
            StringBuilder currentChunk,
            List<String> allChunks) {
        if (StringUtils.isBlank(paragraph)) {
            return currentChunk;
        }

        int start = 0;
        StringBuilder chunk = currentChunk;
        while (start < paragraph.length()) {
            int end = appendNextSegment(paragraph, start, chunk);
            chunk = rotateChunkIfFull(chunk, allChunks);
            start = end;
        }

        return chunk;
    }

    /**
     * 从段落中截取一段可放入当前分块的文本。
     *
     * @param paragraph    当前段落文本
     * @param start        截取起始位置
     * @param currentChunk 当前正在构建的分块
     * @return 下一次截取的起始位置
     */
    private int appendNextSegment(String paragraph, int start, StringBuilder currentChunk) {
        int remainingSpace = chunkSize - currentChunk.length();
        int end = Math.min(start + remainingSpace, paragraph.length());

        appendLineBreakIfNeeded(currentChunk);
        currentChunk.append(paragraph, start, end);
        return end;
    }

    /**
     * 在跨段落合并时保留段落边界。
     *
     * @param currentChunk 当前正在构建的分块
     */
    private void appendLineBreakIfNeeded(StringBuilder currentChunk) {
        if (!currentChunk.isEmpty()) {
            currentChunk.append("\n");
        }
    }

    /**
     * 当当前分块达到大小上限时保存分块并生成带重叠前缀的新分块。
     *
     * @param currentChunk 当前正在构建的分块
     * @param allChunks    已完成的分块列表
     * @return 原分块未满时返回原对象；已满时返回包含 overlap 文本的新分块
     */
    private StringBuilder rotateChunkIfFull(StringBuilder currentChunk, List<String> allChunks) {
        if (currentChunk.length() < chunkSize) {
            return currentChunk;
        }

        allChunks.add(currentChunk.toString());
        return new StringBuilder(getOverlapText(currentChunk));
    }

    /**
     * 从当前分块末尾提取下一块需要继承的重叠文本。
     *
     * @param currentChunk 当前刚完成的分块
     * @return 重叠文本；未配置 overlap 时返回空字符串
     */
    private String getOverlapText(StringBuilder currentChunk) {
        if (overlap == 0) {
            return "";
        }

        int overlapStart = Math.max(0, currentChunk.length() - overlap);
        return currentChunk.substring(overlapStart);
    }

    /**
     * 保存最后一个未满的分块。
     *
     * @param currentChunk 当前正在构建的分块
     * @param allChunks    已完成的分块列表
     */
    private void addRemainingChunk(StringBuilder currentChunk, List<String> allChunks) {
        if (!currentChunk.isEmpty()) {
            allChunks.add(currentChunk.toString());
        }
    }
}
