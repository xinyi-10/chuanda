package com.life.controller;

import com.life.common.PageResult;
import com.life.common.Result;
import com.life.common.UserContext;
import com.life.domain.LedgerRecord;
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

/** 记账流水接口 */
@RestController
@RequestMapping("/ledger/record")
public class LedgerRecordController {

    @Autowired
    private LedgerService ledgerService;

    /** 分页：GET /ledger/record/page?type=&categoryId=&startDate=&endDate=&pageNum=&pageSize= */
    @GetMapping("/page")
    public Result<PageResult<LedgerRecord>> page(@RequestParam(required = false) Integer type,
                                                 @RequestParam(required = false) Long categoryId,
                                                 @RequestParam(required = false) String startDate,
                                                 @RequestParam(required = false) String endDate,
                                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.ok(ledgerService.pageRecord(UserContext.getUserId(), type, categoryId,
                startDate, endDate, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public Result<LedgerRecord> detail(@PathVariable Long id) {
        return Result.ok(ledgerService.getRecord(id, UserContext.getUserId()));
    }

    @PostMapping
    public Result<Long> create(@RequestBody LedgerRecord record) {
        return Result.ok("记账成功", ledgerService.createRecord(UserContext.getUserId(), record));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody LedgerRecord record) {
        record.setId(id);
        ledgerService.updateRecord(UserContext.getUserId(), record);
        return Result.ok("保存成功", null);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        ledgerService.deleteRecord(UserContext.getUserId(), id);
        return Result.ok("删除成功", null);
    }
}
