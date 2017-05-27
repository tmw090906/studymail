package edu.ouc.mail.service.impl;

import com.github.pagehelper.PageHelper;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import edu.ouc.mail.common.Const;
import edu.ouc.mail.common.ResponseCode;
import edu.ouc.mail.common.ServerResponse;
import edu.ouc.mail.dao.CartMapper;
import edu.ouc.mail.dao.ProductMapper;
import edu.ouc.mail.pojo.Cart;
import edu.ouc.mail.pojo.Product;
import edu.ouc.mail.service.ICartService;
import edu.ouc.mail.util.BigDecimalUtil;
import edu.ouc.mail.util.PropertiesUtil;
import edu.ouc.mail.vo.CartProductVo;
import edu.ouc.mail.vo.CartVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by tmw090906 on 2017/5/26.
 */
@Service("iCartService")
public class CartServiceImpl implements ICartService {

    @Autowired
    private CartMapper cartMapper;

    @Autowired
    private ProductMapper productMapper;


    @Override
    public ServerResponse<CartVo> add(Long userId,Long productId,Long count){
        if(productId == null || count == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Cart cart = cartMapper.selectByUserIdAndProductId(userId, productId);
        if(cart == null){
            Cart cartItem = new Cart();
            cartItem.setProductId(productId);
            cartItem.setUserId(userId);
            cartItem.setQuantity(count);
            cartItem.setChecked(Const.Cart.CHECK);
            cartMapper.insert(cartItem);
        }else {
            count = cart.getQuantity() + count;
            cart.setQuantity(count);
            cartMapper.updateByPrimaryKeySelective(cart);
        }
        return getList(userId);
    }

    @Override
    public ServerResponse<CartVo> getList(Long userId){
        CartVo cartVo = getPortalCartList(userId);
        return ServerResponse.createBySuccess(cartVo);
    }

    @Override
    public ServerResponse<CartVo> updateCount(Long userId,Long productId,Long count){
        if(productId == null || count == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        Cart cart = cartMapper.selectByUserIdAndProductId(userId, productId);
        if(cart != null){
            cart.setQuantity(count);
        }
        cartMapper.updateByPrimaryKey(cart);
        return getList(userId);
    }

    @Override
    public ServerResponse<CartVo> deleteProduct(Long userId, String productIds) {
        List<String> productIdList = Splitter.on(",").splitToList(productIds);
        if(CollectionUtils.isEmpty(productIdList)){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.ILLEGAL_ARGUMENT.getCode(),ResponseCode.ILLEGAL_ARGUMENT.getDesc());
        }
        cartMapper.deleteByUserIdAndProductIds(userId,productIdList);
        return getList(userId);
    }

    @Override
    public ServerResponse<CartVo> selectOrUnSelect(Long userId,Long productId,Long checked){
        cartMapper.setCheckedOrUnCheckedStatus(userId, productId, checked);
        return this.getList(userId);
    }

    @Override
    public ServerResponse<Integer> getCartCount(Long userId){
        Integer count = cartMapper.selectCartProductCount(userId);
        return ServerResponse.createBySuccess(count);
    }





    private CartVo getPortalCartList(Long userId){
        CartVo cartVo = new CartVo();
        List<Cart> cartList = cartMapper.selectByUserId(userId);
        List<CartProductVo> cartProductVoList = Lists.newArrayList();

        BigDecimal cartTotalPrice = new BigDecimal("0");

        if(!CollectionUtils.isEmpty(cartList)){
            for(Cart cart : cartList){
                CartProductVo cartProductVo = new CartProductVo();
                cartProductVo.setId(cart.getId());
                cartProductVo.setUserId(userId);
                cartProductVo.setProductId(cart.getProductId());

                Product product = productMapper.selectByPrimaryKey(cart.getProductId());
                if(product != null){
                    cartProductVo.setProductMainImage(product.getMainImage());
                    cartProductVo.setProductName(product.getName());
                    cartProductVo.setProductSubtitle(product.getSubtitle());
                    cartProductVo.setProductStatus(product.getStatus().longValue());
                    cartProductVo.setProductPrice(product.getPrice());
                    cartProductVo.setProductStock(product.getStock());

                    //判断库存
                    long buyLimitCount = 0;
                    if(product.getStock() >= cart.getQuantity()){
                        buyLimitCount = cart.getQuantity();
                        cartProductVo.setLimitQuantity(Const.Cart.LIMIT_NUM_SUCCESS);
                    }else {
                        buyLimitCount = product.getStock();
                        cartProductVo.setLimitQuantity(Const.Cart.LIMIT_NUM_FAIL);
                        Cart cartTemp = new Cart();
                        cartTemp.setId(cart.getId());
                        cartTemp.setQuantity(product.getStock());
                        cartMapper.updateByPrimaryKeySelective(cartTemp);
                    }
                    cartProductVo.setQuantity(buyLimitCount);
                    //计算某样物品总价
                    cartProductVo.setProductTotalPrice(BigDecimalUtil.mul(product.getPrice().doubleValue(),cartProductVo.getQuantity()));
                    cartProductVo.setProductChecked(cart.getChecked());
                }
                if(cart.getChecked() == Const.Cart.CHECK){
                    //计算整个购物车总价
                    cartTotalPrice = BigDecimalUtil.add(cartTotalPrice.doubleValue(),cartProductVo.getProductTotalPrice().doubleValue());
                }
                cartProductVoList.add(cartProductVo);
            }
        }
        cartVo.setCartProductVoList(cartProductVoList);
        cartVo.setCartTotalPrice(cartTotalPrice);
        cartVo.setAllChecked(this.getAllCheckedStatus(userId));
        cartVo.setImageHost(PropertiesUtil.getProperty("ftp.server.http.prefix"));
        return cartVo;
    }

    private boolean getAllCheckedStatus(Long userId){
        if(userId == null){
            return false;
        }
        return cartMapper.selectCartProductCheckedStatusByUserId(userId) == 0;
    }



















}
