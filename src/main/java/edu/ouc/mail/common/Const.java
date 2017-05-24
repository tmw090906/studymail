package edu.ouc.mail.common;

import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Created by tmw090906 on 2017/5/19.
 * 常量类
 */
public class Const {

    public static final String CURRENT_USER = "currentUser";

    public static final String EMAIL= "email";

    public static final String USERNAME= "username";

    public interface Role{
        short ROLE_CUSTOMER = 0; //普通用户
        short ROLE_ADMIN = 1; //管理员
    }

    public interface ProductListOrderBy {
        Set<String> PRICE_DESC_ASC = Sets.newHashSet("price_desc","price_asc");
    }

    public enum ProductStatusEnum{
        ON_SALE(1,"在销售");
        private String value;
        private int code;

        ProductStatusEnum(int code,String value){
            this.code = code;
            this.value = value;
        }

        public String getValue() {
            return value;
        }
        public int getCode() {
            return code;
        }

    }

}
