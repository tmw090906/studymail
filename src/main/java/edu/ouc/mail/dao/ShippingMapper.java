package edu.ouc.mail.dao;

import edu.ouc.mail.pojo.Shipping;
import org.apache.ibatis.annotations.Param;
import sun.nio.cs.ext.SJIS;

import java.util.List;

public interface ShippingMapper {
    int deleteByPrimaryKey(Long id);

    int insert(Shipping record);

    int insertSelective(Shipping record);

    Shipping selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Shipping record);

    int updateByPrimaryKey(Shipping record);

    int updateByShipping(Shipping shipping);

    Shipping selectByUserIdShippingId(@Param("userId") Long userId, @Param("shippingId") Long shippingId);

    List<Shipping> selectShippingListByUserId(Long userId);

    int deleteByUserIdShippingId(@Param("userId") Long userId, @Param("shippingId") Long shippingId);
}