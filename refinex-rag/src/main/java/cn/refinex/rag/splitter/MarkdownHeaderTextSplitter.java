package cn.refinex.rag.splitter;

import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.document.Document;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Markdown 文档分割器，基于标题层级进行文档分段。支持保留元数据、父子分段关系等高级特性。
 *
 * @author andyflury（https://github.com/langchain4j/langchain4j/issues/574）
 * @author refinex, 增加对父子分段的支持
 */
public class MarkdownHeaderTextSplitter extends OverlapParagraphTextSplitter {

    /**
     * 兼容旧构造器的默认最大块大小，实际等价于不触发二次切分。
     */
    private static final int DEFAULT_CHUNK_SIZE = Integer.MAX_VALUE;

    /**
     * 兼容旧构造器的默认重叠字符数。
     */
    private static final int DEFAULT_OVERLAP = 0;

    /**
     * 代码块定界符
     */
    private static final String BACKTICK_FENCE = "```";

    /**
     * 波浪号定界符
     */
    private static final String TILDE_FENCE = "~~~";

    /**
     * 标题级别元数据键名
     */
    private static final String HEADER_LEVEL_METADATA_KEY = "headerLevel";

    /**
     * 分段 ID 元数据键名
     */
    private static final String CHUNK_ID_METADATA_KEY = "chunkId";

    /**
     * 父分段 ID 元数据键名
     */
    private static final String PARENT_CHUNK_ID_METADATA_KEY = "parentChunkId";

    /**
     * 子分片索引在元数据中的键名。
     */
    private static final String SEGMENT_INDEX_METADATA_KEY = "segmentIndex";

    /**
     * 是否由大小限制二次切分的元数据键名。
     */
    private static final String IS_SPLIT_METADATA_KEY = "isSplit";

    /**
     * 分段分隔符
     */
    private static final String CHUNK_SEPARATOR = "  \n";

    /**
     * 最大标题级别
     */
    private static final int TOP_HEADER_LEVEL = 1;

    /**
     * 需要分割的标题列表，按标题标记长度倒序排列
     */
    private final List<Map.Entry<String, String>> headersToSplitOn;

    /**
     * 是否按行返回结果
     */
    private final boolean returnEachLine;

    /**
     * 是否剥离标题行本身
     */
    private final boolean stripHeaders;

    /**
     * 是否启用父子分段模式
     */
    private final boolean parentChildModel;

