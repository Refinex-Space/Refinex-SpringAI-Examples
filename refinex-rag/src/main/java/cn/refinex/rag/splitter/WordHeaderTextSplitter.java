package cn.refinex.rag.splitter;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.poifs.filesystem.FileMagic;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.ai.document.Document;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 基于 Word 标题样式和标题文本模式切分文档的分割器。
 *
 * <p>该分割器支持 {@code .docx}、{@code .doc} 和纯文本兜底输入。Word 二进制输入通过
 * {@code wordInputStream} 元数据传入，输出片段会继承原始元数据，并补充标题层级、
 * chunkId 以及可选的 parentChunkId。</p>
 *
 * @author refinex
 */
public class WordHeaderTextSplitter extends OverlapParagraphTextSplitter {

    /**
     * Word 输入流在元数据中的键名。
     */
    private static final String WORD_INPUT_STREAM_METADATA_KEY = "wordInputStream";

    /**
     * 当前标题级别在元数据中的键名。
     */
    private static final String HEADING_LEVEL_METADATA_KEY = "headingLevel";

    /**
     * 当前片段 ID 在元数据中的键名。
     */
    private static final String CHUNK_ID_METADATA_KEY = "chunkId";

    /**
     * 父片段 ID 在元数据中的键名。
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
     * 顶级标题级别。
     */
    private static final int TOP_HEADING_LEVEL = 1;

    /**
     * 文件头探测时的读取标记上限。
     */
    private static final int FILE_MAGIC_READ_LIMIT = 8192;

    /**
     * 默认参与分割的 Word 标题级别。
     */
    private static final List<Integer> DEFAULT_HEADING_LEVELS = List.of(1, 2, 3, 4, 5, 6);

    /**
     * Word 标准标题样式和中文标题样式，例如 Heading1 或 标题 1。
     */
    private static final Pattern DOCX_HEADING_STYLE_PATTERN = Pattern.compile("(?i)(?:heading|标题)\\s*(\\d)");

    /**
     * 中文规则型标题模式，用于没有设置 Word 标题样式的文档。
     */
    private static final List<HeadingPattern> TEXT_HEADING_PATTERNS = List.of(
            new HeadingPattern(Pattern.compile("^第[一二三四五六七八九十百]+(?:章|部分|条).*"), 1),
            new HeadingPattern(Pattern.compile("^[一二三四五六七八九十百]+、.*"), 2),
            new HeadingPattern(Pattern.compile("^[（(][一二三四五六七八九十百]+[)）].*"), 3),
            new HeadingPattern(Pattern.compile("^\\d+\\.\\s*[^0-9].*"), 3),
            new HeadingPattern(Pattern.compile("^[（(]\\d+[)）].*"), 3),
            new HeadingPattern(Pattern.compile("^\\d+\\.\\d+.*"), 4));

    /**
     * 短中文顶级标题常见关键词。
     */
    private static final List<String> TOP_HEADING_KEYWORDS = List.of(
            "总则", "附则", "说明", "须知", "规定", "制度", "办法", "条例");

    /**
     * 二级标题常见业务关键词。
     */
    private static final List<String> SECOND_LEVEL_HEADING_KEYWORDS = List.of(
            "管理", "制度", "规范", "流程", "职责", "权限", "考核", "培训", "招聘", "薪酬", "福利", "假期");

    /**
     * 二级标题常见结尾词。
     */
    private static final List<String> SECOND_LEVEL_HEADING_SUFFIXES = List.of("制度", "管理", "规定", "办法");

    /**
     * 需要分割的标题级别列表（1-9），对应 Word 的标题样式。
     */
    private final List<Integer> headingLevelsToSplitOn;

    /**
     * 是否按段落返回结果。
     */
    private final boolean returnEachParagraph;

    /**
     * 是否剥离标题段落本身。
     */
    private final boolean stripHeadings;

    /**
     * 是否启用父子分段模式。
     */
    private final boolean parentChildModel;

