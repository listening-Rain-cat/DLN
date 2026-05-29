package org.example.dln.service;

import org.example.dln.config.CacheNames;
import org.example.dln.dto.CreateFolderDTO;
import org.example.dln.dto.CreateKnowledgeBaseDTO;
import org.example.dln.dto.UpdateFolderDTO;
import org.example.dln.dto.UpdateKnowledgeBaseDTO;
import org.example.dln.entity.Folder;
import org.example.dln.entity.KnowledgeBase;
import org.example.dln.entity.Note;
import org.example.dln.entity.NoteContent;
import org.example.dln.entity.NoteLink;
import org.example.dln.entity.NoteTag;
import org.example.dln.entity.Tag;
import org.example.dln.exception.BusinessException;
import org.example.dln.mapper.FolderMapper;
import org.example.dln.mapper.KnowledgeBaseMapper;
import org.example.dln.mapper.NoteContentMapper;
import org.example.dln.mapper.NoteLinkMapper;
import org.example.dln.mapper.NoteMapper;
import org.example.dln.mapper.NoteTagMapper;
import org.example.dln.mapper.TagMapper;
import org.example.dln.util.LongStringUtils;
import org.example.dln.vo.KnowledgeBaseVO;
import org.example.dln.vo.KnowledgeGraphEdgeVO;
import org.example.dln.vo.KnowledgeGraphNodeVO;
import org.example.dln.vo.KnowledgeGraphVO;
import org.example.dln.vo.NoteSearchResultVO;
import org.example.dln.vo.TagVO;
import org.example.dln.vo.TreeNodeVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 包名：org.example.dln.service
 * 类名：KnowledgeBaseService
 * 类描述：处理知识库、文件夹和树结构相关业务逻辑。
 * 创建人：@author Rain_润
 */
@Service
public class KnowledgeBaseService {
    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private FolderMapper folderMapper;

    @Autowired
    private NoteMapper noteMapper;

    @Autowired
    private NoteContentMapper noteContentMapper;

    @Autowired
    private NoteLinkMapper noteLinkMapper;

    @Autowired
    private NoteTagMapper noteTagMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private CacheInvalidationService cacheInvalidationService;

    /**
    * 创建知识库。
     * @param userId 用户ID
     * @param dto 创建知识库请求参数
    */
    public KnowledgeBaseVO createKnowledgeBase(Long userId, CreateKnowledgeBaseDTO dto) {
        String name = dto.getName().trim();
        //先业务层检验一遍唯一性
        checkKnowledgeBaseNameExists(userId, name, null);
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setUserId(userId);
        knowledgeBase.setName(name);
        knowledgeBase.setDescription(trimToNull(dto.getDescription()));
        knowledgeBase.setStatus(1);
        //根据阿里巴巴 Java 开发手册，所有Long类型的赋值一律用大写L
        knowledgeBase.setDeleteToken(0L);
        if (knowledgeBaseMapper.insert(knowledgeBase) <= 0) {
            throw new BusinessException("创建知识库失败");
        }
        return toKnowledgeBaseVO(knowledgeBase);
    }

    /**
    * 查询知识库列表。
     * @param userId 用户ID
    */
    public List<KnowledgeBaseVO> listKnowledgeBases(Long userId) {
        List<KnowledgeBase> knowledgeBases = knowledgeBaseMapper.selectActiveByUserIdOrderByUpdatedTimeDesc(userId);
        List<KnowledgeBaseVO> result = new ArrayList<>();
        for (KnowledgeBase knowledgeBase : knowledgeBases) {
            result.add(toKnowledgeBaseVO(knowledgeBase));
        }
        return result;
    }

    /**
    * 更新知识库。
     * @param userId 用户ID
     * @param knowledgeBaseId 知识库ID
     * @param dto 更新知识库请求参数
    */
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseVO updateKnowledgeBase(Long userId, Long knowledgeBaseId, UpdateKnowledgeBaseDTO dto) {
        //判数据库存在与合法性
        KnowledgeBase knowledgeBase = getKnowledgeBaseOrThrow(userId, knowledgeBaseId);
        String name = dto.getName().trim();
        //检查重名
        checkKnowledgeBaseNameExists(userId, name, knowledgeBaseId);
        knowledgeBase.setName(name);
        knowledgeBase.setDescription(trimToNull(dto.getDescription()));
        if (knowledgeBaseMapper.updateById(knowledgeBase) <= 0) {
            throw new BusinessException("更新知识库失败");
        }
        return toKnowledgeBaseVO(knowledgeBase);
    }

