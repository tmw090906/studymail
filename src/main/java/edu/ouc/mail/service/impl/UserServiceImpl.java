package edu.ouc.mail.service.impl;

import edu.ouc.mail.common.Const;
import edu.ouc.mail.common.ServerResponse;
import edu.ouc.mail.common.TokenCache;
import edu.ouc.mail.dao.UserMapper;
import edu.ouc.mail.pojo.User;
import edu.ouc.mail.service.IUserService;
import edu.ouc.mail.util.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Created by tmw090906 on 2017/5/19.
 */
@Service("iUserService")
public class UserServiceImpl implements IUserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public ServerResponse<User> login(String username, String password) {
        int resultCount = userMapper.checkUsername(username);
        if(resultCount == 0){
            return ServerResponse.createByErrorMessage("用户名不存在");
        }
        //MD5加密后比较
        String md5Password = MD5Util.MD5EncodeUtf8(password);
        User user = userMapper.selectLogin(username,md5Password);
        if(user == null){
            return ServerResponse.createByErrorMessage("密码错误");
        }
        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess("登陆成功",user);
    }

    @Override
    public ServerResponse<User> register(User user) {
        ServerResponse checkValid = this.checkValid(user.getUsername(),Const.USERNAME);
        if(!checkValid.isSuccess()){
            return checkValid;
        }
        checkValid = this.checkValid(user.getEmail(),Const.EMAIL);
        if(!checkValid.isSuccess()){
            return checkValid;
        }
        user.setRole(Const.Role.ROLE_CUSTOMER);
        //MD5加密密码
        user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));
        int resultCount = userMapper.insert(user);
        if(resultCount == 0){
            return ServerResponse.createByErrorMessage("注册失败");
        }
        return ServerResponse.createBySuccessMessage("注册成功");
    }

    @Override
    public ServerResponse<String> checkValid(String str, String type) {
        if(StringUtils.isNotBlank(type)){
            //开始校验
            if(Const.USERNAME.equals(type)){
                int resultCount = userMapper.checkUsername(str);
                if(resultCount > 0){
                    return ServerResponse.createByErrorMessage("用户名已存在");
                }
            }
            if(Const.EMAIL.equals(type)){
                int resultCount = userMapper.checkEmail(str);
                if(resultCount > 0){
                    return ServerResponse.createByErrorMessage("email已存在");
                }
            }
        }else {
            return ServerResponse.createByErrorMessage("参数错误");
        }
        return ServerResponse.createBySuccessMessage("校验成功");
    }

    @Override
    public ServerResponse<String> selectQuestion(String username) {
        ServerResponse checkValid = this.checkValid(username,Const.USERNAME);
        if(checkValid.isSuccess()){
            //用户不存在
            return ServerResponse.createByErrorMessage("用户不存在");
        }
        String question = userMapper.selectQuestionByUsername(username);
        if(StringUtils.isNotBlank(question)){
            return ServerResponse.createBySuccess(question);
        }
        return ServerResponse.createByErrorMessage("找回密码的问题是空的");
    }

    @Override
    public ServerResponse<String> checkAnswer(String username, String question,String answer) {
        int resultCount = userMapper.checkAnswer(username,question,answer);
        if(resultCount > 0){
            //使用UUID作为本地缓存的value
            String forgetToken = UUID.randomUUID().toString();
            //使用常量+username的形式作为本地缓存的key
            TokenCache.setKey(TokenCache.TOKEN_PREFIX + username,forgetToken);
            return ServerResponse.createBySuccess(forgetToken);
        }
        return ServerResponse.createByErrorMessage("问题答案错误");
    }

    @Override
    public ServerResponse<String> resetPasswordByAnswer(String username, String passwordNew,String forgetToken) {
        if(StringUtils.isBlank(forgetToken)){
            return ServerResponse.createByErrorMessage("参数错误,Token需要传递");
        }
        ServerResponse checkValid = this.checkValid(username,Const.USERNAME);
        if(checkValid.isSuccess()){
            //用户不存在
            return ServerResponse.createByErrorMessage("用户不存在");
        }
        String localCacheToken = TokenCache.getKey(TokenCache.TOKEN_PREFIX + username);
        if(StringUtils.isBlank(localCacheToken)){
            return ServerResponse.createByErrorMessage("token无效或过期");
        }
        if(forgetToken.equals(localCacheToken)){
            String MD5PasswordNew =  MD5Util.MD5EncodeUtf8(passwordNew);
            int rowCount = userMapper.resetPasswordByforget(username,MD5PasswordNew);
            if(rowCount > 0){
                return ServerResponse.createBySuccess("重置密码成功");
            }
        }else {
            return ServerResponse.createByErrorMessage("Token错误，请重新获取重置密码Token");
        }
        return ServerResponse.createByErrorMessage("修改密码失败");
    }

    @Override
    public ServerResponse<String> resetPassword(User user, String passwordOld, String passwordNew) {
        //防止横向越权，校验用户旧密码
        int resultCount = userMapper.checkPassword(MD5Util.MD5EncodeUtf8(passwordOld),user.getId());
        if(resultCount == 0){
            return ServerResponse.createByErrorMessage("旧密码输入错误");
        }
        user.setPassword(MD5Util.MD5EncodeUtf8(passwordNew));
        resultCount = userMapper.updateByPrimaryKeySelective(user);
        if(resultCount > 0){
            return ServerResponse.createBySuccessMessage("密码修改成功");
        }
        return ServerResponse.createByErrorMessage("未知错误，密码修改失败");
    }

    @Override
    public ServerResponse<User> updateUserInfo(User user) {
        //username不能被修改
        //email也要进行校验，校验新的Email是否存在，并且如果相同的话不能是当前这个用户的
        int resultCount = userMapper.checkEmailByUserId(user.getEmail(),user.getId());
        if(resultCount > 0){
            return ServerResponse.createByErrorMessage("email已经存在，请更换后再进行更新");
        }
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setEmail(user.getEmail());
        updateUser.setPhone(user.getPhone());
        updateUser.setAnswer(user.getAnswer());
        updateUser.setQuestion(user.getQuestion());
        int updateCount = userMapper.updateByPrimaryKeySelective(updateUser);
        if(updateCount > 0 ){
            updateUser = userMapper.selectByPrimaryKey(updateUser.getId());
            updateUser.setPassword(StringUtils.EMPTY);
            return ServerResponse.createBySuccess("修改个人信息成功",updateUser);
        }
        return ServerResponse.createByErrorMessage("未知错误，修改个人信息失败");
    }

    @Override
    public ServerResponse<User> getInformation(Long userId) {
        User user = userMapper.selectByPrimaryKey(userId);
        if(user == null){
            return ServerResponse.createByErrorMessage("找不到当前用户");
        }
        user.setPassword(org.apache.commons.lang3.StringUtils.EMPTY);
        return ServerResponse.createBySuccess(user);
    }
}
