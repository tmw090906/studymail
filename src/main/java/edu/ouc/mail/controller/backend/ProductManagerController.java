package edu.ouc.mail.controller.backend;

import com.google.common.collect.Maps;
import edu.ouc.mail.common.Const;
import edu.ouc.mail.common.ResponseCode;
import edu.ouc.mail.common.ServerResponse;
import edu.ouc.mail.pojo.Product;
import edu.ouc.mail.pojo.User;
import edu.ouc.mail.service.IFileService;
import edu.ouc.mail.service.IProductService;
import edu.ouc.mail.service.IUserService;
import edu.ouc.mail.util.PropertiesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Created by tmw090906 on 2017/5/23.
 */
@Controller
@RequestMapping("/manager/product")
public class ProductManagerController {

    @Autowired
    private IUserService iUserService;

    @Autowired
    private IProductService iProductService;

    @Autowired
    private IFileService iFileService;

    /**
     * 保存或新增商品
     * @param session
     * @param product
     * @return
     */
    @RequestMapping("save.do")
    @ResponseBody
    public ServerResponse productSava(HttpSession session, Product product){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"请先登陆");
        }
        if(iUserService.checkAdminRole(user).isSuccess()){
            return iProductService.saveOrUpdateProduct(product);
        }else {
            return ServerResponse.createByErrorMessage("用户无权限");
        }
    }

    /**
     * 设置商品状态
     * @param session
     * @param productId
     * @param status
     * @return
     */
    @RequestMapping("set_sale_status.do")
    @ResponseBody
    public ServerResponse setSaleStatus(HttpSession session,Long productId,Integer status){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"请先登陆");
        }
        if(iUserService.checkAdminRole(user).isSuccess()){
            return iProductService.setSaleStatus(productId,status);
        }else {
            return ServerResponse.createByErrorMessage("用户无权限");
        }
    }

    /**
     * 查看商品详情
     * @param session
     * @param productId
     * @return
     */
    @RequestMapping("detail.do")
    @ResponseBody
    public ServerResponse getProductDetail(HttpSession session,Long productId){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"请先登陆");
        }
        if(iUserService.checkAdminRole(user).isSuccess()){
            return iProductService.managerProductDetail(productId);
        }else {
            return ServerResponse.createByErrorMessage("用户无权限");
        }
    }

    /**
     * 查看所有商品列表
     * @param session
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestMapping("list.do")
    @ResponseBody
    public ServerResponse getList(HttpSession session, @RequestParam(value = "pageNum",defaultValue = "1") int pageNum, @RequestParam(value = "pageSize",defaultValue = "10")int pageSize){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"请先登陆");
        }
        if(iUserService.checkAdminRole(user).isSuccess()){
            return iProductService.getProductList(pageNum, pageSize);
        }else {
            return ServerResponse.createByErrorMessage("用户无权限");
        }
    }

    /**
     * 根据关键字查找商品
     * @param session
     * @param productName
     * @param productId
     * @param pageNum
     * @param pageSize
     * @return
     */
    @RequestMapping("search.do")
    @ResponseBody
    public ServerResponse searchProduct(HttpSession session,String productName,Long productId, @RequestParam(value = "pageNum",defaultValue = "1") int pageNum, @RequestParam(value = "pageSize",defaultValue = "10")int pageSize){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"请先登陆");
        }
        if(iUserService.checkAdminRole(user).isSuccess()){
            return iProductService.searchProduct(productName, productId, pageNum, pageSize);
        }else {
            return ServerResponse.createByErrorMessage("用户无权限");
        }
    }

    /**
     * 上传图片
     * @param session
     * @param upload_file
     * @param request
     * @return
     */
    @RequestMapping("upload.do")
    @ResponseBody
    public ServerResponse upload(HttpSession session,MultipartFile upload_file, HttpServletRequest request){
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        if(user == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"请先登陆");
        }
        if(iUserService.checkAdminRole(user).isSuccess()){
            String path  = request.getSession().getServletContext().getRealPath("upload");
            String targetFileName = iFileService.upload(upload_file,path,user.getUsername());
            String url = PropertiesUtil.getProperty("ftp.server.http.prefix") + "img/"+ targetFileName;
            Map fileMap = Maps.newHashMap();
            fileMap.put("uri",targetFileName);
            fileMap.put("url",url);

            return ServerResponse.createBySuccess(fileMap);
        }else {
            return ServerResponse.createByErrorMessage("用户无权限");
        }
    }

    /**
     * 使用CKEditor富文本编辑器编辑上传图片
     * @param session
     * @param upload_file
     * @param request
     * @param response
     * @throws IOException
     */
    @RequestMapping("ckeditor_upload.do")
    @ResponseBody
    public void uploadCkeditor(HttpSession session, @RequestParam("upload")MultipartFile upload_file, HttpServletRequest request, HttpServletResponse response) throws IOException {
        User user = (User)session.getAttribute(Const.CURRENT_USER);
        //PrintWriter
        PrintWriter out = response.getWriter();
        String callback = request.getParameter("CKEditorFuncNum");

        if(user == null){
            out.println("<script type=\"text/javascript\">");
            out.println("window.parent.CKEDITOR.tools.callFunction("
                    + callback + ",'',"
                    + "'用户未登录，请先登陆');");
            out.println("</script>");
            return;
        }
        if(iUserService.checkAdminRole(user).isSuccess()){
            String path  = request.getSession().getServletContext().getRealPath("upload");
            String targetFileName = iFileService.upload(upload_file,path,user.getUsername());
            String url = PropertiesUtil.getProperty("ftp.server.http.prefix") + "img/"+ targetFileName;
            out.println("<script type=\"text/javascript\">");
            out.println("window.parent.CKEDITOR.tools.callFunction("
                    + callback + ",'" + url
                    + "','')");
            out.println("</script>");
            return;
        }else {
            out.println("<script type=\"text/javascript\">");
            out.println("window.parent.CKEDITOR.tools.callFunction("
                    + callback + ",'',"
                    + "'用户无权限');");
            out.println("</script>");
            return;
        }
    }




}
