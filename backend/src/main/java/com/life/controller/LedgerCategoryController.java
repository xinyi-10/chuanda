package com.life.controller;

import com.life.common.Result;
import com.life.common.UserContext;
import com.life.domain.LedgerCategory;
import com.life.service.LedgerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 记账分类接口 */
@RestController
@RequestMapping("/ledger/category")
public class LedgerCategoryController {

    @Autowired
    private LedgerService ledgerService;

    /** 分类列表：GET /ledger/category/list?type=（type 可空查全部） */
    @GetMapping("/list")
    public Result<List<LedgerCategory>> list(@RequestParam(required = false) Integer type) {
        return Result.ok(ledgerService.listCategory(UserContext.getUserId(), type));
    }

    @PostMapping
    public Result<Long> create(@RequestBody LedgerCategory category) {
        return Result.ok("创建成功", ledgerService.createCategory(UserContext.getUserId(), category));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody LedgerCategory category) {
        category.setId(id);
        ledgerService.updateCategory(UserContext.getUserId(), category);
        return Result.ok("保存成功", null);
    }

    /** 删除（有流水时拒绝） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        ledgerService.deleteCategory(UserContext.getUserId(), id);
        return Result.ok("删除成功", null);
    }
}
