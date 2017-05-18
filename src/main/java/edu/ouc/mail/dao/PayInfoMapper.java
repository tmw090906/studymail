package edu.ouc.mail.dao;

import edu.ouc.mail.pojo.PayInfo;

public interface PayInfoMapper {
    int deleteByPrimaryKey(Long id);

    int insert(PayInfo record);

    int insertSelective(PayInfo record);

    PayInfo selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(PayInfo record);

    int updateByPrimaryKey(PayInfo record);
}