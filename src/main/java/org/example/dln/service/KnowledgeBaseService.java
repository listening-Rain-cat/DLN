package org.example.dln.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.dln.dto.CreateFolderDTO;
import org.example.dln.dto.CreateKnowledgeBaseDTO;
import org.example.dln.dto.UpdateFolderDTO;
import org.example.dln.dto.UpdateKnowledgeBaseDTO;
import org.example.dln.entity.Folder;
import org.example.dln.entity.KnowledgeBase;
import org.example.dln.entity.Note;
import org.example.dln.exception.BusinessException;
import org.example.dln.mapper.FolderMapper;
import org.example.dln.mapper.KnowledgeBaseMapper;
import org.example.dln.mapper.NoteMapper;
import org.example.dln.vo.KnowledgeBaseVO;
import org.example.dln.vo.TreeNodeVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 知识库与目录树业务逻辑。
 */
@Service
public class KnowledgeBaseService {
    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private FolderMapper folderMapper;

    @Autowired
    private NoteMapper noteMapper;
    // 创建知识库
    public KnowledgeBaseVO createKnowledgeBase(Long userId, CreateKnowledgeBaseDTO dto) {
        String name = dto.getName().trim();
        checkKnowledgeBaseNameExists(userId, name, null);

        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.setUserId(userId);
        knowledgeBase.setName(name);
        knowledgeBase.setDescription(trimToNull(dto.getDescription()));
        knowledgeBase.setStatus(1);
        if (knowledgeBaseMapper.insert(knowledgeBase) <= 0) {
            throw new BusinessException("创建知识库失败");
        }
        return toKnowledgeBaseVO(knowledgeBase);
    }
    // 获取全部知识库
    public List<KnowledgeBaseVO> listKnowledgeBases(Long userId) {
        return knowledgeBaseMapper.selectList(new LambdaQueryWrapper<KnowledgeBase>()
                        .eq(KnowledgeBase::getUserId, userId)
                        .eq(KnowledgeBase::getStatus, 1)
                        .orderByDesc(KnowledgeBase::getUpdatedTime))
                .stream()
                .map(this::toKnowledgeBaseVO)
                .toList();
    }
    // 更新知识库
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
    // 删除知识库
    @Transactional(rollbackFor = Exception.class)
    public void deleteKnowledgeBase(Long userId, Long knowledgeBaseId) {
        KnowledgeBase knowledgeBase = getKnowledgeBaseOrThrow(userId, knowledgeBaseId);
        knowledgeBase.setStatus(0);
        if (knowledgeBaseMapper.updateById(knowledgeBase) <= 0) {
            throw new BusinessException("删除知识库失败");
        }
    }
    // 获取知识库目录树
    public List<TreeNodeVO> getKnowledgeBaseTree(Long userId, Long knowledgeBaseId) {
        getKnowledgeBaseOrThrow(userId, knowledgeBaseId);

        List<Folder> folders = folderMapper.selectList(new LambdaQueryWrapper<Folder>()
                .eq(Folder::getKnowledgeBaseId, knowledgeBaseId)
                .eq(Folder::getStatus, 1)
                .orderByAsc(Folder::getName));
        List<Note> notes = noteMapper.selectList(new LambdaQueryWrapper<Note>()
                .eq(Note::getUserId, userId)
                .eq(Note::getKnowledgeBaseId, knowledgeBaseId)
                .eq(Note::getStatus, 1)
                .orderByAsc(Note::getTitle));

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
            node.setUpdatedTime(note.getUpdatedTime());
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
    // 创建文件夹
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
        if (folderMapper.insert(folder) <= 0) {
            throw new BusinessException("创建文件夹失败");
        }
        return toFolderNode(folder);
    }
    // 更新文件夹
    @Transactional(rollbackFor = Exception.class)
    public TreeNodeVO updateFolder(Long userId, Long folderId, UpdateFolderDTO dto) {
        Folder folder = getFolderByIdOrThrow(folderId);
        getKnowledgeBaseOrThrow(userId, folder.getKnowledgeBaseId());
        validateParentFolder(folder.getKnowledgeBaseId(), dto.getParentId());
        if (folder.getId().equals(dto.getParentId())) {
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
        return toFolderNode(folder);
    }
    // 删除文件夹
    @Transactional(rollbackFor = Exception.class)
    public void deleteFolder(Long userId, Long folderId) {
        Folder folder = getFolderByIdOrThrow(folderId);
        getKnowledgeBaseOrThrow(userId, folder.getKnowledgeBaseId());

        Long childFolderCount = folderMapper.selectCount(new LambdaQueryWrapper<Folder>()
                .eq(Folder::getParentId, folderId)
                .eq(Folder::getStatus, 1));
        if (childFolderCount > 0) {
            throw new BusinessException("请先清空子文件夹后再删除");
        }

        Long childNoteCount = noteMapper.selectCount(new LambdaQueryWrapper<Note>()
                .eq(Note::getFolderId, folderId)
                .eq(Note::getStatus, 1));
        if (childNoteCount > 0) {
            throw new BusinessException("请先清空文件夹中的笔记后再删除");
        }

        folder.setStatus(2);
        if (folderMapper.updateById(folder) <= 0) {
            throw new BusinessException("删除文件夹失败");
        }
    }
    // 获取知识库
    public KnowledgeBase getKnowledgeBaseOrThrow(Long userId, Long knowledgeBaseId) {
        KnowledgeBase knowledgeBase = knowledgeBaseMapper.selectById(knowledgeBaseId);
        if (knowledgeBase == null
                || !Objects.equals(knowledgeBase.getUserId(), userId)
                || knowledgeBase.getStatus() == null
                || knowledgeBase.getStatus() != 1) {
            throw new BusinessException("知识库不存在或无权限访问");
        }
        return knowledgeBase;
    }

    public Folder getFolderInKnowledgeBaseOrThrow(Long knowledgeBaseId, Long folderId) {
        Folder folder = getFolderByIdOrThrow(folderId);
        if (!Objects.equals(folder.getKnowledgeBaseId(), knowledgeBaseId)
                || folder.getStatus() == null
                || folder.getStatus() != 1) {
            throw new BusinessException("文件夹不存在");
        }
        return folder;
    }
    // 获取文件夹
    private Folder getFolderByIdOrThrow(Long folderId) {
        Folder folder = folderMapper.selectById(folderId);
        if (folder == null || folder.getStatus() == null || folder.getStatus() != 1) {
            throw new BusinessException("文件夹不存在");
        }
        return folder;
    }
    // 判断文件夹是否是子文件夹
    private boolean isDescendantFolder(Long folderId, Long targetParentId) {
        Long currentParentId = targetParentId;
        while (currentParentId != null) {
            if (Objects.equals(currentParentId, folderId)) {
                return true;
            }
            Folder parent = folderMapper.selectById(currentParentId);
            if (parent == null) {
                return false;
            }
            currentParentId = parent.getParentId();
        }
        return false;
    }
    // 验证父文件夹
    private void validateParentFolder(Long knowledgeBaseId, Long parentId) {
        if (parentId == null) {
            return;
        }
        Folder parentFolder = getFolderByIdOrThrow(parentId);
        if (!Objects.equals(parentFolder.getKnowledgeBaseId(), knowledgeBaseId)) {
            throw new BusinessException("父文件夹不属于当前知识库");
        }
    }
    // 检查知识库名称是否存在
    private void checkKnowledgeBaseNameExists(Long userId, String name, Long excludeId) {
        List<KnowledgeBase> existing = knowledgeBaseMapper.selectList(new LambdaQueryWrapper<KnowledgeBase>()
                .eq(KnowledgeBase::getUserId, userId)
                .eq(KnowledgeBase::getName, name)
                .eq(KnowledgeBase::getStatus, 1));
        boolean exists = existing.stream().anyMatch(item -> !Objects.equals(item.getId(), excludeId));
        if (exists) {
            throw new BusinessException("知识库名称已存在");
        }
    }
    // 检查文件夹名称是否存在
    private void checkFolderNameExists(Long knowledgeBaseId, Long parentId, String name, Long excludeId) {
        List<Folder> folders = folderMapper.selectList(new LambdaQueryWrapper<Folder>()
                .eq(Folder::getKnowledgeBaseId, knowledgeBaseId)
                .eq(Folder::getName, name)
                .eq(Folder::getStatus, 1));
        boolean exists = folders.stream()
                .filter(folder -> Objects.equals(folder.getParentId(), parentId))
                .anyMatch(folder -> !Objects.equals(folder.getId(), excludeId));
        if (exists) {
            throw new BusinessException("同级目录下已存在同名文件夹");
        }
    }
    // 排序节点
    private void sortNodesRecursively(List<TreeNodeVO> nodes) {
        nodes.sort(Comparator.comparing(TreeNodeVO::getType).reversed()
                .thenComparing(TreeNodeVO::getName, String.CASE_INSENSITIVE_ORDER));
        for (TreeNodeVO node : nodes) {
            if (node.getChildren() != null && !node.getChildren().isEmpty()) {
                sortNodesRecursively(node.getChildren());
            }
        }
    }
    // 转换为知识库
    private KnowledgeBaseVO toKnowledgeBaseVO(KnowledgeBase knowledgeBase) {
        KnowledgeBaseVO vo = new KnowledgeBaseVO();
        BeanUtils.copyProperties(knowledgeBase, vo);
        return vo;
    }
    // 转换为文件夹
    private TreeNodeVO toFolderNode(Folder folder) {
        TreeNodeVO node = new TreeNodeVO();
        node.setId(folder.getId());
        node.setParentId(folder.getParentId());
        node.setName(folder.getName());
        node.setType("folder");
        node.setKnowledgeBaseId(folder.getKnowledgeBaseId());
        node.setUpdatedTime(folder.getUpdatedTime());
        return node;
    }
    // 去空
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