    /**
     * 构造函数
     *
     * @param headersToSplitOn 标题分割映射表，key 为标题标记（如 "#"、"##"），value 为元数据中的键名
     * @param returnEachLine   是否按行返回结果，false 时会聚合相同元数据的行
     * @param stripHeaders     是否在结果中移除标题行
     * @param parentChildModel 是否启用父子分段模式，启用后会在元数据中添加 parentChunkId
     */
    public MarkdownHeaderTextSplitter(
            Map<String, String> headersToSplitOn,
            boolean returnEachLine,
            boolean stripHeaders,
            boolean parentChildModel) {
        this(headersToSplitOn, returnEachLine, stripHeaders, parentChildModel, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
    }

    /**
     * 创建支持标题层级和大小重叠二次切分的 Markdown 分割器。
     *
     * @param headersToSplitOn 标题分割映射表，key 为标题标记（如 "#"、"##"），value 为元数据中的键名
     * @param returnEachLine   是否按行返回结果，false 时会聚合相同元数据的行并启用大小切分
     * @param stripHeaders     是否在结果中移除标题行
     * @param parentChildModel 是否启用父子分段模式，启用后会在元数据中添加 parentChunkId
     * @param chunkSize        每块最大字符数，必须大于 0
     * @param overlap          相邻块之间重叠字符数，必须大于等于 0 且小于 {@code chunkSize}
     */
    public MarkdownHeaderTextSplitter(
            Map<String, String> headersToSplitOn,
            boolean returnEachLine,
            boolean stripHeaders,
            boolean parentChildModel,
            int chunkSize,
            int overlap) {
        super(chunkSize, overlap);
        // 按标题标记长度倒序排列，确保优先匹配更长的标记（如 "###" 优先于 "##"）
        this.headersToSplitOn = headersToSplitOn.entrySet().stream()
                .sorted(Comparator.comparingInt(e -> -e.getKey().length()))
                .toList();
        this.returnEachLine = returnEachLine;
        this.stripHeaders = stripHeaders;
        this.parentChildModel = parentChildModel;
    }

    /**
     * 应用文档分割: 重写 apply 方法以支持元数据的传递
     *
     * @param documents 待分割的文档列表
     * @return 分割后的文档列表
     */
    @Override
    public List<Document> apply(List<Document> documents) {
        if (documents == null || documents.isEmpty()) {
            return Collections.emptyList();
        }

        List<Document> result = new ArrayList<>();
        for (Document doc : documents) {
            List<DocumentWithMetadata> segments = null;
            if (doc.getText() != null) {
                segments = splitWithMetadata(doc.getText(), doc.getMetadata());
            }
            if (segments != null) {
                for (DocumentWithMetadata segment : segments) {
                    result.add(new Document(segment.content(), segment.metadata()));
                }
            }
        }

        return result;
    }

    /**
     * 使用元数据进行文档分割：简化版分割方法，不保留元数据
     *
     * @param text 待分割的文档内容
     * @return 分割后的文档内容列表
     */
    @Override
    protected List<String> splitText(String text) {
        return splitWithMetadata(text, new HashMap<>()).stream()
                .map(DocumentWithMetadata::content)
                .toList();
    }

    /**
     * 使用元数据进行文档分割：主分割方法，支持保留元数据
     *
     * @param text         待分割的文档内容
     * @param baseMetadata 基础元数据
     * @return 分割后的文档内容列表
     */
    private List<DocumentWithMetadata> splitWithMetadata(String text, Map<String, Object> baseMetadata) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Object> safeBaseMetadata = baseMetadata == null ? Collections.emptyMap() : baseMetadata;
        String[] lines = text.split("\n");
        List<Line> linesWithMetadata = new ArrayList<>();
        SplitContext context = new SplitContext(safeBaseMetadata);

        for (String line : lines) {
            processLine(line.trim(), linesWithMetadata, context);
        }

        // 处理最后累积的内容
        context.flushContentTo(linesWithMetadata);

        // 根据配置决定返回方式
        if (!returnEachLine) {
            // 聚合模式：将相同元数据的行合并
            return aggregateLinesToChunks(linesWithMetadata);
        }
        // 逐行模式：保持每行独立
        return linesWithMetadata.stream()
                .map(line -> new DocumentWithMetadata(line.getContent(), line.getMetadata()))
                .toList();
    }

    /**
     * 将相同元数据的行聚合为一个分段, 并处理父子关系
     *
     * @param lines 待聚合的行列表
     * @return 聚合后的分段列表
     */
    private List<DocumentWithMetadata> aggregateLinesToChunks(List<Line> lines) {
        List<Line> aggregatedChunks = mergeLinesByMetadata(lines);
        List<Line> sizeAdjustedChunks = applySizeBasedSplitting(aggregatedChunks);

        // 处理父子分段关系
        if (parentChildModel) {
            fillParentChunkIds(sizeAdjustedChunks);
        }

        return sizeAdjustedChunks.stream()
                .map(chunk -> new DocumentWithMetadata(chunk.getContent(), chunk.getMetadata()))
                .toList();
    }

    /**
     * 合并元数据相同或标题子级承接关系明确的相邻行。
     *
     * @param lines 待合并的行列表
     * @return 聚合后的行分块
     */
    private List<Line> mergeLinesByMetadata(List<Line> lines) {
        List<Line> aggregatedChunks = new ArrayList<>();

        for (Line line : lines) {
            if (shouldMergeWithLastChunk(aggregatedChunks, line)) {
                mergeWithLastChunk(aggregatedChunks, line);
                continue;
            }

            aggregatedChunks.add(line);
        }

        return aggregatedChunks;
    }

