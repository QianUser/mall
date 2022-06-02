package com.example.mall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.example.common.constant.CartConstant;
import com.example.common.exception.NoStockException;
import com.example.common.to.OrderTo;
import com.example.common.to.mq.SeckillOrderTo;
import com.example.mall.order.constant.PayConstant;
import com.example.mall.order.entity.OrderItemEntity;
import com.example.mall.order.entity.PaymentInfoEntity;
import com.example.mall.order.enume.OrderStatusEnum;
import com.example.mall.order.feign.ProductFeignService;
import com.example.mall.order.feign.WmsFeignService;
import com.example.mall.order.feign.MemberFeignService;
import com.example.mall.order.feign.CartFeignService;
import com.example.common.utils.R;
import com.example.common.vo.MemberResponseVo;
import com.example.mall.order.interceptor.LoginUserInterceptor;
import com.example.mall.order.service.OrderItemService;
import com.example.mall.order.service.PaymentInfoService;
import com.example.mall.order.to.OrderCreateTo;
import com.example.mall.order.vo.*;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.mall.order.dao.OrderDao;
import com.example.mall.order.entity.OrderEntity;
import com.example.mall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import static com.example.mall.order.constant.OrderConstant.USER_ORDER_TOKEN_PREFIX;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private CartFeignService cartFeignService;

    @Autowired
    private WmsFeignService wmsFeignService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private PaymentInfoService paymentInfoService;

    private final ThreadLocal<OrderSubmitVo> confirmVoThreadLocal = new ThreadLocal<>();

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 订单确认页返回需要用的数据
     */
    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        // 开启第一个异步任务
        CompletableFuture<Void> addressFuture = CompletableFuture.runAsync(() -> {
            // 每一个线程都来共享之前的请求数据
            RequestContextHolder.setRequestAttributes(requestAttributes);
            // 远程查询所有的收获地址列表
            List<MemberAddressVo> address = memberFeignService.getAddress(memberResponseVo.getId());
            confirmVo.setMemberAddressVos(address);
        }, threadPoolExecutor);
        // 开启第二个异步任务
        CompletableFuture<Void> cartInfoFuture = CompletableFuture.runAsync(() -> {
            // 每一个线程都来共享之前的请求数据
            RequestContextHolder.setRequestAttributes(requestAttributes);
            // 远程查询购物车所有选中的购物项
            List<OrderItemVo> currentCartItems = cartFeignService.getCurrentCartItems();
            confirmVo.setItems(currentCartItems);
            // feign在远程调用之前要构造请求，调用很多的拦截器
        }, threadPoolExecutor).thenRunAsync(() -> {
            List<OrderItemVo> items = confirmVo.getItems();
            // 获取全部商品的id
            List<Long> skuIds = items.stream()
                    .map((OrderItemVo::getSkuId))
                    .collect(Collectors.toList());
            // 远程查询商品库存信息
            R skuHasStock = wmsFeignService.getSkuHasStock(skuIds);
            List<SkuStockVo> skuStockVos = skuHasStock.getData("data", new TypeReference<List<SkuStockVo>>() {});
            if (skuStockVos != null && skuStockVos.size() > 0) {
                // 将skuStockVos集合转换为map
                Map<Long, Boolean> skuHasStockMap = skuStockVos.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                confirmVo.setStocks(skuHasStockMap);
            }
        }, threadPoolExecutor);
        // 查询用户积分
        Integer integration = memberResponseVo.getIntegration();
        confirmVo.setIntegration(integration);
        // 价格数据自动计算
        // 为用户设置一个token，三十分钟过期时间
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId(), token,30, TimeUnit.MINUTES);
        confirmVo.setOrderToken(token);
        CompletableFuture.allOf(addressFuture, cartInfoFuture).get();
        return confirmVo;
    }

    /**
     * 提交订单
     */
    @GlobalTransactional
    @Transactional(rollbackFor = Exception.class)
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {
        confirmVoThreadLocal.set(vo);
        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();
        // 去创建、下订单、验令牌、验价格、锁定库存...
        // 获取当前用户登录的信息
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();
        responseVo.setCode(0);
        // 验证令牌是否合法（令牌的对比和删除必须保证原子性）
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        String orderToken = vo.getOrderToken();
        // 通过lua脚本原子验证令牌和删除令牌
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                Collections.singletonList(USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId()), orderToken);
        if (result == null || result == 0L) {
            // 令牌验证失败
            responseVo.setCode(1);
            return responseVo;
        } else {
            // 令牌验证成功
            // 创建订单、订单项等信息
            OrderCreateTo order = createOrder();
            // 验证价格
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = vo.getPayPrice();
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                // 金额对比
                saveOrder(order);
                // 库存锁定,只要有异常，回滚订单数据
                // 订单号、所有订单项信息(skuId,skuNum,skuName)
                WareSkuLockVo lockVo = new WareSkuLockVo();
                lockVo.setOrderSn(order.getOrder().getOrderSn());

                //获取出要锁定的商品数据信息
                List<OrderItemVo> orderItemVos = order.getOrderItems().stream().map((item) -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setSkuId(item.getSkuId());
                    orderItemVo.setCount(item.getSkuQuantity());
                    orderItemVo.setTitle(item.getSkuName());
                    return orderItemVo;
                }).collect(Collectors.toList());
                lockVo.setLocks(orderItemVos);

                R r = wmsFeignService.orderLockStock(lockVo);
                if (r.getCode() == 0) {
                    // 锁定成功
                    responseVo.setOrder(order.getOrder());
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", order);
                    // 删除购物车里的数据
                    redisTemplate.delete(CartConstant.CART_PREFIX + memberResponseVo.getId());
                    return responseVo;
                } else {
                    // 锁定失败
                    String msg = (String) r.get("msg");
                    throw new NoStockException(msg);
                }

            } else {
                responseVo.setCode(2);
                return responseVo;
            }
        }
    }

    /**
     * 按照订单号获取订单信息
     */
    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        return this.baseMapper.selectOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
    }

    /**
     * 关闭订单
     */
    @Override
    public void closeOrder(OrderEntity orderEntity) {
        // 关闭订单之前先查询一下数据库，判断此订单状态是否已支付
        OrderEntity orderInfo = this.getOne(new QueryWrapper<OrderEntity>().
                eq("order_sn",orderEntity.getOrderSn()));

        if (orderInfo.getStatus().equals(OrderStatusEnum.CREATE_NEW.getCode())) {
            // 代付款状态进行关单
            OrderEntity orderUpdate = new OrderEntity();
            orderUpdate.setId(orderInfo.getId());
            orderUpdate.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(orderUpdate);
            // 发送消息给MQ
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(orderInfo, orderTo);

            try {
                // TODO 确保每个消息发送成功，给每个消息做好日志记录，(给数据库保存每一个详细信息)保存每个消息的详细信息
                rabbitTemplate.convertAndSend("order-event-exchange", "order.release.other", orderTo);
            } catch (Exception e) {
                // TODO 定期扫描数据库，重新发送失败的消息
            }
        }
    }

    @Override
    public PayVo getOrderPay(String orderSn) {
        PayVo payVo = new PayVo();
        OrderEntity orderInfo = this.getOrderByOrderSn(orderSn);
        // 保留两位小数点，向上取值
        BigDecimal payAmount = orderInfo.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
        payVo.setTotal_amount(payAmount.toString());
        payVo.setOut_trade_no(orderInfo.getOrderSn());
        // 查询订单项的数据
        List<OrderItemEntity> orderItemInfo = orderItemService.list(
                new QueryWrapper<OrderItemEntity>().eq("order_sn", orderSn));
        OrderItemEntity orderItemEntity = orderItemInfo.get(0);
        payVo.setBody(orderItemEntity.getSkuAttrsVals());
        payVo.setSubject(orderItemEntity.getSkuName());
        return payVo;
    }

    /**
     * 查询当前用户所有订单数据
     */
    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
                        .eq("member_id",memberResponseVo.getId()).orderByDesc("create_time")
        );
        // 遍历所有订单集合
        List<OrderEntity> orderEntityList = page.getRecords().stream().peek(order -> {
            // 根据订单号查询订单项里的数据
            List<OrderItemEntity> orderItemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>()
                    .eq("order_sn", order.getOrderSn()));
            order.setItemEntities(orderItemEntities);
        }).collect(Collectors.toList());
        page.setRecords(orderEntityList);
        return new PageUtils(page);
    }

    /**
     * 处理支付宝的支付结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String handlePayResult(PayAsyncVo asyncVo) {
        // 保存交易流水信息
        PaymentInfoEntity paymentInfo = new PaymentInfoEntity();
        paymentInfo.setOrderSn(asyncVo.getOut_trade_no());
        paymentInfo.setAlipayTradeNo(asyncVo.getTrade_no());
        paymentInfo.setTotalAmount(new BigDecimal(asyncVo.getBuyer_pay_amount()));
        paymentInfo.setSubject(asyncVo.getBody());
        paymentInfo.setPaymentStatus(asyncVo.getTrade_status());
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setCallbackTime(asyncVo.getNotify_time());
        // 添加到数据库中
        paymentInfoService.save(paymentInfo);
        // 修改订单状态
        String tradeStatus = asyncVo.getTrade_status();
        if (tradeStatus.equals("TRADE_SUCCESS") || tradeStatus.equals("TRADE_FINISHED")) {
            // 支付成功状态
            String orderSn = asyncVo.getOut_trade_no(); //获取订单号
            this.updateOrderStatus(orderSn, OrderStatusEnum.PAYED.getCode(), PayConstant.ALIPAY);
        }
        return "success";
    }

    /**
     * 创建秒杀单
     */
    @Override
    public void createSeckillOrder(SeckillOrderTo orderTo) {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderSn(orderTo.getOrderSn());
        orderEntity.setMemberId(orderTo.getMemberId());
        orderEntity.setCreateTime(new Date());
        BigDecimal totalPrice = orderTo.getSeckillPrice().multiply(BigDecimal.valueOf(orderTo.getNum()));
        orderEntity.setPayAmount(totalPrice);
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        // 保存订单
        this.save(orderEntity);
        // 保存订单项信息
        OrderItemEntity orderItem = new OrderItemEntity();
        orderItem.setOrderSn(orderTo.getOrderSn());
        orderItem.setRealAmount(totalPrice);
        orderItem.setSkuQuantity(orderTo.getNum());
        // 保存商品的spu信息
        R spuInfo = productFeignService.getSpuInfoBySkuId(orderTo.getSkuId());
        SpuInfoVo spuInfoData = spuInfo.getData("data", new TypeReference<SpuInfoVo>() {
        });
        orderItem.setSpuId(spuInfoData.getId());
        orderItem.setSpuName(spuInfoData.getSpuName());
        orderItem.setSpuBrand(spuInfoData.getBrandName());
        orderItem.setCategoryId(spuInfoData.getCatalogId());
        // 保存订单项数据
        orderItemService.save(orderItem);
    }

    /**
     * 修改订单状态
     */
    private void updateOrderStatus(String orderSn, Integer code, Integer payType) {
        this.baseMapper.updateOrderStatus(orderSn,code,payType);
    }

    /**
     * 保存订单所有数据
     */
    private void saveOrder(OrderCreateTo orderCreateTo) {

        //获取订单信息
        OrderEntity order = orderCreateTo.getOrder();
        order.setModifyTime(new Date());
        order.setCreateTime(new Date());
        //保存订单
        this.baseMapper.insert(order);

        //获取订单项信息
        List<OrderItemEntity> orderItems = orderCreateTo.getOrderItems();
        //批量保存订单项数据
        orderItemService.saveBatch(orderItems);
    }


    private OrderCreateTo createOrder() {
        OrderCreateTo createTo = new OrderCreateTo();
        // 生成订单号
        String orderSn = IdWorker.getTimeId();
        OrderEntity orderEntity = builderOrder(orderSn);
        // 获取到所有的订单项
        List<OrderItemEntity> orderItemEntities = builderOrderItems(orderSn);
        // 验价(计算价格、积分等信息)
        computePrice(orderEntity,orderItemEntities);
        createTo.setOrder(orderEntity);
        createTo.setOrderItems(orderItemEntities);
        return createTo;
    }

    /**
     * 构建订单数据
     */
    private OrderEntity builderOrder(String orderSn) {

        // 获取当前用户登录信息
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();

        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setMemberId(memberResponseVo.getId());
        orderEntity.setOrderSn(orderSn);
        orderEntity.setMemberUsername(memberResponseVo.getUsername());

        OrderSubmitVo orderSubmitVo = confirmVoThreadLocal.get();

        // 远程获取收货地址和运费信息
        R fareAddressVo = wmsFeignService.getFare(orderSubmitVo.getAddrId());
        FareVo fareResp = fareAddressVo.getData("data", new TypeReference<FareVo>() {});

        // 获取到运费信息
        BigDecimal fare = fareResp.getFare();
        orderEntity.setFreightAmount(fare);

        // 获取到收货地址信息
        MemberAddressVo address = fareResp.getAddress();
        // 设置收货人信息
        orderEntity.setReceiverName(address.getName());
        orderEntity.setReceiverPhone(address.getPhone());
        orderEntity.setReceiverPostCode(address.getPostCode());
        orderEntity.setReceiverProvince(address.getProvince());
        orderEntity.setReceiverCity(address.getCity());
        orderEntity.setReceiverRegion(address.getRegion());
        orderEntity.setReceiverDetailAddress(address.getDetailAddress());

        // 设置订单相关的状态信息
        orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        orderEntity.setAutoConfirmDay(7);
        orderEntity.setConfirmStatus(0);
        return orderEntity;
    }

    /**
     * 计算价格的方法
     */
    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> orderItemEntities) {
        // 总价
        BigDecimal total = new BigDecimal("0.0");
        // 优惠价
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal intergration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");

        // 积分、成长值
        Integer integrationTotal = 0;
        Integer growthTotal = 0;

        // 订单总额，叠加每一个订单项的总额信息
        for (OrderItemEntity orderItem : orderItemEntities) {
            // 优惠价格信息
            coupon = coupon.add(orderItem.getCouponAmount());
            promotion = promotion.add(orderItem.getPromotionAmount());
            intergration = intergration.add(orderItem.getIntegrationAmount());
            // 总价
            total = total.add(orderItem.getRealAmount());

            // 积分信息和成长值信息
            integrationTotal += orderItem.getGiftIntegration();
            growthTotal += orderItem.getGiftGrowth();
        }
        // 订单价格相关的
        orderEntity.setTotalAmount(total);
        // 设置应付总额(总额+运费)
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        orderEntity.setCouponAmount(coupon);
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setIntegrationAmount(intergration);
        // 设置积分成长值信息
        orderEntity.setIntegration(integrationTotal);
        orderEntity.setGrowth(growthTotal);
        //设置删除状态(0-未删除，1-已删除)
        orderEntity.setDeleteStatus(0);
    }

    /**
     * 构建所有订单项数据
     */
    public List<OrderItemEntity> builderOrderItems(String orderSn) {

        List<OrderItemEntity> orderItemEntityList = new ArrayList<>();

        // 最后确定每个购物项的价格
        List<OrderItemVo> currentCartItems = cartFeignService.getCurrentCartItems();
        if (currentCartItems != null && currentCartItems.size() > 0) {
            orderItemEntityList = currentCartItems.stream().map((items) -> {
                // 构建订单项数据
                OrderItemEntity orderItemEntity = builderOrderItem(items);
                orderItemEntity.setOrderSn(orderSn);

                return orderItemEntity;
            }).collect(Collectors.toList());
        }

        return orderItemEntityList;
    }

    /**
     * 构建某一个订单项的数据
     */
    private OrderItemEntity builderOrderItem(OrderItemVo items) {

        OrderItemEntity orderItemEntity = new OrderItemEntity();

        // 商品的spu信息
        Long skuId = items.getSkuId();
        // 获取spu的信息
        R spuInfo = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo spuInfoData = spuInfo.getData("data", new TypeReference<SpuInfoVo>() {
        });
        orderItemEntity.setSpuId(spuInfoData.getId());
        orderItemEntity.setSpuName(spuInfoData.getSpuName());
        orderItemEntity.setSpuBrand(spuInfoData.getBrandName());
        orderItemEntity.setCategoryId(spuInfoData.getCatalogId());

        // 商品的sku信息
        orderItemEntity.setSkuId(skuId);
        orderItemEntity.setSkuName(items.getTitle());
        orderItemEntity.setSkuPic(items.getImage());
        orderItemEntity.setSkuPrice(items.getPrice());
        orderItemEntity.setSkuQuantity(items.getCount());

        // 使用StringUtils.collectionToDelimitedString将list集合转换为String
        String skuAttrValues = StringUtils.collectionToDelimitedString(items.getSkuAttrValues(), ";");
        orderItemEntity.setSkuAttrsVals(skuAttrValues);

        // 商品的优惠信息

        // 商品的积分信息
        orderItemEntity.setGiftGrowth(items.getPrice().multiply(new BigDecimal(items.getCount())).intValue());
        orderItemEntity.setGiftIntegration(items.getPrice().multiply(new BigDecimal(items.getCount())).intValue());

        // 订单项的价格信息
        orderItemEntity.setPromotionAmount(BigDecimal.ZERO);
        orderItemEntity.setCouponAmount(BigDecimal.ZERO);
        orderItemEntity.setIntegrationAmount(BigDecimal.ZERO);

        // 当前订单项的实际金额.总额 - 各种优惠价格
        // 原来的价格
        BigDecimal origin = orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity().toString()));
        // 原价减去优惠价得到最终的价格
        BigDecimal subtract = origin.subtract(orderItemEntity.getCouponAmount())
                .subtract(orderItemEntity.getPromotionAmount())
                .subtract(orderItemEntity.getIntegrationAmount());
        orderItemEntity.setRealAmount(subtract);

        return orderItemEntity;
    }

}