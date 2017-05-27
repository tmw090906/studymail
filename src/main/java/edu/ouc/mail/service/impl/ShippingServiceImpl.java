package edu.ouc.mail.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import edu.ouc.mail.common.ServerResponse;
import edu.ouc.mail.dao.ShippingMapper;
import edu.ouc.mail.pojo.Shipping;
import edu.ouc.mail.service.IShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by tmw090906 on 2017/5/26.
 */
@Service("iShippingService")
public class ShippingServiceImpl implements IShippingService {

    @Autowired
    private ShippingMapper shippingMapper;


    @Override
    public ServerResponse addShipping(Long userId, Shipping shipping){
        if(shipping == null){
            return ServerResponse.createByErrorMessage("新建地址失败");
        }
        shipping.setUserId(userId);
        int rowCount = shippingMapper.insert(shipping);
        if(rowCount >= 0 ){
            return ServerResponse.createBySuccess("新建地址成功");
        }else {
            return ServerResponse.createByErrorMessage("新建地址失败");
        }
    }

    @Override
    public ServerResponse deleteShipping(Long userId, Long shippingId) {
        int rowCount = shippingMapper.deleteByPrimaryKey(shippingId);
        if(rowCount >= 0 ){
            return ServerResponse.createBySuccess("删除地址成功");
        }else {
            return ServerResponse.createByErrorMessage("删除地址失败");
        }
    }

    @Override
    public ServerResponse updateShipping(Long userId, Shipping shipping) {
        shipping.setUserId(userId);
        int rowCount = shippingMapper.updateByShipping(shipping);
        if(rowCount >= 0 ){
            return ServerResponse.createBySuccess("更新地址成功");
        }else {
            return ServerResponse.createByErrorMessage("更新地址失败");
        }
    }

    @Override
    public ServerResponse<Shipping> selectShippingDetail(Long userId, Long shippingId) {
        Shipping shipping = shippingMapper.selectByUserIdShippingId(userId, shippingId);
        if(shipping == null){
            return ServerResponse.createByErrorMessage("无法查询到该地址");
        }
        return ServerResponse.createBySuccess(shipping);
    }

    @Override
    public ServerResponse<PageInfo> getShippingList(Long userId,int pageNum,int pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        List<Shipping> shippingList = shippingMapper.selectShippingListByUserId(userId);
        PageInfo pageInfo = new PageInfo(shippingList);
        return ServerResponse.createBySuccess(pageInfo);
    }
}