    /**
     * 创建 Word 标题分割器。
     *
     * @param headingLevelsToSplitOn 标题级别列表；为空时默认分割 1-6 级标题
     * @param returnEachParagraph    是否按段落返回结果，{@code false} 时会聚合同元数据段落
     * @param stripHeadings          是否从输出内容中移除标题段落
     * @param parentChildModel       是否在元数据中生成 parentChunkId
     * @param chunkSize              每块最大字符数，必须大于 0
     * @param overlap                相邻块之间重叠字符数，必须大于等于 0 且小于 {@code chunkSize}
     */
    public WordHeaderTextSplitter(List<Integer> headingLevelsToSplitOn, boolean returnEachParagraph,
                                  boolean stripHeadings, boolean parentChildModel,
                                  int chunkSize, int overlap) {
        super(chunkSize, overlap);
        this.headingLevelsToSplitOn = normalizeHeadingLevels(headingLevelsToSplitOn);
        this.returnEachParagraph = returnEachParagraph;
        this.stripHeadings = stripHeadings;
        this.parentChildModel = parentChildModel;
    }

    /**
     * 对 Word 文档列表进行标题分割。
     *
     * @param documents 待处理文档列表，Word 输入流可通过 {@code wordInputStream} 元数据传入
     * @return 分割后的 Spring AI 文档列表；输入为空时返回空列表
     * @throws WordDocumentSplitException 当 Word 文件格式不支持或解析失败时抛出
     */
    @Override
    public List<Document> apply(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return Collections.emptyList();
        }

