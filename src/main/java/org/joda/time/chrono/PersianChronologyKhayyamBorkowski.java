/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.joda.time.chrono;

import org.joda.time.Chronology;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

/**
 * The algorithms used in this implementation are adapted from the paper titled
 * '<i>The Persian calendar for 3000 years</i>' by Kazimierz M. Borkowski and
 * published at <a
 * href="http://www.astro.uni.torun.pl/~kb/Papers/EMP/PersianC-EMP.htm"> Torun
 * Radio Astronomy Observatory</a>. The algorithm extends the
 * {@link PersianChronologyKhayyam} formula to accurately calculate leap years
 * up to 3177 Jalaali Year by using historical and future predicted vernal
 * equinox times. </br> </br> The Jalaali year starts on the instant of spring
 * in the Northern Hemisphere when the North and South poles of the Earth are
 * equidistant from the Sun (the Vernal equinox). Should the equinox occur after
 * 12pm, the year starts from the next day (and the previous year counts as a
 * leap).</br> </br> The effect of the observation character of the calendar on
 * equinox predictions, and on mathematical formulae that predict Jalaali leap
 * years is that these predictions/formulae loose their accuracy over a period
 * of time because of various astronomical reasons - for example
 * <ul>
 * <li>The Earth's revolution can be affected by various factors, like the
 * tsunami of 2004 that slightly shifted the earths axis</li>
 * <li>The rotation of the Earth is gradually slowing down, so after about 140
 * million years we will have days that are 25 hours long</li>
 * </ul>
 * </br> Thus the following points should be taken into consideration when using
 * this implementation of the Persian calendar ~
 * <ul>
 * <li>The leap year cycle is accurate within the years -61 AP (Anno Persico) to
 * 3177 AP</li>
 * <li>The break years have been determined by using algorithms by Jean Meeus to
 * predict Vernal equinox times at the </i>Tehran</i> Longitude. This means
 * certain years will not have the correct leap year <i>if</i> the Iran Standard
 * Time Longitude is used to declare a year leap by the Iran Govt.</li>
 * <li>The max year supported by this implementation is 3177, and the min is -61
 * (limited to it's leap accuracy).
 * <li>From a historical point of view, the calendar follows the current Iranian
 * Civil calendar from 1304 AP (1925 CE) onwards as this is when the Iranian
 * calendar law came in to effect. Before this, <a
 * href="http://www.farsiweb.ir/wiki/FAQ">the length of the Persian months were
 * different from their current lengths. For example, all years included at
 * least one 32-day month</a></li>
 * </ul>
 * 
 * @author Zubin Kavarana
 * @since 2.1
 * 
 */
public class PersianChronologyKhayyamBorkowski extends PersianChronology {

    private static final double AVERAGE_DAYS_PER_YEAR = 365.24219858156;

    private static final int[] BREAK_YEARS = { -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210, 1635, 2060, 2097, 2192,
            2262, 2324, 2394, 2456, 3178 };

    private static final String KEY = "KB";

    private static final long serialVersionUID = -5110915276696182916L;

    /**
     * 
     * Method Description: Returns an instance of a
     * PersianChronologyKhayyamBorkowski in the default JVM time zone
     * 
     * @return - PersianChronologyKhayyamBorkowski instance
     */
    public static PersianChronology getInstance() {
        return getInstance(DateTimeZone.getDefault(), new PersianChronologyKhayyamBorkowski());
    }

    /**
     * 
     * Method Description: Returns an instance of a
     * PersianChronologyKhayyamBorkowski in the given JVM time zone
     * 
     * @return - PersianChronologyKhayyamBorkowski instance
     */
    public static PersianChronology getInstance(DateTimeZone zone) {
        return getInstance(zone, new PersianChronologyKhayyamBorkowski());
    }

    /**
     * 
     * Method Description: Returns an instance of a
     * PersianChronologyKhayyamBorkowski in the UTC time zone
     * 
     * @return - PersianChronologyKhayyamBorkowski instance
     */
    public static PersianChronology getInstanceUTC() {
        return getInstance(DateTimeZone.UTC, new PersianChronologyKhayyamBorkowski());
    }

    PersianChronologyKhayyamBorkowski() {
        super(null, null);
    }

    PersianChronologyKhayyamBorkowski(Chronology base, Object param) {
        super(base, param);
    }

    private int[] breakYearCalculations(int persianYear, boolean calcJalaaliLeaps) {
        int jalaaliLeaps = -14;
        int jump = 0;
        int differenceToNextBreakYear = 0;
        int breakYear = BREAK_YEARS[0];

        for (int breakYearIndex = 1; breakYearIndex < BREAK_YEARS.length; breakYearIndex++) {
            int nextBreakYear = BREAK_YEARS[breakYearIndex];
            jump = nextBreakYear - breakYear;
            if (!(persianYear < nextBreakYear)) {
                if (calcJalaaliLeaps) {
                    jalaaliLeaps = jalaaliLeaps + jump / 33 * 8 + mod(jump, 33) / 4;
                }
                breakYear = nextBreakYear;
            } else {
                break;
            }
        }
        differenceToNextBreakYear = persianYear - breakYear;
        return new int[] { jump, differenceToNextBreakYear, jalaaliLeaps };
    }

    @Override
    long calculateFirstDayOfYearMillis(int persianYear) {

        int[] calResult = breakYearCalculations(persianYear, true);
        int jump = calResult[0];
        int differenceToNextBreakYear = calResult[1];
        int leapJ = calResult[2] + differenceToNextBreakYear / 33 * 8 + (mod(differenceToNextBreakYear, 33) + 3) / 4;
        if (mod(jump, 33) == 4 && jump - differenceToNextBreakYear == 4) {
            leapJ = leapJ + 1;
        }

        int isoYear = persianYear + 621;
        int gregorianLeaps = isoYear / 4 - (isoYear / 100 + 1) * 3 / 4 - 150;
        int dayInISOMarch = 20 + leapJ - gregorianLeaps;
        return new DateTime(isoYear, 3, dayInISOMarch, 0, 0, 0, 0, ISOChronology.getInstanceUTC()).getMillis();
    }

    @Override
    protected Double getAverageDaysPerYear() {
        return AVERAGE_DAYS_PER_YEAR;
    }

    @Override
    protected String getKey() {
        return KEY;
    }

    @Override
    int getMaxYear() {
        return 3177;
    }

    @Override
    int getMinYear() {
        return -61;
    }

    @Override
    protected boolean isLeapYear(int persianYear) {
        int[] calResult = breakYearCalculations(persianYear, false);
        int jump = calResult[0];
        int differenceToNextBreakYear = calResult[1];

        if (jump - differenceToNextBreakYear < 6) {
            differenceToNextBreakYear = differenceToNextBreakYear - jump + (jump + 4) / 33 * 33;
        }
        int numberOfYearsSinceLastLeap = mod(mod(differenceToNextBreakYear + 1, 33) - 1, 4);
        if (numberOfYearsSinceLastLeap == 0) {
            return true;
        } else {
            return false;
        }
    }

    private int mod(int a, int b) {
        return a % b;
    }

    @Override
    protected PersianChronology newInstance(Chronology base, Object param) {
        return new PersianChronologyKhayyamBorkowski(base, param);
    }
}
