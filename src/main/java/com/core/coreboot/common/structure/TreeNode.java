package com.core.coreboot.common.structure;

import java.util.List;

/**
 * 通用树节点接口
 * @param <T> ID 数据类型
 * @param <N> 树节点数据类型，必须实现 TreeNode<T, N> 接口
 */
public interface TreeNode<T, N extends TreeNode<T, N>> {
    /**
     * 获取节点 ID
     * @return 节点 ID
     */
    T getId();

    /**
     * 获取父节点 ID
     * @return 父节点 ID
     */
    T getParentId();

    /**
     * 获取子节点列表
     * @return 子节点列表
     */
    List<N> getChildrenList();

    /**
     * 设置子节点列表
     * @param children 子节点列表
     */
    void setChildrenList(List<N> children);
}
