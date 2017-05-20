package edu.ouc.mail.service.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import edu.ouc.mail.common.ServerResponse;
import edu.ouc.mail.dao.CategoryMapper;
import edu.ouc.mail.pojo.Category;
import edu.ouc.mail.service.ICategoryService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;

/**
 * Created by tmw090906 on 2017/5/20.
 */
@Service("iCategoryService")
public class CategoryServiceImpl implements ICategoryService {

    private Logger logger = LoggerFactory.getLogger(CategoryServiceImpl.class);

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    public ServerResponse addNewCategory(String categoryName, Long parentId) {
        if(parentId == null || StringUtils.isBlank(categoryName)){
            return ServerResponse.createByErrorMessage("添加品类参数错误");
        }
        Category category = new Category();
        category.setName(categoryName);
        category.setParentId(parentId);

        int rowCount = categoryMapper.insert(category);
        if(rowCount > 0){
            return ServerResponse.createBySuccessMessage("品类添加成功");
        }
        return ServerResponse.createByErrorMessage("品类添加失败");
    }

    @Override
    public ServerResponse updateCategoryName(Long categoryId, String categoryName) {
        if(categoryId == null || StringUtils.isBlank(categoryName)){
            return ServerResponse.createByErrorMessage("添加品类参数错误");
        }

        Category category = new Category();
        category.setName(categoryName);
        category.setId(categoryId);

        int rowCount = categoryMapper.updateByPrimaryKeySelective(category);
        if(rowCount > 0){
            return ServerResponse.createBySuccessMessage("品类名称修改成功");
        }
        return ServerResponse.createByErrorMessage("品类名称修改失败");
    }

    @Override
    public ServerResponse<List<Category>> getChildrenParallelCategory(Long categoryId) {
        List<Category> categoryList = categoryMapper.selectCategoryChildrenByParentId(categoryId);
        if(CollectionUtils.isEmpty(categoryList)){
            logger.info("未找到当前分类的子分类");
        }
        return ServerResponse.createBySuccess(categoryList);
    }


    /**
     * 调用私有方法findChildCategory方法查询本节点和孩子节点ID
     * @param categoryId
     * @return
     */
    @Override
    public ServerResponse<List<Long>> selectCategoryAndDeepChildrenCategory(Long categoryId) {
        Set<Category> categorySet = Sets.newHashSet();
        findChildCategory(categorySet,categoryId);

        List<Long> categoryIds = Lists.newArrayList();
        if(categoryId != null){
            for(Category categoryTemp : categorySet){
                categoryIds.add(categoryTemp.getId());
            }
        }
        return ServerResponse.createBySuccess(categoryIds);
    }

    //递归算法算出子节点
    private void findChildCategory(Set<Category> categorySet,Long categoryId){
        Category category = categoryMapper.selectByPrimaryKey(categoryId);
        if(category != null){
            categorySet.add(category);
        }
        //查找子节点，退出递归的条件
        List<Category> categoryList = categoryMapper.selectCategoryChildrenByParentId(categoryId);
        for(Category categoryTemp:categoryList){
            findChildCategory(categorySet,categoryTemp.getId());
        }
        return;
    }











}
