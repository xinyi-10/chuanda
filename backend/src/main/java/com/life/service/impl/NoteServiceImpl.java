package com.life.service.impl;

import com.life.common.BizException;
import com.life.common.PageResult;
import com.life.common.ResultCode;
import com.life.dao.NoteDao;
import com.life.domain.Note;
import com.life.service.NoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/** 笔记服务实现 */
@Service
public class NoteServiceImpl implements NoteService {

    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private NoteDao noteDao;

    @Override
    public PageResult<Note> page(Long userId, Long categoryId, String keyword,
                                 Integer pageNum, Integer pageSize) {
        int pn = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int ps = (pageSize == null || pageSize < 1 || pageSize > MAX_PAGE_SIZE) ? 10 : pageSize;
        String kw = StringUtils.hasText(keyword) ? keyword.trim() : null;

        long total = noteDao.countByCondition(userId, categoryId, kw);
        List<Note> list = noteDao.listByPage(userId, categoryId, kw, (pn - 1) * ps, ps);
        return PageResult.of(list, total, pn, ps);
    }

    @Override
    public Note getById(Long id, Long userId) {
        Note note = noteDao.selectById(id, userId);
        if (note == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        return note;
    }

    @Override
    public Long create(Long userId, Note note) {
        if (!StringUtils.hasText(note.getTitle())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "标题不能为空");
        }
        note.setUserId(userId);
        noteDao.insert(note);
        return note.getId();
    }

    @Override
    public void update(Long userId, Note note) {
        if (note.getId() == null) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "缺少笔记 ID");
        }
        // 先校验归属，避免越权改他人笔记
        if (noteDao.selectById(note.getId(), userId) == null) {
            throw new BizException(ResultCode.NOT_FOUND);
        }
        if (!StringUtils.hasText(note.getTitle())) {
            throw new BizException(ResultCode.PARAM_ERROR.getCode(), "标题不能为空");
        }
        note.setUserId(userId);
        noteDao.update(note);
    }

    @Override
    public void delete(Long userId, Long id) {
        noteDao.logicDelete(id, userId);
    }
}
