package edu.ouc.mail.controller.backend;

import edu.ouc.mail.common.Const;
import edu.ouc.mail.common.ResponseCode;
import edu.ouc.mail.common.ServerResponse;
import edu.ouc.mail.pojo.User;
import edu.ouc.mail.service.IOrderService;
import edu.ouc.mail.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

/**
 * Created by tmw090906 on 2017/5/29.
 */
@Controller
@RequestMapping(value = "/manager/order/")
public class OrderManagerController {

    @Autowired
    private IUserService iUserService;

    @Autowired
    private IOrderService iOrderService;

    /**
     * 后台获取订单列表
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
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"请先登陆");
        }
        if(iUserService.checkAdminRole(user).isSuccess()){
            return iOrderService.manageList(pageNum,pageSize);
        }else {
            return ServerResponse.createByErrorMessage("用户无权限");
        }
    }

    /**
     * 后台通过订单号获取详情，应返回一个List,实际上这里是一个模糊查询？
     * @param session
     * @param orderNo
     * @return
     */
    @RequestMapping("search.do")
    @ResponseBody
    public ServerResponse search(HttpSession session,Long orderNo,
                                 @RequestParam(defaultValue = "1")int pageNum,
                                 @RequestParam(defaultValue = "10")int pageSize){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"请先登陆");
        }
        if(iUserService.checkAdminRole(user).isSuccess()){
            return iOrderService.manageSearch(orderNo,pageNum,pageSize);
        }else {
            return ServerResponse.createByErrorMessage("用户无权限");
        }
    }

    /**
     * 后台通过通过orderNo查看Order详情
     * @param session
     * @param orderNo
     * @return
     */
    @RequestMapping("detail.do")
    @ResponseBody
    public ServerResponse detail(HttpSession session,Long orderNo){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"请先登陆");
        }
        if(iUserService.checkAdminRole(user).isSuccess()){
            return iOrderService.managerDetail(orderNo);
        }else {
            return ServerResponse.createByErrorMessage("用户无权限");
        }
    }

    @RequestMapping("send.do")
    @ResponseBody
    public ServerResponse send(HttpSession session,Long orderNo){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"请先登陆");
        }
        if(iUserService.checkAdminRole(user).isSuccess()){
            return iOrderService.managerSend(orderNo);
        }else {
            return ServerResponse.createByErrorMessage("用户无权限");
        }
    }

}
