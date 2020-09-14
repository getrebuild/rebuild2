/*
Copyright (c) REBUILD <https://getrebuild.com/> and/or its owners. All rights reserved.

rebuild is dual-licensed under commercial and open source licenses (GPLv3).
See LICENSE and COMMERCIAL in the project root for license information.
*/

package com.rebuild.core.support;

import com.rebuild.TestSupport;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author devezhao zhaofang123@gmail.com
 * @since 2019/03/08
 */
public class SMSenderTest extends TestSupport {

    @Ignore
    @Test
    public void testSendSMS() {
        if (SMSender.availableSMS()) {
            SMSender.sendSMS("17187472172", "SMSenderTest#testSendSMS");
        }
    }

    @Ignore
    @Test
    public void testSendMail() {
        if (SMSender.availableMail()) {
            SMSender.sendMail("getrebuild@sina.com", "SMSenderTest#testSendMail", "test content");
        }
    }
}
