package edu.ouc.mail.service.impl;

import com.alipay.api.AlipayResponse;
import com.alipay.api.response.AlipayTradePrecreateResponse;
import com.alipay.demo.trade.config.Configs;
import com.alipay.demo.trade.model.ExtendParams;
import com.alipay.demo.trade.model.GoodsDetail;
import com.alipay.demo.trade.model.builder.AlipayTradePrecreateRequestBuilder;
import com.alipay.demo.trade.model.result.AlipayF2FPrecreateResult;
import com.alipay.demo.trade.service.AlipayTradeService;
import com.alipay.demo.trade.service.impl.AlipayTradeServiceImpl;
import com.alipay.demo.trade.utils.ZxingUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import edu.ouc.mail.common.Const;
import edu.ouc.mail.common.ServerResponse;
import edu.ouc.mail.dao.*;
import edu.ouc.mail.pojo.*;
import edu.ouc.mail.service.IOrderService;
import edu.ouc.mail.util.BigDecimalUtil;
import edu.ouc.mail.util.DateTimeUtil;
import edu.ouc.mail.util.FTPUtil;
import edu.ouc.mail.util.PropertiesUtil;
import edu.ouc.mail.vo.OrderItemVo;
import edu.ouc.mail.vo.OrderProductVo;
import edu.ouc.mail.vo.OrderVo;
import edu.ouc.mail.vo.ShippingVo;
import org.apache.commons.lang.StringUtils;
import org.aspectj.weaver.ast.Or;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * Created by tmw090906 on 2017/5/28.
 */
@Service("iOrderService")
public class OrderServiceImpl implements IOrderService {

    private static AlipayTradeService tradeService;

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    static {
        /** 一定要在创建AlipayTradeService之前调用Configs.init()设置默认参数
         *  Configs会读取classpath下的zfbinfo.properties文件配置信息，如果找不到该文件则确认该文件是否在classpath目录
         */
        Configs.init("zfbinfo.properties");

        /** 使用Configs提供的默认参数
         *  AlipayTradeService可以使用单例或者为静态成员对象，不需要反复new
         */
        tradeService = new AlipayTradeServiceImpl.ClientBuilder().build();

    }


    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderItemMapper orderItemMapper;
    @Autowired
    private PayInfoMapper payInfoMapper;
    @Autowired
    private CartMapper cartMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ShippingMapper shippingMapper;



    //前台订单逻辑
    @Override
    public ServerResponse createOrder(Long userId,Long shippingId){
        List<Cart> cartList = cartMapper.getCheckedCartByUserId(userId);
        if(cartList == null){
            return ServerResponse.createByErrorMessage("没有选定商品");
        }
        //计算订单总价
        ServerResponse serverResponse = this.getCartOrderItem(userId,cartList);
        if(!serverResponse.isSuccess()){
            return serverResponse;
        }
        List<OrderItem> orderItemList = (List<OrderItem>)serverResponse.getDate();
        BigDecimal orderTotalPrice = this.getOrderTotalPrice(orderItemList);
        // 接下来是生成订单，可以直接在方法里写，也可以封装到一个私有方法中，这里把它封装到一个私有方法中
        Order order = this.assembleOrder(userId,shippingId,orderTotalPrice);
        // 现在可以通过orderItme和Order拼接OrderVo
        if(order == null){
            return ServerResponse.createByErrorMessage("生成订单错误");
        }
        // 已经判断过cartList，所以不用判断OrderItemList，因为OrderItem是通过CartList生成的
        // 将所有orderItem的orderNo设置和Order的orderNo相同后才能插入数据库,在设置orderItem的创建时间和更新时间都相同
        Date now = new Date();
        for(OrderItem orderItem : orderItemList){
            orderItem.setOrderNo(order.getOrderNo());
            order.setCreateTime(now);
            order.setUpdateTime(now);
        }
        // 设置完成orderNo后将所有OrderItem插入数据库中
        orderItemMapper.batchInsert(orderItemList);
        // orderItem和order插入数据库后，通过这两个数据拼接成OrderVo返回到前台页面，Order是在生成Order对象时，通过私有方法插入的
        OrderVo orderVo = this.assembleOrderVo(order,orderItemList);

        //生成OrderVo成功后，要减少库存，还要清空购物车中，这个订单所选中的商品
        //1、减少库存 通过 product减少库存 ,封装到一个私有方法中
        this.reduceStockForOrderCreate(orderItemList);
        //2、清空购物车
        this.clearCartProduct(cartList);

        //最后终于可以返回OrderVo
        return ServerResponse.createBySuccess(orderVo);
    }

