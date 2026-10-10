package com.life.service.impl;

import com.life.common.BizException;
import com.life.common.ResultCode;
import com.life.dao.NoteCategoryDao;
import com.life.dao.NoteDao;
import com.life.domain.NoteCategory;
import com.life.service.NoteCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/** 笔记分类服务实现 */
@Service
public class NoteCategoryServiceImpl implements NoteCategoryService {

    @Autowired
    private NoteCategoryDao noteCategoryDao;

    @Autowired
    private NoteDao noteDao;

    @Override
    public List<NoteCategory> list(Long userId) {
        return noteCategoryDao.listByUser(userId);
    }

    @Override
    public Long create(Long userId, String name, Integer sort) {
        if (!StringUtils.hasText(name)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "分类名不能为空");
        }
        if (noteCategoryDao.selectByName(userId, name.trim()) != null) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "分类名已存在");
        }
        NoteCategory category = new NoteCategory();
        category.setUserId(userId);
        category.setName(name.trim());
        category.setSort(sort == null ? 0 : sort);
        noteCategoryDao.insert(category);
        return category.getId();
    }

    @Override
    public void update(Long userId, Long id, String name, Integer sort) {
        NoteCategory exist = noteCategoryDao.selectById(id, userId);
        if (exist == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        if (!StringUtils.hasText(name)) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "分类名不能为空");
        }
        NoteCategory category = new NoteCategory();
        category.setId(id);
        category.setUserId(userId);
        category.setName(name.trim());
        category.setSort(sort == null ? exist.getSort() : sort);
        noteCategoryDao.update(category);
    }

    @Override
    public void delete(Long userId, Long id) {
        if (noteCategoryDao.selectById(id, userId) == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        // 分类删除保护：分类下仍有笔记时不允许删除
        if (noteDao.countByCategory(userId, id) > 0) {
            throw new BizException(ResultCode.CATEGORY_IN_USE);
        }
        noteCategoryDao.logicDelete(id, userId);
    }
}
