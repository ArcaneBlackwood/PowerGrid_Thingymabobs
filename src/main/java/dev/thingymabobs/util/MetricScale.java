package dev.thingymabobs.util;

import java.util.Arrays;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import com.mojang.datafixers.util.Pair;
import dev.thingymabobs.Thingymabobs;
import dev.thingymabobs.config.properties.CProperties;
import net.neoforged.bus.api.IEventBus;

public enum MetricScale {
	NONE("", -0, -0),

	DECI("d", -1, -3, true),
	CENTI("cC", -2, -7, true),
	MILLI("m", -3, -10),
	MICRO("uUμ", -6, -20),
	NANO("nN", -9, -30),
	PICO("p", -12, -40),
	FEMTO("fF", -15, -50),
	ATTO("a", -18, -60),
	ZEPTO("z", -21, -70),

	DECA("DA", 1, 3, true),
	HECTO("hH", 2, 7, true),
	KILO("kK", 3, 10),
	MEGA("M", 6, 20),
	GIGA("Gg", 9, 30),
	TERA("Tt", 12, 40),
	PETA("P", 15, 50),
	EXA("Ee", 18, 60),
	ZETTA("Z", 21, 70);
    private static final MetricScale[] VALUES = values();
	private static final MetricScale[] VALUES_ORDERED = Arrays.stream(VALUES)
        .sorted((c1, c2) -> Integer.compare(c1.dec10, c2.dec10))
        .toArray(MetricScale[]::new);
    private static final int[] ORDINAL_TO_ORDERED_INDEX = buildOrdinalOrderMap();
	private static int[] buildOrdinalOrderMap() {
		int[] result = new int[VALUES_ORDERED.length];
		for (int i = 0; i < result.length; i++)
			result[VALUES_ORDERED[i].ordinal()] = i;
		return result;
	}

	public static void register(IEventBus modBus) {
		CProperties.register(Thingymabobs.asResource("config.metric_scale"), "metric_scale")
			.registerInt("unit_style", 0, 0, 1, 
				"Used for property fields in components, it specifies how metric numbers are formatted.  0 has the format '1k2', 1 has the format '1.2k'");
	}
    protected static CProperties.Prop CONFIG = null;
	protected static int UNIT_STYLE = 0;
    public static void configUpdated(CProperties.Prop prop) {
        CONFIG = prop;
		UNIT_STYLE = prop.getInt("unit_style").get();
	}
	

	private final String validChars;
	@SuppressWarnings("unused")
	private final int dec10, dec2;
	private double scale10, scale2;
	private boolean subUnit = false;
	private MetricScale(String validChars, int dec10, int dec2) {
		this.validChars = validChars;
		this.dec10 = dec10;
		this.dec2 = dec2;
		int scale2Int = 1 << Math.abs(dec2);
		scale2 = dec2 < 0 ? 1 / (double)scale2Int : scale2Int;
		int scale10Int = pow10(dec10);
		scale10 = dec10 < 0 ? 1 / (double)scale10Int : scale10Int;
	}
	private MetricScale(String validChars, int dec10, int dec2, boolean subUnit) {
		this.validChars = validChars;
		this.dec10 = dec10;
		this.dec2 = dec2;
		int scale2Int = 1 << Math.abs(dec2);
		scale2 = dec2 < 0 ? 1 / (double)scale2Int : scale2Int;
		int scale10Int = pow10(dec10);
		scale10 = dec10 < 0 ? 1 / (double)scale10Int : scale10Int;
		this.subUnit = subUnit;
	}

