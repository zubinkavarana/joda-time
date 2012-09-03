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
import org.joda.time.DateTimeUtils;
import org.joda.time.DateTimeZone;

/**
 * A <i>proleptic</i> implementation of the Persian calendar using Ahmad
 * Birashk's 2820 year cycle for it's leap year calculations. The calendar is
 * divided into periods of 2820 years. These periods are then divided into 88
 * cycles whose lengths follow <a
 * href="http://www.tondering.dk/claus/cal/persian.php">this</a> pattern:
 * 
 * 29, 33, 33, 33, 29, 33, 33, 33, 29, 33, 33, 33, ...
 * 
 * This gives 2816 years. The total of 2820 years is achieved by extending the
 * last cycle by 4 years (for a total of 37 years).
 * 
 * This calendar is not <a
 * href="http://aramis.obspm.fr/~heydari/divers/ir-cal-eng.html">accurate</a>
 * and fails as early as 1403 (2025 CE).</br></br> Thus the following points
 * should be taken into consideration when using this implementation of the
 * Persian calendar ~
 * <ul>
 * <li>The leap year cycle is accurate within the years 1244 AP (Anno Persico)
 * to 1402 AP</li>
 * <li>The max year supported by this implementation is 2134300219, and the min
 * is -621 AP.</li>
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
public class PersianChronologyBirashk extends PersianChronology {

    // see http://www.jostrans.org/issue17/art_darani.php
    private static final double AVERAGE_DAYS_PER_YEAR = 365.24219878;

    private static final String KEY = "AB";

    private static final double PERSIAN_EPOCH = 1948320.5;

    private static final long serialVersionUID = -5690829091404517045L;

    /**
     * 
     * Method Description: Returns an instance of a PersianChronologyBirashk in
     * the default JVM time zone
     * 
     * @return - PersianChronologyBirashk instance
     */
    public static PersianChronology getInstance() {
        return getInstance(DateTimeZone.getDefault(), new PersianChronologyBirashk());
    }

    /**
     * 
     * Method Description: Returns an instance of a PersianChronologyBirashk in
     * the given JVM time zone
     * 
     * @return - PersianChronologyBirashk instance
     */
    public static PersianChronology getInstance(DateTimeZone zone) {
        return getInstance(zone, new PersianChronologyBirashk());
    }

    /**
     * 
     * Method Description: Returns an instance of a PersianChronologyBirashk in
     * the UTC time zone
     * 
     * @return - PersianChronologyBirashk instance
     */
    public static PersianChronology getInstanceUTC() {
        return getInstance(DateTimeZone.UTC, new PersianChronologyBirashk());
    }

    PersianChronologyBirashk() {
        super(null, null);
    }

    PersianChronologyBirashk(Chronology base, Object param) {
        super(base, param);
    }

    @Override
    long calculateFirstDayOfYearMillis(int persianYear) {
        double julianDay = convertPersianDateToJulianDay(persianYear);
        return DateTimeUtils.julianDayToISODateTimeAtUTC(julianDay).getMillis();
    }

    private double convertPersianDateToJulianDay(int year) {
        int month = 1, day = 1;

        int epbase = year - ((year >= 0) ? 474 : 473);
        int epyear = 474 + mod(epbase, 2820);

        return day + ((month <= 7) ? ((month - 1) * 31) : (((month - 1) * 30) + 6)) + (((epyear * 682) - 110) / 2816)
                + (epyear - 1) * 365 + (epbase / 2820) * 1029983 + (PERSIAN_EPOCH - 1);
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
        return 4912282;
    }

    @Override
    int getMinYear() {
        return -621;
    }

    @Override
    protected boolean isLeapYear(int persianYear) {
        // 683 leap years every 2820 years starting AP 474
        return ((((((persianYear - ((persianYear > 0) ? 474 : 473)) % 2820) + 474) + 38) * 682) % 2816) < 682;
    }

    private int mod(int a, int b) {
        return a % b;
    }

    @Override
    protected PersianChronology newInstance(Chronology base, Object param) {
        return new PersianChronologyBirashk(base, param);
    }
}