    /**
     * 处理单行内容：更新代码块状态、处理标题行、处理普通行内容
     *
     * @param strippedLine      去除首尾空格的行内容
     * @param linesWithMetadata 待添加行的列表
     * @param context           分割上下文
     */
    private void processLine(String strippedLine, List<Line> linesWithMetadata, SplitContext context) {
        updateCodeBlockState(strippedLine, context);

        // 代码块内的内容直接添加，不做标题检测
        if (context.isInCodeBlock()) {
            context.addContent(strippedLine);
            return;
        }

        if (processHeaderLine(strippedLine, linesWithMetadata, context)) {
            context.refreshCurrentMetadata();
            return;
        }

        processContentLine(strippedLine, linesWithMetadata, context);
        context.refreshCurrentMetadata();
    }

    /**
     * 更新代码块状态：如果当前行是代码块的结束标记，则关闭代码块；否则，如果当前行是代码块的开始标记，则打开代码块
     *
     * @param strippedLine 去除首尾空格的行内容
     * @param context      分割上下文
     */
    private void updateCodeBlockState(String strippedLine, SplitContext context) {
        if (context.isInCodeBlock()) {
            closeCodeBlockIfNeeded(strippedLine, context);
            return;
        }

        openCodeBlockIfNeeded(strippedLine, context);
    }

    /**
     * 打开代码块：如果当前行是代码块的开始标记，则打开代码块
     *
     * @param strippedLine 去除首尾空格的行内容
     * @param context      分割上下文
     */
    private void openCodeBlockIfNeeded(String strippedLine, SplitContext context) {
        if (strippedLine.startsWith(BACKTICK_FENCE)) {
            context.openCodeBlock(BACKTICK_FENCE);
        } else if (strippedLine.startsWith(TILDE_FENCE)) {
            context.openCodeBlock(TILDE_FENCE);
        }
    }

    /**
     * 关闭代码块：如果当前行是代码块的结束标记，则关闭代码块
     *
     * @param strippedLine 去除首尾空格的行内容
     * @param context      分割上下文
     */
    private void closeCodeBlockIfNeeded(String strippedLine, SplitContext context) {
        if (strippedLine.startsWith(context.getOpeningFence())) {
            context.closeCodeBlock();
        }
    }

    /**
     * 处理标题行：如果当前行是标题行，则更新元数据、添加标题内容
     *
     * @param strippedLine      去除首尾空格的行内容
     * @param linesWithMetadata 待添加行的列表
     * @param context           分割上下文
     * @return 是否是标题行
     */
    private boolean processHeaderLine(String strippedLine, List<Line> linesWithMetadata, SplitContext context) {
        for (Map.Entry<String, String> header : headersToSplitOn) {
            if (isHeaderLine(strippedLine, header.getKey())) {
                updateHeaderMetadata(strippedLine, header, context);
                context.flushContentTo(linesWithMetadata);
                addHeaderContentIfNeeded(strippedLine, context);
                return true;
            }
        }

        return false;
    }

    /**
     * 判断是否是标题行：判断行内容是否以指定分隔符开头，并且分隔符后跟空格或行尾
     *
     * @param strippedLine 去除首尾空格的行内容
     * @param separator    分隔符
     * @return 是否是标题行
     */
    private boolean isHeaderLine(String strippedLine, String separator) {
        return strippedLine.startsWith(separator)
                && (strippedLine.length() == separator.length() || strippedLine.charAt(separator.length()) == ' ');
    }

    /**
     * 更新标题元数据：更新当前标题级别、添加标题元数据
     *
     * @param strippedLine 去除首尾空格的行内容
     * @param header       标题分隔符和名称
     * @param context      分割上下文
     */
    private void updateHeaderMetadata(String strippedLine, Map.Entry<String, String> header, SplitContext context) {
        String name = header.getValue();
        if (name == null) {
            return;
        }

        int currentHeaderLevel = (int) header.getKey().chars()
                .filter(ch -> ch == '#')
                .count();
        context.removeStaleHeaders(currentHeaderLevel);
        context.addHeader(new Header(
                currentHeaderLevel,
                name,
                strippedLine.substring(header.getKey().length()).trim()));
    }

    /**
     * 添加标题内容：如果当前行是标题行，则添加标题内容
     *
     * @param strippedLine 去除首尾空格的行内容
     * @param context      分割上下文
     */
    private void addHeaderContentIfNeeded(String strippedLine, SplitContext context) {
        if (!stripHeaders) {
            context.addContent(strippedLine);
        }
    }