    /**
    * 删除知识库。
     * @param userId 用户ID
     * @param knowledgeBaseId 知识库ID
    */
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeBase(Long userId, Long knowledgeBaseId) {
        //判数据库存在与合法性
        KnowledgeBase knowledgeBase = getKnowledgeBaseOrThrow(userId, knowledgeBaseId);
        //删除知识库下的文件夹和笔记
        List<Folder> folders = folderMapper.selectActiveByKnowledgeBaseIdOrderByNameAsc(knowledgeBaseId);
        List<Note> notes = noteMapper.selectActiveByKnowledgeBaseId(knowledgeBaseId);
        LocalDateTime deletedTime = LocalDateTime.now();
        for (Note note : notes) {
            note.setStatus(2);
            note.setDeletedTime(deletedTime);
            //删除token设置为笔记ID，确保全局唯一，避免误删
            note.setDeleteToken(note.getId());
            if (noteMapper.updateById(note) <= 0) {
                throw new BusinessException("删除知识库下的笔记失败");
            }
            noteLinkMapper.deleteBySourceNoteId(note.getId());
            noteTagMapper.deleteByNoteId(note.getId());
        }
        for (Folder folder : folders) {
            folder.setStatus(2);
            folder.setDeletedTime(deletedTime);
            folder.setDeleteToken(folder.getId());
            if (folderMapper.updateById(folder) <= 0) {
                throw new BusinessException("删除知识库下的文件夹失败");
            }
        }
        tagMapper.deleteByKnowledgeBaseId(knowledgeBaseId);
        knowledgeBase.setStatus(0);
        knowledgeBase.setDeletedTime(deletedTime);
        knowledgeBase.setDeleteToken(knowledgeBase.getId());
        if (knowledgeBaseMapper.updateById(knowledgeBase) <= 0) {
            throw new BusinessException("删除知识库失败");
        }
        cacheInvalidationService.evictKnowledgeBaseCaches(userId, knowledgeBaseId);
    }

