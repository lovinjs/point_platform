package com.core.coreboot.order.converter;

import com.core.coreboot.common.Constant;
import com.core.coreboot.order.entity.Order;
import com.core.coreboot.order.entity.OrderItem;
import com.core.coreboot.order.vo.OrderItemVO;
import com.core.coreboot.order.vo.OrderVO;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface OrderConverter {
    OrderItemVO toOrderItemVo(OrderItem orderItem);

    List<OrderItemVO> toOrderItemVoList(List<OrderItem> orderItemList);

    @Mapping(target = "orderStatusName", ignore = true)
    @Mapping(target = "orderItemVOList", source = "orderItemList")
    OrderVO toOrderVO(Order order, List<OrderItem> orderItemList);

    @AfterMapping
    default void fillOrderStatusName(@MappingTarget OrderVO orderVO) {
        if (orderVO != null && orderVO.getOrderStatus() != null) {
            orderVO.setOrderStatusName(Constant.OrderStatusEnum.getByCode(orderVO.getOrderStatus()).getValue());
        }
    }
}
