// 统一的单据编码生成器：日期 + 时分秒 + 两位随机数，避免并发下分钟级碰撞。
export function generateCode(prefix: string) {
  const d = new Date();
  const date = `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, "0")}${String(d.getDate()).padStart(2, "0")}`;
  const time = `${String(d.getHours()).padStart(2, "0")}${String(d.getMinutes()).padStart(2, "0")}${String(d.getSeconds()).padStart(2, "0")}`;
  const rand = String(Math.floor(Math.random() * 100)).padStart(2, "0");
  return `${prefix}-${date}-${time}${rand}`;
}