    @Override
    public ServerResponse getOrderCartProduct(Long userId){
        OrderProductVo orderProductVo = new OrderProductVo();

        List<Cart> cartList = cartMapper.getCheckedCartByUserId(userId);
        if(cartList == null){
            return ServerResponse.createByErrorMessage("没有选定商品");
        }
        ServerResponse serverResponse = this.getCartOrderItem(userId,cartList);
        if(!serverResponse.isSuccess()){
            return serverResponse;
        }
        List<OrderItem> orderItemList = (List<OrderItem>)serverResponse.getDate();
        List<OrderItemVo> orderItemVoList = Lists.newArrayList();

        BigDecimal payment = new BigDecimal("0");
        for(OrderItem orderItem : orderItemList){
            OrderItemVo orderItemVo = this.assembleOrderItemVo(orderItem);
            payment = BigDecimalUtil.add(payment.doubleValue(),orderItem.getTotalPrice().doubleValue());
            orderItemVoList.add(orderItemVo);
        }

        orderProductVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));
        orderProductVo.setOrderItemVoList(orderItemVoList);
        orderProductVo.setProductTotalPrice(payment);

        return ServerResponse.createBySuccess(orderProductVo);
    }

    @Override
    public ServerResponse getOrderList(Long userId,int pageNum,int pageSize){
        PageHelper.startPage(pageNum, pageSize);
        List<Order> orderList = orderMapper.getOrderListByUserId(userId);
        List<OrderVo> orderVoList = this.assembleOrderVoList(orderList);
        PageInfo pageInfo = new PageInfo(orderVoList);
        return ServerResponse.createBySuccess(pageInfo);
    }

    @Override
    public ServerResponse getOrderDetail(Long userId,Long orderNo){
        Order order = orderMapper.selectByUserIdOderNo(userId,orderNo);
        if(order != null){
            List<OrderItem> orderItemList = orderItemMapper.getOrderItemListByOrderNo(order.getOrderNo().longValue());
            OrderVo orderVo = this.assembleOrderVo(order,orderItemList);
            return ServerResponse.createBySuccess(orderVo);
        }
        return ServerResponse.createByErrorMessage("找不到该订单");
    }

    @Override
    public ServerResponse cancelOrderByOrderNo(Long userId,Long orderNo){
        Order order = orderMapper.selectByUserIdOderNo(userId,orderNo);
        if(order == null){
            return ServerResponse.createByErrorMessage("找不到该订单");
        }
        if(order.getStatus() != Const.OrderStatusEnum.NO_PAY.getCode()){
            return ServerResponse.createByErrorMessage("订单已经付款或已经取消，无法操作");
        }
        order.setStatus(Long.valueOf(Const.OrderStatusEnum.CANCELED.getCode()));
        int rowCount = orderMapper.updateByPrimaryKey(order);
        if(rowCount > 0){
            return ServerResponse.createBySuccessMessage("订单取消成功");
        }
        return ServerResponse.createByErrorMessage("订单取消失败");
    }






    //后台订单逻辑业务
    @Override
    public ServerResponse manageList(int pageNum,int pageSize){
        PageHelper.startPage(pageNum,pageSize);
        List<Order> orderList = orderMapper.getAllOrder();
        List<OrderVo> orderVoList = this.assembleOrderVoList(orderList);
        PageInfo pageInfo = new PageInfo(orderVoList);
        return ServerResponse.createBySuccess(pageInfo);
    }

    @Override
    public ServerResponse manageSearch(Long orderNo,int pageNum,int pageSize){
        PageHelper.startPage(pageNum,pageSize);
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order != null){
            List<OrderItem> orderItemList = orderItemMapper.getOrderItemListByOrderNo(orderNo);
            OrderVo orderVo = assembleOrderVo(order,orderItemList);
            PageInfo pageResult = new PageInfo(Lists.newArrayList(orderVo));
            return ServerResponse.createBySuccess(pageResult);
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }

    @Override
    public ServerResponse managerDetail(Long orderNo){
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order != null){
            List<OrderItem> orderItemList = orderItemMapper.getOrderItemListByOrderNo(orderNo);
            OrderVo orderVo = assembleOrderVo(order,orderItemList);
            return ServerResponse.createBySuccess(orderVo);
        }
        return ServerResponse.createByErrorMessage("订单不存在");
    }

    @Override
    public ServerResponse managerSend(Long orderNo){
        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order != null){
            if(order.getStatus() != Const.OrderStatusEnum.PAID.getCode()){
                return ServerResponse.createByErrorMessage("该订单不能进行发货操作");
            }
            order.setStatus(Long.valueOf(Const.OrderStatusEnum.SHIPPED.getCode()));
            order.setSendTime(new Date());
            int rowCount = orderMapper.updateByPrimaryKey(order);
            if(rowCount > 0){
                return ServerResponse.createBySuccessMessage("操作成功");
            }
        }
        return ServerResponse.createByErrorMessage("操作失败");
    }








    //私有方法

    /**
     * 得到订单明细OrderItem的List
     * @param userId
     * @param cartList
     * @return
     */
    private ServerResponse getCartOrderItem(Long userId, List<Cart> cartList){
        List<OrderItem> orderItemList = Lists.newArrayList();

        for(Cart cartItem : cartList){
            OrderItem orderItem = new OrderItem();
            Product product = productMapper.getListByProductId(cartItem.getProductId());
            //校验是否在销售状态
            if(product.getStatus() != Const.ProductStatusEnum.ON_SALE.getCode()){
                return ServerResponse.createByErrorMessage("产品" + product.getName() + "不在销售状态");
            }
            //校验库存
            if(product.getStock() < cartItem.getQuantity()){
                return ServerResponse.createByErrorMessage("产品" + product.getName() + "库存不足");
            }
            //开始填充OrderItem
            orderItem.setUserId(userId);
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());
            orderItem.setProductImage(product.getMainImage());
            orderItem.setCurrentUnitPrice(product.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(),cartItem.getQuantity().doubleValue()));
            orderItemList.add(orderItem);
        }
        return ServerResponse.createBySuccess(orderItemList);
    }

    /**
     * 通过订单明细计算出订单总价
     * @param orderItemList
     * @return
     */
    private BigDecimal getOrderTotalPrice(List<OrderItem> orderItemList){
        BigDecimal totalPrice = new BigDecimal("0");
        for(OrderItem orderItem : orderItemList){
            totalPrice = BigDecimalUtil.add(totalPrice.doubleValue(),orderItem.getTotalPrice().doubleValue());
        }
        return totalPrice;
    }


    /**
     * 拼接订单POJI类，并插入数据库中
     * @param userId
     * @param shippingId
     * @param orderTotalPrice
     * @return
     */
    private Order assembleOrder(Long userId,Long shippingId,BigDecimal orderTotalPrice){
        Order order = new Order();
        Long orderNo = this.createOrderNo();
        order.setOrderNo(new BigDecimal(orderNo));
        order.setStatus(Long.valueOf(Const.OrderStatusEnum.NO_PAY.getCode()));
        order.setPostage(0L);
        order.setPayment(orderTotalPrice);
        order.setPaymentType((short)Const.PaymentTypeEnum.ONLINE_PAY.getCode());


        order.setUserId(userId);
        order.setShippingId(shippingId);

        int rowCount = orderMapper.insert(order);
        if(rowCount > 0){
            return order;
        }
        return null;
    }


    /**
     * 通过order和orderItemList拼接orderVo
     * @param order
     * @param orderItemList
     * @return
     */
    private OrderVo assembleOrderVo(Order order,List<OrderItem> orderItemList){
        OrderVo orderVo = new OrderVo();
        //先将所有可以直接通过set设置的属性设置了，再通过for循环，拼接shippingVo，拼接orderItemVo等
        //orderNo和支付信息
        orderVo.setOrderNo(order.getOrderNo().longValue());
        orderVo.setPayment(order.getPayment());
        orderVo.setPaymentType(Const.PaymentTypeEnum.ONLINE_PAY.getCode());
        orderVo.setPaymentTypeDesc(Const.PaymentTypeEnum.ONLINE_PAY.getValue());

        //订单状态，订单信息
        orderVo.setPostage(new BigDecimal(order.getPostage()));
        orderVo.setStatus(order.getStatus().intValue());
        orderVo.setStatusDesc(Const.OrderStatusEnum.codeOf(orderVo.getStatus()).getValue());

        //订单各种关于时间的信息
        orderVo.setPaymentTime(DateTimeUtil.dateToStr(order.getPaymentTime()));
        orderVo.setSendTime(DateTimeUtil.dateToStr(order.getSendTime()));
        orderVo.setEndTime(DateTimeUtil.dateToStr(order.getEndTime()));
        orderVo.setCreateTime(DateTimeUtil.dateToStr(order.getCreateTime()));
        orderVo.setCloseTime(DateTimeUtil.dateToStr(order.getCloseTime()));

        //设置图片服务器
        orderVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));

        //接下来设置OrderItemVo，并将OrderItemVo放入OrderVo中
        List<OrderItemVo> orderItemVoList = Lists.newArrayList();
        for(OrderItem orderItem : orderItemList){
            OrderItemVo orderItemVo = this.assembleOrderItemVo(orderItem);
            orderItemVoList.add(orderItemVo);
        }
        orderVo.setOrderItemVoList(orderItemVoList);

        //先获得Shipping，再拼接成shippingVo
        Shipping shipping = shippingMapper.selectByPrimaryKey(order.getShippingId());
        //订单发货地址,收货人姓名，收货地址详情
        orderVo.setShippingId(order.getShippingId());
        if(shipping != null){
            orderVo.setReceiverName(shipping.getReceiverName());
            orderVo.setShippingVo(assembleShippingVo(shipping));
        }

        return orderVo;
    }

    /**
     * 通过OrderItem拼接出OrderItemVo
     * @param orderItem
     * @return
     */
    private OrderItemVo assembleOrderItemVo(OrderItem orderItem){
        OrderItemVo orderItemVo = new OrderItemVo();

        orderItemVo.setOrderNo(orderItem.getOrderNo().longValue());
        orderItemVo.setProductId(orderItem.getProductId());
        orderItemVo.setProductName(orderItem.getProductName());
        orderItemVo.setProductImage(orderItem.getProductImage());
        orderItemVo.setCurrentUnitPrice(orderItem.getCurrentUnitPrice());
        orderItemVo.setQuantity(orderItem.getQuantity());
        orderItemVo.setTotalPrice(orderItem.getTotalPrice());
        orderItemVo.setCreateTime(DateTimeUtil.dateToStr(orderItem.getCreateTime()));

        return orderItemVo;
    }

    /**
     * 生成订单号
     * @return
     */
    private Long createOrderNo(){
        return System.currentTimeMillis()*10 + new Random().nextInt(10);
    }

    /**
     * 通过shipping拼接shippingVo对象
     * @param shipping
     * @return
     */
    private ShippingVo assembleShippingVo(Shipping shipping){
        ShippingVo shippingVo = new ShippingVo();

        shippingVo.setReceiverAddress(shipping.getReceiverAddress());
        shippingVo.setReceiverCity(shipping.getReceiverCity());
        shippingVo.setReceiverDistrict(shipping.getReceiverDistrict());
        shippingVo.setReceiverMobile(shipping.getReceiverMobile());
        shippingVo.setReceiverZip(shipping.getReceiverZip());
        shippingVo.setReceiverName(shipping.getReceiverName());
        shippingVo.setReceiverPhone(shipping.getReceiverPhone());
        shippingVo.setReceiverProvince(shipping.getReceiverProvince());

        return shippingVo;
    }


    /**
     * 创建订单成功后减少库存
     * @param orderItemList
     */
    private void reduceStockForOrderCreate(List<OrderItem> orderItemList){
        for(OrderItem orderItem : orderItemList){
            Product product = productMapper.selectByPrimaryKey(orderItem.getProductId());
            product.setStock(product.getStock() - orderItem.getQuantity());
            productMapper.updateByPrimaryKeySelective(product);
        }
    }

    /**
     * 通过创建订单的商品来清空购物车中的商品
     * @param cartList
     */
    private void clearCartProduct(List<Cart> cartList){
        for(Cart cart : cartList){
            cartMapper.deleteByPrimaryKey(cart.getId());
        }
    }


    /**
     * 前台通过userId查询的Order
     * 传进来得到OrderVoList
     * @param orderList
     * @return
     */
    private List<OrderVo> assembleOrderVoList(List<Order> orderList) {
        List<OrderVo> orderVoList = Lists.newArrayList();
        for (Order order : orderList) {
            List<OrderItem> orderItemList = orderItemMapper.getOrderItemListByOrderNo(order.getOrderNo().longValue());
            OrderVo orderVo = this.assembleOrderVo(order,orderItemList);
            orderVoList.add(orderVo);
        }
        return orderVoList;
    }









































    //支付相关业务逻辑
    @Override
    public ServerResponse pay(Long orderNo, Long userId, String path){
        Map<String,String> resultMap = Maps.newHashMap();

        Order order = orderMapper.selectByUserIdOderNo(userId,orderNo);
        if(order == null){
            return ServerResponse.createByErrorMessage("用户没有该订单");
        }
        resultMap.put("orderNo",String.valueOf(order.getOrderNo()));

        // (必填) 商户网站订单系统中唯一订单号，64个字符以内，只能包含字母、数字、下划线，
        // 需保证商户系统端不能重复，建议通过数据库sequence生成，
        String outTradeNo = order.getOrderNo().toString();

        // (必填) 订单标题，粗略描述用户的支付目的。如“xxx品牌xxx门店当面付扫码消费”
        String subject = new StringBuilder().append("studymail扫码支付，订单号").append(outTradeNo).toString();

        // (必填) 订单总金额，单位为元，不能超过1亿元
        // 如果同时传入了【打折金额】,【不可打折金额】,【订单总金额】三者,则必须满足如下条件:【订单总金额】=【打折金额】+【不可打折金额】
        String totalAmount = order.getPayment().toString();

        // (可选) 订单不可打折金额，可以配合商家平台配置折扣活动，如果酒水不参与打折，则将对应金额填写至此字段
        // 如果该值未传入,但传入了【订单总金额】,【打折金额】,则该值默认为【订单总金额】-【打折金额】
        String undiscountableAmount = "0";

        // 卖家支付宝账号ID，用于支持一个签约账号下支持打款到不同的收款账号，(打款到sellerId对应的支付宝账号)
        // 如果该字段为空，则默认为与支付宝签约的商户的PID，也就是appid对应的PID
        String sellerId = "";

        // 订单描述，可以对交易或商品进行一个详细地描述，比如填写"购买商品2件共15.00元"
        String body = new StringBuilder().append("订单").append(outTradeNo).append("购买商品共").append(totalAmount).append("元").toString();

        // 商户操作员编号，添加此参数可以为商户操作员做销售统计
        String operatorId = "test_operator_id";

        // (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
        String storeId = "test_store_id";

        // 业务扩展参数，目前可添加由支付宝分配的系统商编号(通过setSysServiceProviderId方法)，详情请咨询支付宝技术支持
        ExtendParams extendParams = new ExtendParams();
        extendParams.setSysServiceProviderId("2088100200300400500");

        // 支付超时，定义为120分钟
        String timeoutExpress = "120m";

        // 商品明细列表，需填写购买商品详细信息，
        List<GoodsDetail> goodsDetailList = new ArrayList<GoodsDetail>();

        List<OrderItem> orderItemList = orderItemMapper.selectOrderItemByOrderNoUserId(order.getUserId(),order.getOrderNo().longValue());
        for(OrderItem orderItemTemp : orderItemList){
            GoodsDetail goodsDetail = GoodsDetail.newInstance(orderItemTemp.getProductId().toString(),
                                                                orderItemTemp.getProductName(),
                    BigDecimalUtil.mul(orderItemTemp.getCurrentUnitPrice().doubleValue(),new Double(100).doubleValue()).longValue(),
                    orderItemTemp.getQuantity().intValue());
            goodsDetailList.add(goodsDetail);
        }
        // 创建一个商品信息，参数含义分别为商品id（使用国标）、名称、单价（单位为分）、数量，如果需要添加商品类别，详见GoodsDetail
//        GoodsDetail goods1 = GoodsDetail.newInstance("goods_id001", "xxx小面包", 1000, 1);
        // 创建好一个商品后添加至商品明细列表
//        goodsDetailList.add(goods1);

        // 继续创建并添加第一条商品信息，用户购买的产品为“黑人牙刷”，单价为5.00元，购买了两件
//        GoodsDetail goods2 = GoodsDetail.newInstance("goods_id002", "xxx牙刷", 500, 2);
//        goodsDetailList.add(goods2);

        // 创建扫码支付请求builder，设置请求参数
        AlipayTradePrecreateRequestBuilder builder = new AlipayTradePrecreateRequestBuilder()
                .setSubject(subject).setTotalAmount(totalAmount).setOutTradeNo(outTradeNo)
                .setUndiscountableAmount(undiscountableAmount).setSellerId(sellerId).setBody(body)
                .setOperatorId(operatorId).setStoreId(storeId).setExtendParams(extendParams)
                .setTimeoutExpress(timeoutExpress)
                .setNotifyUrl(PropertiesUtil.getProperty("alipay.callback.url"))//支付宝服务器主动通知商户服务器里指定的页面http路径,根据需要设置
                .setGoodsDetailList(goodsDetailList);

        AlipayF2FPrecreateResult result = tradeService.tradePrecreate(builder);
        switch (result.getTradeStatus()) {
            case SUCCESS:
                log.info("支付宝预下单成功: )");

                AlipayTradePrecreateResponse response = result.getResponse();
                dumpResponse(response);

                File folder = new File(path);
                if(!folder.exists()){
                    folder.setWritable(true);
                    folder.mkdirs();
                }

                // 需要修改为运行机器上的路径
                String qrPath = String.format(path+"/qr-%s.png", response.getOutTradeNo());
                String qrPathName = String.format("qr-%s.png",response.getOutTradeNo());
                ZxingUtils.getQRCodeImge(response.getQrCode(), 256, qrPath);

                File targetFile = new File(path,qrPathName);

                try {
                    FTPUtil.uploadFile(Lists.newArrayList(targetFile));
                } catch (IOException e) {
                    log.error("上传二维码异常",e);
                }
                log.info("qrPath:" + qrPath);
                //                ZxingUtils.getQRCodeImge(response.getQrCode(), 256, filePath);


                String qrUrl = PropertiesUtil.getProperty("ftp.server.http.prefix") + targetFile.getName();
                resultMap.put("qrUrl",qrUrl);
                return ServerResponse.createBySuccess(resultMap);

            case FAILED:
                log.error("支付宝预下单失败!!!");
                return ServerResponse.createByErrorMessage("支付宝预下单失败!!!");

            case UNKNOWN:
                log.error("系统异常，预下单状态未知!!!");
                return ServerResponse.createByErrorMessage("系统异常，预下单状态未知!!!");

            default:
                log.error("不支持的交易状态，交易返回异常!!!");
                return ServerResponse.createByErrorMessage("不支持的交易状态，交易返回异常!!!");
        }

    }

    private void dumpResponse(AlipayResponse response) {
        if (response != null) {
            log.info(String.format("code:%s, msg:%s", response.getCode(), response.getMsg()));
            if (StringUtils.isNotEmpty(response.getSubCode())) {
                log.info(String.format("subCode:%s, subMsg:%s", response.getSubCode(),
                        response.getSubMsg()));
            }
            log.info("body:" + response.getBody());
        }
    }


    @Override
    public ServerResponse aliCallBack(Map<String,String> params){
        Long orderNo = Long.parseLong(params.get("out_trade_no"));
        String tradeNo = params.get("trade_no");
        String tradeStatus = params.get("trade_status");

        Order order = orderMapper.selectByOrderNo(orderNo);
        if(order == null){
            return ServerResponse.createByErrorMessage("非studymail的订单，回调忽略");
        }
        if(order.getStatus() >= Const.OrderStatusEnum.PAID.getCode()){
            return ServerResponse.createBySuccess("支付宝重复调用",order);
        }
        if(Const.AlipayCallback.TRADE_STATUS_TRADE_SUCCESS.equals(tradeStatus)){
            order.setStatus((long)Const.OrderStatusEnum.PAID.getCode());
            order.setPaymentTime(DateTimeUtil.strToDate(params.get("gmt_payment")));
            orderMapper.updateByPrimaryKeySelective(order);
        }
        PayInfo payInfo = new PayInfo();
        payInfo.setOrderNo(new BigDecimal(orderNo));
        payInfo.setPayPlatform((long)Const.PayPlatformEnum.ALIPAY.getCode());
        payInfo.setUserId(order.getUserId());
        payInfo.setPlatformStatus(tradeStatus);
        payInfo.setPlatformNumber(tradeNo);

        payInfoMapper.insert(payInfo);

        return ServerResponse.createBySuccess();

    }

    @Override
    public ServerResponse<Boolean> queryOrderPayStatus(Long userId,Long oderNo){
        Order order = orderMapper.selectByUserIdOderNo(userId, oderNo);
        if(order == null){
            return ServerResponse.createByErrorMessage("用户没有该订单");
        }
        if(order.getStatus() >= Const.OrderStatusEnum.PAID.getCode()){
            return ServerResponse.createBySuccess();
        }
        return ServerResponse.createByError();
    }

    @Override
    public boolean checkedOrder(Map<String,String> params){
        Long orderNo = Long.parseLong(params.get("out_trade_no"));

        Order order = orderMapper.selectByOrderNo(orderNo);

        if(order == null){
            return false;
        }
        BigDecimal orderTotalPrice = new BigDecimal(params.get("total_amount"));
        if(orderTotalPrice.compareTo(order.getPayment()) == 0){
            return true;
        }
        return false;
    }

}
