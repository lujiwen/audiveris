//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   H e a d C h o r d I n t e r                                  //
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sig.relation.ChordStemRelation;
import org.audiveris.omr.sig.relation.BasicContainment;
import org.audiveris.omr.sig.relation.FlagStemRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.util.Entities;
import org.audiveris.omr.util.HorizontalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code HeadChordInter} is a AbstractChordInter composed of heads and possibly
 * a stem.
 * <p>
 * Heads are linked via {@link BasicContainment} relation and stem via {@link ChordStemRelation}.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "head-chord")
public class HeadChordInter
        extends AbstractChordInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            HeadChordInter.class);

    /**
     * Compare two heads (assumed to be) of the same chord, ordered by
     * increasing distance from chord head ordinate.
     */
    public static final Comparator<HeadInter> headComparator = new Comparator<HeadInter>()
    {
        @Override
        public int compare (HeadInter n1,
                            HeadInter n2)
        {
            if (n1 == n2) {
                return 0;
            }

            AbstractChordInter c1 = n1.getChord();

            return c1.getStemDir() * (n1.getCenter().y - n2.getCenter().y);
        }
    };

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code HeadChordInter} object.
     *
     * @param grade the intrinsic grade
     */
    public HeadChordInter (double grade)
    {
        super(grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    protected HeadChordInter ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //----------//
    // contains //
    //----------//
    @Override
    public boolean contains (Point point)
    {
        final StemInter stem = getStem();

        if ((stem != null) && stem.contains(point)) {
            return true;
        }

        return super.contains(point);
    }

    //-----------//
    // duplicate //
    //-----------//
    /**
     * Make a clone of a chord (just its heads, not its stem or its beams).
     * <p>
     * This duplication is needed when a chord is shared by two BeamGroups.
     *
     * @param toBlack should we duplicate to black head? (for void head)
     * @return a clone of this chord (including heads, but stem and beams are not copied)
     */
    public HeadChordInter duplicate (boolean toBlack)
    {
        // Beams are not copied
        HeadChordInter clone = new HeadChordInter(getGrade());
        clone.setMirror(this);
        sig.addVertex(clone);
        setMirror(clone);

        clone.setStaff(staff);

        // Notes (we make a deep copy of each note head)
        for (Inter note : getMembers()) {
            HeadInter head = (HeadInter) note;
            AbstractNoteInter newHead = null;

            switch (head.getShape()) {
            case NOTEHEAD_BLACK:
                newHead = head.duplicate();

                break;

            case NOTEHEAD_VOID:
                newHead = toBlack ? head.duplicateAs(Shape.NOTEHEAD_BLACK) : head.duplicate();

                break;

            default:
                logger.error("No duplication supported for {}", note);

                break;
            }

            if (newHead != null) {
                clone.addMember(newHead);
            }
        }

        return clone;
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        if (bounds == null) {
            bounds = Entities.getBounds(getMembers()); // Based on heads

            final StemInter stem = getStem();

            if (stem != null) {
                if (bounds == null) {
                    bounds = stem.getBounds();
                } else {
                    bounds.add(getTailLocation());
                }
            }
        }

        return super.getBounds();
    }

    //------------//
    // getDetails //
    //------------//
    @Override
    public String getDetails ()
    {
        StringBuilder sb = new StringBuilder(super.getDetails());

        final StemInter stem = getStem();

        if (stem != null) {
            if (sb.length() > 0) {
                sb.append(' ');
            }

            sb.append("stem:").append(stem);
        }

        return sb.toString();
    }

    //----------------//
    // getFlagsNumber //
    //----------------//
    /**
     * Report the number of (individual) flags attached to the chord stem
     *
     * @return the number of individual flags
     */
    @Override
    public int getFlagsNumber ()
    {
        int count = 0;

        final StemInter stem = getStem();

        if ((stem != null) && !stem.isRemoved()) {
            final Set<Relation> rels = sig.getRelations(stem, FlagStemRelation.class);

            for (Relation rel : rels) {
                AbstractFlagInter flagInter = (AbstractFlagInter) sig.getOppositeInter(stem, rel);
                count += flagInter.getValue();
            }
        }

        return count;
    }

    //----------------//
    // getHeadsBounds //
    //----------------//
    /**
     * Report the bounding box of just the chord heads, without the stem if any.
     *
     * @return the heads bounding box
     */
    public Rectangle getHeadsBounds ()
    {
        return Entities.getBounds(getMembers());
    }

    //----------------//
    // getHeadsBounds //
    //----------------//
    /**
     * Report the bounding box of the heads located on desired side of the stem if any.
     *
     * @param stemSide desired side of the stem
     * @return the side heads bounding box, or null if none
     */
    public Rectangle getHeadsBounds (HorizontalSide stemSide)
    {
        final StemInter stem = getStem();

        if (stem == null) {
            return null;
        }

        final HorizontalSide headSide = stemSide.opposite();
        Rectangle rect = null;

        for (Relation rel : sig.getRelations(stem, HeadStemRelation.class)) {
            HeadStemRelation hsRel = (HeadStemRelation) rel;

            // Check side
            if (hsRel.getHeadSide() == headSide) {
                final Rectangle headBox = sig.getEdgeSource(rel).getBounds();

                if (rect == null) {
                    rect = headBox;
                } else {
                    rect.add(headBox);
                }
            }
        }

        return rect;
    }

    //----------------//
    // getLeadingNote //
    //----------------//
    /**
     * Report the note which if vertically farthest from stem tail.
     * For wholes and breves, it's the head itself.
     * For rest chords, it's the rest itself
     *
     * @return the leading note
     */
    @Override
    public HeadInter getLeadingNote ()
    {
        final List<Inter> notes = getMembers();

        if (!notes.isEmpty()) {
            final StemInter stem = getStem();

            if (stem != null) {
                // Find the note farthest from stem middle point
                Point middle = stem.getCenter();
                Inter bestNote = null;
                int bestDy = Integer.MIN_VALUE;

                for (Inter note : notes) {
                    int noteY = note.getCenter().y;
                    int dy = Math.abs(noteY - middle.y);

                    if (dy > bestDy) {
                        bestNote = note;
                        bestDy = dy;
                    }
                }

                return (HeadInter) bestNote;
            } else {
                return (HeadInter) notes.get(0);
            }
        } else {
            logger.warn("No notes in chord " + this);

            return null;
        }
    }

    //---------//
    // getStem //
    //---------//
    /**
     * Report the chord stem. It may be null temporarily such as when building the chord.
     *
     * @return the chord stem, if any
     */
    @Override
    public StemInter getStem ()
    {
        if (isRemoved()) {
            logger.debug("HeadChord#{} not in sig", id);

            return null;
        }

        for (Relation rel : sig.getRelations(this, ChordStemRelation.class)) {
            return (StemInter) sig.getOppositeInter(this, rel);
        }

        return null;
    }

    //------------//
    // getStemDir //
    //------------//
    /**
     * Report the stem direction of this chord, from head to tail
     *
     * @return -1 if stem is up, 0 if no stem, +1 if stem is down
     */
    @Override
    public int getStemDir ()
    {
        if (getStem() == null) {
            return 0;
        } else {
            return Integer.signum(getTailLocation().y - getHeadLocation().y);
        }
    }

    //---------//
    // setStem //
    //---------//
    /**
     * @param stem the stem to set
     */
    public final void setStem (StemInter stem)
    {
        Objects.requireNonNull(sig, "Chord not in SIG.");

        sig.addEdge(this, stem, new ChordStemRelation());
    }

    //-------------//
    // shapeString //
    //-------------//
    @Override
    public String shapeString ()
    {
        return "HeadChord";
    }

    //------------------//
    // computeLocations //
    //------------------//
    /**
     * Compute the head and tail locations for this chord.
     */
    @Override
    protected void computeLocations ()
    {
        AbstractNoteInter leading = getLeadingNote();

        if (leading == null) {
            return;
        }

        final StemInter stem = getStem();

        if (stem == null) {
            tailLocation = headLocation = leading.getCenter();
        } else {
            Rectangle stemBox = stem.getBounds();

            if (stem.getCenter().y < leading.getCenter().y) {
                // Stem is up
                tailLocation = new Point(stemBox.x + (stemBox.width / 2), stemBox.y);
            } else {
                // Stem is down
                tailLocation = new Point(
                        stemBox.x + (stemBox.width / 2),
                        ((stemBox.y + stemBox.height) - 1));
            }

            headLocation = new Point(tailLocation.x, leading.getCenter().y);
        }
    }
}
