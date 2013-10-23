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

import java.io.File;
import java.text.MessageFormat;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * Container for artifact identity information. Used for mojo configuration. Either Maven
 * artifact identifiers can be provided or a direct path to a local file.
 */
public class ArtifactIdentity {

	/** Group identifier */
	protected String groupId;
	
	/** Artifact identifier */
	protected String artifactId;

	/** Version number */
	protected String version;
	
	/** Packaging type */
	protected String packaging;
	
	/** Direct path to a local file containing the artifact */
	protected String file;

	/**
	 * Constructs a new uninitialized instance.
	 */
	public ArtifactIdentity() {
	}
	
	/**
	 * Constructs a new instance initialized from the specified identifiers.
	 * 
	 * @param groupId group identifier
	 * @param artifactId artifact identifier
	 * @param version version number
	 * @param type type of the artifact
	 */
	public ArtifactIdentity(String groupId, String artifactId, String version, String type) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.packaging = type;
	}
	
	/**
	 * Returns the group identifier.
	 * 
	 * @return group identifier
	 */
	public String getGroupId() {
		return groupId;
	}

	/**
	 * Returns the artifact identifier.
	 * 
	 * @return artifact identifier
	 */
	public String getArtifactId() {
		return artifactId;
	}

	/**
	 * Returns the version number.
	 * 
	 * @return version number
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Returns the packaging type.
	 * 
	 * @return packaging type
	 */
	public String getPackaging() {
		return packaging == null ? "jar" : packaging;
	}

	/**
	 * Returns a path to a local file.
	 * 
	 * @return path to a local file
	 */
	public String getFile() {
		return file;
	}

	/**
	 * Sets a path to a local file.
	 * 
	 * @param file path to a local file
	 */
	public void setFile(String file) {
		this.file = file;
	}

	/**
	 * Validates this artifact identity and throws an exception if it is incomplete or
	 * invalid.
	 * 
	 * @throws MojoExecutionException if identity is invalid
	 */
	public void validate() throws MojoExecutionException {
		if (file == null) {
			if (groupId == null) {
				throw new MojoExecutionException("groupId must be specified");
			}
			if (artifactId == null) {
				throw new MojoExecutionException("artifactId must be specified");
			}
			if (version == null) {
				throw new MojoExecutionException("version must be specified");
			}
		} else {
			if (!(new File(file).exists())) {
				throw new MojoExecutionException(MessageFormat.format("file {0} does not exist", new Object[] { file }));
			}
		}
	}

}
