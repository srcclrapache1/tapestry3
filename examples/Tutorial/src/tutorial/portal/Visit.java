/*
 *  ====================================================================
 *  The Apache Software License, Version 1.1
 *
 *  Copyright (c) 2002 The Apache Software Foundation.  All rights
 *  reserved.
 *
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *
 *  1. Redistributions of source code must retain the above copyright
 *  notice, this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright
 *  notice, this list of conditions and the following disclaimer in
 *  the documentation and/or other materials provided with the
 *  distribution.
 *
 *  3. The end-user documentation included with the redistribution,
 *  if any, must include the following acknowledgment:
 *  "This product includes software developed by the
 *  Apache Software Foundation (http://www.apache.org/)."
 *  Alternately, this acknowledgment may appear in the software itself,
 *  if and wherever such third-party acknowledgments normally appear.
 *
 *  4. The names "Apache" and "Apache Software Foundation" and
 *  "Apache Tapestry" must not be used to endorse or promote products
 *  derived from this software without prior written permission. For
 *  written permission, please contact apache@apache.org.
 *
 *  5. Products derived from this software may not be called "Apache",
 *  "Apache Tapestry", nor may "Apache" appear in their name, without
 *  prior written permission of the Apache Software Foundation.
 *
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 *  ====================================================================
 *
 *  This software consists of voluntary contributions made by many
 *  individuals on behalf of the Apache Software Foundation.  For more
 *  information on the Apache Software Foundation, please see
 *  <http://www.apache.org/>.
 */
package tutorial.portal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sf.tapestry.ApplicationRuntimeException;
import net.sf.tapestry.form.IPropertySelectionModel;

/**
 *  Central object tracking the available Portlets and Channels.
 *
 *  @author Howard Lewis Ship
 *  @version $Id$
 *
 **/

public class Visit implements Serializable
{
    private static PortletChannel[] channels =
        {
            new PortletChannel(87, "Slashdot Headlines", "Slashdot", "content"),
            new PortletChannel(23, "Stock Quotes", "Stocks", "content"),
            new PortletChannel(373, "Weather", "Weather", "content")};

    private List models;

    public void removeModel(PortletModel model)
    {
        if (models != null)
            models.remove(model);
    }

    public Collection getModels()
    {
        return models;
    }

    public void addModel(int id)
    {
        for (int i = 0; i < channels.length; i++)
        {
            PortletChannel channel = channels[i];

            if (channel.getId() == id)
            {
                PortletModel model = channel.getModel();

                if (models == null)
                    models = new ArrayList();

                models.add(model);

                return;
            }
        }

        throw new ApplicationRuntimeException("No portlet channel with id " + id + ".");
    }

    /**
     *  Returns a portlet selection model that will produce a list
     *  of all portlets <em>not already in use</em>.  It will generate
     *  an integer property which is the id of they selected
     *  porlet, suitable for passing to {@link #addModel(int)}.
     *
     */

    public IPropertySelectionModel getPortletSelectionModel()
    {
        PortletSelectionModel model = new PortletSelectionModel();

        for (int i = 0; i < channels.length; i++)
        {
            PortletChannel channel = channels[i];

            if (!inUse(channel.getId()))
                model.add(channel);
        }

        return model;
    }

    private boolean inUse(int id)
    {
        if (models == null)
            return false;

        int count = models.size();
        for (int i = 0; i < count; i++)
        {
            PortletModel model = (PortletModel) models.get(i);

            if (model.getId() == id)
                return true;
        }

        return false;
    }

    /**
     *  Returns true if there are any additional portlets that the user has not
     *  already added.  If it returns false, then the UI should omit the
     *  form for adding a new portlet.
     *
     */

    public boolean getMaySelectPortlet()
    {
        if (models == null)
            return true;

        return models.size() < channels.length;
    }
}