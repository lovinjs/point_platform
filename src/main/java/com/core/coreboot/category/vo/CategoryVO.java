package com.core.coreboot.category.vo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.core.coreboot.common.structure.TreeNode;

@Getter
@Setter
@ToString(exclude = {"childrenList"})
public class CategoryVO implements TreeNode<Integer, CategoryVO>, Serializable {
    private Integer id;

    private String name;

    private Integer rank;

    private Integer order;

    private Integer parentId;

    private Date createTime;

    private Date updateTime;

    private List<CategoryVO> childrenList = new ArrayList<>();
}