        List<Document> result = new ArrayList<>();
        for (Document document : documents) {
            result.addAll(splitToDocuments(document));
        }
        return result;
    }

    /**
     * 以纯文本兜底模式切分输入。
     *
     * @param text 待切分文本，允许为空
     * @return 切分后的文本片段；输入为空白文本时返回空列表
     */
    @Override
    protected List<String> splitText(String text) {
        return splitPlainText(text, Collections.emptyMap()).stream()
                .map(DocumentWithMetadata::content)
                .toList();
    }

    /**
     * 规范化需要参与分割的标题级别。
     *
     * @param headingLevelsToSplitOn 调用方传入的标题级别列表，允许为空
     * @return 排序后的有效标题级别；输入为空时返回默认 1-6 级标题
     */
    private static List<Integer> normalizeHeadingLevels(List<Integer> headingLevelsToSplitOn) {
        if (CollectionUtils.isEmpty(headingLevelsToSplitOn)) {
            return DEFAULT_HEADING_LEVELS;
        }

        return headingLevelsToSplitOn.stream()
                .filter(level -> level != null && level > 0)
                .sorted()
                .toList();
    }

    /**
     * 将单个 Spring AI 文档切分并转换为输出文档。
     *
     * @param document 待切分的文档
     * @return 切分后的文档列表
     * @throws WordDocumentSplitException 当 Word 解析或格式识别失败时抛出
     */
    private List<Document> splitToDocuments(Document document) {
        try {
            return toDocuments(splitDocument(document));
        } catch (IOException | IllegalArgumentException e) {
            throw new WordDocumentSplitException("Word文档分割失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据文档元数据中的 Word 输入源选择二进制解析或纯文本兜底解析。
     *
     * @param document 待切分的文档
     * @return 带元数据的内部文档片段
     * @throws IOException 当 Word 输入流读取失败时抛出
     */
    private List<DocumentWithMetadata> splitDocument(Document document) throws IOException {
        Map<String, Object> metadata = sanitizeMetadata(document.getMetadata());
        Object wordInputStream = document.getMetadata().get(WORD_INPUT_STREAM_METADATA_KEY);

        if (wordInputStream instanceof InputStream inputStream) {
            try (inputStream) {
                return splitWordDocument(inputStream, metadata);
            }
        }

        if (wordInputStream instanceof byte[] byteArray) {
            try (InputStream inputStream = new ByteArrayInputStream(byteArray)) {
                return splitWordDocument(inputStream, metadata);
            }
        }

        return splitPlainText(document.getText(), metadata);
    }

    /**
     * 复制基础元数据并移除不可序列化的 Word 输入流对象。
     *
     * @param metadata 原始文档元数据
     * @return 可安全写入输出文档的元数据副本
     */
    private Map<String, Object> sanitizeMetadata(Map<String, Object> metadata) {
        Map<String, Object> sanitizedMetadata = new HashMap<>(metadata);
        // 输入流只用于解析 Word 文件，不能传播到输出 Document 元数据中。
        sanitizedMetadata.remove(WORD_INPUT_STREAM_METADATA_KEY);
        return sanitizedMetadata;
    }

    /**
     * 将内部片段转换为 Spring AI Document。
     *
     * @param segments 内部文档片段
     * @return Spring AI 文档列表
     */
    private List<Document> toDocuments(List<DocumentWithMetadata> segments) {
        return segments.stream()
                .map(segment -> new Document(segment.content(), segment.metadata()))
                .toList();
    }

    /**
     * 根据文件头识别 Word 格式并分派到对应解析器。
     *
     * @param inputStream  Word 文档输入流
     * @param baseMetadata 基础元数据，会复制到每个输出片段
     * @return 带元数据的文档片段列表
     * @throws IOException 当输入流读取或 Word 解析失败时抛出
     */
    private List<DocumentWithMetadata> splitWordDocument(
            InputStream inputStream,
            Map<String, Object> baseMetadata) throws IOException {
        BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        // FileMagic 会消费文件头字节，mark/reset 保证后续 POI 解析器仍能从流起点读取。
        bufferedInputStream.mark(FILE_MAGIC_READ_LIMIT);

        FileMagic fileMagic = FileMagic.valueOf(bufferedInputStream);
        bufferedInputStream.reset();

        return switch (fileMagic) {
            case OLE2 -> splitDocDocument(bufferedInputStream, baseMetadata);
            case OOXML -> splitDocxDocument(bufferedInputStream, baseMetadata);
            default -> throw new WordDocumentSplitException("不支持的文件格式，仅支持 .doc 和 .docx 文件");
        };
    }

    /**
     * 解析 OOXML 格式的 {@code .docx} 文档。
     *
     * @param inputStream  docx 输入流
     * @param baseMetadata 基础元数据
     * @return 按标题切分后的文档片段
     * @throws IOException 当 POI 读取文档失败时抛出
     */
    private List<DocumentWithMetadata> splitDocxDocument(
            InputStream inputStream,
            Map<String, Object> baseMetadata) throws IOException {
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            WordSplitContext context = new WordSplitContext(baseMetadata);

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                processParagraph(
                        paragraph.getText(),
                        extractHeadingLevelFromDocx(paragraph.getStyle(), paragraph),
                        context);
            }

            return processSegments(context.toParagraphs());
        }
    }

    /**
     * 解析 OLE2 格式的 {@code .doc} 文档。
     *
     * @param inputStream  doc 输入流
     * @param baseMetadata 基础元数据
     * @return 按标题切分后的文档片段
     * @throws IOException 当 POI 读取文档失败时抛出
     */
    private List<DocumentWithMetadata> splitDocDocument(
            InputStream inputStream,
            Map<String, Object> baseMetadata) throws IOException {
        try (HWPFDocument document = new HWPFDocument(inputStream)) {
            Range range = document.getRange();
            WordSplitContext context = new WordSplitContext(baseMetadata);

            for (int index = 0; index < range.numParagraphs(); index++) {
                Paragraph paragraph = range.getParagraph(index);
                processParagraph(paragraph.text(), detectHeadingByTextPattern(paragraph.text()), context);
            }

            return processSegments(context.toParagraphs());
        }
    }

    /**
     * 按标题级别处理单个段落，并维护当前标题上下文。
     *
     * @param rawText      原始段落文本
     * @param headingLevel 已识别出的标题级别；非标题时为 {@code null}
     * @param context      当前 Word 分割上下文
     */
    private void processParagraph(String rawText, Integer headingLevel, WordSplitContext context) {
        String text = StringUtils.trimToEmpty(rawText);
        if (text.isEmpty()) {
            return;
        }

        if (isConfiguredHeadingLevel(headingLevel)) {
            context.addHeading(headingLevel, text, stripHeadings);
            return;
        }

        context.addContent(text);
    }

    /**
     * 判断标题级别是否在本分割器配置范围内。
     *
     * @param headingLevel 识别出的标题级别
     * @return 标题级别非空且在配置列表中时返回 {@code true}
     */
    private boolean isConfiguredHeadingLevel(Integer headingLevel) {
        return headingLevel != null && headingLevelsToSplitOn.contains(headingLevel);
    }

    /**
     * 根据返回模式生成最终片段。
     *
     * @param paragraphsWithMetadata 已附加元数据的段落列表
     * @return 最终输出片段
     */
    private List<DocumentWithMetadata> processSegments(List<ParagraphWithMetadata> paragraphsWithMetadata) {
        if (returnEachParagraph) {
            return paragraphsWithMetadata.stream()
                    .map(paragraph -> new DocumentWithMetadata(paragraph.getContent(), paragraph.getMetadata()))
                    .toList();
        }

        return aggregateParagraphsToChunks(paragraphsWithMetadata);
    }

    /**
     * 从 docx 段落样式或文本模式中提取标题级别。
     *
     * @param style     Word 段落样式
     * @param paragraph Word 段落对象
     * @return 标题级别；非标题段落返回 {@code null}
     */
    private Integer extractHeadingLevelFromDocx(String style, XWPFParagraph paragraph) {
        return parseHeadingLevelFromStyle(style)
                .orElseGet(() -> detectHeadingByTextPattern(paragraph.getText()));
    }

    /**
     * 从 Word 段落样式名中解析标题级别。
     *
     * @param style 段落样式名，例如 {@code Heading1} 或 {@code 标题 1}
     * @return 样式命中时返回标题级别，否则返回空
     */
    private Optional<Integer> parseHeadingLevelFromStyle(String style) {
        if (style == null) {
            return Optional.empty();
        }

        java.util.regex.Matcher matcher = DOCX_HEADING_STYLE_PATTERN.matcher(style);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        return Optional.of(Integer.parseInt(matcher.group(1)));
    }

    /**
     * 通过文本模式检测标题级别。
     *
     * <p>该方法用于没有应用 Word 标准标题样式的文档，优先识别制度、规范、章节类中文文档中的
     * 常见编号模式和短标题关键词。</p>
     *
     * @param text 段落文本内容，允许为空
     * @return 标题级别；非标题段落返回 {@code null}
     */
    private Integer detectHeadingByTextPattern(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }

        String trimmedText = text.trim();
        // 先匹配强结构编号，再匹配短标题关键词，避免普通短文本被过早判为标题。
        return detectHeadingByPattern(trimmedText)
                .orElseGet(() -> detectHeadingByKeyword(trimmedText).orElse(null));
    }

    /**
     * 使用预编译正则规则识别标题级别。
     *
     * @param text 已裁剪空白的段落文本
     * @return 正则命中时返回标题级别，否则返回空
     */
    private Optional<Integer> detectHeadingByPattern(String text) {
        return TEXT_HEADING_PATTERNS.stream()
                .filter(rule -> rule.pattern().matcher(text).matches())
                .map(HeadingPattern::level)
                .findFirst();
    }

    /**
     * 使用常见业务关键词识别无编号短标题。
     *
     * @param text 已裁剪空白的段落文本
     * @return 关键词命中时返回标题级别，否则返回空
     */
    private Optional<Integer> detectHeadingByKeyword(String text) {
        if (isShortChineseTopHeading(text)) {
            return Optional.of(1);
        }

        if (isSecondLevelKeywordHeading(text)) {
            return Optional.of(2);
        }

        return Optional.empty();
    }

    /**
     * 判断文本是否为常见短中文顶级标题。
     *
     * @param text 已裁剪空白的段落文本
     * @return 命中短中文顶级标题规则时返回 {@code true}
     */
    private boolean isShortChineseTopHeading(String text) {
        return text.length() <= 20
                && text.matches("^[一-龥]+$")
                && containsAny(text, TOP_HEADING_KEYWORDS);
    }

    /**
     * 判断文本是否为常见二级业务标题。
     *
     * @param text 已裁剪空白的段落文本
     * @return 命中二级标题关键词和后缀规则时返回 {@code true}
     */
    private boolean isSecondLevelKeywordHeading(String text) {
        return text.length() <= 30
                && containsAny(text, SECOND_LEVEL_HEADING_KEYWORDS)
                && endsWithAny(text, SECOND_LEVEL_HEADING_SUFFIXES);
    }

    /**
     * 判断文本是否包含任一关键词。
     *
     * @param text     待判断文本
     * @param keywords 关键词列表
     * @return 包含任一关键词时返回 {@code true}
     */
    private boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }

    /**
     * 判断文本是否以任一后缀结尾。
     *
     * @param text     待判断文本
     * @param suffixes 后缀列表
     * @return 以任一后缀结尾时返回 {@code true}
     */
    private boolean endsWithAny(String text, List<String> suffixes) {
        return suffixes.stream().anyMatch(text::endsWith);
    }

    /**
     * 纯文本兜底切分逻辑。
     *
     * @param text         待输出的纯文本
     * @param baseMetadata 基础元数据
     * @return 带 chunkId 的单片段列表；文本为空白时返回空列表
     */
    private List<DocumentWithMetadata> splitPlainText(String text, Map<String, Object> baseMetadata) {
        if (StringUtils.isBlank(text)) {
            return Collections.emptyList();
        }

        Map<String, Object> metadata = new HashMap<>(baseMetadata);
        metadata.put(CHUNK_ID_METADATA_KEY, UUID.randomUUID().toString());
        return List.of(new DocumentWithMetadata(text, metadata));
    }

    /**
     * 聚合同元数据段落，并按配置补充大小切分和父子关系。
     *
     * @param paragraphs 待聚合的段落列表
     * @return 聚合后的文档片段列表
     */
    private List<DocumentWithMetadata> aggregateParagraphsToChunks(List<ParagraphWithMetadata> paragraphs) {
        List<ParagraphWithMetadata> aggregatedChunks = mergeParagraphsByMetadata(paragraphs);
        List<ParagraphWithMetadata> sizeAdjustedChunks = applySizeBasedSplitting(aggregatedChunks);

        if (parentChildModel) {
            fillParentChunkIds(sizeAdjustedChunks);
        }

        return sizeAdjustedChunks.stream()
                .map(chunk -> new DocumentWithMetadata(chunk.getContent(), chunk.getMetadata()))
                .toList();
    }

    /**
     * 合并元数据完全相同的相邻段落。
     *
     * @param paragraphs 待合并段落列表
     * @return 合并后的段落列表
     */
    private List<ParagraphWithMetadata> mergeParagraphsByMetadata(List<ParagraphWithMetadata> paragraphs) {
        List<ParagraphWithMetadata> aggregatedChunks = new ArrayList<>();

        for (ParagraphWithMetadata paragraph : paragraphs) {
            if (shouldMergeWithLastParagraph(aggregatedChunks, paragraph)) {
                mergeWithLastParagraph(aggregatedChunks, paragraph);
                continue;
            }

            aggregatedChunks.add(paragraph);
        }

        return aggregatedChunks;
    }

    /**
     * 判断当前段落能否合并到上一个聚合分块。
     *
     * @param aggregatedChunks 已聚合分块列表
     * @param paragraph        当前段落
     * @return 上一个分块存在且元数据相同时返回 {@code true}
     */
    private boolean shouldMergeWithLastParagraph(
            List<ParagraphWithMetadata> aggregatedChunks,
            ParagraphWithMetadata paragraph) {
        return !aggregatedChunks.isEmpty()
                && aggregatedChunks.getLast().getMetadata().equals(paragraph.getMetadata());
    }

    /**
     * 将当前段落内容追加到最后一个聚合分块。
     *
     * @param aggregatedChunks 已聚合分块列表
     * @param paragraph        当前段落
     */
    private void mergeWithLastParagraph(List<ParagraphWithMetadata> aggregatedChunks, ParagraphWithMetadata paragraph) {
        ParagraphWithMetadata last = aggregatedChunks.getLast();
        last.setContent(last.getContent() + "\n" + paragraph.getContent());
    }

    /**
     * 对超过 chunkSize 的聚合分块执行二次切分。
     *
     * @param chunks 已聚合的分块列表
     * @return 按大小切分后的分块列表
     */
    private List<ParagraphWithMetadata> applySizeBasedSplitting(List<ParagraphWithMetadata> chunks) {
        List<ParagraphWithMetadata> result = new ArrayList<>();

        for (ParagraphWithMetadata chunk : chunks) {
            result.addAll(splitOversizedChunk(chunk));
        }

        return result;
    }

    /**
     * 对单个超长分块执行大小判断和必要的二次切分。
     *
     * @param chunk 原始聚合分块
     * @return 原分块或切分后的子分块列表
     */
    private List<ParagraphWithMetadata> splitOversizedChunk(ParagraphWithMetadata chunk) {
        if (chunk.getContent().length() <= getChunkSize()) {
            return List.of(chunk);
        }

        return splitChunkBySize(chunk);
    }

    /**
     * 使用父类 overlap 策略切分超长 Word 段落。
     *
     * @param chunk 原始聚合分块
     * @return 继承原元数据并带 segmentIndex 的子分块列表
     */
    private List<ParagraphWithMetadata> splitChunkBySize(ParagraphWithMetadata chunk) {
        List<String> subContents = super.splitText(chunk.getContent());
        List<ParagraphWithMetadata> result = new ArrayList<>(subContents.size());

        for (int index = 0; index < subContents.size(); index++) {
            result.add(new ParagraphWithMetadata(subContents.get(index), createSegmentMetadata(chunk, index)));
        }

        return result;
    }

    /**
     * 为二次切分后的子分块创建元数据副本。
     *
     * @param chunk        原始聚合分块
     * @param segmentIndex 子分块序号
     * @return 子分块元数据
     */
    private Map<String, Object> createSegmentMetadata(ParagraphWithMetadata chunk, int segmentIndex) {
        Map<String, Object> segmentMetadata = new HashMap<>(chunk.getMetadata());
        Object originalChunkId = chunk.getMetadata().get(CHUNK_ID_METADATA_KEY);
        segmentMetadata.put(CHUNK_ID_METADATA_KEY, originalChunkId + "_" + segmentIndex);
        segmentMetadata.put(SEGMENT_INDEX_METADATA_KEY, segmentIndex);
        segmentMetadata.put(IS_SPLIT_METADATA_KEY, true);
        return segmentMetadata;
    }

    /**
     * 为所有非顶级标题分块补充父分块 ID。
     *
     * @param aggregatedChunks 已完成聚合和大小切分的分块列表
     */
    private void fillParentChunkIds(List<ParagraphWithMetadata> aggregatedChunks) {
        for (int index = 0; index < aggregatedChunks.size(); index++) {
            fillParentChunkId(aggregatedChunks, index);
        }
    }

    /**
     * 为指定分块查找并写入父分块 ID。
     *
     * @param aggregatedChunks 已完成聚合和大小切分的分块列表
     * @param index            当前分块索引
     */
    private void fillParentChunkId(List<ParagraphWithMetadata> aggregatedChunks, int index) {
        Map<String, Object> currentMetadata = aggregatedChunks.get(index).getMetadata();
        Integer headingLevel = getHeadingLevel(currentMetadata);
        if (headingLevel == null || headingLevel <= TOP_HEADING_LEVEL) {
            return;
        }

        findParentMetadata(aggregatedChunks, index, headingLevel)
                .ifPresent(parentMetadata -> currentMetadata.put(
                        PARENT_CHUNK_ID_METADATA_KEY,
                        parentMetadata.get(CHUNK_ID_METADATA_KEY)));
    }

    /**
     * 向前查找第一个标题级别更低的分块作为父分块。
     *
     * @param aggregatedChunks 已完成聚合和大小切分的分块列表
     * @param currentIndex     当前分块索引
     * @param headingLevel     当前分块标题级别
     * @return 父分块元数据；找不到时返回空
     */
    private Optional<Map<String, Object>> findParentMetadata(
            List<ParagraphWithMetadata> aggregatedChunks,
            int currentIndex,
            int headingLevel) {
        for (int index = currentIndex - 1; index >= 0; index--) {
            Map<String, Object> previousMetadata = aggregatedChunks.get(index).getMetadata();
            Integer previousHeadingLevel = getHeadingLevel(previousMetadata);
            if (previousHeadingLevel != null && previousHeadingLevel < headingLevel) {
                return Optional.of(previousMetadata);
            }
        }

        return Optional.empty();
    }

    /**
     * 从元数据中读取标题级别。
     *
     * @param metadata 分块元数据
     * @return 标题级别；不存在或类型不匹配时返回 {@code null}
     */
    private Integer getHeadingLevel(Map<String, Object> metadata) {
        Object headingLevel = metadata.get(HEADING_LEVEL_METADATA_KEY);
        if (headingLevel instanceof Integer level) {
            return level;
        }

        return null;
    }

    private static final class WordSplitContext {

        private final List<ParagraphWithMetadata> paragraphsWithMetadata = new ArrayList<>();
        private final List<String> currentContent = new ArrayList<>();
        private final List<HeadingInfo> headingStack = new ArrayList<>();
        private final Map<String, Object> activeMetadata;
        private Map<String, Object> currentMetadata;

        /**
         * 创建 Word 分割上下文。
         *
         * @param baseMetadata 基础元数据
         */
        private WordSplitContext(Map<String, Object> baseMetadata) {
            this.activeMetadata = new HashMap<>(baseMetadata);
            this.currentMetadata = new HashMap<>(baseMetadata);
        }

        /**
         * 处理标题段落并刷新标题栈。
         *
         * @param headingLevel  标题级别
         * @param text          标题文本
         * @param stripHeadings 是否从输出中移除标题段落
         */
        private void addHeading(Integer headingLevel, String text, boolean stripHeadings) {
            removeStaleHeadings(headingLevel);
            putHeadingMetadata(headingLevel, text);
            flushContent();

            if (!stripHeadings) {
                currentContent.add(text);
            }

            refreshCurrentMetadata();
        }

        /**
         * 追加普通正文段落。
         *
         * @param text 正文段落文本
         */
        private void addContent(String text) {
            currentContent.add(text);
            refreshCurrentMetadata();
        }

        /**
         * 输出上下文中已经收集的段落。
         *
         * @return 带元数据的段落列表
         */
        private List<ParagraphWithMetadata> toParagraphs() {
            flushContent();
            return paragraphsWithMetadata;
        }

        /**
         * 移除不再属于当前标题路径的下级或同级标题。
         *
         * @param headingLevel 新标题级别
         */
        private void removeStaleHeadings(int headingLevel) {
            while (!headingStack.isEmpty() && headingStack.getLast().level() >= headingLevel) {
                HeadingInfo poppedHeading = headingStack.removeLast();
                activeMetadata.remove(poppedHeading.metadataKey());
            }
        }

        /**
         * 写入当前标题元数据并生成新的 chunkId。
         *
         * @param headingLevel 标题级别
         * @param text         标题文本
         */
        private void putHeadingMetadata(int headingLevel, String text) {
            String metadataKey = "heading" + headingLevel;
            headingStack.add(new HeadingInfo(headingLevel, metadataKey, text));
            activeMetadata.put(metadataKey, text);
            activeMetadata.put(HEADING_LEVEL_METADATA_KEY, headingLevel);
            activeMetadata.put(CHUNK_ID_METADATA_KEY, UUID.randomUUID().toString());
        }

        /**
         * 将当前累积正文保存为一个带元数据快照的段落。
         */
        private void flushContent() {
            if (currentContent.isEmpty()) {
                return;
            }

            paragraphsWithMetadata.add(new ParagraphWithMetadata(String.join("\n", currentContent), currentMetadata));
            currentContent.clear();
        }

        /**
         * 刷新当前段落使用的元数据快照。
         */
        private void refreshCurrentMetadata() {
            // 使用快照隔离后续标题变化，避免已收集段落的元数据被继续修改。
            this.currentMetadata = new HashMap<>(activeMetadata);
        }
    }

    /**
     * Word 文档分割失败异常。
     */
    public static class WordDocumentSplitException extends RuntimeException {

        /**
         * 创建 Word 文档分割异常。
         *
         * @param message 失败原因
         */
        public WordDocumentSplitException(String message) {
            super(message);
        }

        /**
         * 创建带原始异常的 Word 文档分割异常。
         *
         * @param message 失败原因
         * @param cause   原始异常
         */
        public WordDocumentSplitException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * 表示文本标题识别正则和对应标题级别。
     *
     * @param pattern 标题识别正则
     * @param level   命中后的标题级别
     */
    private record HeadingPattern(Pattern pattern, int level) {
    }

    /**
     * 表示 Word 标题在元数据中的层级信息。
     *
     * @param level       标题级别（1-9）
     * @param metadataKey 元数据中的键名
     * @param text        标题文本内容
     */
    private record HeadingInfo(int level, String metadataKey, String text) {
    }

    /**
     * 表示带元数据的 Word 段落。
     */
    @Setter
    @Getter
    private static class ParagraphWithMetadata {

        /**
         * 段落文本内容。
         */
        private String content;

        /**
         * 段落继承的元数据快照。
         */
        private Map<String, Object> metadata;

        /**
         * 创建带元数据的段落。
         *
         * @param content  段落文本内容
         * @param metadata 段落元数据快照
         */
        private ParagraphWithMetadata(String content, Map<String, Object> metadata) {
            this.content = content;
            this.metadata = metadata;
        }
    }

    /**
     * 表示最终输出的文档片段。
     *
     * @param content  输出文本内容
     * @param metadata 输出元数据快照
     */
    private record DocumentWithMetadata(String content, Map<String, Object> metadata) {

        /**
         * 创建输出文档片段，并复制元数据以隔离后续修改。
         *
         * @param content  输出文本内容
         * @param metadata 输出元数据
         */
        private DocumentWithMetadata(String content, Map<String, Object> metadata) {
            this.content = content;
            this.metadata = new HashMap<>(metadata);
        }
    }
}
