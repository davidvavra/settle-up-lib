package cz.destil.settleup.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Helper class for handling BigDecimal
 * Created by Destil on 24.8.13.
 */
public class BigDecimals {

    // for BigDecimal comparisons:
    private static int FIRST_SMALLER = -1;
    private static int IDENTICAL = 0;
    private static int FIRST_LARGER = 1;

    public static boolean isFirstSmaller(BigDecimal first, BigDecimal second) {
        return first.compareTo(second) == FIRST_SMALLER;
    }

    public static boolean isFirstSmallerOrEqual(BigDecimal first, BigDecimal second) {
        return first.compareTo(second) == FIRST_SMALLER || first.compareTo(second) == IDENTICAL;
    }

    public static boolean isFirstLarger(BigDecimal first, BigDecimal second) {
        return first.compareTo(second) == FIRST_LARGER;
    }

    public static boolean isFirstLargerOrEqual(BigDecimal first, BigDecimal second) {
        return first.compareTo(second) == FIRST_LARGER || first.compareTo(second) == IDENTICAL;
    }

    public static boolean areTheyEqual(BigDecimal first, BigDecimal second) {
        return first.compareTo(second) == IDENTICAL;
    }

    public static BigDecimal sum(List<BigDecimal> list) {
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal amount : list) {
            sum = sum.add(amount);
        }
        return sum;
    }

    public static boolean isPositive(BigDecimal number) {
        return number.signum() > 0;
    }

    public static boolean isNegative(BigDecimal number) {
        return number.signum() < 0;
    }

    public static boolean isZero(BigDecimal number) {
        return number.signum() == 0;
    }

    public static boolean isNotZero(BigDecimal number) {
        return number.signum() != 0;
    }

    public static BigDecimal safeDivide(BigDecimal first, BigDecimal second) {
        return first.divide(second, 20, RoundingMode.HALF_UP);
    }

    public static BigDecimal roundAlmostZero(BigDecimal number) {
        if (isFirstLargerOrEqual(number, new BigDecimal(-0.01)) && isFirstSmallerOrEqual(number, new BigDecimal(0.01))) {
            return BigDecimal.ZERO;
        }
        return number;
    }
}
