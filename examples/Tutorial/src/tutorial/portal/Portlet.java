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

import net.sf.tapestry.BaseComponent;
import net.sf.tapestry.IActionListener;
import net.sf.tapestry.IAsset;
import net.sf.tapestry.IBinding;
import net.sf.tapestry.IComponent;
import net.sf.tapestry.IMarkupWriter;
import net.sf.tapestry.IRequestCycle;
import net.sf.tapestry.RequestCycleException;
import net.sf.tapestry.components.Block;

/**
 *  A Portlet component knows how to render the frame around a portlet block,
 *  as well as manage the controls (close and minimize/maximize).
 *
 *  @author Howard Lewis Ship
 *  @version $Id$
 *
 **/

public class Portlet extends BaseComponent
{
    private IBinding modelBinding;
    private PortletModel model;

    public IBinding getModelBinding()
    {
        return modelBinding;
    }

    public void setModelBinding(IBinding value)
    {
        modelBinding = value;
    }

    public Object getModel()
    {
        return model;
    }

	// Simplify for new scheme
    protected void renderComponent(IMarkupWriter writer, IRequestCycle cycle) throws RequestCycleException
    {
        try
        {
            model = (PortletModel) modelBinding.getObject("model", PortletModel.class);

            super.renderComponent(writer, cycle);
        }
        finally
        {
            model = null;
        }
    }

    public IAsset getChangeStateImage()
    {
        return getAsset(model.isExpanded() ? "minimize" : "maximize");
    }

    public IAsset getChangeStateFocus()
    {
        return getAsset(model.isExpanded() ? "minimizeFocus" : "maximizeFocus");
    }

    public String getChangeStateLabel()
    {
        return model.isExpanded() ? "[Minimize]" : "[Maximize]";
    }

    public Block getBodyBlock()
    {
        if (model.isExpanded())
            return model.getBodyBlock(getPage().getRequestCycle());

        // If minimized, return null to prevent any display.

        return null;
    }

    private void changeState()
    {
        model.toggleExpanded();
    }

    public IActionListener getChangeStateListener()
    {
        return new IActionListener()
        {
            public void actionTriggered(IComponent component, IRequestCycle cycle) throws RequestCycleException
            {
                changeState();
            }
        };
    }
}