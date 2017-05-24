package edu.ouc.mail.controller.backend;

import edu.ouc.mail.common.Const;
import edu.ouc.mail.common.ServerResponse;
import edu.ouc.mail.pojo.User;
import edu.ouc.mail.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

/**
 * Created by tmw090906 on 2017/5/20.
 */
@Controller
@RequestMapping(value = "/manager/user/")
public class UserManagerController {

    @Autowired
    private IUserService iUserService;

    /**
     * 后台管理员登陆
     * @param username
     * @param password
     * @param session
     * @return
     */
    @RequestMapping(value = "login.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> login(String username, String password, HttpSession session){
        ServerResponse<User> response = iUserService.login(username, password);
        if(response.isSuccess()){
            User user = response.getDate();
            if(user.getRole() == Const.Role.ROLE_ADMIN){
                session.setAttribute(Const.CURRENT_USER,user);
                return response;
            }else {
                return ServerResponse.createBySuccessMessage("权限低，无法登陆");
            }
        }
        return response;
    }
}
