package edu.ouc.mail.common;

/**
 * Created by tmw090906 on 2017/5/19.
 */
public class Const {

    public static final String CURRENT_USER = "currentUser";

    public static final String EMAIL= "email";

    public static final String USERNAME= "username";

    public interface Role{
        short ROLE_CUSTOMER = 0; //普通用户
        short ROLE_ADMIN = 1; //管理员
    }

}
