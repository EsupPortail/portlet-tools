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

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Container for library artifact identity information. Mostly exists to be able use proper
 * element name in configuration.
 */
public class Library extends ArtifactIdentity {
	
	/**
	 * Constructs a new uninitialized instance.
	 */
	public Library() {
		super();
	}
	
	/**
	 * Constructs a new instance and initializes it with the specified data.
	 * 
	 * @param groupId group identifier
	 * @param artifactId artifact identifier
	 * @param version version number
	 */
	public Library(String groupId, String artifactId, String version) {
		super(groupId, artifactId, version, "jar");
	}

	/**
	 * @see ArtifactIdentity#validate()
	 */
	public void validate() throws MojoExecutionException {
		super.validate();
		if (file == null) {
			if (!"jar".equals(getPackaging())) {
				throw new MojoExecutionException("type must be jar");
			}
		}
	}

}
