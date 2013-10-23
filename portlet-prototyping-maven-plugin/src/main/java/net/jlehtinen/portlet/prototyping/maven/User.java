/*
 * Copyright 2010 Johannes Lehtinen 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.jlehtinen.portlet.prototyping.maven;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * User configuration record for the user realm.
 */
public class User implements Principal {

	/** Username */
	protected String username;
	
	/** User password in plaintext */
	protected String password;
	
	/** User roles as a comma separated string */
	protected String roles;
	
	/** User roles as a set */
	protected Set<String> rolesSet;
	
	/** Mutex for accessing the roles set */
	protected final Object mutex = new Object();

	/**
	 * Constructs a new uninitialized instance.
	 */
	public User() {
	}
	
	/**
	 * Constructs and initializes a new instance.
	 * 
	 * @param username username
	 * @param password user password in plaintext
	 * @param roles user roles as a string
	 */
	public User(String username, String password, String roles) {
		this.username = username;
		this.password = password;
		this.roles = roles;
	}
	
	/**
	 * Returns the username.
	 * 
	 * @return username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Returns the plaintext password.
	 * 
	 * @return plaintext password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Returns the set of user roles.
	 * 
	 * @return set of user roles
	 */
	protected Set<String> getRolesSet() {
		synchronized (mutex) {
			if (rolesSet == null) {
				rolesSet = new HashSet<String>();
				if (roles != null && !roles.equals("")) {
					String[] parsedRoles = roles.split(",");
					for (int i = 0; i < parsedRoles.length; i++) {
						rolesSet.add(parsedRoles[i]);
					}
				}
			}
		}
		return rolesSet;
	}
	
	/**
	 * Returns whether this user has the specified role.
	 * 
	 * @param role role
	 * @return whether this user has the specified role
	 */
	public boolean hasRole(String role) {
		return getRolesSet().contains(role);
	}

	public String getName() {
		return "Anonymous User";
	}

	/**
	 * Validates the user entry and throws an exception if data is invalid.
	 * 
	 * @throws MojoExecutionException on invalid data
	 */
	public void validate() throws MojoExecutionException {
		if (username == null || username.equals("")) {
			throw new MojoExecutionException("name must be specified and non-empty");
		}
		if (password == null) {
			throw new MojoExecutionException("password must be specified");
		}
	}

	/**
	 * Checks user authentication using the specified credentials.
	 * 
	 * @param credentials authentication credentials
	 * @return true if authentication succeeds, false otherwise
	 */
	public boolean authenticate(Object credentials) {
		if (credentials != null && credentials instanceof String) {
			return credentials.equals(password);
		} else {
			return false;
		}
	}
	
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((username == null) ? 0 : username.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof User))
			return false;
		User other = (User) obj;
		if (username == null) {
			if (other.username != null)
				return false;
		} else if (!username.equals(other.username))
			return false;
		return true;
	}

	public String toString() {
		return username;
	}
}