	public char getLetter() {
        if (validChars.length() == 0) return '\0';
		return validChars.charAt(0);
	}
	public double getScale() {
		return scale10;
	}
	public double getScaleBin() {
		return scale2;
	}
	public MetricScale getNext() {
		return getNext(false);
	}
	public MetricScale getNext(boolean useSubUnits) {
		int orderIndex = ORDINAL_TO_ORDERED_INDEX[ordinal()] + 1;
		if (!useSubUnits)
			while (orderIndex < VALUES.length && VALUES_ORDERED[orderIndex].subUnit)
				orderIndex++;
		return orderIndex >= VALUES.length ? null : VALUES_ORDERED[orderIndex];
	}
	public MetricScale getNextSubUnit() {
		int orderIndex = ORDINAL_TO_ORDERED_INDEX[ordinal()] + 1;
		return orderIndex >= VALUES.length ? null : VALUES_ORDERED[orderIndex];
	}
	public MetricScale getPrev() {
		return getPrev(false);
	}
	public MetricScale getPrev(boolean useSubUnits) {
		int orderIndex = ORDINAL_TO_ORDERED_INDEX[ordinal()] - 1;
		if (!useSubUnits)
			while (orderIndex > 0 && VALUES_ORDERED[orderIndex].subUnit)
				orderIndex--;
		return orderIndex <= -1 ? null : VALUES_ORDERED[orderIndex];
	}
	public MetricScale getPrevSubUnit() {
		int orderIndex = ORDINAL_TO_ORDERED_INDEX[ordinal()] - 1;
		return orderIndex <= -1 ? null : VALUES_ORDERED[orderIndex];
	}
	@Override
	public String toString() {
        if (validChars.length() == 0) return "";
		return validChars.substring(0, 1);
	}



	//public static float SCALE_WIGGLE_ROOM_LARGE = 10/11f;
	//public static float SCALE_WIGGLE_ROOM_SMALL = 12/10f;
	public static @NotNull MetricScale getScaleOf(double value) {
		return getScaleOf(value, false);
	}
	public static @NotNull MetricScale getScaleOf(double value, boolean useSubUnits) {
		if (value == 0) return NONE;
		if (value < 0) value = -value;
		//value *= value > 10 ? SCALE_WIGGLE_ROOM_LARGE : SCALE_WIGGLE_ROOM_SMALL;
        MetricScale prev = VALUES_ORDERED[0];
		for (MetricScale scale : VALUES_ORDERED) {
			if (!useSubUnits && scale.subUnit) continue;
			if (scale.scale10 > value) return prev;
            prev = scale;
		}
		return VALUES_ORDERED[VALUES_ORDERED.length-1];
	}
	public static MetricScale getScaleOf(double value, MetricScale prefer, int preferDigitRange) {
		if (withinScaleRange(value, prefer, preferDigitRange, false))
			return prefer;
		return getScaleOf(value, false);
	}
	public static MetricScale getScaleOf(double value, MetricScale prefer, int preferDigitRange, boolean useSubUnits) {
		if (withinScaleRange(value, prefer, preferDigitRange, useSubUnits))
			return prefer;
		return getScaleOf(value, useSubUnits);
	}
	public static @Nullable MetricScale getScaleOf(char c) {
		for (MetricScale scale : VALUES_ORDERED)
			if (scale.validChars.indexOf(c)!=-1) return scale;
		return null;
	}
	public static boolean withinScaleRange(double value, MetricScale scale, int digitRange) {
		return withinScaleRange(value, scale, digitRange, false);
	}
	public static boolean withinScaleRange(double value, MetricScale scale, int digitRange, boolean useSubUnits) {
		if (value < 0) value = -value;
		int range = pow10(digitRange);
		MetricScale prev = scale.getPrev(useSubUnits);
		if (prev != null) {
			double rangeLower = scale.scale10/range;
			if (value < rangeLower) return false;
		}
		MetricScale next = scale.getNext(useSubUnits);
		if (next != null) {
			double rangeUpper = scale.scale10*range;
			if (value > rangeUpper) return false;
		}
		return true;
	}


