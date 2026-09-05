package com.core.coreboot.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.core.coreboot.common.dto.BasePageReq;
import com.core.coreboot.order.converter.OrderConverter;
import com.core.coreboot.order.service.OrderItemService;
import com.core.coreboot.utils.PriceUtils;
import com.google.zxing.WriterException;
import com.core.coreboot.common.Constant;
import com.core.coreboot.exception.CustomException;
import com.core.coreboot.exception.ExceptionEnum;
import com.core.coreboot.filter.UserRoleFilter;
import com.core.coreboot.order.mapper.OrderMapper;
import com.core.coreboot.order.entity.OrderItem;
import com.core.coreboot.order.entity.Order;
import com.core.coreboot.user.entity.User;
import com.core.coreboot.order.dto.CreateOrderReq;
import com.core.coreboot.cart.vo.CartVO;
import com.core.coreboot.order.vo.OrderVO;
import com.core.coreboot.product.service.ProductService;
import com.core.coreboot.cart.service.CartService;
import com.core.coreboot.order.service.OrderService;
import com.core.coreboot.user.service.UserService;
import com.core.coreboot.utils.OrderNoFactory;
import com.core.coreboot.utils.QRCodeGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    private final CartService cartService;

    private final UserService userService;

    private final ProductService productService;

    private final OrderItemService orderItemService;

    private final OrderConverter orderConverter;

    @Value("${qrcode.generate.ip}")
    String ip;

    /**
     * 创建订单
     *
     * @param createOrderReq 创建订单请求
     * @return 订单号
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String create(CreateOrderReq createOrderReq) {
        // 1 获取用户id，查找购物车中对应的已勾选的商品
        Integer currentUserId = UserRoleFilter.getCurrentUser().getId();
        // TODO 需要重构，改为由前端传递购物车 id 列表过来，后端校验正确性
        List<CartVO> cartVOList = cartService.list(currentUserId);
        if (CollectionUtils.isEmpty(cartVOList)) {
            throw new CustomException(ExceptionEnum.CART_NOT_SELECTED);
        }
        // 2 判断购物车商品合理性，包括是否存在、上下架状态、库存是否充足
        cartService.validCartVOList(cartVOList);
        // 3 创建订单项，根据已选商品创建对应订单项
        List<OrderItem> orderItemList = cartVOListToOrderItemList(cartVOList);
        // 4 原子扣减商品库存，避免并发超卖
        for (OrderItem orderItem : orderItemList) {
            productService.decreaseStock(orderItem.getProductId(), orderItem.getQuantity());
        }
        // 5 从购物车中删除已选商品
        cartService.clearCartVOList(cartVOList);
        // 6 创建订单，生成订单并保存
        String orderNo = OrderNoFactory.getOrderNo();
        Order order = Order.builder()
                .orderNo(orderNo)
                .userId(currentUserId)
                .totalPrice(getTotalPrice(orderItemList))
                .receiverName(createOrderReq.getReceiverName())
                .receiverPhone(createOrderReq.getReceiverPhone())
                .receiverAddress(createOrderReq.getReceiverAddress())
                .orderStatus(Constant.OrderStatusEnum.UNPAID.getCode())
                .postage(BigDecimal.ZERO)
                .paymentType(1)
                .build();
        boolean saveOrderSuccess = save(order);
        if (!saveOrderSuccess) {
            throw new CustomException(ExceptionEnum.INSERT_FAILED);
        }
        // 7 创建订单项，根据订单号创建订单项
        orderItemList.forEach(item -> item.setOrderNo(orderNo));
        boolean saveOrderItemListSuccess = orderItemService.saveBatch(orderItemList);
        if (!saveOrderItemListSuccess) {
            throw new CustomException(ExceptionEnum.INSERT_FAILED);
        }
        return orderNo;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void cancel(String orderNo) {
        Order order = getOrderByOrderNo(orderNo);
        checkOrderOwner(order);
        if (order.getOrderStatus().equals(Constant.OrderStatusEnum.UNPAID.getCode())) {
            List<OrderItem> orderItemList = orderItemService.list(new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderNo, orderNo)
            );
            for (OrderItem orderItem : orderItemList) {
                productService.increaseStock(orderItem.getProductId(), orderItem.getQuantity());
            }
            order.setOrderStatus(Constant.OrderStatusEnum.CANCELLED.getCode());
            order.setEndTime(new Date());
            boolean success = updateById(order);
            if (!success) {
                throw new CustomException(ExceptionEnum.UPDATE_FAILED);
            }
        } else {
            throw new CustomException(ExceptionEnum.WRONG_ORDER_STATUS);
        }
    }

    @Override
    public String qrcode(String orderNo) {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = requestAttributes.getRequest();
        String payUrl = "http://" + ip + ":" + request.getLocalPort() + "/pay?orderNo=" + orderNo;
        String qrCode = null;
        try {
            qrCode = QRCodeGenerator.generateQRCode(payUrl, 350, 350, orderNo);
        } catch (WriterException | IOException e) {
            throw new RuntimeException(e);
        }
        String qrCodeAddress = "http://" + ip + ":" + request.getLocalPort() + "/qrcode/" + qrCode;
        return qrCodeAddress;
    }

    @Override
    public void pay(String orderNo) {
        Order order = getOrderByOrderNo(orderNo);
        if (order.getOrderStatus().equals(Constant.OrderStatusEnum.UNPAID.getCode())) {
            order.setOrderStatus(Constant.OrderStatusEnum.PAID.getCode());
            order.setPayTime(new Date());
            updateById(order);
        } else {
            throw new CustomException(ExceptionEnum.WRONG_ORDER_STATUS);
        }
    }

    @Override
    public void shipped(String orderNo) {
        Order order = getOrderByOrderNo(orderNo);
        if (order.getOrderStatus().equals(Constant.OrderStatusEnum.PAID.getCode())) {
            order.setOrderStatus(Constant.OrderStatusEnum.SHIPPED.getCode());
            order.setDeliveryTime(new Date());
            updateById(order);
        } else {
            throw new CustomException(ExceptionEnum.WRONG_ORDER_STATUS);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void done(String orderNo) {
        Order order = getOrderByOrderNo(orderNo);
        checkOrderOwnerOrAdmin(order);
        if (order.getOrderStatus().equals(Constant.OrderStatusEnum.SHIPPED.getCode())) {
            List<OrderItem> orderItemList = orderItemService.list(new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderNo, orderNo)
            );
            for (OrderItem orderItem : orderItemList) {
                productService.increaseSale(orderItem.getProductId(), orderItem.getQuantity());
            }
            order.setOrderStatus(Constant.OrderStatusEnum.DONE.getCode());
            order.setEndTime(new Date());
            boolean success = updateById(order);
            if (!success) {
                throw new CustomException(ExceptionEnum.UPDATE_FAILED);
            }
        } else {
            throw new CustomException(ExceptionEnum.WRONG_ORDER_STATUS);
        }
    }

    @Override
    public OrderVO detail(String orderNo) {
        Order order = getOrderByOrderNo(orderNo);
        checkOrderOwnerOrAdmin(order);
        List<OrderItem> orderItemList = orderItemService.list(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderNo, orderNo)
        );
        return orderConverter.toOrderVO(order, orderItemList);
    }

    @Override
    public IPage<OrderVO> listForUser(BasePageReq basePageReq) {
        Integer currentUserId = UserRoleFilter.getCurrentUser().getId();
        Page<Order> page = page(basePageReq.getPaginationPage(), new LambdaQueryWrapper<Order>()
                .eq(Order::getUserId, currentUserId)
                .orderByDesc(Order::getCreateTime)
        );
        return buildOrderVOPage(page);
    }

    @Override
    public IPage<OrderVO> listForAdmin(BasePageReq basePageReq) {
        Page<Order> page = page(basePageReq.getPaginationPage(), new LambdaQueryWrapper<Order>()
                .orderByDesc(Order::getCreateTime)
        );
        return buildOrderVOPage(page);
    }

    /**
     * 购物车信息转为订单项
     *
     * @param cartVOList 购物车信息
     * @return 订单项
     */
    private List<OrderItem> cartVOListToOrderItemList(List<CartVO> cartVOList) {
        ArrayList<OrderItem> orderItemList = new ArrayList<>();
        for (CartVO cartVO : cartVOList) {
            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(cartVO.getProductId());
            // 记录订单项快照
            orderItem.setProductName(cartVO.getProductName());
            orderItem.setProductImage(cartVO.getProductImage());
            orderItem.setUnitPrice(cartVO.getPrice());
            orderItem.setQuantity(cartVO.getQuantity());
            orderItem.setTotalPrice(cartVO.getTotalPrice());
            orderItemList.add(orderItem);
        }
        return orderItemList;
    }

    /**
     * 获取订单总价
     *
     * @param orderItemList 订单项列表
     * @return 订单总价
     */
    private BigDecimal getTotalPrice(List<OrderItem> orderItemList) {
        if (CollectionUtils.isEmpty(orderItemList)) {
            return BigDecimal.ZERO;
        }
        return orderItemList.stream()
                .map(OrderItem::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, PriceUtils::add);
    }

    private Order getOrderByOrderNo(String orderNo) {
        Order order = getOne(new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderNo, orderNo)
        );
        if (order == null) {
            throw new CustomException(ExceptionEnum.NO_ORDER);
        }
        return order;
    }

    /**
     * 校验当前用户是否订单本人
     *
     * @param order 订单
     */
    private void checkOrderOwner(Order order) {
        Integer currentUserId = UserRoleFilter.getCurrentUser().getId();
        if (!order.getUserId().equals(currentUserId)) {
            throw new CustomException(ExceptionEnum.NOT_YOUR_ORDER);
        }
    }

    /**
     * 校验当前用户是否订单本人或管理员
     *
     * @param order 订单
     */
    private void checkOrderOwnerOrAdmin(Order order) {
        User currentUser = UserRoleFilter.getCurrentUser();
        if (!userService.checkIsAdmin(currentUser) && !order.getUserId().equals(currentUser.getId())) {
            throw new CustomException(ExceptionEnum.NOT_YOUR_ORDER);
        }
    }

    /**
     * 构建订单 VO 分页结果
     * 将订单项一次性批量查出，避免 N+1 查询
     *
     * @param page 订单分页结果
     * @return 订单 VO 分页结果
     */
    private IPage<OrderVO> buildOrderVOPage(Page<Order> page) {
        List<Order> orderList = page.getRecords();
        if (CollectionUtils.isEmpty(orderList)) {
            // 空列表时保留分页元信息返回，同时避免 in() 空集合生成非法 SQL
            return new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        }
        List<String> orderNoList = orderList.stream()
                .map(Order::getOrderNo)
                .collect(Collectors.toList());
        List<OrderItem> orderItemList = orderItemService.list(new LambdaQueryWrapper<OrderItem>()
                .in(OrderItem::getOrderNo, orderNoList)
        );
        Map<String, List<OrderItem>> itemMap = orderItemList.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderNo));
        return page.convert(order -> orderConverter.toOrderVO(order,
                itemMap.getOrDefault(order.getOrderNo(), Collections.emptyList())));
    }
}

