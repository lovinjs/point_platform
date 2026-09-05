package com.core.coreboot.order.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.core.coreboot.common.dto.BasePageReq;
import com.core.coreboot.order.entity.Order;
import com.core.coreboot.order.dto.CreateOrderReq;
import com.core.coreboot.order.vo.OrderVO;
import org.springframework.stereotype.Service;

@Service
public interface OrderService extends IService<Order> {

    String create(CreateOrderReq createOrderReq);

    OrderVO detail(String orderNo);

    void cancel(String orderNo);

    String qrcode(String orderNo);

    void pay(String orderNo);

    void shipped(String orderNo);

    void done(String orderNo);

    IPage<OrderVO> listForUser(BasePageReq basePageReq);

    IPage<OrderVO> listForAdmin(BasePageReq basePageReq);
}
