/**
 * Copyright (C) 2014 Benoit Lacelle (benoit.lacelle@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package blasd.apex.shared.util;

import java.time.ZoneId;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

import blasd.apex.shared.logging.ApexLogHelper;

/**
 * Some utility methods relative to time, typically for pretty-printing of performance
 * 
 * @author Benoit Lacelle
 *
 */
public class ApexTimeHelper {

	protected static final Logger LOGGER = LoggerFactory.getLogger(ApexTimeHelper.class);

	/**
	 * How many times occurrences has to be captured before logging about an outlier?
	 */
	protected static final long NB_OCCURENCES_FOR_INFO = 1000;

	@VisibleForTesting
	public static final AtomicLong NB_LOG_FOR_OUTLIER = new AtomicLong();

	protected ApexTimeHelper() {
		// hidden
	}

	/**
	 * @deprecated use ApexLogHelper.getNiceTime
	 */
	@Deprecated
	public static Object getNiceTimeInTransaction(long timeInMs) {
		return ApexLogHelper.getNiceTime(timeInMs);
	}

	/**
	 * @deprecated use ApexLogHelper.getNiceRate
	 */
	@Deprecated
	public static String getNiceRate(long nbEntries, long time, TimeUnit timeUnit) {
		return ApexLogHelper.getNiceRate(nbEntries, time, timeUnit).toString();
	}

	/**
	 * @deprecated use ApexLogHelper.getNicePercentage
	 */
	@Deprecated
	public static Object getPercentage(long numerator, long denominator, int digits) {
		return ApexLogHelper.getNicePercentage(numerator, denominator);
	}

	/**
	 * This class enables logging of outlyer detection, typically when a small operation takes more time than usual, or
	 * a result size is bigger
	 * 
	 * @param clazz
	 * @param methodName
	 * @param nb
	 * @param max
	 * @param newValue
	 * @return true if maxTiming has been updated
	 */
	public static boolean updateOutlierDetectorStatistics(AtomicLong nb,
			AtomicLong max,
			long newValue,
			Object className,
			Object methodName,
			Object... more) {
		nb.incrementAndGet();

		// How long did this operation lasted
		long currentMax = max.get();
		if (newValue > currentMax) {
			// We encountered a new max time
			if (max.compareAndSet(currentMax, newValue)) {
				// We start logging only after encountered at least N operations, preventing to log too much during
				// start-up
				if (nb.get() > NB_OCCURENCES_FOR_INFO) {
					NB_LOG_FOR_OUTLIER.incrementAndGet();

					if (more != null && more.length > 0) {
						LOGGER.info("{}.{}.{} increased its max from {} to {}",
								className,
								methodName,
								Arrays.asList(more),
								currentMax,
								newValue);
					} else {
						LOGGER.info("{}.{} increased its max from {} to {}", className, methodName, newValue);
					}
				}
			} else {
				LOGGER.trace("Race-condition. We may have lost a max");
			}
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 
	 * @param localDateTime
	 * @return the number of milliseconds since 1970
	 */
	public static long jodaToTime(org.joda.time.LocalDateTime localDateTime) {
		return localDateTime.toDate().getTime();
	}

	/**
	 * 
	 * @param localDateTime
	 * @return the number of milliseconds since 1970
	 */
	public static long java8ToTime(java.time.LocalDateTime localDateTime) {
		return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}
}
