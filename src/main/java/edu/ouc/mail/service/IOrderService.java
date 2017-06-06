package edu.ouc.mail.service;

import edu.ouc.mail.common.ServerResponse;

import java.util.Map;

/**
 * Created by tmw090906 on 2017/5/28.
 */
public interface IOrderService {

    ServerResponse pay(Long orderNo, Long userId, String path);

    ServerResponse aliCallBack(Map<String,String> params);

    ServerResponse<Boolean> queryOrderPayStatus(Long userId,Long oderNo);

    boolean checkedOrder(Map<String,String> params);

    ServerResponse createOrder(Long userId,Long shippingId);

    ServerResponse getOrderCartProduct(Long userId);

    ServerResponse getOrderList(Long userId,int pageNum,int pageSize);

    ServerResponse getOrderDetail(Long userId,Long orderNo);

    ServerResponse<String> cancelOrderByOrderNo(Long userId,Long orderNo);

    ServerResponse manageList(int pageNum,int pageSize);

    ServerResponse manageSearch(Long orderNo,int pageNum,int pageSize);

    ServerResponse managerDetail(Long orderNo);

    ServerResponse managerSend(Long orderNo);
}
