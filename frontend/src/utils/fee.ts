export const FEE_RATE = 0.00015;
export const TAX_RATE = 0.0020;

export function calculateFee(amount: number): number {
    if (amount <= 0) return 0;
    return Math.round(amount * FEE_RATE);
}

export function calculateTax(amount: number): number {
    if (amount <= 0) return 0;
    return Math.round(amount * TAX_RATE);
}

/** 지금 이 금액어치를 매도한다면 나갈 수수료+거래세 합계 */
export function calculateSellCost(amount: number): number {
    return calculateFee(amount) + calculateTax(amount);
}

/** 손익분기매입금액 = 매입원금액 / (1 - 매도수수료율 - 거래세율) */
export function calculateBreakevenAmount(costAmount: number): number {
    if (costAmount <= 0) return 0;
    return Math.round(costAmount / (1 - FEE_RATE - TAX_RATE));
}
