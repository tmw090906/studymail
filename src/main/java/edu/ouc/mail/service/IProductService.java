package edu.ouc.mail.service;

import com.github.pagehelper.PageInfo;
import edu.ouc.mail.common.ServerResponse;
import edu.ouc.mail.pojo.Product;
import edu.ouc.mail.vo.ProductDetailVo;

/**
 * Created by tmw090906 on 2017/5/23.
 */
public interface IProductService {

    ServerResponse saveOrUpdateProduct(Product product);

    ServerResponse<String> setSaleStatus(Long productId,Integer status);

    ServerResponse<ProductDetailVo> managerProductDetail(Long productId);

    ServerResponse<PageInfo> getProductList(int pageNum, int pageSize);

    ServerResponse<PageInfo> searchProduct(String productName,Long productId,int pageNum,int pageSize);

    ServerResponse<ProductDetailVo> getProductDetail(Long productId);

    ServerResponse<PageInfo> getProductBySearch(String keyword,Long categoryId,int pageNum,int pageSize,String orderBy);
}
