package com.asiainfo.datacenter.mail;

/**
 * Created by 董建斌 on 2018/9/26.
 */

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.TriggeringEventEvaluator;

public class MailEvaluator implements TriggeringEventEvaluator {

	@Override
	public boolean isTriggeringEvent(LoggingEvent loggingevent) {
		return loggingevent.getLevel().isGreaterOrEqual(Level.ERROR);
	}

}
