/*
 * Joda Software License, Version 1.0
 *
 *
 * Copyright (c) 2001-03 Stephen Colebourne.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Joda project (http://www.joda.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The name "Joda" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact licence@joda.org.
 *
 * 5. Products derived from this software may not be called "Joda",
 *    nor may "Joda" appear in their name, without prior written
 *    permission of the Joda project.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE JODA AUTHORS OR THE PROJECT
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Joda project and was originally
 * created by Stephen Colebourne <scolebourne@joda.org>. For more
 * information on the Joda project, please see <http://www.joda.org/>.
 */
package org.joda.time;

import java.io.Serializable;
import java.util.Comparator;
import org.joda.time.convert.ConverterManager;

/**
 * DateTimeComparator is the standard implementation of the Comparator
 * interface for various Joda and Java objects. The following types
 * are supported for comparison:
 * <ul>
 * <li>ReadableInstant
 * <li>java.util.Date
 * <li>java.util.Calendar
 * <li>java.util.Long (milliseconds)
 * </ul>
 *
 * <p>
 * DateTimeComparator is thread-safe and immutable.
 *
 * @author Guy Allard
 * @author Stephen Colebourne
 * @author Brian S O'Neill
 * @since 1.0
 */
public class DateTimeComparator implements Comparator, Serializable {

    static final long serialVersionUID = -6097339773320178364L;

    private static final DateTimeComparator INSTANCE = new DateTimeComparator(null, null);

    /**
     * Returns a DateTimeComparator the compares the entire date time value.
     */
    public static DateTimeComparator getInstance() {
        return INSTANCE;
    }

    /**
     * Returns a DateTimeComparator with a lower limit only. Fields of a
     * magnitude less than the lower limit are excluded from comparisons.
     *
     * @param lowerLimit inclusive lower limit for fields to be compared
     */
    public static DateTimeComparator getInstance(DateTimeField lowerLimit) {
        return getInstance(lowerLimit, null);
    }

    /**
     * Returns a DateTimeComparator with a lower and upper limit. Fields of a
     * magnitude less than the lower limit are excluded from comparisons.
     * Fields of a magnitude greater than or equal to the upper limit are also
     * excluded from comparisons. Either limit may be specified as null, which
     * indicates an unbounded limit.
     *
     * @param lowerLimit optional, inclusive lower limit for fields to be compared
     * @param upperLimit optional, exclusive upper limit for fields to be compared
     */
    public static DateTimeComparator getInstance(DateTimeField lowerLimit, DateTimeField upperLimit) {
        if (lowerLimit == null && upperLimit == null) {
            return INSTANCE;
        }
        return new DateTimeComparator(lowerLimit, upperLimit);
    }

    /**
     * Returns a comparator that only considers date fields. Time of day is
     * ignored.
     */
    public static DateTimeComparator getDateOnlyInstance(Chronology chrono) {
        return getInstance(chrono.dayOfYear(), null);
    }

    /**
     * Returns a comparator that only considers time fields. Date is ignored.
     */
    public static DateTimeComparator getTimeOnlyInstance(Chronology chrono) {
        return getInstance(null, chrono.dayOfYear());
    }

    private final DateTimeField iLowerLimit;
    private final DateTimeField iUpperLimit;

    private DateTimeComparator(DateTimeField lowerLimit, DateTimeField upperLimit) {
        iLowerLimit = lowerLimit;
        iUpperLimit = upperLimit;
    }

    /**
     * @return null if no lower limit
     */
    public DateTimeField getLowerLimit() {
        return iLowerLimit;
    }

    /**
     * @return null if no upper limit
     */
    public DateTimeField getUpperLimit() {
        return iUpperLimit;
    }

    /**
     * Compare two objects against only the range of date time fields as
     * specified in the constructor.
     * 
     * @param lhsObj The first object, logically on the left of a &lt;
     * comparison
     * @param rhsObj The second object, logically on the right of a &lt;
     * comparison
     * @return zero if order does not matter, negative value if lhsObj &lt;
     * rhsObj, positive value otherwise.
     * @throws IllegalArgumentException if either argument is not supported
     */
    public int compare(Object lhsObj, Object rhsObj) {
        long lhsMillis, rhsMillis;

        if (lhsObj instanceof ReadableInstant) {
            ReadableInstant lhsInstant = (ReadableInstant) lhsObj;

            if (rhsObj instanceof ReadableInstant) {
                ReadableInstant rhsInstant = (ReadableInstant) rhsObj;

                // If instants are partial, then they can use each other to
                // fill in missing fields.
                lhsMillis = lhsInstant.getMillis(rhsInstant);
                rhsMillis = rhsInstant.getMillis(lhsInstant);
            } else {
                lhsMillis = lhsInstant.getMillis();
                rhsMillis = getMillisFromObject(rhsObj);
            }
        } else {
            lhsMillis = getMillisFromObject(lhsObj);

            if (rhsObj instanceof ReadableInstant) {
                rhsMillis = ((ReadableInstant) rhsObj).getMillis();
            } else {
                rhsMillis = getMillisFromObject(rhsObj);
            }
        }

        DateTimeField field;
        if ((field = iLowerLimit) != null) {
            lhsMillis = field.roundFloor(lhsMillis);
            rhsMillis = field.roundFloor(rhsMillis);
        }

        if ((field = iUpperLimit) != null) {
            lhsMillis = field.remainder(lhsMillis);
            rhsMillis = field.remainder(rhsMillis);
        }

        if (lhsMillis < rhsMillis) {
            return -1;
        } else if (lhsMillis > rhsMillis) {
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Support serialization singletons
     */
    private Object readResolve() {
        return getInstance(iLowerLimit, iUpperLimit);
    }

    /*
     * Developer's note: The 'equals' method specified by the interface is not
     * overridden here. It does not make sense to do so, since 'this' is a
     * DateTimeComparator, and 'that' would (presumably) be one of the
     * supported object types described elsewhere. The '==' logic provided by
     * java.lang.Object for a DateTimeComparator object suffices.
     */

    /*
     * @param obj
     * @return millis since the epoch
     */
    private static long getMillisFromObject(Object obj) {
        return ConverterManager.getInstance().getInstantConverter(obj).getInstantMillis(obj);
    }

}