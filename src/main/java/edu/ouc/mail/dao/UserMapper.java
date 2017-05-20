package edu.ouc.mail.dao;

import edu.ouc.mail.pojo.User;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {
    int deleteByPrimaryKey(Long id);

    int insert(User record);

    int insertSelective(User record);

    User selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(User record);

    int updateByPrimaryKey(User record);

    int checkUsername(String username);

    int checkEmail(String email);

    User selectLogin(@Param("username") String username,@Param("password") String password);

    int checkAnswer(@Param("username")String username,@Param("question")String question,@Param("answer")String answer);

    int resetPasswordByforget(@Param("username")String username,@Param("passwordNew")String passwordNew);

    String selectQuestionByUsername(String username);

    int checkPassword(@Param("password")String password,@Param("userId")Long userId);

    int checkEmailByUserId(@Param("email")String email,@Param("userId")Long userID);
}