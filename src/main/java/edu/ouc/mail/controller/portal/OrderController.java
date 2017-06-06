package edu.ouc.mail.controller.portal;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.demo.trade.config.Configs;
import com.google.common.collect.Maps;
import edu.ouc.mail.common.Const;
import edu.ouc.mail.common.ResponseCode;
import edu.ouc.mail.common.ServerResponse;
import edu.ouc.mail.pojo.User;
import edu.ouc.mail.service.IOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by tmw090906 on 2017/5/28.
 */
@Controller
@RequestMapping("/order/")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private IOrderService iOrderService;

    //portal订单处理

    /**
     * 创建订单
     * @param session
     * @param shippingId
     * @return
     */
    @RequestMapping("create.do")
    @ResponseBody
    public ServerResponse create(HttpSession session , Long shippingId){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorMessage("用户未登录，请先登陆");
        }
        return iOrderService.createOrder(user.getId(),shippingId);
    }


    /**
     * 用于创建订单的前一步。
     * 创建订单时，要获取商品详情，此时从购物车中选中的商品展示给用户
     * 用户确认商品，总价后，通过提交订单（create.do）接口创建订单
     * @param session
     * @return
     */
    @RequestMapping("get_order_cart_product.do")
    @ResponseBody
    public ServerResponse getOrderCartProduct(HttpSession session){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorMessage("用户未登录，请先登陆");
        }
        return iOrderService.getOrderCartProduct(user.getId());
    }

    /**
     * 个人中心查看自己所有订单
     * @param session
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestMapping("list.do")
    @ResponseBody
    public ServerResponse list(HttpSession session,
                               @RequestParam(defaultValue = "1")int pageNum,
                               @RequestParam(defaultValue = "10")int pageSize){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorMessage("用户未登录，请先登陆");
        }
        return iOrderService.getOrderList(user.getId(),pageNum,pageSize);
    }


    /**
     * 查看所有订单详情
     * @param session
     * @param orderNo
     * @return
     */
    @RequestMapping("detail.do")
    @ResponseBody
    public ServerResponse detail(HttpSession session,Long orderNo){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorMessage("用户未登录，请先登陆");
        }
        return iOrderService.getOrderDetail(user.getId(),orderNo);
    }


    /**
     * 取消当前订单
     * 若订单已经付款，则无法取消
     * @param session
     * @param orderNo
     * @return
     */
    @RequestMapping("cancel.do")
    @ResponseBody
    public ServerResponse cancel(HttpSession session,Long orderNo){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorMessage("用户未登录，请先登陆");
        }
        return iOrderService.cancelOrderByOrderNo(user.getId(),orderNo);
    }
















    //支付相关

    @RequestMapping("pay.do")
    @ResponseBody
    public ServerResponse pay(HttpSession session, Long orderNo, HttpServletRequest request){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        String path = request.getSession().getServletContext().getRealPath("upload");
        return iOrderService.pay(orderNo,user.getId(),path);
    }

    @RequestMapping("callback.do")
    @ResponseBody
    public Object callback(HttpServletRequest request){
        Map<String,String> params = Maps.newHashMap();
        Map requestMap = request.getParameterMap();
        for(Iterator iterator = requestMap.keySet().iterator(); iterator.hasNext() ;){
            String name = (String)iterator.next();
            String[] values = (String[])requestMap.get(name);
            String valueStr = "";
            for(int i = 0;i < values.length ; i++){
                valueStr = ( i == values.length - 1)? valueStr + values[i]:valueStr + values[i] + ",";
            }
            params.put(name,valueStr);
        }
        logger.info("支付宝回调,sign:{},trade_status:{},参数：{}",params.get("sign"),params.get("trade_status"),params);

        //验证回调的正确性，还要避免重复通知
        params.remove("sign_type");
        try {
            //订单号，和订单实际金额在Service中判定
            boolean orderChecked = iOrderService.checkedOrder(params);
            boolean aliPayRSACheckedV2 = AlipaySignature.rsaCheckV2(params, Configs.getAlipayPublicKey(),"utf-8",Configs.getSignType());
            boolean sellerIdChecked = Configs.getPid().equals(params.get("seller_id"));
            boolean resultChecked = orderChecked && aliPayRSACheckedV2 && sellerIdChecked;
            if(!resultChecked){
                System.out.println("进入这儿");
                System.out.println("orderChecked:" + orderChecked);
                System.out.println("aliPayRSACheckedV2:" + aliPayRSACheckedV2);
                System.out.println("sellerIdChecked:" + sellerIdChecked);
                System.out.println("resultChecked:" + resultChecked);
                return ServerResponse.createByErrorMessage("非法请求,验证不通过,再恶意请求我就报警找网警了");
            }
        } catch (AlipayApiException e) {
            logger.error("支付宝回调异常",e);
        }


        ServerResponse serverResponse = iOrderService.aliCallBack(params);
        if(serverResponse.isSuccess()){
            return Const.AlipayCallback.RESPONSE_SUCCESS;
        }
        return Const.AlipayCallback.RESPONSE_FAILED;

    }

    @RequestMapping("query_order_pay_status.do")
    @ResponseBody
    public ServerResponse<Boolean> queryOderPayStatus(HttpSession session, Long orderNo){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),ResponseCode.NEED_LOGIN.getDesc());
        }
        ServerResponse serverResponse = iOrderService.queryOrderPayStatus(user.getId(),orderNo);
        if(serverResponse.isSuccess()){
            return ServerResponse.createBySuccess(true);
        }
        return ServerResponse.createBySuccess(false);
    }
}
