package com.core.coreboot.utils;

import com.core.coreboot.common.structure.TreeNode;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 树工具类
 */
public final class TreeUtils {
    // 防止被实例化
    private TreeUtils() {}

    /**
     * 构建节点树
     * 将实现了 TreeNode 的树节点列表构建成节点树
     * @param treeNodes 树节点列表
     * @param rootParentId 根节点父 ID
     * @return 节点树
     * @param <T> ID 数据类型
     * @param <N> 节点数据类型
     */
    public static <T, N extends TreeNode<T, N>> List<N> buildTree(List<N> treeNodes, T rootParentId) {
        // 树节点为空则返回空列表
        if (CollectionUtils.isEmpty(treeNodes)) {
            return new ArrayList<>();
        }
        // 构建 Map 节点以方便查找
        Map<T, N> nodeMap = treeNodes.stream()
                .peek(node -> node.setChildrenList(new ArrayList<>()))
                .collect(Collectors.toMap(TreeNode::getId, node -> node));
        // 构建节点树
        List<N> resultTree = new ArrayList<>();
        for (N treeNode : treeNodes) {
            // 检查是否为根节点
            if (Objects.equals(treeNode.getParentId(), rootParentId)) {
                resultTree.add(treeNode);
                continue;
            }
            // 不是根节点，则找到其父节点，将自己添加到父节点的子节点列表
            N parentNode = nodeMap.get(treeNode.getParentId());
            if (parentNode != null) {
                parentNode.getChildrenList().add(treeNode);
            }
        }
        return resultTree;
    }

    /**
     * 查找树中指定节点
     * @param tree 树
     * @param targetId 指定节点 ID
     * @return 指定节点
     * @param <T> ID 数据类型
     * @param <N> 节点数据类型
     */
    public static <T, N extends TreeNode<T, N>> N findNodeInTree(List<N> tree, Integer targetId) {
        if (tree == null || CollectionUtils.isEmpty(tree)) return null;
        for (N node : tree) {
            if (node.getId().equals(targetId)) {
                return node;
            }
            N found = findNodeInTree(node.getChildrenList(), targetId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

}
