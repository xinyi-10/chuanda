import request from './request'

/** 读取收入配置（未填写时 initialized=false、remainModify=2） */
export const getIncomeConfig = () => request.get('/income/config')

/** 首次填写（不计入修改次数） */
export const initIncome = (data) => request.post('/income/init', data)

/** 修改（自然月最多 2 次） */
export const updateIncome = (data) => request.put('/income/config', data)

/** 立即补记当月工资（幂等，同月不重复） */
export const postSalaryNow = () => request.post('/income/post-now')