    /**
     * 处理普通行内容：如果当前行非空，则添加行内容；否则，将当前内容flush到列表中
     *
     * @param strippedLine      去除首尾空格的行内容
     * @param linesWithMetadata 待添加行的列表
     * @param context           分割上下文
     */
    private void processContentLine(String strippedLine, List<Line> linesWithMetadata, SplitContext context) {
        if (!strippedLine.isEmpty()) {
            context.addContent(strippedLine);
            return;
        }

        context.flushContentTo(linesWithMetadata);
    }

    /**
     * 判断是否应该合并到最后一个分段：如果最后一个分段的元数据与当前行的元数据相同，则合并
     *
     * @param aggregatedChunks 已聚合的分段列表
     * @param line             当前行
     * @return 是否应该合并
     */
    private boolean shouldMergeWithLastChunk(List<Line> aggregatedChunks, Line line) {
        if (aggregatedChunks.isEmpty()) {
            return false;
        }

        Line lastChunk = aggregatedChunks.getLast();
        return lastChunk.getMetadata().equals(line.getMetadata())
                || shouldMergeChildChunkIntoHeader(lastChunk, line);
    }

    /**
     * 判断是否应该将子分段合并到标题中：如果最后一个分段的元数据小于当前行的元数据，并且最后一个分段的内容以标题开头，则合并
     *
     * @param lastChunk 最后一个分段
     * @param line      当前行
     * @return 是否应该合并
     */
    private boolean shouldMergeChildChunkIntoHeader(Line lastChunk, Line line) {
        return !stripHeaders
                && lastChunk.getMetadata().size() < line.getMetadata().size()
                && lastContentLineStartsWithHeader(lastChunk.getContent())
                && !lastChunk.getMetadata().equals(line.getMetadata());
    }

    /**
     * 判断最后一个内容行是否以标题开头：将内容按行分割，并判断最后一行是否以#开头
     *
     * @param content 内容
     * @return 是否以标题开头
     */
    private boolean lastContentLineStartsWithHeader(String content) {
        String[] contentLines = content.split("\n");
        return contentLines[contentLines.length - 1].startsWith("#");
    }

    /**
     * 合并到最后一个分段：将当前行的内容合并到最后一个分段中
     *
     * @param aggregatedChunks 已聚合的分段列表
     * @param line             当前行
     */
    private void mergeWithLastChunk(List<Line> aggregatedChunks, Line line) {
        Line last = aggregatedChunks.getLast();
        last.setContent(last.getContent() + CHUNK_SEPARATOR + line.getContent());
    }

    /**
     * 对超过 chunkSize 的 Markdown 聚合分块执行二次切分。
     *
     * @param chunks 已聚合的分块列表
     * @return 按大小切分后的分块列表
     */
    private List<Line> applySizeBasedSplitting(List<Line> chunks) {
        List<Line> result = new ArrayList<>();

        for (Line chunk : chunks) {
            result.addAll(splitOversizedChunk(chunk));
        }

        return result;
    }

    /**
     * 对单个 Markdown 分块执行大小判断和必要的二次切分。
     *
     * @param chunk 原始聚合分块
     * @return 原分块或切分后的子分块列表
     */
    private List<Line> splitOversizedChunk(Line chunk) {
        if (chunk.getContent().length() <= getChunkSize()) {
            return List.of(chunk);
        }

        return splitChunkBySize(chunk);
    }

    /**
     * 复用父类段落 overlap 策略切分超长 Markdown 分块。
     *
     * @param chunk 原始聚合分块
     * @return 继承原元数据并带 segmentIndex 的子分块列表
     */
    private List<Line> splitChunkBySize(Line chunk) {
        List<String> subContents = super.splitText(chunk.getContent());
        List<Line> result = new ArrayList<>(subContents.size());

        for (int index = 0; index < subContents.size(); index++) {
            result.add(new Line(subContents.get(index), createSegmentMetadata(chunk, index)));
        }

        return result;
    }

