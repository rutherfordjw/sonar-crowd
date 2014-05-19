/*
 * Sonar Crowd Plugin
 * Copyright (C) 2009 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.plugins.crowd;

import com.atlassian.crowd.exception.ApplicationPermissionException;
import com.atlassian.crowd.exception.ExpiredCredentialException;
import com.atlassian.crowd.exception.InactiveAccountException;
import com.atlassian.crowd.exception.InvalidAuthenticationException;
import com.atlassian.crowd.exception.OperationFailedException;
import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.crowd.integration.rest.service.factory.RestCrowdClientFactory;
import com.atlassian.crowd.service.client.CrowdClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.sonar.api.security.LoginPasswordAuthenticator;

import java.util.Properties;

/**
 * @author Evgeny Mandrikov
 */
public class CrowdAuthenticator implements LoginPasswordAuthenticator {

  private static final Logger LOG = LoggerFactory.getLogger(CrowdAuthenticator.class);

  private final CrowdClient client;

  /**
   * Creates new instance of CrowdAuthenticator with specified configuration.
   *
   * @param configuration Crowd configuration
   */
  public CrowdAuthenticator(CrowdConfiguration configuration) {
    this.client = createCrowdClient(configuration);
  }

  private CrowdClient createCrowdClient(CrowdConfiguration configuration) {
    Properties props = configuration.getClientProperties();

    String crowdUrl = props.getProperty(CrowdConfiguration.KEY_CROWD_URL);
    String applicationName = props.getProperty(CrowdConfiguration.KEY_CROWD_APP_NAME);
    String applicationPassword = props.getProperty(CrowdConfiguration.KEY_CROWD_APP_PASSWORD);

    return new RestCrowdClientFactory().newInstance(crowdUrl, applicationName, applicationPassword);
  }

  @Override
  public void init() {
    // noop
  }

  @Override
  public boolean authenticate(String login, String password) {
    try {
      client.authenticateUser(login, password);
      return true;
    } catch (UserNotFoundException e) {
      LOG.debug("User {} not found", login);
      return false;
    } catch (InactiveAccountException e) {
      LOG.debug("User {} is not active", login);
      return false;
    } catch (ExpiredCredentialException e) {
      LOG.debug("Credentials of user {} have expired", login);
      return false;
    } catch (ApplicationPermissionException e) {
      LOG.error("Access to crowd has been denied for this application", e);
      return false;
    } catch (InvalidAuthenticationException e) {
      LOG.error("Invalid crowd credentials for this application", e);
      return false;
    } catch (OperationFailedException e) {
      LOG.error("Unable to authenticate user " + login, e);
      return false;
    }
  }
}
