package edu.ouc.mail.service;

import com.github.pagehelper.PageInfo;
import edu.ouc.mail.common.ServerResponse;
import edu.ouc.mail.pojo.Shipping;

/**
 * Created by tmw090906 on 2017/5/26.
 */
public interface IShippingService {

    ServerResponse addShipping(Long userId, Shipping shipping);

    ServerResponse deleteShipping(Long userId,Long shippingId);

    ServerResponse updateShipping(Long userId,Shipping shipping);

    ServerResponse<Shipping> selectShippingDetail(Long userId,Long shippingId);

    ServerResponse<PageInfo> getShippingList(Long userId,int pageNum,int pageSize);
}
