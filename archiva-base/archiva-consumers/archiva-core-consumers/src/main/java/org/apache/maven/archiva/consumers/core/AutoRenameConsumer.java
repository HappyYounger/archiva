package org.apache.maven.archiva.consumers.core;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.archiva.consumers.AbstractMonitoredConsumer;
import org.apache.maven.archiva.consumers.ConsumerException;
import org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer;
import org.apache.maven.archiva.model.ArchivaRepository;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * AutoRenameConsumer
 *
 * @author <a href="mailto:joakime@apache.org">Joakim Erdfelt</a>
 * @version $Id$
 * @plexus.component role="org.apache.maven.archiva.consumers.KnownRepositoryContentConsumer"
 * role-hint="auto-rename"
 * instantiation-strategy="per-lookup"
 */
public class AutoRenameConsumer
    extends AbstractMonitoredConsumer
    implements KnownRepositoryContentConsumer
{
    /**
     * @plexus.configuration default-value="auto-rename"
     */
    private String id;

    /**
     * @plexus.configuration default-value="Automatically rename common artifact mistakes."
     */
    private String description;

    private static final String RENAME_FAILURE = "rename_failure";

    private File repositoryDir;

    private List<String> includes = new ArrayList<String>();

    private Map<String, String> extensionRenameMap = new HashMap<String, String>();

    public AutoRenameConsumer()
    {
        includes.add( "**/*.distribution-tgz" );
        includes.add( "**/*.distribution-zip" );
        includes.add( "**/*.plugin" );

        extensionRenameMap.put( ".distribution-tgz", ".tar.gz" );
        extensionRenameMap.put( ".distribution-zip", ".zip" );
        extensionRenameMap.put( ".plugin", ".jar" );
    }

    public String getId()
    {
        return this.id;
    }

    public String getDescription()
    {
        return this.description;
    }

    public boolean isPermanent()
    {
        return false;
    }

    public void beginScan( ArchivaRepository repository )
        throws ConsumerException
    {
        this.repositoryDir = new File( repository.getUrl().getPath() );
    }

    public void completeScan()
    {
        /* do nothing */
    }

    public List<String> getExcludes()
    {
        return null;
    }

    public List<String> getIncludes()
    {
        return includes;
    }

    public void processFile( String path )
        throws ConsumerException
    {
        File file = new File( this.repositoryDir, path );
        if ( file.exists() )
        {
            Iterator<String> itExtensions = this.extensionRenameMap.keySet().iterator();
            while ( itExtensions.hasNext() )
            {
                String extension = (String) itExtensions.next();
                if ( path.endsWith( extension ) )
                {
                    String fixedExtension = (String) this.extensionRenameMap.get( extension );
                    String correctedPath = path.substring( 0, path.length() - extension.length() ) + fixedExtension;
                    File to = new File( this.repositoryDir, correctedPath );
                    try
                    {
                        FileUtils.rename( file, to );
                    }
                    catch ( IOException e )
                    {
                        triggerConsumerWarning( RENAME_FAILURE, "Unable to rename " + path + " to " + correctedPath +
                            ": " + e.getMessage() );
                    }
                }
            }

            triggerConsumerInfo( "(Auto) Removing File: " + file.getAbsolutePath() );
            file.delete();
        }
    }
}