	public static final char[] NUMBERS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.', ' ', '_', '\'', '"', ','};
	public static Pair<Double, MetricScale> parse(String value) {
		return parse(value, false);
	}
	public static Pair<Double, MetricScale> parse(String value, boolean useSubUnits) {
		value = value.trim();
		int size = value.length();
		if (size < 1) return new Pair<>(null, null);;
		boolean isNegative = value.charAt(0) == '-';
		if (isNegative) {
			if (size < 2) return new Pair<>(null, null);
			size--;
			value = value.substring(1);
		}


		//Find decimal & unit
		int decimalIndex = -1, unitIndex = -1;
		boolean unitOnStart = true, unitOnEnd = true;
		for (int i = 0; i < size; i++) {
			char c = value.charAt(i);
			int digit = -1;
            for (int j=0; j<NUMBERS.length; j++) if (NUMBERS[j] == c) {
                digit = j;
                break;
            }
			if (c == '.') decimalIndex = decimalIndex < 0 ? i : -2;
			if (digit!=-1) {
				if (digit < 10 && unitIndex < 0) unitOnStart = false;
				if (digit < 10 && unitIndex > -1) unitOnEnd = false;
				continue;
			}
			if (unitIndex < 0) unitIndex = unitIndex == -1 ? i : -2;
		}
		if (decimalIndex == -2) {
			Thingymabobs.LOGGER.warn("MetricScale.parse failed.  Too many decimal points, expected just one in: '"+value+"'");
			return new Pair<>(null, null);
		}
		if (unitIndex == -2) {
			Thingymabobs.LOGGER.warn("MetricScale.parse failed.  Too many unit, expected just one in: '"+value+"'");
			return new Pair<>(null, null);
		}

		boolean hasDecimal = decimalIndex != -1;
		boolean hasUnit = unitIndex != -1;
		unitOnStart &= hasUnit;
		unitOnEnd &= hasUnit;

		//Get unit scale
		MetricScale scale = null;
		if (hasUnit) {
			scale = getScaleOf(value.charAt(unitIndex));
			if (scale == null) {
				Thingymabobs.LOGGER.warn("MetricScale.parse failed.  Unit value does not match any valid metric scale unit: '"+value+"' at "+unitIndex+": '"+value.charAt(unitIndex)+"'");
				return new Pair<>(null, null);
			}
		}

        //Handle if unit is used as decimal
        if (!hasDecimal && hasUnit) {
            decimalIndex = unitIndex;
            hasDecimal = true;
            hasUnit = false;
        }

		//Handle if has both decimal and unit
		if (hasDecimal && hasUnit) {
			if (!unitOnStart && !unitOnEnd) {
				Thingymabobs.LOGGER.warn("MetricScale.parse failed.  Unit must be on end or start if a decimal is used: '"+value+"'");
				return new Pair<>(null, null);
			}
			hasUnit = false;
		} else if (!hasDecimal && !hasUnit) {
            decimalIndex = size-1;
        }

		//Fetch number
		double number = 0, place = 1;
		for (int i = decimalIndex; i >= 0; i--) {
            char c = value.charAt(i);
			int digit = -1;
            for (int j=0; j<10; j++) if (NUMBERS[j] == c) {
                digit = j;
                break;
            }
			if (digit==-1 ) continue;
			number += digit * place;
			place *= 10;
		}
		place = 0.1;
        if (hasDecimal) for (int i = decimalIndex; i < size; i++) {
            char c = value.charAt(i);
			int digit = -1;
            for (int j=0; j<10; j++) if (NUMBERS[j] == c) {
                digit = j;
                break;
            }
			if (digit==-1) continue;
			number += digit * place;
			place /= 10;
		}
        
		//Apply or calculate scale
		if (scale != null) number *= scale.getScale();
		return new Pair<>(number, scale);
	}


	public static int FORMAT_DECIMAL_PLACES = 3;
	public static String format1K1(double value) {
		return format1K1(value, getScaleOf(value));
	}
	public static String format1K1(double value, MetricScale scale) {
		SpecialStringBuilder sb = new SpecialStringBuilder();
		char decimal = scale.getLetter();
		if (decimal == '\0') decimal = '.';
		doubleToString(sb, value, decimal, FORMAT_DECIMAL_PLACES,
			0, false, scale != NONE, false);
		return sb.toString();
	}
	public static String format1D1K(double value) {
		return format1D1K(value, getScaleOf(value));
	}
	public static String format1D1K(double value, MetricScale scale) {
		SpecialStringBuilder sb = new SpecialStringBuilder();
		doubleToString(sb, value, '.', FORMAT_DECIMAL_PLACES,
			0, false, true, false);
		char decimal = scale.getLetter();
		if (decimal != '\0') sb.append(scale.getLetter());
		return sb.toString();
	}
	public static String formatDefault(double value, MetricScale scale) {
		return UNIT_STYLE==0 ? format1K1(value, scale) : format1D1K(value, scale);
	}
	public static String format1D1K(double value, String unit, int precision) {
		SpecialStringBuilder sb = new SpecialStringBuilder();
		MetricScale scale = getScaleOf(value);
		doubleToString(sb, value / scale.getScale(), '.', FORMAT_DECIMAL_PLACES,
			0, false, true, false);
		char decimal = scale.getLetter();
		if (decimal != '\0') sb.append(scale.getLetter());
		sb.append(unit);
		return sb.toString();
	}
	


	private static int pow10(int x) {
		int y = 1;
		//Not super efficient, its fineee dont worry
		for (int i = 0, im = Math.abs(x); i < im; i++)
			y *= 10;
		return y;
	}
	public static void doubleToString(
		SpecialStringBuilder sb,
		double value,
		char decimalChar,
		int maxFraction,
		int minWhole,
		boolean alwaysMaxDecimals,
		boolean forceDecimal,
		boolean showPositiveSign
	) {
		StringDouble primitive = doubleToString(value, decimalChar, maxFraction);
		if (showPositiveSign || primitive.isNegative)
			sb.append(primitive.isNegative ? '-' : '+');
		if (primitive.wholeDigits < minWhole)
			sb.append(NUMBERS[0], minWhole - primitive.wholeDigits);
		sb.append(primitive.string);
		if (primitive.fractionDigits == 0 && (forceDecimal || alwaysMaxDecimals))
			sb.append(decimalChar);
		if (alwaysMaxDecimals && primitive.fractionDigits < maxFraction)
			sb.append(NUMBERS[0], maxFraction - primitive.fractionDigits);
	}
	public static StringDouble doubleToString(
		double value,
		char decimalChar,
		int maxFraction
	) {
		StringDouble result = new StringDouble();

		//Scale up real fractions to all whole and cast to long digits
		long digits = (long)(value * pow10(maxFraction+1));
		result.isNegative = digits < 0;
		digits = Math.abs(digits);

		//Round up smallest number
		if (digits % 10 > 5) digits += 10;
		digits /= 10;

		//If zero return
		if (digits == 0) {
			result.string = new byte[] {(byte)NUMBERS[0]};
			result.wholeDigits = 1;
			return result;
		}

		//Truncate starting zeros from decimal places & calculate decimal count
		int startingZeros = 0;
		for (; startingZeros < maxFraction && digits % 10 == 0; digits /= 10) {
			startingZeros++;
			if (digits < 10) return null;
		}
		result.fractionDigits = maxFraction - startingZeros;
		boolean hasFraction = result.fractionDigits != 0;

		//Calculate number of total digits
		long divisor = 1;
		int digitCount = 0;
        for (;digitCount<result.fractionDigits; digitCount++)
			divisor *= 10;
		for (; digitCount < 16 && divisor <= digits; digitCount++)
			divisor *= 10;
		if (digitCount == 17) return null;
        if (digitCount == result.fractionDigits) {
            result.wholeDigits = 1;
            digitCount++;
        } else
            result.wholeDigits = digitCount - result.fractionDigits;
		if (hasFraction) digitCount += 1;
        
		//Convert digits to string characters
		result.string = new byte[digitCount];
		for (int pos = digitCount-1, place = 0; place < 20 && pos >= 0; digits /= 10, place++) {
			if (hasFraction && place == result.fractionDigits)
				result.string[pos--] = (byte)decimalChar;
			result.string[pos--] = (byte)NUMBERS[(int) (digits % 10)];
		}
		return result;
	}
	public static class StringDouble {
		public byte[] string = null;
		public int wholeDigits = 0;
		public int fractionDigits = 0;
		public boolean isNegative;
	}
}
