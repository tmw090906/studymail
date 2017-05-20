package edu.ouc.mail.controller.portal;

import edu.ouc.mail.common.Const;
import edu.ouc.mail.common.ResponseCode;
import edu.ouc.mail.common.ServerResponse;
import edu.ouc.mail.pojo.User;
import edu.ouc.mail.service.IUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

/**
 * Created by tmw090906 on 2017/5/19.
 */
@Controller
@RequestMapping(value = "/user/")
public class UserController {

    @Autowired
    private IUserService iUserService;

    /**
     * 用户登录接口
     * @param username
     * @param password
     * @param session
     * @return
     */
    @RequestMapping(value = "login.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> login(String username, String password, HttpSession session){
        ServerResponse<User> response = iUserService.login(username,password);
        if(response.isSuccess()){
            session.setAttribute(Const.CURRENT_USER,response.getDate());
        }
        return response;
    }

    /**
     * 登出接口
     * @param session
     * @return
     */
    @RequestMapping(value = "logout.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> logout(HttpSession session){
        session.removeAttribute(Const.CURRENT_USER);
        return ServerResponse.createBySuccess();
    }

    /**
     * 用户注册接口
     * @param user
     * @return
     */
    @RequestMapping(value = "register.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> register(User user){
        return iUserService.register(user);
    }

    /**
     * 校验用户名和邮箱是否存在接口
     * @param str
     * @param type
     * @return
     */
    @RequestMapping(value = "checkValid.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> checkValid(String str,String type){
        return iUserService.checkValid(str,type);
    }

    /**
     * 获取用户信息接口
     * @param session
     * @return
     */
    @RequestMapping(value = "get_user_info.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> getUserInfo(HttpSession session){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user != null){
            return ServerResponse.createBySuccess(user);
        }
        return ServerResponse.createBySuccessMessage("用户未登陆，无法获取信息");
    }

    /**
     * 获取用户的问题提示接口
     * @param username
     * @return
     */
    @RequestMapping(value = "forget_get_question.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetByQuestion(String username){
        return iUserService.selectQuestion(username);
    }

    /**
     * 用户根据问题回答接口
     * @param username
     * @param question
     * @param answer
     * @return
     */
    @RequestMapping(value = "forget_check_answer.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetCheckAnswer(String username,String question,String answer){
        return iUserService.checkAnswer(username,question,answer);
    }

    /**
     * 未登录状态下重置密码接口
     * @param username
     * @param passwordNew
     * @param forgetToken
     * @return
     */
    @RequestMapping(value = "forget_reset_answer.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetResetPasswordByAnswer(String username,String passwordNew,String forgetToken){
        return iUserService.resetPasswordByAnswer(username,passwordNew,forgetToken);
    }

    /**
     * 登陆状态下修改密码接口
     * @param passwordOld
     * @param passwordNew
     * @param session
     * @return
     */
    @RequestMapping(value = "reset_password.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> resetPassword(String passwordOld,String passwordNew,HttpSession session){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorMessage("用户未登录");
        }
        return iUserService.resetPassword(user,passwordOld,passwordNew);
    }

    /**
     * 修改个人信息接口
     * @param session
     * @param user
     * @return
     */
    @RequestMapping(value = "updateUserInfo.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> updateUserInfo(HttpSession session,User user){
        User currentUser = (User)session.getAttribute(Const.CURRENT_USER);
        if(currentUser == null){
            return ServerResponse.createByErrorMessage("用户未登录");
        }
        user.setId(currentUser.getId());
        ServerResponse response = iUserService.updateUserInfo(user);
        if(response.isSuccess()){
            session.setAttribute(Const.CURRENT_USER,response.getDate());
        }
        return response;
    }

    /**
     * 获取用户详细信息接口
     * @param session
     * @return
     */
    @RequestMapping(value = "getUserDetail.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> getUserDetail(HttpSession session){
        User currentUser = (User)session.getAttribute(Const.CURRENT_USER);
        if(currentUser == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"未登录，请登陆后查看个人信息");
        }
        return iUserService.getInformation(currentUser.getId());
    }

}
