package edu.ouc.mail.service;

import edu.ouc.mail.common.ServerResponse;
import edu.ouc.mail.vo.CartVo;

/**
 * Created by tmw090906 on 2017/5/26.
 */
public interface ICartService {


    ServerResponse<CartVo> getList(Long userId);

    ServerResponse<CartVo> add(Long userId,Long productId,Long count);

    ServerResponse<CartVo> updateCount(Long userId,Long productId,Long count);

    ServerResponse<CartVo> deleteProduct(Long userId,String productIds);

    ServerResponse<CartVo> selectOrUnSelect(Long userId,Long productId,Long checked);

    ServerResponse<Integer> getCartCount(Long userId);
}