    /**
    * 获取知识库目录树。
     * @param userId 用户ID
     * @param knowledgeBaseId 知识库ID
    */
    @Cacheable(cacheNames = CacheNames.KNOWLEDGE_BASE_TREE, key = "#userId + ':' + #knowledgeBaseId", sync = true)
    public List<TreeNodeVO> getKnowledgeBaseTree(Long userId, Long knowledgeBaseId) {
        getKnowledgeBaseOrThrow(userId, knowledgeBaseId);
        //获取知识库下的文件夹和笔记
        //TODO
        List<Folder> folders = folderMapper.selectActiveByKnowledgeBaseIdOrderByNameAsc(knowledgeBaseId);
        List<Note> notes = noteMapper.selectActiveByUserIdAndKnowledgeBaseIdOrderByTitleAsc(userId, knowledgeBaseId);
        //构建笔记与其标签的键值对
        Map<Long, List<TagVO>> noteTagMap = buildNoteTagMap(notes);
        //构建文件夹ID与树节点的键值对
        Map<String, TreeNodeVO> folderNodeMap = new HashMap<>();
        //把所有文件夹转化为视图对象并放入map，key为字符串类型的文件夹ID，方便后续构建树结构
        for (Folder folder : folders) {
            TreeNodeVO node = toFolderNode(folder);
            folderNodeMap.put(LongStringUtils.toStringValue(folder.getId()), node);
        }
        //树的根节点列表，包含所有父节点ID为null的文件夹和父节点不存在的文件夹，以及所有不在文件夹内的笔记
        List<TreeNodeVO> roots = new ArrayList<>();

        //根据文件夹的父子关系构建树结构，父节点ID为null的文件夹作为根节点，如果父节点不存在则也作为根节点
        for (TreeNodeVO folderNode : folderNodeMap.values()) {
            if (folderNode.getParentId() == null) {
                roots.add(folderNode);
                continue;
            }
            //找到父节点
            TreeNodeVO parent = folderNodeMap.get(folderNode.getParentId());
            //父节点存在则添加到父节点的子节点列表，否则添加到根节点列表
            if (parent != null) {
                parent.getChildren().add(folderNode);
            }
        }

        //处理笔记
        for (Note note : notes) {
            TreeNodeVO node = new TreeNodeVO();
            node.setId(LongStringUtils.toStringValue(note.getId()));
            node.setParentId(LongStringUtils.toStringValue(note.getFolderId()));
            node.setName(note.getTitle());
            node.setType("note");
            node.setKnowledgeBaseId(LongStringUtils.toStringValue(note.getKnowledgeBaseId()));
            node.setCreatedTime(note.getCreatedTime());
            node.setUpdatedTime(note.getUpdatedTime());
            node.setTags(noteTagMap.getOrDefault(note.getId(), new ArrayList<>()));
            //如果笔记的父节点ID为null或者父节点不存在，则把笔记作为根节点，否则添加到父节点的子节点列表
            if (note.getFolderId() == null) {
                roots.add(node);
            } else {
                TreeNodeVO parent = folderNodeMap.get(LongStringUtils.toStringValue(note.getFolderId()));
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    roots.add(node);
                }
            }
        }
        //递归排序树节点，文件夹优先于笔记，同级节点按名称字母序排序，忽略大小写
        sortNodesRecursively(roots);
        return roots;
    }

    /**
    * 获取知识图谱。
     * @param userId 用户ID
     * @param knowledgeBaseId 知识库ID
    */
    @Cacheable(cacheNames = CacheNames.KNOWLEDGE_GRAPH, key = "#userId + ':' + #knowledgeBaseId", sync = true)
    public KnowledgeGraphVO getKnowledgeGraph(Long userId, Long knowledgeBaseId) {
        //判存在和合法性
        getKnowledgeBaseOrThrow(userId, knowledgeBaseId);
        //获取知识库下的笔记和双链关系
        List<Note> notes = noteMapper.selectActiveByUserIdAndKnowledgeBaseIdOrderByTitleAsc(userId, knowledgeBaseId);
        List<NoteLink> links = noteLinkMapper.selectByKnowledgeBaseId(knowledgeBaseId);
        //构建笔记ID与笔记对象的键值对
        Map<Long, Note> noteMap = new HashMap<>();
        //入度
        Map<Long, Integer> incomingCountMap = new HashMap<>();
        //出度
        Map<Long, Integer> outgoingCountMap = new HashMap<>();
        for (Note note : notes) {
            noteMap.put(note.getId(), note);
            //初始化入度和出度为0
            incomingCountMap.put(note.getId(), 0);
            outgoingCountMap.put(note.getId(), 0);
        }
        //构建边列表，过滤掉目标笔记不存在的边，并统计入度和出度
        List<KnowledgeGraphEdgeVO> edgeList = new ArrayList<>();
        for (NoteLink link : links) {
            if (link.getTargetNoteId() == null) {
                continue;
            }
            //如果边的源笔记或目标笔记不存在，则说明边无效，跳过不处理，后续可以考虑删除这些无效边以保持数据清洁
            if (!noteMap.containsKey(link.getSourceNoteId()) || !noteMap.containsKey(link.getTargetNoteId())) {
                continue;
            }
            //统计入度和出度，使用computeIfPresent方法避免空指针异常，如果笔记ID不存在于入度或出度统计中，则说明数据不一致，跳过不处理
            outgoingCountMap.computeIfPresent(link.getSourceNoteId(), (key, value) -> value + 1);
            incomingCountMap.computeIfPresent(link.getTargetNoteId(), (key, value) -> value + 1);
            //构建边的视图对象
            KnowledgeGraphEdgeVO edge = new KnowledgeGraphEdgeVO();
            edge.setId(LongStringUtils.toStringValue(link.getId()));
            edge.setSourceNoteId(LongStringUtils.toStringValue(link.getSourceNoteId()));
            edge.setTargetNoteId(LongStringUtils.toStringValue(link.getTargetNoteId()));
            edge.setTargetNoteName(link.getTargetNoteName());
            edge.setIsBroken(link.getIsBroken());
            edgeList.add(edge);
        }
        //构建节点列表，统计入度和出度，并转换为视图对象
        List<KnowledgeGraphNodeVO> nodeList = new ArrayList<>();
        for (Note note : notes) {
            KnowledgeGraphNodeVO node = new KnowledgeGraphNodeVO();
            node.setNoteId(LongStringUtils.toStringValue(note.getId()));
            node.setFolderId(LongStringUtils.toStringValue(note.getFolderId()));
            node.setTitle(note.getTitle());
            node.setIncomingCount(incomingCountMap.getOrDefault(note.getId(), 0));
            node.setOutgoingCount(outgoingCountMap.getOrDefault(note.getId(), 0));
            nodeList.add(node);
        }
        //构建知识图谱视图对象
        KnowledgeGraphVO graph = new KnowledgeGraphVO();
        graph.setKnowledgeBaseId(LongStringUtils.toStringValue(knowledgeBaseId));
        graph.setNodes(nodeList);
        graph.setEdges(edgeList);
        return graph;
    }

    /**
    * 检索知识库内笔记。
     * @param userId 用户ID
     * @param knowledgeBaseId 知识库ID
     * @param keyword 检索关键词
     * @param scope 检索范围
     * @param folderId 文件夹ID
     * @param tagIds 标签ID列表
    */
    public List<NoteSearchResultVO> searchNotes(Long userId,
                                                Long knowledgeBaseId,
                                                String keyword,
                                                String scope,
                                                Long folderId,
                                                List<Long> tagIds) {
        getKnowledgeBaseOrThrow(userId, knowledgeBaseId);
        //获取知识库下的文件夹列表，构建文件夹ID与文件夹对象的键值对
        List<Folder> folders = folderMapper.selectActiveByKnowledgeBaseIdOrderByNameAsc(knowledgeBaseId);
        Map<Long, Folder> folderMap = new HashMap<>();
        for (Folder folder : folders) {
            folderMap.put(folder.getId(), folder);
        }
        //构建文件夹过滤ID集合，如果folderId不为null，则包含该文件夹及其所有子文件夹的ID，否则为空集合表示不过滤
        Set<Long> folderFilterIds = buildFolderFilterIds(folderId, folderMap);
        Set<Long> requiredTagIds = normalizeTagIds(tagIds);
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String keywordLower = normalizedKeyword.toLowerCase();
        String normalizedScope = normalizeSearchScope(scope);
        //获取知识库下的笔记列表，按标题字母序排序，构建笔记ID与笔记内容的键值对，以及笔记ID与标签列表的键值对
        List<Note> notes = noteMapper.selectActiveByUserIdAndKnowledgeBaseIdOrderByTitleAsc(userId, knowledgeBaseId);
        if (notes.isEmpty()) {
            return new ArrayList<>();
        }
        //批量查询笔记内容
        List<Long> noteIds = notes.stream().map(Note::getId).toList();
        Map<Long, NoteContent> noteContentMap = new HashMap<>();
        for (NoteContent noteContent : noteContentMapper.selectByNoteIds(noteIds)) {
            noteContentMap.put(noteContent.getNoteId(), noteContent);
        }

        Map<Long, List<TagVO>> noteTagMap = buildNoteTagMap(notes);
        Map<Long, Set<Long>> noteTagIdMap = buildNoteTagIdMap(noteIds);
        Map<Long, LinkStats> linkStatsMap = buildLinkStatsMap(knowledgeBaseId);
        //根据检索条件过滤笔记，并构建搜索结果列表，最后按相关度、更新时间和标题排序返回前120条结果
        List<ScoredSearchResult> results = new ArrayList<>();
        for (Note note : notes) {
            if (!folderFilterIds.isEmpty()) {
                if (note.getFolderId() == null || !folderFilterIds.contains(note.getFolderId())) {
                    continue;
                }
            }
        //如果笔记的标签ID集合不包含所有必选标签ID，则跳过该笔记
            Set<Long> currentTagIds = noteTagIdMap.getOrDefault(note.getId(), Set.of());
            if (!requiredTagIds.isEmpty() && !currentTagIds.containsAll(requiredTagIds)) {
                continue;
            }
        //笔记标题、内容和标签都可能为null，转换为小写后再进行匹配，如果keywordLower在标题、内容或任一标签名称中出现，则认为匹配成功
            String title = note.getTitle() == null ? "" : note.getTitle();
            String markdownContent = normalizeSearchText(noteContentMap.get(note.getId()) == null
                    ? null
                    : noteContentMap.get(note.getId()).getMarkdownContent());
            List<TagVO> tags = noteTagMap.getOrDefault(note.getId(), new ArrayList<>());

            boolean matchedByTitle = !keywordLower.isEmpty() && title.toLowerCase().contains(keywordLower);
            boolean matchedByContent = !keywordLower.isEmpty() && markdownContent.toLowerCase().contains(keywordLower);
            boolean matchedByTag = !keywordLower.isEmpty()
                    && tags.stream().anyMatch(tag -> tag.getName() != null && tag.getName().toLowerCase().contains(keywordLower));

            boolean matched = keywordLower.isEmpty()
                    || switch (normalizedScope) {
                        case "title" -> matchedByTitle;
                        case "content" -> matchedByContent;
                        case "tag" -> matchedByTag;
                        default -> matchedByTitle || matchedByContent || matchedByTag;
                    };

            if (!matched) {
                continue;
            }

            LinkStats linkStats = linkStatsMap.getOrDefault(note.getId(), new LinkStats());

            NoteSearchResultVO result = new NoteSearchResultVO();
            result.setNoteId(LongStringUtils.toStringValue(note.getId()));
            result.setKnowledgeBaseId(LongStringUtils.toStringValue(note.getKnowledgeBaseId()));
            result.setFolderId(LongStringUtils.toStringValue(note.getFolderId()));
            result.setTitle(title);
            result.setFolderPath(buildFolderPath(note.getFolderId(), folderMap));
            result.setSnippet(buildSearchSnippet(title, markdownContent, tags, keywordLower, matchedByTitle, matchedByContent));
            result.setMatchedByTitle(matchedByTitle);
            result.setMatchedByContent(matchedByContent);
            result.setMatchedByTag(matchedByTag);
            result.setIncomingCount(linkStats.incomingCount);
            result.setOutgoingCount(linkStats.outgoingCount);
            result.setBrokenLinkCount(linkStats.brokenLinkCount);
            result.setCreatedTime(note.getCreatedTime());
            result.setUpdatedTime(note.getUpdatedTime());
            result.setTags(new ArrayList<>(tags));
            //计算相关度得分，标题匹配6分，标签匹配4分，内容匹配2分，包含必选标签加1分
            int score = 0;
            if (matchedByTitle) {
                score += 6;
            }
            if (matchedByTag) {
                score += 4;
            }
            if (matchedByContent) {
                score += 2;
            }
            if (!requiredTagIds.isEmpty()) {
                score += 1;
            }

            results.add(new ScoredSearchResult(score, result));
        }

        results.sort(
                Comparator.comparingInt(ScoredSearchResult::score).reversed()
                        .thenComparing(item -> item.result().getUpdatedTime(), Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(item -> item.result().getTitle(), String.CASE_INSENSITIVE_ORDER)
        );

        return results.stream().map(ScoredSearchResult::result).limit(120).toList();
    }

    /**
    * 创建文件夹。
     * @param userId 用户ID
     * @param dto 创建文件夹请求参数
    */
    @Transactional(rollbackFor = Exception.class)
    public TreeNodeVO createFolder(Long userId, CreateFolderDTO dto) {
        KnowledgeBase knowledgeBase = getKnowledgeBaseOrThrow(userId, dto.getKnowledgeBaseId());
        //校验父文件夹是否合法
        validateParentFolder(knowledgeBase.getId(), dto.getParentId());
        //校验同级目录下文件夹名称是否已存在
        String name = dto.getName().trim();
        checkFolderNameExists(knowledgeBase.getId(), dto.getParentId(), name, null);
        //创建文件夹
        Folder folder = new Folder();
        folder.setKnowledgeBaseId(knowledgeBase.getId());
        folder.setParentId(dto.getParentId());
        folder.setName(name);
        folder.setStatus(1);
        folder.setDeleteToken(0L);
        if (folderMapper.insert(folder) <= 0) {
            throw new BusinessException("创建文件夹失败");
        }
        //更新知识库更新时间
        touchKnowledgeBase(knowledgeBase.getId());
        return toFolderNode(folder);
    }

    /**
    * 更新文件夹。
     * @param userId 用户ID
     * @param folderId 文件夹ID
     * @param dto 更新文件夹请求参数
    */
    @Transactional(rollbackFor = Exception.class)
    public TreeNodeVO updateFolder(Long userId, Long folderId, UpdateFolderDTO dto) {
        Folder folder = getFolderByIdOrThrow(folderId);
        getKnowledgeBaseOrThrow(userId, folder.getKnowledgeBaseId());
        validateParentFolder(folder.getKnowledgeBaseId(), dto.getParentId());

        if (Objects.equals(folder.getId(), dto.getParentId())) {
            throw new BusinessException("文件夹不能移动到自身下面");
        }
        if (dto.getParentId() != null && isDescendantFolder(folder.getId(), dto.getParentId())) {
            throw new BusinessException("文件夹不能移动到自己的子目录下");
        }

        String name = dto.getName().trim();
        checkFolderNameExists(folder.getKnowledgeBaseId(), dto.getParentId(), name, folderId);

        folder.setParentId(dto.getParentId());
        folder.setName(name);
        if (folderMapper.updateById(folder) <= 0) {
            throw new BusinessException("更新文件夹失败");
        }

        touchKnowledgeBase(folder.getKnowledgeBaseId());
        return toFolderNode(folder);
    }

    /**
    * 删除文件夹。
     * @param userId 用户ID
     * @param folderId 文件夹ID
    */
    @Transactional(rollbackFor = Exception.class)
    public void deleteFolder(Long userId, Long folderId) {
        Folder folder = getFolderByIdOrThrow(folderId);
        getKnowledgeBaseOrThrow(userId, folder.getKnowledgeBaseId());

        List<Folder> folders = folderMapper.selectActiveByKnowledgeBaseIdOrderByNameAsc(folder.getKnowledgeBaseId());
        Set<Long> folderIdsToDelete = collectFolderIdsToDelete(folders, folderId);
        LocalDateTime deletedTime = LocalDateTime.now();

        List<Note> notes = noteMapper.selectActiveByKnowledgeBaseId(folder.getKnowledgeBaseId());
        for (Note note : notes) {
            if (!folderIdsToDelete.contains(note.getFolderId())) {
                continue;
            }

            note.setStatus(2);
            note.setDeletedTime(deletedTime);
            note.setDeleteToken(note.getId());
            if (noteMapper.updateById(note) <= 0) {
                throw new BusinessException("删除文件夹下的笔记失败");
            }
            noteLinkMapper.deleteBySourceNoteId(note.getId());
            noteTagMapper.deleteByNoteId(note.getId());
        }

        for (Folder currentFolder : folders) {
            if (!folderIdsToDelete.contains(currentFolder.getId())) {
                continue;
            }

            currentFolder.setStatus(2);
            currentFolder.setDeletedTime(deletedTime);
            currentFolder.setDeleteToken(currentFolder.getId());
            if (folderMapper.updateById(currentFolder) <= 0) {
                throw new BusinessException("删除文件夹失败");
            }
        }

        refreshBrokenLinks(folder.getKnowledgeBaseId());
        touchKnowledgeBase(folder.getKnowledgeBaseId());
    }

    /**
    * 刷新知识库更新时间。
     * @param knowledgeBaseId 知识库ID
    */
    public void touchKnowledgeBase(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            return;
        }
        if (knowledgeBaseMapper.touchUpdatedTime(knowledgeBaseId) <= 0) {
            throw new BusinessException("更新知识库更新时间失败");
        }
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectByKnowledgeBaseId(knowledgeBaseId);
        if (knowledgeBase != null) {
            cacheInvalidationService.evictKnowledgeBaseCaches(knowledgeBase.getUserId(), knowledgeBaseId);
        }
    }

    /**
    * 获取知识库，不存在时抛出异常。
     * @param userId 用户ID
     * @param knowledgeBaseId 知识库ID
    */
    public KnowledgeBase getKnowledgeBaseOrThrow(Long userId, Long knowledgeBaseId) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectByKnowledgeBaseId(knowledgeBaseId);
        if (knowledgeBase == null
                || !Objects.equals(knowledgeBase.getUserId(), userId)
                || knowledgeBase.getStatus() == null
                || knowledgeBase.getStatus() != 1
                || !Objects.equals(knowledgeBase.getDeleteToken(), 0L)) {
            throw new BusinessException("知识库不存在或无权限访问");
        }
        return knowledgeBase;
    }

    /**
    * 获取知识库中的文件夹，不存在时抛出异常。
     * @param knowledgeBaseId 知识库ID
     * @param folderId 文件夹ID
    */
    public Folder getFolderInKnowledgeBaseOrThrow(Long knowledgeBaseId, Long folderId) {
        Folder folder = getFolderByIdOrThrow(folderId);
        if (!Objects.equals(folder.getKnowledgeBaseId(), knowledgeBaseId)
                || folder.getStatus() == null
                || folder.getStatus() != 1
                || !Objects.equals(folder.getDeleteToken(), 0L)) {
            throw new BusinessException("文件夹不存在");
        }
        return folder;
    }

    /**
    * 根据 ID 获取文件夹，不存在时抛出异常。
     * @param folderId 文件夹ID
    */
    private Folder getFolderByIdOrThrow(Long folderId) {
        Folder folder = folderMapper.selectByFolderId(folderId);
        if (folder == null
                || folder.getStatus() == null
                || folder.getStatus() != 1
                || !Objects.equals(folder.getDeleteToken(), 0L)) {
            throw new BusinessException("文件夹不存在");
        }
        return folder;
    }

    /**
    * 判断目标文件夹是否为子级目录。
     * @param folderId 文件夹ID
     * @param targetParentId 目标父文件夹ID
    */
    private boolean isDescendantFolder(Long folderId, Long targetParentId) {
        Long currentParentId = targetParentId;
        while (currentParentId != null) {
            if (Objects.equals(currentParentId, folderId)) {
                return true;
            }
            Folder parent = folderMapper.selectByFolderId(currentParentId);
            if (parent == null) {
                return false;
            }
            currentParentId = parent.getParentId();
        }
        return false;
    }

    /**
    * 收集待删除的文件夹 ID。
     * @param folders 文件夹列表
     * @param rootFolderId 根文件夹ID
    */
    private Set<Long> collectFolderIdsToDelete(List<Folder> folders, Long rootFolderId) {
        Map<Long, List<Long>> childrenMap = new HashMap<>();
        for (Folder folder : folders) {
            if (folder.getParentId() == null) {
                continue;
            }
            childrenMap.computeIfAbsent(folder.getParentId(), key -> new ArrayList<>()).add(folder.getId());
        }

        Set<Long> folderIds = new HashSet<>();
        collectFolderIdsRecursively(rootFolderId, childrenMap, folderIds);
        return folderIds;
    }

    /**
    * 递归收集文件夹 ID。
     * @param folderId 文件夹ID
     * @param childrenMap 父子节点映射
     * @param collector 结果收集器
    */
    private void collectFolderIdsRecursively(Long folderId, Map<Long, List<Long>> childrenMap, Set<Long> collector) {
        if (folderId == null || !collector.add(folderId)) {
            return;
        }

        for (Long childFolderId : childrenMap.getOrDefault(folderId, List.of())) {
            collectFolderIdsRecursively(childFolderId, childrenMap, collector);
        }
    }

    /**
    * 刷新失效双链状态。
     * @param knowledgeBaseId 知识库ID
    */
    private void refreshBrokenLinks(Long knowledgeBaseId) {
        List<Note> activeNotes = noteMapper.selectActiveByKnowledgeBaseId(knowledgeBaseId);
        Map<String, Long> titleMap = new HashMap<>();
        for (Note note : activeNotes) {
            titleMap.put(note.getTitle(), note.getId());
        }

        List<NoteLink> links = noteLinkMapper.selectByKnowledgeBaseId(knowledgeBaseId);
        for (NoteLink link : links) {
            Long targetNoteId = titleMap.get(link.getTargetNoteName());
            link.setTargetNoteId(targetNoteId);
            link.setIsBroken(targetNoteId == null ? 1 : 0);
            noteLinkMapper.updateById(link);
        }
    }

    /**
    * 校验父级文件夹是否合法。
     * @param knowledgeBaseId 知识库ID
     * @param parentId parentID
    */
    private void validateParentFolder(Long knowledgeBaseId, Long parentId) {
        if (parentId == null) {
            return;
        }
        Folder parentFolder = getFolderByIdOrThrow(parentId);
        if (!Objects.equals(parentFolder.getKnowledgeBaseId(), knowledgeBaseId)) {
            throw new BusinessException("父文件夹不属于当前知识库");
        }
    }

    /**
    * 检查知识库名称是否已存在。
     * @param userId 用户ID
     * @param name 知识库名称
     * @param excludeId 排除的知识库ID（更新时使用，新增时传null）
     * @throws BusinessException 如果知识库名称已存在则抛出异常
    */
    private void checkKnowledgeBaseNameExists(Long userId, String name, Long excludeId) {
        List<KnowledgeBase> existing = knowledgeBaseMapper.selectActiveByUserIdAndName(userId, name);
        boolean exists = existing.stream().anyMatch(item -> !Objects.equals(item.getId(), excludeId));
        if (exists) {
            throw new BusinessException("知识库名称已存在");
        }
    }

    /**
    * 检查同父目录下文件夹名称是否已存在。
     * @param knowledgeBaseId 知识库ID
     * @param parentId parentID
     * @param name 名称
     * @param excludeId 排除的文件夹ID（更新时使用，新增时传null）
    */
    private void checkFolderNameExists(Long knowledgeBaseId, Long parentId, String name, Long excludeId) {
        List<Folder> folders = folderMapper.selectActiveByKnowledgeBaseIdAndParentIdAndName(knowledgeBaseId, parentId, name);
        boolean exists = folders.stream().anyMatch(folder -> !Objects.equals(folder.getId(), excludeId));
        if (exists) {
            throw new BusinessException("同级目录下已存在同名文件夹");
        }
    }

    /**
    * 递归排序树节点。
     * @param nodes 树节点列表
    */
    private void sortNodesRecursively(List<TreeNodeVO> nodes) {
        //文件夹优先于笔记，同级节点按名称字母序排序，忽略大小写
        //直接按type字典序排就是文件夹在前，笔记在后，无需额外处理
        //后面常量为忽略大小写
        nodes.sort(Comparator.comparing(TreeNodeVO::getType).reversed()
                .thenComparing(TreeNodeVO::getName, String.CASE_INSENSITIVE_ORDER));
        for (TreeNodeVO node : nodes) {
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                sortNodesRecursively(node.getChildren());
            }
        }
    }

    /**
    * 构建文件夹过滤 ID 集合。
     * @param folderId 文件夹ID
     * @param folderMap 文件夹映射
    */
    private Set<Long> buildFolderFilterIds(Long folderId, Map<Long, Folder> folderMap) {
        if (folderId == null) {
            return new HashSet<>();
        }

        Folder folder = folderMap.get(folderId);
        if (folder == null) {
            throw new BusinessException("检索指定的文件夹不存在");
        }

        Map<Long, List<Long>> childrenMap = new HashMap<>();
        for (Folder currentFolder : folderMap.values()) {
            if (currentFolder.getParentId() == null) {
                continue;
            }
            childrenMap.computeIfAbsent(currentFolder.getParentId(), key -> new ArrayList<>()).add(currentFolder.getId());
        }

        Set<Long> folderIds = new HashSet<>();
        collectFolderIdsRecursively(folderId, childrenMap, folderIds);
        return folderIds;
    }

    /**
    * 规范化标签 ID 列表。
     * @param tagIds 标签ID列表
    */
    private Set<Long> normalizeTagIds(List<Long> tagIds) {
        Set<Long> result = new LinkedHashSet<>();
        if (tagIds == null) {
            return result;
        }
        for (Long tagId : tagIds) {
            if (tagId != null) {
                result.add(tagId);
            }
        }
        return result;
    }

    /**
    * 规范化检索范围。
     * @param scope 检索范围
    */
    private String normalizeSearchScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return "all";
        }

        String normalized = scope.trim().toLowerCase();
        return switch (normalized) {
            case "title", "content", "tag" -> normalized;
            default -> "all";
        };
    }

    /**
    * 构建文件夹路径文本。
     * @param folderId 文件夹ID
     * @param folderMap 文件夹映射
    */
    private String buildFolderPath(Long folderId, Map<Long, Folder> folderMap) {
        if (folderId == null) {
            return "知识库根目录";
        }

        List<String> pathSegments = new ArrayList<>();
        Long currentFolderId = folderId;
        while (currentFolderId != null) {
            Folder folder = folderMap.get(currentFolderId);
            if (folder == null) {
                break;
            }
            pathSegments.add(folder.getName());
            currentFolderId = folder.getParentId();
        }

        if (pathSegments.isEmpty()) {
            return "知识库根目录";
        }

        java.util.Collections.reverse(pathSegments);
        return String.join(" / ", pathSegments);
    }

    /**
    * 规范化检索文本。
     * @param value 待处理的值
    */
    private String normalizeSearchText(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.replace('\r', '\n').replaceAll("\\s+", " ").trim();
    }

    /**
    * 构建搜索结果摘要。
     * @param title 笔记标题
     * @param markdownContent Markdown正文
     * @param tags 标签列表
     * @param keywordLower 小写检索关键词
     * @param matchedByTitle 是否按标题匹配
     * @param matchedByContent 是否按正文匹配
    */
    private String buildSearchSnippet(String title,
                                      String markdownContent,
                                      List<TagVO> tags,
                                      String keywordLower,
                                      boolean matchedByTitle,
                                      boolean matchedByContent) {
        if (matchedByContent && !markdownContent.isBlank()) {
            return buildContentSnippet(markdownContent, keywordLower);
        }

        if (matchedByTitle && title != null && !title.isBlank()) {
            return title;
        }

        if (!keywordLower.isEmpty()) {
            List<String> matchedTagNames = tags.stream()
                    .map(TagVO::getName)
                    .filter(Objects::nonNull)
                    .filter(name -> name.toLowerCase().contains(keywordLower))
                    .toList();
            if (!matchedTagNames.isEmpty()) {
                return "匹配标签：" + matchedTagNames.stream().map(name -> "#" + name).collect(Collectors.joining(" "));
            }
        }

        if (!markdownContent.isBlank()) {
            return markdownContent.length() <= 140 ? markdownContent : markdownContent.substring(0, 140) + "...";
        }

        return "当前笔记暂无可预览的正文内容。";
    }

    /**
    * 构建正文片段摘要。
     * @param content 正文内容
     * @param keywordLower 小写检索关键词
    */
    private String buildContentSnippet(String content, String keywordLower) {
        if (content.isBlank()) {
            return "当前笔记暂无可预览的正文内容。";
        }

        if (keywordLower.isBlank()) {
            return content.length() <= 180 ? content : content.substring(0, 180) + "...";
        }

        int index = content.toLowerCase().indexOf(keywordLower);
        if (index < 0) {
            return content.length() <= 180 ? content : content.substring(0, 180) + "...";
        }

        int start = Math.max(0, index - 48);
        int end = Math.min(content.length(), index + keywordLower.length() + 96);
        String snippet = content.substring(start, end).trim();
        if (start > 0) {
            snippet = "..." + snippet;
        }
        if (end < content.length()) {
            snippet = snippet + "...";
        }
        return snippet;
    }

    /**
    * 将知识库实体转换为知识库视图对象。
     * @param knowledgeBase 知识库实体
    */
    private KnowledgeBaseVO toKnowledgeBaseVO(KnowledgeBase knowledgeBase) {
        KnowledgeBaseVO vo = new KnowledgeBaseVO();
        BeanUtils.copyProperties(knowledgeBase, vo);
        vo.setId(LongStringUtils.toStringValue(knowledgeBase.getId()));
        return vo;
    }

    /**
    * 将文件夹实体转换为树节点对象。
     * @param folder 文件夹实体
    */
    private TreeNodeVO toFolderNode(Folder folder) {
        TreeNodeVO node = new TreeNodeVO();
        node.setId(LongStringUtils.toStringValue(folder.getId()));
        node.setParentId(LongStringUtils.toStringValue(folder.getParentId()));
        node.setName(folder.getName());
        node.setType("folder");
        node.setKnowledgeBaseId(LongStringUtils.toStringValue(folder.getKnowledgeBaseId()));
        node.setCreatedTime(folder.getCreatedTime());
        node.setUpdatedTime(folder.getUpdatedTime());
        return node;
    }

    /**
    * 构建笔记标签映射。
     * @param notes 笔记列表
    */
    private Map<Long, List<TagVO>> buildNoteTagMap(List<Note> notes) {
        Map<Long, List<TagVO>> result = new HashMap<>();
        if (notes == null || notes.isEmpty()) {
            return result;
        }

        List<Long> noteIds = new ArrayList<>();
        for (Note note : notes) {
            noteIds.add(note.getId());
        }

        List<NoteTag> noteTags = noteTagMapper.selectByNoteIds(noteIds);
        if (noteTags == null || noteTags.isEmpty()) {
            return result;
        }

        Set<Long> tagIds = new HashSet<>();
        for (NoteTag noteTag : noteTags) {
            tagIds.add(noteTag.getTagId());
        }

        Map<Long, TagVO> tagMap = new HashMap<>();
        for (Tag tag : tagMapper.selectByTagIds(tagIds)) {
            tagMap.put(tag.getId(), toTagVO(tag));
        }

        for (NoteTag noteTag : noteTags) {
            TagVO tag = tagMap.get(noteTag.getTagId());
            if (tag == null) {
                continue;
            }
            result.computeIfAbsent(noteTag.getNoteId(), key -> new ArrayList<>()).add(tag);
        }

        for (List<TagVO> tags : result.values()) {
            tags.sort(Comparator.comparing(TagVO::getName, String.CASE_INSENSITIVE_ORDER));
        }
        return result;
    }

    /**
    * 构建笔记标签 ID 映射。
     * @param noteIds 笔记ID列表
    */
    private Map<Long, Set<Long>> buildNoteTagIdMap(List<Long> noteIds) {
        Map<Long, Set<Long>> result = new HashMap<>();
        if (noteIds == null || noteIds.isEmpty()) {
            return result;
        }

        for (NoteTag noteTag : noteTagMapper.selectByNoteIds(noteIds)) {
            result.computeIfAbsent(noteTag.getNoteId(), key -> new LinkedHashSet<>()).add(noteTag.getTagId());
        }
        return result;
    }

    /**
    * 构建双链统计映射。
     * @param knowledgeBaseId 知识库ID
    */
    private Map<Long, LinkStats> buildLinkStatsMap(Long knowledgeBaseId) {
        Map<Long, LinkStats> result = new HashMap<>();
        for (NoteLink link : noteLinkMapper.selectByKnowledgeBaseId(knowledgeBaseId)) {
            result.computeIfAbsent(link.getSourceNoteId(), key -> new LinkStats()).outgoingCount += 1;
            if (link.getTargetNoteId() != null) {
                result.computeIfAbsent(link.getTargetNoteId(), key -> new LinkStats()).incomingCount += 1;
            }
            if (Objects.equals(link.getIsBroken(), 1)) {
                result.computeIfAbsent(link.getSourceNoteId(), key -> new LinkStats()).brokenLinkCount += 1;
            }
        }
        return result;
    }

    /**
    * 将标签实体转换为标签视图对象。
     * @param tag 标签实体
    */
    private TagVO toTagVO(Tag tag) {
        TagVO vo = new TagVO();
        BeanUtils.copyProperties(tag, vo);
        vo.setId(LongStringUtils.toStringValue(tag.getId()));
        vo.setKnowledgeBaseId(LongStringUtils.toStringValue(tag.getKnowledgeBaseId()));
        return vo;
    }

    /**
    * 去除字符串首尾空白并在为空时返回 null。
     * @param value 待处理的值
    */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static final class LinkStats {
        private int incomingCount;
        private int outgoingCount;
        private int brokenLinkCount;
    }

    private record ScoredSearchResult(int score, NoteSearchResultVO result) {}
}
