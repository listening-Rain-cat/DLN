package org.example.dln.service;

import org.example.dln.dto.CreateFolderDTO;
import org.example.dln.dto.CreateKnowledgeBaseDTO;
import org.example.dln.dto.UpdateFolderDTO;
import org.example.dln.dto.UpdateKnowledgeBaseDTO;
import org.example.dln.entity.Folder;
import org.example.dln.entity.KnowledgeBase;
import org.example.dln.entity.Note;
import org.example.dln.entity.NoteLink;
import org.example.dln.entity.NoteTag;
import org.example.dln.entity.Tag;
import org.example.dln.exception.BusinessException;
import org.example.dln.mapper.FolderMapper;
import org.example.dln.mapper.KnowledgeBaseMapper;
import org.example.dln.mapper.NoteLinkMapper;
import org.example.dln.mapper.NoteMapper;
import org.example.dln.mapper.NoteTagMapper;
import org.example.dln.mapper.TagMapper;
import org.example.dln.vo.KnowledgeBaseVO;
import org.example.dln.vo.KnowledgeGraphEdgeVO;
import org.example.dln.vo.KnowledgeGraphNodeVO;
import org.example.dln.vo.KnowledgeGraphVO;
import org.example.dln.vo.TagVO;
import org.example.dln.vo.TreeNodeVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
    private NoteLinkMapper noteLinkMapper;

    @Autowired
    private NoteTagMapper noteTagMapper;

    @Autowired
    private TagMapper tagMapper;

    /**
    * 创建知识库。
    */
    public KnowledgeBaseVO createKnowledgeBase(Long userId, CreateKnowledgeBaseDTO dto) {
        String name = dto.getName().trim();
        checkKnowledgeBaseNameExists(userId, name, null);

        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setUserId(userId);
        knowledgeBase.setName(name);
        knowledgeBase.setDescription(trimToNull(dto.getDescription()));
        knowledgeBase.setStatus(1);
        knowledgeBase.setDeleteToken(0L);
        if (knowledgeBaseMapper.insert(knowledgeBase) <= 0) {
            throw new BusinessException("创建知识库失败");
        }
        return toKnowledgeBaseVO(knowledgeBase);
    }

    /**
    * 查询知识库列表。
    */
    public List<KnowledgeBaseVO> listKnowledgeBases(Long userId) {
        return knowledgeBaseMapper.selectActiveByUserIdOrderByUpdatedTimeDesc(userId)
                .stream()
                .map(this::toKnowledgeBaseVO)
                .toList();
    }

    /**
    * 更新知识库。
    */
    @Transactional(rollbackFor = Exception.class)
    public KnowledgeBaseVO updateKnowledgeBase(Long userId, Long knowledgeBaseId, UpdateKnowledgeBaseDTO dto) {
        KnowledgeBase knowledgeBase = getKnowledgeBaseOrThrow(userId, knowledgeBaseId);
        String name = dto.getName().trim();
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
    */
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeBase(Long userId, Long knowledgeBaseId) {
        KnowledgeBase knowledgeBase = getKnowledgeBaseOrThrow(userId, knowledgeBaseId);
        List<Folder> folders = folderMapper.selectActiveByKnowledgeBaseIdOrderByNameAsc(knowledgeBaseId);
        List<Note> notes = noteMapper.selectActiveByKnowledgeBaseId(knowledgeBaseId);
        LocalDateTime deletedTime = LocalDateTime.now();
        for (Note note : notes) {
            note.setStatus(2);
            note.setDeletedTime(deletedTime);
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
    }

    /**
    * 获取知识库目录树。
    */
    public List<TreeNodeVO> getKnowledgeBaseTree(Long userId, Long knowledgeBaseId) {
        getKnowledgeBaseOrThrow(userId, knowledgeBaseId);

        List<Folder> folders = folderMapper.selectActiveByKnowledgeBaseIdOrderByNameAsc(knowledgeBaseId);
        List<Note> notes = noteMapper.selectActiveByUserIdAndKnowledgeBaseIdOrderByTitleAsc(userId, knowledgeBaseId);
        Map<Long, List<TagVO>> noteTagMap = buildNoteTagMap(notes);

        Map<Long, TreeNodeVO> folderNodeMap = new HashMap<>();
        for (Folder folder : folders) {
            TreeNodeVO node = toFolderNode(folder);
            folderNodeMap.put(folder.getId(), node);
        }

        List<TreeNodeVO> roots = new ArrayList<>();
        for (TreeNodeVO folderNode : folderNodeMap.values()) {
            if (folderNode.getParentId() == null) {
                roots.add(folderNode);
                continue;
            }

            TreeNodeVO parent = folderNodeMap.get(folderNode.getParentId());
            if (parent != null) {
                parent.getChildren().add(folderNode);
            }
        }

        for (Note note : notes) {
            TreeNodeVO node = new TreeNodeVO();
            node.setId(note.getId());
            node.setParentId(note.getFolderId());
            node.setName(note.getTitle());
            node.setType("note");
            node.setKnowledgeBaseId(note.getKnowledgeBaseId());
            node.setCreatedTime(note.getCreatedTime());
            node.setUpdatedTime(note.getUpdatedTime());
            node.setTags(noteTagMap.getOrDefault(note.getId(), new ArrayList<>()));

            if (note.getFolderId() == null) {
                roots.add(node);
            } else {
                TreeNodeVO parent = folderNodeMap.get(note.getFolderId());
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    roots.add(node);
                }
            }
        }

        sortNodesRecursively(roots);
        return roots;
    }

    /**
    * 获取知识图谱。
    */
    public KnowledgeGraphVO getKnowledgeGraph(Long userId, Long knowledgeBaseId) {
        getKnowledgeBaseOrThrow(userId, knowledgeBaseId);

        List<Note> notes = noteMapper.selectActiveByUserIdAndKnowledgeBaseIdOrderByTitleAsc(userId, knowledgeBaseId);
        List<NoteLink> links = noteLinkMapper.selectByKnowledgeBaseId(knowledgeBaseId);

        Map<Long, Note> noteMap = new HashMap<>();
        Map<Long, Integer> incomingCountMap = new HashMap<>();
        Map<Long, Integer> outgoingCountMap = new HashMap<>();

        for (Note note : notes) {
            noteMap.put(note.getId(), note);
            incomingCountMap.put(note.getId(), 0);
            outgoingCountMap.put(note.getId(), 0);
        }

        List<KnowledgeGraphEdgeVO> edgeList = new ArrayList<>();
        for (NoteLink link : links) {
            if (link.getTargetNoteId() == null) {
                continue;
            }
            if (!noteMap.containsKey(link.getSourceNoteId()) || !noteMap.containsKey(link.getTargetNoteId())) {
                continue;
            }

            outgoingCountMap.computeIfPresent(link.getSourceNoteId(), (key, value) -> value + 1);
            incomingCountMap.computeIfPresent(link.getTargetNoteId(), (key, value) -> value + 1);

            KnowledgeGraphEdgeVO edge = new KnowledgeGraphEdgeVO();
            edge.setId(link.getId());
            edge.setSourceNoteId(link.getSourceNoteId());
            edge.setTargetNoteId(link.getTargetNoteId());
            edge.setTargetNoteName(link.getTargetNoteName());
            edge.setIsBroken(link.getIsBroken());
            edgeList.add(edge);
        }

        List<KnowledgeGraphNodeVO> nodeList = new ArrayList<>();
        for (Note note : notes) {
            KnowledgeGraphNodeVO node = new KnowledgeGraphNodeVO();
            node.setNoteId(note.getId());
            node.setFolderId(note.getFolderId());
            node.setTitle(note.getTitle());
            node.setIncomingCount(incomingCountMap.getOrDefault(note.getId(), 0));
            node.setOutgoingCount(outgoingCountMap.getOrDefault(note.getId(), 0));
            nodeList.add(node);
        }

        KnowledgeGraphVO graph = new KnowledgeGraphVO();
        graph.setKnowledgeBaseId(knowledgeBaseId);
        graph.setNodes(nodeList);
        graph.setEdges(edgeList);
        return graph;
    }

    /**
    * 创建文件夹。
    */
    @Transactional(rollbackFor = Exception.class)
    public TreeNodeVO createFolder(Long userId, CreateFolderDTO dto) {
        KnowledgeBase knowledgeBase = getKnowledgeBaseOrThrow(userId, dto.getKnowledgeBaseId());
        validateParentFolder(knowledgeBase.getId(), dto.getParentId());
        String name = dto.getName().trim();
        checkFolderNameExists(knowledgeBase.getId(), dto.getParentId(), name, null);

        Folder folder = new Folder();
        folder.setKnowledgeBaseId(knowledgeBase.getId());
        folder.setParentId(dto.getParentId());
        folder.setName(name);
        folder.setStatus(1);
        folder.setDeleteToken(0L);
        if (folderMapper.insert(folder) <= 0) {
            throw new BusinessException("创建文件夹失败");
        }

        touchKnowledgeBase(knowledgeBase.getId());
        return toFolderNode(folder);
    }

    /**
    * 更新文件夹。
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
    */
    public void touchKnowledgeBase(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            return;
        }
        if (knowledgeBaseMapper.touchUpdatedTime(knowledgeBaseId) <= 0) {
            throw new BusinessException("更新知识库更新时间失败");
        }
    }

    /**
    * 获取知识库，不存在时抛出异常。
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
    */
    private void checkKnowledgeBaseNameExists(Long userId, String name, Long excludeId) {
        List<KnowledgeBase> existing = knowledgeBaseMapper.selectActiveByUserIdAndName(userId, name);
        boolean exists = existing.stream().anyMatch(item -> !Objects.equals(item.getId(), excludeId));
        if (exists) {
            throw new BusinessException("知识库名称已存在");
        }
    }

    /**
    * 检查文件夹名称是否已存在。
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
    */
    private void sortNodesRecursively(List<TreeNodeVO> nodes) {
        nodes.sort(Comparator.comparing(TreeNodeVO::getType).reversed()
                .thenComparing(TreeNodeVO::getName, String.CASE_INSENSITIVE_ORDER));
        for (TreeNodeVO node : nodes) {
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                sortNodesRecursively(node.getChildren());
            }
        }
    }

    /**
    * 将知识库实体转换为知识库视图对象。
    */
    private KnowledgeBaseVO toKnowledgeBaseVO(KnowledgeBase knowledgeBase) {
        KnowledgeBaseVO vo = new KnowledgeBaseVO();
        BeanUtils.copyProperties(knowledgeBase, vo);
        return vo;
    }

    /**
    * 将文件夹实体转换为树节点对象。
    */
    private TreeNodeVO toFolderNode(Folder folder) {
        TreeNodeVO node = new TreeNodeVO();
        node.setId(folder.getId());
        node.setParentId(folder.getParentId());
        node.setName(folder.getName());
        node.setType("folder");
        node.setKnowledgeBaseId(folder.getKnowledgeBaseId());
        node.setCreatedTime(folder.getCreatedTime());
        node.setUpdatedTime(folder.getUpdatedTime());
        return node;
    }

    /**
    * 构建笔记标签映射。
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
    * 将标签实体转换为标签视图对象。
    */
    private TagVO toTagVO(Tag tag) {
        TagVO vo = new TagVO();
        BeanUtils.copyProperties(tag, vo);
        return vo;
    }

    /**
    * 去除字符串首尾空白并在为空时返回 null。
    */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
