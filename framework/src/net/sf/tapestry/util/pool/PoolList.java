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
package net.sf.tapestry.util.pool;

/**
 *  A wrapper around a list of objects for a given key in a {@link Pool}.
 *  The current implementation of this is FIFO.  This class is closely
 *  tied to {@link Pool}, which controls synchronization for it.
 *
 *  <p>This class, and {@link Pool}, were heavily revised in 1.0.5
 *  to support generational cleaning.  The PoolList acts like a first-in
 *  first-out queue and each pooled object is tagged with a "generation
 *  count", provided by the {@link Pool}. The generation count is
 *  incremented periodically.  This allows us to track, roughly,
 *  how often a pooled object has been accessed; unused objects will
 *  be buried with relatively low generation counts.
 *
 *  @author Howard Lewis Ship
 *  @version $Id$
 *
 **/

class PoolList
{
    /**
     *  Linked list of pooled objects.
     *
     * @since 1.0.5
     **/

    private Entry first;

    /**
     *  Linked list of "spare" Entries, ready to be re-used.
     *
     * @since 1.0.5
     **/

    private Entry spare;

    /**
     *  Overall count of items pooled.
     *
     **/

    private int count;

    /**
     * A simple linked-list entry for items stored in the PoolList.
     *
     * @since 1.0.5
     **/

    private static class Entry
    {
        int generation;
        Object pooled;
        Entry next;
    }

    /**
     *  Returns the number of pooled objects currently stored.
     *
     *  @since 1.0.5
     **/

    public int getPooledCount()
    {
        return count;
    }

    /**
     *  Returns an object previously stored into the list, or null if the list
     *  is empty.  The returned object is removed from the list.
     *
     **/

    public Object retrieve()
    {
        if (count == 0)
            return null;

        count--;

        Entry e = first;
        Object result = e.pooled;

        // Okay, store e into the list of spare entries.

        first = e.next;

        e.next = spare;
        spare = e;
        e.generation = 0;
        e.pooled = null;

        return result;
    }

    /**
     *  Adds the object to this PoolList.  An arbitrary number of objects can be
     *  stored.  The objects can later be retrieved using {@link #get()}.
     *  The list requires that generation never decrease.  On each subsequent
     *  invocation, it should be the same as, or greater, than the previous value.
     *
     *  @return The number of objects stored in the list (after adding the new object).
     **/

    public int store(int generation, Object object)
    {
        Entry e;

        if (spare == null)
        {
            e = new Entry();
        }
        else
        {
            e = spare;
            spare = spare.next;
        }

        e.generation = generation;
        e.pooled = object;
        e.next = first;
        first = e;

        return ++count;
    }

    /**
     *  Invoked to cleanup the list, freeing unneeded objects.
     *
     * @param generation pooled objects stored in this generation or
     * earlier are released.
     *
     * @since 1.0.5
     **/

    public int cleanup(int generation)
    {
        spare = null;

        count = 0;

        Entry prev = null;

        // Walk through the list.  They'll be sorted by generation.

        Entry e = first;
        while (true)
        {
            if (e == null)
                break;

            // If found a too-old entry then we want to
            // delete it.

            if (e.generation <= generation)
            {
                // Set the next pointer of the previous node to null.
                // If the very first node inspected was too old,
                // set the first pointer to null.

                if (prev == null)
                    first = null;
                else
                    prev.next = null;

                break;
            }

            prev = e;
            e = e.next;
            count++;
        }

        return count;
    }
    public String toString()
    {
        return "PoolList[" + count + "]";
    }
}