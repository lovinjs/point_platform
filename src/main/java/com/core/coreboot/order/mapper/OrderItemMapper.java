package com.core.coreboot.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;
import com.core.coreboot.order.entity.OrderItem;

@Repository
public interface OrderItemMapper extends BaseMapper<OrderItem> {
}
