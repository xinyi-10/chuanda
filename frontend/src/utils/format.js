/**
 * 金额展示格式化。
 *
 * 后端 BigDecimal 统一以「字符串」返回（见后端 BigDecimalModule，避免 JS 浮点丢精度），
 * 前端只做展示层面的格式化，不参与金额运算。
 */
export function formatAmount(value) {
  if (value === null || value === undefined || value === '') return '0.00'
  return Number(value).toFixed(2)
}

/** 日期时间：'2026-10-08T09:12:00' → '2026-10-08 09:12:00' */
export function formatDateTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 19)
}

/** 日期：只取 yyyy-MM-dd */
export function formatDate(value) {
  if (!value) return ''
  return String(value).slice(0, 10)
}