    /**
     * 为二次切分后的 Markdown 子分块创建元数据副本。
     *
     * @param chunk        原始聚合分块
     * @param segmentIndex 子分块序号
     * @return 子分块元数据
     */
    private Map<String, Object> createSegmentMetadata(Line chunk, int segmentIndex) {
        Map<String, Object> segmentMetadata = new HashMap<>(chunk.getMetadata());
        Object originalChunkId = chunk.getMetadata().getOrDefault(CHUNK_ID_METADATA_KEY, UUID.randomUUID().toString());
        segmentMetadata.put(CHUNK_ID_METADATA_KEY, originalChunkId + "_" + segmentIndex);
        segmentMetadata.put(SEGMENT_INDEX_METADATA_KEY, segmentIndex);
        segmentMetadata.put(IS_SPLIT_METADATA_KEY, true);
        return segmentMetadata;
    }

    /**
     * 填充父分段ID：遍历已聚合的分段列表，为每个分段填充父分段ID
     *
     * @param aggregatedChunks 已聚合的分段列表
     */
    private void fillParentChunkIds(List<Line> aggregatedChunks) {
        for (int index = 0; index < aggregatedChunks.size(); index++) {
            fillParentChunkId(aggregatedChunks, index);
        }
    }

    /**
     * 填充父分段ID：为指定索引的分段填充父分段ID
     *
     * @param aggregatedChunks 已聚合的分段列表
     * @param index            分段索引
     */
    private void fillParentChunkId(List<Line> aggregatedChunks, int index) {
        Map<String, Object> currentMetaData = aggregatedChunks.get(index).getMetadata();
        Integer headerLevel = getHeaderLevel(currentMetaData);

        // 顶级标题（level=1）或无标题的分块跳过
        if (headerLevel == null || headerLevel <= TOP_HEADER_LEVEL) {
            return;
        }

        findParentMetadata(aggregatedChunks, index, headerLevel)
                .ifPresent(parentMetadata -> currentMetaData.put(
                        PARENT_CHUNK_ID_METADATA_KEY,
                        parentMetadata.get(CHUNK_ID_METADATA_KEY)));
    }

    /**
     * 查找父元数据：从指定索引向前遍历已聚合的分段列表，查找第一个元数据级别小于指定级别的元数据
     *
     * @param aggregatedChunks 已聚合的分段列表
     * @param currentIndex     当前分段索引
     * @param headerLevel      标题级别
     * @return 父元数据
     */
    private Optional<Map<String, Object>> findParentMetadata(
            List<Line> aggregatedChunks,
            int currentIndex,
            int headerLevel) {
        for (int index = currentIndex - 1; index >= 0; index--) {
            Map<String, Object> previousMetadata = aggregatedChunks.get(index).getMetadata();
            Integer previousHeaderLevel = getHeaderLevel(previousMetadata);
            if (previousHeaderLevel != null && previousHeaderLevel < headerLevel) {
                return Optional.of(previousMetadata);
            }
        }

        return Optional.empty();
    }

    /**
     * 获取标题级别：从元数据中获取标题级别
     *
     * @param metadata 元数据
     * @return 标题级别
     */
    private Integer getHeaderLevel(Map<String, Object> metadata) {
        Object headerLevel = metadata.get(HEADER_LEVEL_METADATA_KEY);
        if (headerLevel instanceof Integer level) {
            return level;
        }

        return null;
    }

    /**
     * 内部类：表示分割上下文
     */
    private static final class SplitContext {

        /**
         * 当前内容行列表
         */
        private final List<String> currentContent = new ArrayList<>();

        /**
         * 标题栈
         */
        private final List<Header> headerStack = new ArrayList<>();

        /**
         * 初始元数据
         */
        private final Map<String, Object> initialMetadata;

        /**
         * 当前元数据
         */
        private Map<String, Object> currentMetadata;

        /**
         * 是否在代码块中
         */
        private boolean inCodeBlock;

        /**
         * 代码块 opening fence
         */
        private String openingFence = "";

        /**
         * 创建一个 SplitContext 对象
         *
         * @param baseMetadata 基础元数据
         */
        private SplitContext(Map<String, Object> baseMetadata) {
            this.initialMetadata = new HashMap<>(baseMetadata);
            this.currentMetadata = new HashMap<>(baseMetadata);
        }

