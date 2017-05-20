package edu.ouc.mail.service;

import edu.ouc.mail.common.ServerResponse;
import edu.ouc.mail.pojo.User;

/**
 * Created by tmw090906 on 2017/5/19.
 */
public interface IUserService {

    ServerResponse<User> login(String username, String password);

    ServerResponse<User> register(User user);

    ServerResponse<String> checkValid(String str,String type);

    ServerResponse<String> selectQuestion(String username);

    ServerResponse<String> checkAnswer(String username,String question,String answer);

    ServerResponse<String> resetPasswordByAnswer(String username,String passwordNew,String forgetToken);

    ServerResponse<String> resetPassword(User user,String passwordOld,String passwordNew);

    ServerResponse<User> updateUserInfo(User user);

    ServerResponse<User> getInformation(Long userId);

    ServerResponse checkAdminRole(User user);
}
