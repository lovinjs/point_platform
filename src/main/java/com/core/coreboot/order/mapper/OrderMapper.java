package com.core.coreboot.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;
import com.core.coreboot.order.entity.Order;

@Repository
public interface OrderMapper extends BaseMapper<Order> {
}
