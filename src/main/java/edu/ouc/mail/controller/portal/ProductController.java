package edu.ouc.mail.controller.portal;

import com.github.pagehelper.PageInfo;
import edu.ouc.mail.common.ServerResponse;
import edu.ouc.mail.service.IProductService;
import edu.ouc.mail.vo.ProductDetailVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by tmw090906 on 2017/5/24.
 */
@Controller
@RequestMapping(value = "/product/")
public class ProductController {

    @Autowired
    private IProductService iProductService;

    @RequestMapping("get_detail.do")
    @ResponseBody
    public ServerResponse<ProductDetailVo> getProductDetail(Long productId){
        return iProductService.getProductDetail(productId);
    }

    @RequestMapping("get_list.do")
    @ResponseBody
    public ServerResponse<PageInfo> getList(@RequestParam(value = "keyword",required = false) String keyword,
                                            @RequestParam(value = "categoryId",required = false)Long categoryId,
                                            @RequestParam(defaultValue = "1")int pageNum,
                                            @RequestParam(defaultValue = "10")int pageSize,
                                            @RequestParam(value = "orderBy",required = false) String orderBy){
        return iProductService.getProductBySearch(keyword, categoryId, pageNum, pageSize, orderBy);
    }
}
