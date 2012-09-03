/*
 *  Copyright 2001-2005 Stephen Colebourne
 *
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

import java.util.Locale;

/**
 * Provides time calculations for the month of the year component of time for
 * Persian chronologies.
 * 
 * @author Zubin Kavarana
 * @since 2.1
 */
final class PersianMonthOfYearDateTimeField extends BasicMonthOfYearDateTimeField {

    /**
     * Restricted constructor
     */
    PersianMonthOfYearDateTimeField(PersianChronology chronology) {
        super(chronology, 12);
    }

    // -----------------------------------------------------------------------
    @Override
    protected int convertText(String text, Locale locale) {
        // TODO - provide a Persian implementation for this method
        // return
        // GJLocaleSymbols.forLocale(locale).monthOfYearTextToValue(text);
        return super.convertText(text, locale);
    }

    // -----------------------------------------------------------------------
    @Override
    public String getAsShortText(int fieldValue, Locale locale) {
        // TODO - provide a Persian implementation for this method
        // return
        // GJLocaleSymbols.forLocale(locale).monthOfYearValueToShortText(fieldValue);
        return super.getAsShortText(fieldValue, locale);
    }

    // -----------------------------------------------------------------------
    @Override
    public String getAsText(int fieldValue, Locale locale) {
        // TODO - provide a Persian implementation for this method
        // return
        // GJLocaleSymbols.forLocale(locale).monthOfYearValueToText(fieldValue);
        return super.getAsText(fieldValue, locale);
    }

    // -----------------------------------------------------------------------
    @Override
    public int getMaximumShortTextLength(Locale locale) {
        // TODO - provide a Persian implementation for this method
        // return
        // GJLocaleSymbols.forLocale(locale).getMonthMaxShortTextLength();
        return super.getMaximumShortTextLength(locale);
    }

    // -----------------------------------------------------------------------
    @Override
    public int getMaximumTextLength(Locale locale) {
        // TODO - provide a Persian implementation for this method
        // return GJLocaleSymbols.forLocale(locale).getMonthMaxTextLength();
        return super.getMaximumTextLength(locale);
    }

}
