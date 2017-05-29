package edu.ouc.mail.dao;

import edu.ouc.mail.pojo.Order;
import org.apache.ibatis.annotations.Param;

public interface OrderMapper {
    int deleteByPrimaryKey(Long id);

    int insert(Order record);

    int insertSelective(Order record);

    Order selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Order record);

    int updateByPrimaryKey(Order record);

    Order selectByUserIdOderNo(@Param("userId") Long userId,@Param("orderNo") Long orderNo);

    Order selectByOrderNo(Long orderNo);
}