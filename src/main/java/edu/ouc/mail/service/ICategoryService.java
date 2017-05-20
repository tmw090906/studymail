package edu.ouc.mail.service;

import edu.ouc.mail.common.ServerResponse;
import edu.ouc.mail.pojo.Category;

import java.util.List;

/**
 * Created by tmw090906 on 2017/5/20.
 */
public interface ICategoryService {

    ServerResponse addNewCategory(String categoryName, Long parentId);

    ServerResponse updateCategoryName(Long categoryId,String categoryName);

    ServerResponse<List<Category>> getChildrenParallelCategory(Long categoryId);

    ServerResponse selectCategoryAndDeepChildrenCategory(Long categoryId);
}
