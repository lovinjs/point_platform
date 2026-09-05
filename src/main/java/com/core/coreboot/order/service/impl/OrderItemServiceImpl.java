package com.core.coreboot.order.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.core.coreboot.order.entity.OrderItem;
import com.core.coreboot.order.mapper.OrderItemMapper;
import com.core.coreboot.order.service.OrderItemService;
import org.springframework.stereotype.Service;

@Service
public class OrderItemServiceImpl extends ServiceImpl<OrderItemMapper, OrderItem> implements OrderItemService {
}
