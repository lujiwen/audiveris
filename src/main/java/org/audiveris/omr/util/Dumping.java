//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         D u m p i n g                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.util;

import org.audiveris.omr.util.Dumper.Column;
import org.audiveris.omr.util.Dumper.Html;
import org.audiveris.omr.util.Dumper.Row;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Class {@code Dumping} is a Dumping service, for which Package dependency can be
 * injected at construction time.
 *
 * @author Hervé Bitteur
 */
public class Dumping
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The relevance filter to be used */
    protected final Relevance relevance;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new Dumping service.
     *
     * @param rootPackages Collection of root packages used to filter which classes are relevant.
     */
    public Dumping (Collection<Package> rootPackages)
    {
        relevance = new PackageRelevance(rootPackages);
    }

    /**
     * Creates a new Dumping service.
     *
     * @param rootPackages 0, 1, or several root packages
     */
    public Dumping (Package... rootPackages)
    {
        relevance = new PackageRelevance(rootPackages);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // dump //
    //------//
    /**
     * Print the internal data of an object onto the standard output.
     *
     * @param obj the instance to dump
     */
    public void dump (Object obj)
    {
        dump(obj, null, 0);
    }

    //------//
    // dump //
    //------//
    /**
     * Print the internal data of an object onto the standard output, with a
     * specified left indentation level.
     *
     * @param obj   the instance to dump
     * @param level the indentation level (0 means no indentation)
     */
    public void dump (Object obj,
                      int level)
    {
        dump(obj, null, level);
    }

    //------//
    // dump //
    //------//
    /**
     * Print the internal data of an object onto the standard output, with the
     * ability to print a related title
     *
     * @param obj   the object to dump
     * @param title the title to print beforehand
     */
    public void dump (Object obj,
                      String title)
    {
        dump(obj, title, 0);
    }

    //------//
    // dump //
    //------//
    /**
     * Print the internal data of an object onto the standard output, with room
     * for a title and left indentation.
     *
     * @param obj   the object to dump
     * @param title the title to print beforehand
     * @param level the indentation level (0 for no indent)
     */
    public void dump (Object obj,
                      String title,
                      int level)
    {
        new Column(relevance, obj, title, level).print();
    }

    //--------//
    // dumpOf //
    //--------//
    /**
     * Return a line which contains the whole set of internal data
     *
     * @param obj the object whose data is to be printed
     *
     * @return the string of data values
     */
    public String dumpOf (Object obj)
    {
        return new Row(relevance, obj).toString();
    }

    //------------//
    // htmlDumpOf //
    //------------//
    /**
     * Return a special kind of information string, using HTML tags so that an
     * html editor can easily render this.
     *
     * @param obj the object to dump
     *
     * @return the HTML string
     */
    public String htmlDumpOf (Object obj)
    {
        return new Html(relevance, obj).toString();
    }

    //~ Inner Interfaces ---------------------------------------------------------------------------
    //-----------//
    // Relevance //
    //-----------//
    public static interface Relevance
    {
        //~ Methods --------------------------------------------------------------------------------

        /**
         * Predicate to determine if a given class is worth being printed.
         *
         * @param classe the class at stake
         * @return true if found relevant
         */
        boolean isClassRelevant (Class<?> classe);

        /**
         * Predicate to determine if a given field is worth being printed.
         *
         * @param field the field at stake
         * @return true if found relevant
         */
        boolean isFieldRelevant (Field field);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------------------//
    // PackageRelevance //
    //------------------//
    /**
     * A relevance filter, based on root packages
     */
    public static class PackageRelevance
            implements Relevance
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Collection of root packages, to filter non-relevant classes */
        protected final Set<Package> rootPackages = new LinkedHashSet<Package>();

        //~ Constructors ---------------------------------------------------------------------------
        public PackageRelevance (Collection<Package> rootPackages)
        {
            this.rootPackages.addAll(rootPackages);
        }

        public PackageRelevance (Package... rootPackages)
        {
            for (Package pkg : rootPackages) {
                this.rootPackages.add(pkg);
            }
        }

        //~ Methods --------------------------------------------------------------------------------
        //-----------------//
        // isClassRelevant //
        //-----------------//
        @Override
        public boolean isClassRelevant (Class<?> classe)
        {
            if (classe == null) {
                return false;
            }

            for (Package pkg : rootPackages) {
                if (classe.getName().startsWith(pkg.getName() + ".")) {
                    return true;
                }
            }

            return false;
        }

        //-----------------//
        // isFieldRelevant //
        //-----------------//
        @Override
        public boolean isFieldRelevant (Field field)
        {
            // We don't print static field since the Dumper is meant for instances
            if (Modifier.isStatic(field.getModifiers())) {
                return false;
            }

            // We don't print non-user visible entities
            if (field.getName().indexOf('$') != -1) {
                return false;
            }

            return true;
        }
    }
}