        /**
         * 判断是否在代码块中
         *
         * @return 是否在代码块中
         */
        private boolean isInCodeBlock() {
            return inCodeBlock;
        }

        /**
         * 获取代码块 opening fence
         *
         * @return 代码块 opening fence
         */
        private String getOpeningFence() {
            return openingFence;
        }

        /**
         * 打开代码块：设置当前为代码块模式，并记录 opening fence
         *
         * @param fence 代码块 fence
         */
        private void openCodeBlock(String fence) {
            this.inCodeBlock = true;
            this.openingFence = fence;
        }

        /**
         * 关闭代码块：设置当前为非代码块模式，并清空 opening fence
         */
        private void closeCodeBlock() {
            this.inCodeBlock = false;
            this.openingFence = "";
        }

        /**
         * 添加内容行：将内容行添加到当前内容行列表中
         *
         * @param content 内容行
         */
        private void addContent(String content) {
            this.currentContent.add(content);
        }

        /**
         * 将当前内容行列表flush到分段列表中：将当前内容行列表中的内容行添加到分段列表中，并清空当前内容行列表
         *
         * @param linesWithMetadata 分段列表
         */
        private void flushContentTo(List<Line> linesWithMetadata) {
            if (currentContent.isEmpty()) {
                return;
            }

            linesWithMetadata.add(new Line(String.join("\n", currentContent), currentMetadata));
            currentContent.clear();
        }

        /**
         * 刷新当前元数据：将初始元数据复制到当前元数据
         */
        private void refreshCurrentMetadata() {
            this.currentMetadata = new HashMap<>(initialMetadata);
        }

        /**
         * 移除过时的标题：移除标题栈中级别大于或等于当前标题级别的标题
         *
         * @param currentHeaderLevel 当前标题级别
         */
        private void removeStaleHeaders(int currentHeaderLevel) {
            while (!headerStack.isEmpty() && headerStack.getLast().getLevel() >= currentHeaderLevel) {
                Header poppedHeader = headerStack.removeLast();
                initialMetadata.remove(poppedHeader.getName());
            }
        }

        /**
         * 添加标题：将标题添加到标题栈中，并更新初始元数据
         *
         * @param header 标题
         */
        private void addHeader(Header header) {
            headerStack.add(header);
            initialMetadata.put(header.getName(), header.getData());
            initialMetadata.put(HEADER_LEVEL_METADATA_KEY, header.getLevel());
            initialMetadata.put(CHUNK_ID_METADATA_KEY, UUID.randomUUID().toString());
        }

    }

    /**
     * 内部类：表示带有元数据的文本行
     */
    @Setter
    @Getter
    public static class Line {

        /**
         * 行内容
         */
        private String content;

        /**
         * 行元数据
         */
        private Map<String, Object> metadata;

        /**
         * 创建一个 Line 对象
         *
         * @param content  行内容
         * @param metadata 行元数据
         */
        public Line(String content, Map<String, Object> metadata) {
            this.content = content;
            this.metadata = metadata;
        }

    }

    /**
     * 内部类：表示 Markdown 标题
     */
    @Setter
    @Getter
    public static class Header {

        /**
         * 标题级别
         */
        private int level;

        /**
         * 标题名称
         */
        private String name;

        /**
         * 标题数据
         */
        private String data;

        /**
         * 创建一个 Header 对象
         *
         * @param level 标题级别
         * @param name  标题名称
         * @param data  标题数据
         */
        public Header(int level, String name, String data) {
            this.level = level;
            this.name = name;
            this.data = data;
        }

    }

    /**
     * 内部类：携带元数据的文档片段
     *
     * @param content  文档内容
     * @param metadata 文档元数据
     */
    private record DocumentWithMetadata(String content, Map<String, Object> metadata) {

        /**
         * 创建一个 DocumentWithMetadata 对象
         *
         * @param content  文档内容
         * @param metadata 文档元数据
         */
        private DocumentWithMetadata(String content, Map<String, Object> metadata) {
            this.content = content;
            this.metadata = new HashMap<>(metadata);
        }

    }
}
