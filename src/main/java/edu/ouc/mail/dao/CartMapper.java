package edu.ouc.mail.dao;

import edu.ouc.mail.pojo.Cart;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CartMapper {
    int deleteByPrimaryKey(Long id);

    int insert(Cart record);

    int insertSelective(Cart record);

    Cart selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Cart record);

    int updateByPrimaryKey(Cart record);

    //List<Cart> selectListByUserId(Long userId);

    Cart selectByUserIdAndProductId(@Param("userId") Long userId,@Param("productId") Long productId);

    List<Cart> selectByUserId(Long userId);

    int selectCartProductCheckedStatusByUserId(Long userId);

    int deleteByUserIdAndProductIds(@Param("userId")Long userId,@Param("productIdList")List<String> productIdList);

    int setCheckedOrUnCheckedStatus(@Param("userId")Long userId,@Param("productId")Long productId,@Param("checked")Long checked);

    int selectCartProductCount(Long userId);

}