package cz.destil.settleup.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Utils {

	@SuppressWarnings("unused")
	private static final String TAG = "Utils";

	/**
	 * Round to two decimal places
	 */
	public static BigDecimal round(BigDecimal number) {
		return number.setScale(2, RoundingMode.HALF_UP);
	}

    public static String round(double weight) {
        DecimalFormat format = new DecimalFormat("#.#");
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(weight);
    }

	public static List<Long> splitToLongs(String text) {
		String[] parts = text.trim().split(" ");
		List<Long> longs = new ArrayList<Long>();
		for (int i = 0; i < parts.length; i++) {
			if (!TextUtils.isEmpty(parts[i])) {
				longs.add(Long.parseLong(parts[i]));
			}
		}
		return longs;
	}

	public static List<String> splitToStrings(String text) {
		String[] parts = text.trim().split(" ");
		List<String> strings = new ArrayList<String>();
		for (int i = 0; i < parts.length; i++) {
			if (!TextUtils.isEmpty(parts[i])) {
				strings.add(parts[i]);
			}
		}
		return strings;
	}

	public static List<Double> splitToDoubles(String text) {
		String[] parts = text.trim().split(" ");
		List<Double> doubles = new ArrayList<Double>();
		for (int i = 0; i < parts.length; i++) {
			if (!TextUtils.isEmpty(parts[i])) {
				doubles.add(Double.parseDouble(parts[i]));
			}
		}
		return doubles;
	}

    public static List<BigDecimal> splitToBigDecimals(String text) {
        String[] parts = text.trim().split(" ");
        List<BigDecimal> decimals = new ArrayList<BigDecimal>();
        for (int i = 0; i < parts.length; i++) {
            if (!TextUtils.isEmpty(parts[i])) {
                decimals.add(new BigDecimal(parts[i]));
            }
        }
        return decimals;
    }

	public static String joinDoubles(List<Double> list) {
		String joined = "";
		for (double item : list) {
			joined += item + " ";
		}
		return joined.trim();
	}

	public static String joinStrings(List<String> list) {
		String joined = "";
		for (String item : list) {
			joined += item + " ";
		}
		return joined.trim();
	}

	public static String joinWithComma(List<String> list) {
		String joined = "";
		for (String item : list) {
			joined += item + ", ";
		}
		joined = joined.trim();
		if (TextUtils.isEmpty(joined)) {
			return "";
		}
		return joined.substring(0, joined.length() - 1);
	}

	public static String joinLongs(List<Long> list) {
		String joined = "";
		if (list == null) {
			return joined;
		}
		for (long item : list) {
			joined += item + " ";
		}
		return joined.trim();
	}

    public static String joinBigDecimals(List<BigDecimal> list) {
        String joined = "";
        if (list == null) {
            return joined;
        }
        for (BigDecimal item : list) {
            joined += item + " ";
        }
        return joined.trim();
    }

	public static double[] doubleListToArray(List<Double> list) {
		double[] array = new double[list.size()];
		for (int i = 0; i < list.size(); i++) {
			array[i] = list.get(i);
		}
		return array;
	}

    public static double[] bigDecimalListToArray(List<BigDecimal> list) {
        double[] array = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i).doubleValue();
        }
        return array;
    }

	public static List<Double> doubleArrayToList(double[] array) {
		List<Double> list = new ArrayList<Double>();
		for (double item : array) {
			list.add(item);
		}
		return list;
	}

    public static List<BigDecimal> doubleArrayToBigDecimalList(double[] array) {
        List<BigDecimal> list = new ArrayList<BigDecimal>();
        for (double item : array) {
            list.add(new BigDecimal(item));
        }
        return list;
    }

	public static long[] longListToArray(List<Long> list) {
		long[] array = new long[list.size()];
		for (int i = 0; i < list.size(); i++) {
			array[i] = list.get(i);
		}
		return array;
	}

	public static List<Long> longArrayToList(long[] array) {
		List<Long> list = new ArrayList<Long>();
		for (long item : array) {
			list.add(item);
		}
		return list;
	}
}
