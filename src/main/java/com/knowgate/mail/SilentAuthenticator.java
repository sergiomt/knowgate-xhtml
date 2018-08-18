package com.knowgate.mail;

/**
 * © Copyright 2016 the original author.
 * This file is licensed under the Apache License version 2.0.
 * You may not use this file except in compliance with the license.
 * You may obtain a copy of the License at:
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.
 */

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

/**
 * @author Sergio Montoro Ten
 * @version 1.0
 */

public class SilentAuthenticator extends Authenticator {
  private PasswordAuthentication oPwdAuth;

  public SilentAuthenticator(String sUserName, String sAuthStr) {
    oPwdAuth = new PasswordAuthentication(sUserName, sAuthStr);
  }

  protected PasswordAuthentication getPasswordAuthentication() {
    return oPwdAuth;
  }
}
